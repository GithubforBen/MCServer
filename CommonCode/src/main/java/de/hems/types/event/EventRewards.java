package de.hems.types.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The rewards of an event, read out of and written into its free settings.
 * <p>
 * Each reward is one {@link RewardRule} under {@code reward.<n>}, numbered from one in the order an admin
 * put them in. Events from before this used fixed slots - {@code prize.place.1} to {@code 3} and
 * {@code prize.participation} - and those are read as the rules they always meant, so nothing that was set
 * up the old way is lost. The first write through here turns them into the new form for good.
 * <p>
 * Handing out works the same for every kind of event: it turns its own record into {@link EventStanding}s,
 * and {@link #evaluate} says who gets what. Every rule that fits pays out.
 */
public final class EventRewards {

    /** The settings key prefix a rule is stored under. */
    public static final String KEY = "reward.";
    /** How many rules an event can carry, which is what fits on one panel. */
    public static final int MAX_RULES = 27;

    private EventRewards() {
    }

    /**
     * @param event the event to read
     * @return its rewards in order, with the old fixed prizes translated
     */
    public static List<RewardRule> of(EventData event) {
        List<RewardRule> rules = new ArrayList<>();
        for (int index = 1; index <= MAX_RULES; index++) {
            String text = event.getSetting(KEY + index, null);
            if (text == null) continue;
            RewardRule rule = RewardRule.parse(text);
            if (rule != null) rules.add(rule);
        }
        if (!rules.isEmpty()) return rules;
        return legacy(event);
    }

    /**
     * The prizes of an event set up before rewards were rules.
     *
     * @param event the event
     * @return the rules those prizes meant
     */
    private static List<RewardRule> legacy(EventData event) {
        List<RewardRule> rules = new ArrayList<>();
        for (int place = 1; place <= PrizeData.PLACES; place++) {
            PrizeData prize = PrizeData.ofPlace(event, place);
            if (!prize.isEmpty()) rules.add(RewardRule.place(place, prize));
        }
        PrizeData participation = PrizeData.ofParticipation(event);
        if (!participation.isEmpty()) rules.add(RewardRule.participation(participation));
        return rules;
    }

    /**
     * Replaces the rewards of an event.
     * <p>
     * Clears the old fixed prizes along the way. Leaving them would bring them back the moment the last
     * rule is deleted, which would look like the delete button not working.
     *
     * @param event the event to write to
     * @param rules its rewards, in order
     */
    public static void set(EventData event, List<RewardRule> rules) {
        Map<String, String> settings = event.getSettings();
        settings.keySet().removeIf(key -> key.startsWith(KEY)
                || key.startsWith(PrizeData.PLACE_KEY)
                || key.equals(PrizeData.PARTICIPATION_KEY));
        int index = 1;
        for (RewardRule rule : rules) {
            if (rule == null || index > MAX_RULES) continue;
            settings.put(KEY + index, rule.serialize());
            index++;
        }
    }

    /**
     * One reward somebody has earned.
     *
     * @param standing how they did
     * @param rule     the rule that rewards it
     */
    public record Earned(EventStanding standing, RewardRule rule) {

        /**
         * @return the placing to note on the award, or {@link AwardData#PARTICIPATION} when the reward was
         *         not for one
         */
        public int place() {
            return rule.getCondition() == RewardRule.Condition.PLACE && standing.isRanked()
                    ? standing.getPlace() : AwardData.PARTICIPATION;
        }

        /**
         * @return what the award is called in the message the player gets
         */
        public String title() {
            return switch (rule.getCondition()) {
                case PLACE -> standing.getPlace() + ". Platz";
                case KILLS -> standing.getKills() + " Kills";
                case PARTICIPATION -> "Teilnahme";
            };
        }
    }

    /**
     * Works out who gets what.
     *
     * @param event     the event whose rules apply
     * @param standings how everybody did
     * @return every reward that was earned, rules in their order and players in theirs within a rule
     */
    public static List<Earned> evaluate(EventData event, List<EventStanding> standings) {
        List<Earned> earned = new ArrayList<>();
        for (RewardRule rule : of(event)) {
            if (rule.getPrize().isEmpty()) continue;
            // a kill rule on an event that counts none would pay nobody, and quietly - skip it the same way
            if (rule.getCondition() == RewardRule.Condition.KILLS && !event.getType().countsKills()) continue;
            for (EventStanding standing : standings) {
                if (rule.matches(standing)) earned.add(new Earned(standing, rule));
            }
        }
        return earned;
    }
}
