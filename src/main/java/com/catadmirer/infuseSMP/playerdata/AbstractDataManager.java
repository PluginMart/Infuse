package com.catadmirer.infuseSMP.playerdata;

import com.catadmirer.infuseSMP.Infuse;
import com.catadmirer.infuseSMP.effects.InfuseEffect;
import net.kyori.adventure.key.Key;
import org.bukkit.OfflinePlayer;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.*;

@NullMarked
public abstract class AbstractDataManager implements DataManager {
    protected final Map<Key,Integer> existingCount = new HashMap<>();
    protected final Map<UUID,Set<UUID>> allTrusts = new HashMap<>();
    protected final Map<OfflinePlayer,Map<String,@Nullable Key>> playerEffects = new HashMap<>();
    protected final Map<OfflinePlayer,Boolean> offhandUsers = new HashMap<>();

    @Override
    public int getExistingCount(InfuseEffect effect) {
        return existingCount.getOrDefault(effect.key(), 0);
    }

    @Override
    public void setExistingCount(InfuseEffect effect, int count) {
        existingCount.put(effect.key(), count);
    }

    @Override
    public Set<UUID> getTrusted(UUID player) {
        return allTrusts.getOrDefault(player, Set.of());
    }

    @Override
    public void addTrust(UUID player, UUID trusted) {
        allTrusts.computeIfAbsent(player, k -> new HashSet<>()).add(trusted);
    }

    @Override
    public void removeTrust(UUID player, UUID trusted) {
        allTrusts.computeIfAbsent(player, k -> new HashSet<>()).remove(trusted);
    }

    @Override
    public void setEffect(OfflinePlayer player, String slot, @Nullable InfuseEffect effect) {
        // Making sure slot is "1" or "2"
        if (!slot.equals("1") && !slot.equals("2")) {
            Infuse.LOGGER.warn("Slot '{}' is not a valid slot.  Please use \"1\" or \"2\"", slot);
            return;
        }

        playerEffects.computeIfAbsent(player, k -> new HashMap<>()).put(slot, effect == null ? null : effect.key());
    }

    @Override
    public @Nullable InfuseEffect getEffect(OfflinePlayer player, String slot) {
        Map<String,@Nullable Key> effects = playerEffects.get(player);
        if (effects == null) return null;

        Key key = effects.get(slot);
        if (key == null) return null;

        InfuseEffect effect = InfuseEffect.getEffect(key);
        if (effect == null) {
            Infuse.LOGGER.error("Effect '{}' not found", key);
        }

        return effect;
    }

    @Override
    public void setControlMode(OfflinePlayer player, String controlMode) {
        boolean offhandControls;
        if (controlMode.equals("offhand")) {
            offhandControls = true;
        } else if (controlMode.equals("command")) {
            offhandControls = false;
        } else {
            Infuse.LOGGER.error("Invalid control mode \"{}\".  Please use \"offhand\" or \"command\"", controlMode);
            return;
        }

        offhandUsers.put(player, offhandControls);
    }

    @Override
    public String getControlMode(OfflinePlayer player) {
        return offhandUsers.getOrDefault(player, true) ? "command" : "offhand";
    }
}
