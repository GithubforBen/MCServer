package de.hems.utils.event;

import de.hems.types.event.AwardData;
import de.hems.types.event.EventData;
import de.hems.types.event.EventRewards;
import de.hems.types.event.EventStanding;

import java.util.List;

/**
 * Puts the rewards of an event aside for the people who earned them.
 * <p>
 * The one place every kind of event pays out through. A race, a poker night and hunger games keep very
 * different records, but each of them turns its record into {@link EventStanding}s and hands them in here,
 * so a reward rule means the same thing whichever event it is on.
 */
public final class RewardPayout {

    private RewardPayout() {
    }

    /**
     * @param awards    where rewards wait until they are collected
     * @param event     the event whose rules apply
     * @param standings how everybody did
     * @return how many rewards were put aside
     */
    public static int pay(AwardStore awards, EventData event, List<EventStanding> standings) {
        int paid = 0;
        for (EventRewards.Earned earned : EventRewards.evaluate(event, standings)) {
            awards.put(new AwardData(earned.standing().getPlayer(), event, earned));
            paid++;
        }
        return paid;
    }
}
