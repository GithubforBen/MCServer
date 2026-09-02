package de.hems.types.event;

import java.io.Serializable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * One attempt at a run event: who took part, when they started, what they have killed and how it ended.
 * <p>
 * The clock is what everything hangs on, so both ends are stored as timestamps rather than a duration -
 * a run that is still going can then be timed against the current moment without anybody updating it.
 */
public class RunData implements Serializable {

    private static final long serialVersionUID = 4310L;

    /** How a run ended. */
    public enum State implements Serializable {
        RUNNING("Läuft"),
        /** Nobody is playing it right now. The clock is stopped and the run can be picked up again. */
        PAUSED("Pausiert"),
        FINISHED("Geschafft"),
        FAILED("Gescheitert"),
        ABANDONED("Abgebrochen");

        private final String title;

        State(String title) {
            this.title = title;
        }

        public String getTitle() {
            return title;
        }
    }

    private UUID id;
    private UUID eventId;
    /** Everyone who ran, in the order they joined. The first is the one who opened the queue. */
    private Set<UUID> participants = new LinkedHashSet<>();
    /** The server this run was given, so it can be cleaned up when the run ends. */
    private String serverName;
    /** When the run was first started, kept for the record rather than for the clock. */
    private long startedAt;
    private long finishedAt;
    /**
     * The play time, counted in server ticks rather than wall clock.
     * <p>
     * A run can be left and picked up again, so the time it took is the time it was actually played - the
     * hours in between must not count. Ticks are what the run server can count exactly, and they measure
     * game time rather than real time, which is the fairer thing to race against.
     */
    private long elapsedTicks;
    /**
     * When the current stretch of play began, epoch millis, or {@code 0} while the run is paused.
     * <p>
     * Only used so a server that is not hosting the run can show a clock that moves between syncs. The
     * exact value always comes from the run server, which counts real ticks.
     */
    private long activeSince;
    /**
     * When the run last went quiet, so a run nobody ever comes back to can be told from one that was
     * paused a minute ago. Zero while somebody is playing it.
     */
    private long quietSince;
    private State state = State.RUNNING;
    /** Which objectives are done, kept as names so an unknown one cannot break deserialisation. */
    private Set<String> completed = new LinkedHashSet<>();
    /** How big the team was meant to be, so an undermanned run can be marked as such afterwards. */
    private int intendedTeamSize;

    public RunData() {
    }

    /**
     * @param eventId      the event being run
     * @param participants who is running
     */
    public RunData(UUID eventId, Set<UUID> participants) {
        this.id = UUID.randomUUID();
        this.eventId = eventId;
        this.participants = new LinkedHashSet<>(participants);
        this.startedAt = System.currentTimeMillis();
    }

    /**
     * @param objective the objective that was just killed
     * @return whether this was new
     */
    public boolean complete(UhcObjective objective) {
        return getCompleted().add(objective.name());
    }

    /**
     * @param objective the objective to check
     * @return whether it is already done
     */
    public boolean hasCompleted(UhcObjective objective) {
        return getCompleted().contains(objective.name());
    }

    /**
     * @param required everything this event asks for
     * @return whether the run has killed all of it
     */
    public boolean hasCompletedAll(List<UhcObjective> required) {
        for (UhcObjective objective : required) {
            if (!hasCompleted(objective)) return false;
        }
        return !required.isEmpty();
    }

    /**
     * @param required everything this event asks for
     * @return what is still missing
     */
    public List<UhcObjective> getRemaining(List<UhcObjective> required) {
        List<UhcObjective> remaining = new ArrayList<>();
        for (UhcObjective objective : required) {
            if (!hasCompleted(objective)) remaining.add(objective);
        }
        return remaining;
    }

    /** How many ticks the server aims to run per second. */
    public static final long TICKS_PER_SECOND = 20L;

    /**
     * How much play time the run has behind it.
     * <p>
     * While it is being played the last stretch is estimated from the clock, because only the run server
     * itself knows the true tick count between two syncs. The moment it syncs, the exact value wins again.
     *
     * @return the play time in ticks
     */
    public long getElapsedTicks() {
        if (activeSince <= 0) return elapsedTicks;
        long since = Math.max(0, System.currentTimeMillis() - activeSince);
        return elapsedTicks + (since * TICKS_PER_SECOND / 1000L);
    }

    /**
     * @return the play time, for display
     */
    public Duration getElapsed() {
        return Duration.ofMillis(getElapsedTicks() * 1000L / TICKS_PER_SECOND);
    }

    /**
     * Adds the ticks played since the last time this was called. Used by the run server, which is the only
     * place that counts them for real.
     *
     * @param ticks how many ticks were played
     */
    public void addTicks(long ticks) {
        if (ticks > 0) elapsedTicks += ticks;
    }

    /**
     * Stops the clock. The run stays open and can be picked up again.
     */
    public void pause() {
        if (getState() != State.RUNNING) return;
        activeSince = 0L;
        quietSince = System.currentTimeMillis();
        state = State.PAUSED;
    }

    /**
     * Starts the clock again.
     */
    public void resume() {
        if (getState() != State.PAUSED && getState() != State.RUNNING) return;
        activeSince = System.currentTimeMillis();
        quietSince = 0L;
        state = State.RUNNING;
    }

    /**
     * @return whether the run is open, whether or not anybody is playing it at this moment
     */
    public boolean isOpen() {
        return getState() == State.RUNNING || getState() == State.PAUSED;
    }

    /**
     * @return whether this run counts for the leaderboard
     */
    public boolean isRanked() {
        // the check is that it has an end at all, not that any time passed: a run that finishes inside a
        // single tick is still a finished run
        return getState() == State.FINISHED && finishedAt > 0;
    }

    /**
     * Closes the run.
     *
     * @param state how it ended
     */
    public void finish(State state) {
        this.state = state;
        this.activeSince = 0L;
        this.quietSince = 0L;
        this.finishedAt = System.currentTimeMillis();
    }

    /**
     * @return since when nobody has been playing this run, {@code 0} while somebody is
     */
    public long getQuietSince() {
        return quietSince;
    }

    public void setQuietSince(long quietSince) {
        this.quietSince = quietSince;
    }

    /**
     * How long this run has been lying around untouched.
     *
     * @param now the current time
     * @return the milliseconds it has been quiet, {@code 0} while it is being played
     */
    public long quietFor(long now) {
        return quietSince <= 0L ? 0L : Math.max(0L, now - quietSince);
    }

    /**
     * @return whether the team started smaller than it was supposed to be
     */
    public boolean isUndermanned() {
        return intendedTeamSize > 0 && getParticipants().size() < intendedTeamSize;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public Set<UUID> getParticipants() {
        if (participants == null) participants = new LinkedHashSet<>();
        return participants;
    }

    public void setParticipants(Set<UUID> participants) {
        this.participants = participants == null ? new LinkedHashSet<>() : participants;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public long getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(long startedAt) {
        this.startedAt = startedAt;
    }

    public long getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(long finishedAt) {
        this.finishedAt = finishedAt;
    }

    public long getElapsedTicksRaw() {
        return elapsedTicks;
    }

    public void setElapsedTicks(long elapsedTicks) {
        this.elapsedTicks = elapsedTicks;
    }

    public long getActiveSince() {
        return activeSince;
    }

    public void setActiveSince(long activeSince) {
        this.activeSince = activeSince;
    }

    public State getState() {
        return state == null ? State.RUNNING : state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public Set<String> getCompleted() {
        if (completed == null) completed = new LinkedHashSet<>();
        return completed;
    }

    public void setCompleted(Set<String> completed) {
        this.completed = completed == null ? new LinkedHashSet<>() : completed;
    }

    public int getIntendedTeamSize() {
        return intendedTeamSize;
    }

    public void setIntendedTeamSize(int intendedTeamSize) {
        this.intendedTeamSize = intendedTeamSize;
    }

    /**
     * Writes a run time the way a speedrun is normally read.
     *
     * @param duration how long it took
     * @return it as hours, minutes and seconds
     */
    public static String formatTime(Duration duration) {
        long seconds = Math.max(0, duration.getSeconds());
        return String.format("%02d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, seconds % 60);
    }

    /**
     * @param ticks a play time in ticks
     * @return it as hours, minutes and seconds
     */
    public static String formatTicks(long ticks) {
        return formatTime(Duration.ofMillis(Math.max(0, ticks) * 1000L / TICKS_PER_SECOND));
    }
}
