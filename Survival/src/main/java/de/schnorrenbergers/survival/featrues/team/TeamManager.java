package de.schnorrenbergers.survival.featrues.team;

import de.schnorrenbergers.survival.featrues.money.MoneyHandler;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;

import java.math.BigDecimal;

public class TeamManager {
    private String name;

    public TeamManager(String name) {
        this.name = name;
    }

    public boolean claimChunk(Chunk chunk, Player player) {
        if (MoneyHandler.getMoney(player.getUniqueId()) < getChunkCost()) {
            return false;
        }
        boolean b = ClaimManager.claimChunk(chunk, name);
        if (b) {
            if (!MoneyHandler.removeMoney(getChunkCost(), player.getUniqueId())) {
                ClaimManager.unclaimChunk(chunk);
                return false;
            }
        }
        player.sendMessage("You have claimed this chunk for " +getChunkCost() +"!");
        return b;
    }
    
    public int getChunkCost() {
        int teamChunkAmount = ClaimManager.getTeamChunkAmount(name);
        int i = 50 * high(1.1, teamChunkAmount);
        if (i > 1000) i = 1000;
        if (i < 0 ) return 1000;
        return i;
    }
    
    private int high(double btm, int top) {
        BigDecimal result = new BigDecimal(btm);
        for (int i = 0; i < top; i++) {
            result = result.multiply(new BigDecimal(btm));
        }
        return result.intValue();
    }
}
