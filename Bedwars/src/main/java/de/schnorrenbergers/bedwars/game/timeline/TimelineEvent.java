package de.schnorrenbergers.bedwars.game.timeline;

/**
 * One thing that happens at a fixed point in the round, out of {@code timeline.yml}.
 * <p>
 * What an event <em>does</em> is one of a handful of known actions rather than free text, for the same
 * reason the upgrades work that way: a config that could describe arbitrary behaviour would need a
 * language of its own. What is free is when it happens and what it is called, which is what a map maker
 * actually wants to change.
 *
 * @param id          how it is referred to in the file
 * @param displayName what players are told it is called, MiniMessage
 * @param description what it means in plain words, shown when a player hovers the entry
 * @param seconds     how many seconds into the round it happens
 * @param action      what happens
 * @param generator   which kind of generator {@link Action#GENERATOR_TIER} raises
 * @param tier        the level it is raised to
 */
public record TimelineEvent(String id, String displayName, String description, int seconds,
                            TimelineEvent.Action action, String generator, int tier) {

    /**
     * @return whether anybody wrote down what this event means
     */
    public boolean hasDescription() {
        return description != null && !description.isBlank();
    }

    /**
     * What an event does when its time comes.
     */
    public enum Action {

        /** The generators in the middle move up a level. */
        GENERATOR_TIER,
        /** Every bed still standing falls. */
        BED_DESTRUCTION,
        /** A dragon per living team is let loose. */
        SUDDEN_DEATH,
        /** The round is over and the score decides. */
        GAME_END,
        /** Nothing but the announcement - for an event an addon listens to. */
        ANNOUNCE;

        /**
         * @param name what the config says
         * @return that action, {@link #ANNOUNCE} when it says something unknown, because an event nobody
         *         can read is better announced than silently dropped
         */
        public static Action byName(String name) {
            if (name == null) return ANNOUNCE;
            for (Action action : values()) {
                if (action.name().equalsIgnoreCase(name)) return action;
            }
            return ANNOUNCE;
        }
    }

    /**
     * @return whether this one is big enough to put on everybody's screen rather than only in chat
     */
    public boolean isMajor() {
        return action == Action.BED_DESTRUCTION || action == Action.SUDDEN_DEATH
                || action == Action.GAME_END;
    }
}
