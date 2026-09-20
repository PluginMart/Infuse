package com.catadmirer.infuseSMP.playerdata;

import com.catadmirer.infuseSMP.Infuse;
import com.catadmirer.infuseSMP.effects.InfuseEffect;
import net.kyori.adventure.key.Key;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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
    public final Map<UUID,@Nullable Key> leftEffects = new HashMap<>();
    public final Map<UUID,@Nullable Key> rightEffects = new HashMap<>();
    public final Map<UUID,Boolean> controlModes = new HashMap<>();
    public final Map<Key,Integer> craftedCounts = new HashMap<>();

    @Override
    public void load() {
        Infuse.LOGGER.error("Cannot call 'DataManager#load' on a CachedData object.", new IllegalAccessException());
    }

    @Override
    public int getExistingCount(InfuseEffect effect) {
        return craftedCounts.getOrDefault(effect.key(), 0);
    }

    @Override
    public void setExistingCount(InfuseEffect effect, int count) {
        craftedCounts.put(effect.key(), count);
    }

    @Override
    public Set<UUID> getTrusted(UUID player) {
        return allTrusts.getOrDefault(player, Set.of());
    }

    @Override
    public void setTrusted(UUID player, Set<UUID> trusted) {
        allTrusts.put(player, trusted);
    }

    @Override
    public void setEffect(OfflinePlayer player, String slot, @Nullable InfuseEffect effect) {
        Key val = effect == null ? null : effect.key();
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
            Key key = leftEffects.get(player.getUniqueId());
            if (key == null) return null;
            return InfuseEffect.getEffect(key);
        } else if (slot.equals("2")) {
            Key key = rightEffects.get(player.getUniqueId());
            if (key == null) return null;
            return InfuseEffect.getEffect(key);
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
