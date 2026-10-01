import de.schnorrenbergers.bedwars.game.*;
import java.lang.reflect.Method;
import java.util.*;

public class BalanceProbe {
    static Method even, opponent;
    public static void main(String[] a) throws Exception {
        even = TeamBalancer.class.getDeclaredMethod("even", List.class, int.class, Set.class); even.setAccessible(true);
        opponent = TeamBalancer.class.getDeclaredMethod("opponent", List.class, int.class); opponent.setAccessible(true);
        Class<?> color = Class.forName("de.schnorrenbergers.bedwars.game.TeamColor");
        Object[] colors = color.getEnumConstants();
        // 4x2, two friends pick RED, one random player
        run("zwei waehlen dasselbe Team, einer zufaellig", colors, 4, 2, new int[]{2,0,0,0}, new boolean[]{false,false}, new int[]{0,0,0,0, 1});
        // 4x2, both pick RED, nobody else
        run("alle im selben Team", colors, 4, 2, new int[]{2,0,0,0}, new boolean[]{false,false}, null);
        // 2x4: 3 auto-assigned all landed in one team (simulated), should even out
        run("drei automatisch in einem Team", colors, 2, 4, new int[]{3,0}, new boolean[]{true,true,true}, null);
    }
    static void run(String name, Object[] colors, int count, int size, int[] start, boolean[] movable, int[] extra) throws Exception {
        List<GameTeam> teams = new ArrayList<>();
        for (int i = 0; i < count; i++) teams.add(new GameTeam((de.schnorrenbergers.bedwars.game.TeamColor) colors[i]));
        Set<GamePlayer> mov = new HashSet<>();
        int n = 0, k = 0;
        for (int t = 0; t < count; t++) for (int j = 0; j < start[t]; j++) {
            GamePlayer p = new GamePlayer(UUID.randomUUID(), "p" + n++);
            teams.get(t).add(p);
            if (k < movable.length && movable[k++]) mov.add(p);
        }
        if (extra != null) { GamePlayer p = new GamePlayer(UUID.randomUUID(), "auto"); teams.get(1).add(p); mov.add(p); }
        even.invoke(null, teams, size, mov);
        opponent.invoke(null, teams, size);
        StringBuilder sb = new StringBuilder(name + ": ");
        for (GameTeam t : teams) sb.append(t.size()).append(' ');
        System.out.println(sb);
    }
}
