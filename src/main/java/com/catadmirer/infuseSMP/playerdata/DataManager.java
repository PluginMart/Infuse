package com.catadmirer.infuseSMP.playerdata;

import com.catadmirer.infuseSMP.effects.InfuseEffect;
import org.bukkit.OfflinePlayer;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Set;

@NullMarked
public interface DataManager {
    /**
     * Reloads the player data.
     */
    void load();

    /**
     * Gets the number of effects that exist.
     * 
     * @param effect The effect to count
     */
    int getExistingCount(InfuseEffect effect);

    /**
     * Sets the number of effects that exists.
     * 
     * @param effect The effect to set the count of
     * @param count The number of this effect that exists
     */
    void setExistingCount(InfuseEffect effect, int count);

    /**
     * Gets a set of the players that the player trusts.
     *
     * @return The set of players trusted by the player.
     */
    Set<OfflinePlayer> getTrusted(OfflinePlayer player);

    /**
     * Sets the players that the truster trusts.
     * 
     * @param player The player to modify
     * @param trusted The set of players the truster should now trust
     */
    void setTrusted(OfflinePlayer player, Set<OfflinePlayer> trusted);

    /**
     * Adds a player to the list of trusted people.
     * 
     * @param player The person whose trusted list to modify.
     * @param trusted The person the truster now trusts.
     */
    default void addTrust(OfflinePlayer player, OfflinePlayer trusted) {
        // TODO: Override in SQL-based data managers
        Set<OfflinePlayer> allTrusted = getTrusted(player);
        allTrusted.add(trusted);
        setTrusted(player, allTrusted);
    }

    /**
     * Removes a player from another player's list of trusted people.
     * 
     * @param player The player whose trusted list to modify.
     * @param untrusted The person to remove from the truster's trust.
     */
    default void removeTrust(OfflinePlayer player, OfflinePlayer untrusted) {
        // TODO: Override in SQL-based data managers
        Set<OfflinePlayer> trusted = getTrusted(player);
        trusted.remove(untrusted);
        setTrusted(player, trusted);
    }

    /**
     * Checks if a player is trusted by another player.
     * 
     * @param player The player whose trusted list to check.
     * @param trusted The player to check if truster trusts.
     * 
     * @return True if the truster trusts the toCheck player, false otherwise
     */
    default boolean isTrusted(OfflinePlayer player, OfflinePlayer trusted) {
        // TODO: Override in SQL-based data managers
        return getTrusted(player).contains(trusted);
    }

    /**
     * Sets the infuse effect in a specific slot for a player.
     * 
     * @param slot The slot to equip the effect in.
     * @param effect The {@link InfuseEffect} for the infuse effect.
     */
    void setEffect(OfflinePlayer player, String slot, @Nullable InfuseEffect effect);

    /**
     * Gets the infuse effect a player has in a specific slot.
     *
     * @return null if there is not an effect equipped there or if the InfuseEffect could not be deserialized.  Otherwise, it returns the deserialized InfuseEffect.
     */
    @Nullable InfuseEffect getEffect(OfflinePlayer player, String slot);

    /**
     * Checks if the player has the infuse effect.
     * It checks both slots and doesn't differentiate between regular and augmented effects.
     *
     * @return True if the player has the effect equipped, false otherwise.
     */
    default boolean hasEffect(OfflinePlayer player, InfuseEffect effect) {
        return hasEffect(player, effect, false);
    }

    /**
     * Checks if the player has the infuse effect.
     * It checks both slots and doesn't differentiate between regular and augmented effects.
     *
     * @return True if the player has the effect equipped, false otherwise.
     */
    default boolean hasEffect(OfflinePlayer player, InfuseEffect effect, boolean differentiateAugmented) {
        return hasEffect(player, effect, differentiateAugmented, "1") || hasEffect(player, effect, differentiateAugmented, "2");        
    }

    /**
     * Checks if the player has the infuse effect.
     * It checks both slots and doesn't differentiate between regular and augmented effects.
     *
     * @return True if the player has the effect equipped, false otherwise.
     */
    default boolean hasEffect(OfflinePlayer player, InfuseEffect effect, String slot) {
        return hasEffect(player, effect, false, slot);
    }

    /**
     * Checks if the player has the infuse effect equipped in a specific slot.
     *
     * @param differentiateAugmented Whether the search should differentiate between regular and augmented effects.
     *
     * @return True if the player has the effect equipped, false otherwise.
     */
    default boolean hasEffect(OfflinePlayer player, InfuseEffect effect, boolean differentiateAugmented, String slot) {
        InfuseEffect equipped = getEffect(player, slot);
        if (equipped == null) return false;

        if (differentiateAugmented) {
            return effect.equals(equipped);
        }

        return effect.getId() == equipped.getId();
    }

    /** Removes an infuse effect from a specific slot for a player. */
    default void removeEffect(OfflinePlayer player, String slot) {
        setEffect(player, slot, null);
    }

    /** Sets the control mode for a player. */
    void setControlMode(OfflinePlayer player, String controlMode);

    /**
     * Gets the control mode of a player.
     *
     * @return Either "command" or "offhand".  Defaults to "command"
     */
    String getControlMode(OfflinePlayer player);

    /**
     * Modifies the config to make any necessary changes to make old versions compatible with this new one.
     */
    void applyUpdates();
}