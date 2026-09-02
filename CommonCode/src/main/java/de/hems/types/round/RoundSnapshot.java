package de.hems.types.round;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Everything the launcher knows about self started rounds, in one answer: the rounds themselves and the
 * rules they are started under.
 * <p>
 * One answer rather than two, because a lobby that has the rounds but not the rules cannot decide anything
 * and would only ask again.
 */
public class RoundSnapshot implements Serializable {

    private static final long serialVersionUID = 4703L;

    private ArrayList<RoundData> rounds = new ArrayList<>();
    private RoundPolicy policy = new RoundPolicy();

    public RoundSnapshot() {
    }

    public RoundSnapshot(ArrayList<RoundData> rounds, RoundPolicy policy) {
        this.rounds = rounds == null ? new ArrayList<>() : rounds;
        this.policy = policy == null ? new RoundPolicy() : policy;
    }

    public List<RoundData> getRounds() {
        return rounds == null ? List.of() : rounds;
    }

    public RoundPolicy getPolicy() {
        return policy == null ? new RoundPolicy() : policy;
    }
}
