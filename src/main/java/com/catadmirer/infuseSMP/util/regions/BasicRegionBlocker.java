package com.catadmirer.infuseSMP.util.regions;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import com.catadmirer.infuseSMP.Infuse;
import com.catadmirer.infuseSMP.effects.InfuseEffect;

public class BasicRegionBlocker extends RegionBlocker {
    @Override
    public void init() {}
    
    @Override
    public boolean canUseSpark(Player player) {
        return true;
    }

    @Override
    public boolean canBeTargetedBySpark(Entity entity) {
        return true;
    }

    @Override
    public boolean canBeTargetedBySpark(Player player) {
        return true;
    }

    @Override
    public Set<InfuseEffect> getBlockedEffects(Entity entity) {
        return getBlockedEffects(entity.getLocation());
        
    }

    @Override
    public Set<InfuseEffect> getBlockedEffects(Player player) {
        return getBlockedEffects(player.getLocation());
    }

    @Override
    public Set<InfuseEffect> getBlockedEffects(Location loc) {
        return InfuseEffect.getRegisteredEffects()
            .values()
            .stream()
            .filter(e -> {
                List<NamespacedKey> worlds = Infuse.getInstance().getMainConfig().getBlacklistedWorlds(e);

                return worlds.contains(loc.getWorld().getKey());
            })
            .collect(Collectors.toSet());
    }

    @Override
    public boolean isEffectBlocked(Entity entity, InfuseEffect effect) {
        return isEffectBlocked(entity.getLocation(), effect);
    }

    @Override
    public boolean isEffectBlocked(Player player, InfuseEffect effect) {
        return isEffectBlocked(player.getLocation(), effect);
    }

    @Override
    public boolean isEffectBlocked(Location loc, InfuseEffect effect) {
        List<NamespacedKey> worlds = Infuse.getInstance().getMainConfig().getBlacklistedWorlds(effect);

        return worlds.contains(loc.getWorld().getKey());
    }
    
}
