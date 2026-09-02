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
    private ArrayList<String> maps = new ArrayList<>();

    public RoundSnapshot() {
    }

    public RoundSnapshot(ArrayList<RoundData> rounds, RoundPolicy policy) {
        this(rounds, policy, null);
    }

    public RoundSnapshot(ArrayList<RoundData> rounds, RoundPolicy policy, ArrayList<String> maps) {
        this.rounds = rounds == null ? new ArrayList<>() : rounds;
        this.policy = policy == null ? new RoundPolicy() : policy;
        this.maps = maps == null ? new ArrayList<>() : maps;
    }

    public List<RoundData> getRounds() {
        return rounds == null ? List.of() : rounds;
    }

    public RoundPolicy getPolicy() {
        return policy == null ? new RoundPolicy() : policy;
    }

    /**
     * The maps a round server will actually have.
     * <p>
     * Answered by the launcher rather than worked out from the blueprint, because the blueprint only knows
     * the maps that were shipped with a release - and a map somebody dropped into {@code ./bedwars-maps}
     * is exactly the one that is not in any release.
     *
     * @return the map ids, empty when the launcher is too old to say
     */
    public List<String> getMaps() {
        return maps == null ? List.of() : maps;
    }
}
