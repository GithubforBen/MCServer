package de.hems.events;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.round.DeleteRoundEvent;
import de.hems.communication.events.round.RequestRoundsEvent;
import de.hems.communication.events.round.RespondRoundSaveEvent;
import de.hems.communication.events.round.RespondRoundsEvent;
import de.hems.communication.events.round.RoundPolicyUpdatedEvent;
import de.hems.communication.events.round.RoundUpdatedEvent;
import de.hems.communication.events.round.SaveRoundEvent;
import de.hems.communication.events.round.SaveRoundPolicyEvent;
import de.hems.types.round.RoundData;
import de.hems.types.round.RoundPolicy;
import de.hems.types.round.RoundState;
import de.hems.utils.round.RoundStore;
import de.hems.utils.server.ServerHandler;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Serves the self started rounds to the network and keeps the list honest.
 * <p>
 * The second half matters more than it sounds. A round is an entry that points at a server, and a server
 * can go away without asking anybody - it crashes, or the idle watchdog switches it off once the last
 * player has left. An entry that still points at it would keep a slot occupied against the per player
 * limit and would offer players a round to join that is not there any more, so the list is walked
 * regularly and anything without a server behind it is closed.
 */
public class RoundEvents {

    /** How often the rounds are checked against the servers that actually run. */
    private static final long SWEEP_INTERVAL_SECONDS = 60L;
    /** How long a finished round stays in the list before it is dropped. */
    private static final long KEEP_ENDED_MS = 5L * 60L * 1000L;
    /** How long a round may wait for its server to come up. */
    private static final long STARTUP_GRACE_MS = 5L * 60L * 1000L;

    private final RoundStore rounds;
    private final ServerHandler servers;
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(runnable, "round-sweeper");
                thread.setDaemon(true);
                return thread;
            });

    public RoundEvents(RoundStore rounds, ServerHandler servers) {
        this.rounds = rounds;
        this.servers = servers;
        ListenerAdapter.register(RequestRoundsEvent.class, event -> onRequest((RequestRoundsEvent) event));
        ListenerAdapter.register(SaveRoundEvent.class, event -> onSave((SaveRoundEvent) event));
        ListenerAdapter.register(DeleteRoundEvent.class, event -> onDelete((DeleteRoundEvent) event));
        ListenerAdapter.register(SaveRoundPolicyEvent.class, event -> onPolicy((SaveRoundPolicyEvent) event));
        scheduler.scheduleWithFixedDelay(this::sweep,
                SWEEP_INTERVAL_SECONDS, SWEEP_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    public void stop() {
        scheduler.shutdownNow();
    }

    private void onRequest(RequestRoundsEvent request) throws Exception {
        ListenerAdapter.sendListeners(new RespondRoundsEvent(
                request.getSender(), rounds.snapshot(), request.getEventId()));
    }

    private void onSave(SaveRoundEvent request) throws Exception {
        RoundData round = request.getRound();
        if (round == null || round.getId() == null) {
            ListenerAdapter.sendListeners(new RespondRoundSaveEvent(
                    request.getSender(), false, "Die Runde hat keine Kennung.", null, request.getEventId()));
            return;
        }
        RoundData stored = rounds.put(round);
        ListenerAdapter.sendListeners(new RespondRoundSaveEvent(
                request.getSender(), stored != null, null, stored, request.getEventId()));
        if (stored != null) announce(stored.getId(), stored);
    }

    private void onDelete(DeleteRoundEvent request) throws Exception {
        if (rounds.delete(request.getRoundId())) announce(request.getRoundId(), null);
    }

    private void onPolicy(SaveRoundPolicyEvent request) throws Exception {
        RoundPolicy policy = request.getPolicy();
        if (policy == null) return;
        rounds.setPolicy(policy);
        ListenerAdapter.sendListeners(new RoundPolicyUpdatedEvent(rounds.getPolicy().copy()));
        System.out.println("Self started rounds are now " + (policy.isSelfStartEnabled() ? "allowed" : "off")
                + " (max " + policy.getMaxPerPlayer() + " per player, " + policy.getMaxRounds() + " in total)");
    }

    private void announce(java.util.UUID id, RoundData round) {
        try {
            ListenerAdapter.sendListeners(new RoundUpdatedEvent(id, round));
        } catch (Exception e) {
            System.out.println("Could not announce the change to round " + id + ": " + e.getMessage());
        }
    }

    /**
     * Closes rounds whose server is gone and drops the ones that have been closed long enough.
     */
    private void sweep() {
        try {
            long now = System.currentTimeMillis();
            for (RoundData round : rounds.getRounds()) {
                if (round.getState() == RoundState.ENDED) {
                    if (now - Math.max(round.getEndedAt(), round.getCreatedAt()) < KEEP_ENDED_MS) continue;
                    if (rounds.delete(round.getId())) announce(round.getId(), null);
                    continue;
                }
                if (round.getServerName() == null) {
                    // never got as far as being named: only a crash between creating and starting does this
                    if (now - round.getCreatedAt() < STARTUP_GRACE_MS) continue;
                    if (rounds.delete(round.getId())) announce(round.getId(), null);
                    continue;
                }
                if (isRunning(round.getServerName())) continue;
                // a server that is still coming up is not a server that is gone
                if (now - round.getCreatedAt() < STARTUP_GRACE_MS
                        && round.getState() == RoundState.PREPARING) continue;
                RoundData ended = round.copy();
                ended.setState(RoundState.ENDED);
                rounds.put(ended);
                announce(ended.getId(), ended);
            }
        } catch (Exception e) {
            System.out.println("Could not tidy up the rounds: " + e.getMessage());
        }
    }

    private boolean isRunning(String serverName) {
        servers.updateInstances();
        return servers.doesInstanceExist(ListenerAdapter.ServerName.valueOf(serverName));
    }

    public RoundStore getRounds() {
        return rounds;
    }
}
