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
        System.out.println("Chunk cost" + getChunkCost());
        if (b) {
            if (!MoneyHandler.removeMoney(getChunkCost(), player.getUniqueId())) {
                ClaimManager.unclaimChunk(chunk);
                return false;
            }
        }
        return b;
    }
    
    public int getChunkCost() {
        int teamChunkAmount = ClaimManager.getTeamChunkAmount(name);
        return high(1.1, teamChunkAmount);
    }
    
    private int high(double btm, int top) {
        BigDecimal result = new BigDecimal(btm);
        for (int i = 0; i < top; i++) {
            result.multiply(new BigDecimal(btm));
        }
        return result.intValue();
    }
}
