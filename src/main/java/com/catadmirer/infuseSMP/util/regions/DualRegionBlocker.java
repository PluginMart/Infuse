package com.catadmirer.infuseSMP.util.regions;

import java.util.Set;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import com.catadmirer.infuseSMP.effects.InfuseEffect;

public class DualRegionBlocker extends RegionBlocker {
    private final BasicRegionBlocker basic = new BasicRegionBlocker();
    private final WorldGuardRegionBlocker wg = new WorldGuardRegionBlocker();

    @Override
    public void init() {
        basic.init();
        wg.init();
    }

    @Override
    public boolean canUseSpark(Player player) {
        // If default state, check basic
        if (wg.canUseSpark(player)) {
            return basic.canUseSpark(player);
        }

        return false;
    }

    @Override
    public boolean canBeTargetedBySpark(Entity entity) {
        // If default state, check basic
        if (wg.canBeTargetedBySpark(entity)) {
            return basic.canBeTargetedBySpark(entity);
        }

        return false;
    }

    @Override
    public boolean canBeTargetedBySpark(Player player) {
        // If default state, check basic
        if (wg.canBeTargetedBySpark(player)) {
            return basic.canBeTargetedBySpark(player);
        }

        return false;
    }

    @Override
    public Set<InfuseEffect> getBlockedEffects(Entity entity) {
        Set<InfuseEffect> blocked = wg.getBlockedEffects(entity);
        if (!blocked.isEmpty()) return blocked;

        return basic.getBlockedEffects(entity);
    }

    @Override
    public Set<InfuseEffect> getBlockedEffects(Player player) {
        Set<InfuseEffect> blocked = wg.getBlockedEffects(player);
        if (!blocked.isEmpty()) return blocked;

        return basic.getBlockedEffects(player);
    }

    @Override
    public Set<InfuseEffect> getBlockedEffects(Location loc) {
        Set<InfuseEffect> blocked = wg.getBlockedEffects(loc);
        if (!blocked.isEmpty()) return blocked;

        return basic.getBlockedEffects(loc);
    }

    @Override
    public boolean isEffectBlocked(Entity entity, InfuseEffect effect) {
        // If default state, check basic
        if (!wg.isEffectBlocked(entity, effect)) {
            return basic.isEffectBlocked(entity, effect);
        }

        return true;
    }

    @Override
    public boolean isEffectBlocked(Player player, InfuseEffect effect) {
        // If default state, check basic
        if (!wg.isEffectBlocked(player, effect)) {
            return basic.isEffectBlocked(player, effect);
        }

        return true;
    }

    @Override
    public boolean isEffectBlocked(Location loc, InfuseEffect effect) {
        // If default state, check basic
        if (!wg.isEffectBlocked(loc, effect)) {
            return basic.isEffectBlocked(loc, effect);
        }

        return true;
    }
}
