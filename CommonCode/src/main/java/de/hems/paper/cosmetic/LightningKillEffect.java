package de.hems.paper.cosmetic;

import de.hems.types.cosmetic.Cosmetics;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;

/**
 * A bolt of lightning where the loser stood.
 * <p>
 * {@code strikeLightningEffect} rather than {@code strikeLightning}: the second one is weather and sets
 * fire to things, kills whoever is standing there and charges creepers. This one is the picture and the
 * thunder and nothing else, which is the whole difference between a cosmetic and an item.
 */
public class LightningKillEffect implements KillEffect {

    @Override
    public String getId() {
        return Cosmetics.KILL_LIGHTNING;
    }

    @Override
    public void play(KillContext context) {
        World world = context.world();
        if (world == null) return;
        Location at = context.where();
        world.strikeLightningEffect(at);
        // quieter than the real thing, and as ambient: a kill effect that drowns out the fight the
        // killer is still in is a kill effect they switch off again
        world.playSound(at, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.AMBIENT, 0.5f, 1.2f);
    }
}
