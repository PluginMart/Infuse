package com.catadmirer.infuseSMP.util.regions;

import com.catadmirer.infuseSMP.Infuse;
import com.catadmirer.infuseSMP.effects.InfuseEffect;
import com.catadmirer.infuseSMP.util.EffectFlag;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.association.RegionAssociable;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.SetFlag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Set;
import java.util.stream.Stream;

public class WorldGuardRegionBlocker extends RegionBlocker {
    private static final SetFlag<InfuseEffect> BLOCKED_EFFECTS = new SetFlag<>("blocked-effects", new EffectFlag(null));
    private static final StateFlag USE_SPARKS = new StateFlag("use-sparks", true);
    private static final StateFlag SPARK_PASSTHROUGH = new StateFlag("spark-passthrough", true);

    @Override
    public void init() {
        final FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();

        Stream.of(BLOCKED_EFFECTS, USE_SPARKS, SPARK_PASSTHROUGH)
            .forEach(flag -> {
                try {
                    registry.register(flag);
                } catch (FlagConflictException err) {
                    Infuse.LOGGER.warn("Another plugin has already registered the flag \"{}\".  Cannot register the flag.", flag.getName());
                }
            });

        Infuse.LOGGER.info("[Infuse] Successfully hooked into WorldGuard and registered the custom flags.");
    }

    @Override
    public boolean canUseSpark(Player player) {
        return queryValue(player.getLocation(), USE_SPARKS, WorldGuardPlugin.inst().wrapPlayer(player)) == StateFlag.State.ALLOW;
    }

    @Override
    public boolean canBeTargetedBySpark(Entity entity) {
        return queryValue(entity.getLocation(), SPARK_PASSTHROUGH, null) == StateFlag.State.ALLOW;
    }

    @Override
    public boolean canBeTargetedBySpark(Player player) {
        return queryValue(player.getLocation(), SPARK_PASSTHROUGH, WorldGuardPlugin.inst().wrapPlayer(player)) == StateFlag.State.ALLOW;
    }

    @Override
    public Set<InfuseEffect> getBlockedEffects(Entity entity) {
        return getBlockedEffects(entity.getLocation(), null);
    }

    @Override
    public Set<InfuseEffect> getBlockedEffects(Player player) {
        return getBlockedEffects(player.getLocation(), WorldGuardPlugin.inst().wrapPlayer(player));
    }

    @Override
    public Set<InfuseEffect> getBlockedEffects(Location loc) {
        return getBlockedEffects(loc, null);
    }

    @NonNull
    private Set<InfuseEffect> getBlockedEffects(Location loc, RegionAssociable assoc) {
        Set<InfuseEffect> effects = queryValue(loc, BLOCKED_EFFECTS, assoc);
        return effects == null ? Set.of() : effects;
    }

    @Override
    public boolean isEffectBlocked(Entity entity, InfuseEffect effect) {
        return isEffectBlocked(entity.getLocation(), null, effect);
    }

    @Override
    public boolean isEffectBlocked(Player player, InfuseEffect effect) {
        return isEffectBlocked(player.getLocation(), WorldGuardPlugin.inst().wrapPlayer(player), effect);
    }

    @Override
    public boolean isEffectBlocked(Location loc, InfuseEffect effect) {
        return isEffectBlocked(loc, null, effect);
    }

    private boolean isEffectBlocked(Location loc, RegionAssociable assoc, InfuseEffect effect) {
        return getBlockedEffects(loc, assoc).stream()
            .anyMatch(e -> e.getId() == effect.getId());
    }

    @Nullable
    private <T> T queryValue(Location loc, Flag<T> flag, RegionAssociable assoc) {
        final RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        final RegionManager manager = container.get(BukkitAdapter.adapt(loc.getWorld()));
        if (manager == null) return null;

        return manager.getApplicableRegions(BukkitAdapter.asBlockVector(loc)).queryValue(assoc, flag);
    }
}
