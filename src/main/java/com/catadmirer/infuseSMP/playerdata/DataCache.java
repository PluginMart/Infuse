package com.catadmirer.infuseSMP.playerdata;

import com.catadmirer.infuseSMP.Infuse;
import com.catadmirer.infuseSMP.effects.InfuseEffect;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * This class is a non-persistent implementation of a {@link DataManager}.<br>
 * It may not reflect the current state of the persistent data.<br>
 * It should only be used by internal persistent implementations to prevent constant read/write operations.
 */
// TODO: Add a DataCacheBuilder
@ApiStatus.Internal
@NullMarked
public class DataCache implements DataManager {
    public final Map<UUID,Set<UUID>> allTrusts = new HashMap<>();
    public final Map<UUID,@Nullable Integer> leftEffects = new HashMap<>();
    public final Map<UUID,@Nullable Integer> rightEffects = new HashMap<>();
    public final Map<UUID,Boolean> controlModes = new HashMap<>();
    public final Map<Integer,Integer> craftedCounts = new HashMap<>();

    @Override
    public void load() {
        Infuse.LOGGER.error("Cannot call 'DataManager#load' on a CachedData object.", new IllegalAccessException());
    }

    @Override
    public int getExistingCount(InfuseEffect effect) {
        return craftedCounts.getOrDefault(effect.serialize(), 0);
    }

    @Override
    public void setExistingCount(InfuseEffect effect, int count) {
        craftedCounts.put(effect.serialize(), count);
    }

    @Override
    public Set<OfflinePlayer> getTrusted(OfflinePlayer player) {
        return allTrusts.getOrDefault(player.getUniqueId(), Set.of()).stream().map(Bukkit::getOfflinePlayer).collect(Collectors.toSet());
    }

    @Override
    public void setTrusted(OfflinePlayer player, Set<OfflinePlayer> trusted) {
        allTrusts.put(player.getUniqueId(), trusted.stream().map(OfflinePlayer::getUniqueId).collect(Collectors.toSet()));
    }

    @Override
    public void setEffect(OfflinePlayer player, String slot, @Nullable InfuseEffect effect) {
        Integer val = effect == null ? null : effect.serialize();
        if (slot.equals("1")) {
            leftEffects.put(player.getUniqueId(), val);
        } else if (slot.equals("2")) {
            rightEffects.put(player.getUniqueId(), val);
        } else {
            Infuse.LOGGER.warn("Slot '{}' is not a valid slot.  Please use \"1\" or \"2\"", slot);
        }
    }

    @Nullable
    @Override
    public InfuseEffect getEffect(OfflinePlayer player, String slot) {
        if (slot.equals("1")) {
            Integer serialized = leftEffects.get(player.getUniqueId());
            if (serialized == null) return null;
            return InfuseEffect.deserialize(serialized);
        } else if (slot.equals("2")) {
            Integer serialized = rightEffects.get(player.getUniqueId());
            if (serialized == null) return null;
            return InfuseEffect.deserialize(serialized);
        } else {
            Infuse.LOGGER.warn("Slot '{}' is not a valid slot.  Please use \"1\" or \"2\"", slot);
        }

        return null;
    }

    @Override
    public void setControlMode(OfflinePlayer player, String controlMode) {
        controlModes.put(player.getUniqueId(), controlMode.equals("offhand"));
    }

    @Override
    public String getControlMode(OfflinePlayer player) {
        return controlModes.getOrDefault(player.getUniqueId(), false) ? "offhand" : "command";
    }

    @Override
    public void applyUpdates() {

    }
}
