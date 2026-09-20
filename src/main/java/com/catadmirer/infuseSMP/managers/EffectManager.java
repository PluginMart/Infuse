package com.catadmirer.infuseSMP.managers;

import com.catadmirer.infuseSMP.Infuse;
import com.catadmirer.infuseSMP.Message;
import com.catadmirer.infuseSMP.Message.MessageType;
import com.catadmirer.infuseSMP.effects.InfuseEffect;
import com.catadmirer.infuseSMP.events.EffectEquipEvent;
import com.catadmirer.infuseSMP.events.EffectUnequipEvent;
import com.catadmirer.infuseSMP.util.regions.RegionBlocker;

import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class EffectManager {
    private final Infuse plugin;

    public EffectManager(Infuse plugin) {
        this.plugin = plugin;
    }

    /**
     * Sets the effect a player has in a slot.
     * Overrides any effect already in that slot.
     *
     * @param player The {@link Player} to give an effect to.
     * @param effect The {@link InfuseEffect} to equip.
     * @param slot The slot to put the effect in.
     */
    public EquipResult setEffect(Player player, InfuseEffect effect, String slot) {
        return equipEffect(player, effect, slot, true);
    }

    /**
     * Equips an effect to a player.
     * Fails if the {@link EffectEquipEvent} was canceled or there was an effect equipped and override was set to false.
     *
     * @param player The {@link Player} to give an effect to.
     * @param effect The {@link InfuseEffect} to equip.
     * @param slot The slot to put the effect in.
     * @param override Whether to replace an existing effect.
     *
     * @return A {@link EquipResult}
     */
    public EquipResult equipEffect(Player player, InfuseEffect effect, String slot, boolean override) {
        // Calling an EffectEquipEvent and stopping if it is canceled.
        EffectEquipEvent event = new EffectEquipEvent(player, effect, slot);
        if (!event.callEvent()) return new EquipResult(EquipResultType.CANCELLED, effect);

        InfuseEffect equipped = plugin.getDataManager().getEffect(player, slot);
        if (equipped != null && !override) return new EquipResult(EquipResultType.FAIL);

        // Unequipping the old effect
        if (equipped != null) {
            EquipResult res = unequipEffect(player, slot);

            if (res.type != EquipResultType.SUCCESS) return new EquipResult(res.type, effect);
        }

        // Equipping the effect and updating the player data
        // If the player is in a blocked location, the effect is equipped but not activated.
        if (!RegionBlocker.getInstance().isEffectBlocked(player, effect)) effect.equip(player);
        plugin.getDataManager().setEffect(player, slot, effect);

        return new EquipResult(EquipResultType.SUCCESS, effect);
    }

    /**
     * Drains an effect from a player.
     * Sends feedback messages to the player and gives them the effect item.
     * Fails if the player's inventory is full or if they don't have an effect equipped.
     *
     * @param player The player who is draining an effect.
     * @param slot The slot to drain an effect from.
     */
    public EquipResult drainEffect(Player player, String slot) {
        // Unequipping the effect

        final InfuseEffect effect = plugin.getDataManager().getEffect(player, slot);
        EquipResult result = unequipEffect(player, slot);

        // Checking if an effect was removed
        if (result.type == EquipResultType.FAIL) {
            Message msg = new Message(MessageType.EFFECT_NONE_EQUIPPED);
            msg.applyPlaceholder("slot", slot);
            player.sendMessage(msg.toComponent());
            return result;
        }

        // Skipping if the unequip event was canceled
        if (result.type == EquipResultType.CANCELLED) {
            if (result.effect == null) {
                throw new IllegalStateException("Cancelled unequip events should still return their related effect");
            }

            Message msg = new Message(MessageType.DRAIN_CANCELLED);
            msg.applyPlaceholder("effect_name", result.effect.getName());
            player.sendMessage(msg.toComponent());
            return result;
        }

        // Making sure the effect is not null
        if (result.effect == null) {
            throw new IllegalStateException("Successful unequip events need to return their related effect.");
        }

        // Making sure the player has inventory space for the drained item if is meant to be given to them.
        if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage(new Message(MessageType.ERROR_INV_FULL).toComponent());
            plugin.getDataManager().setEffect(player, slot, effect);
            return new EquipResult(EquipResultType.FAIL);
        }

        player.getInventory().addItem(result.effect.createItem());
        // Sending the success message
        Message msg = new Message(MessageType.DRAIN_SUCCESS);
        msg.applyPlaceholder("effect_name", result.effect.getName());
        player.sendMessage(msg.toComponent());

        return result;
    }

    /**
     * Removes a player's effect from the specified slot and drops it on the ground.
     * Fails if the player doesn't have an effect equipped or the event was canceled.
     *
     * @param player The player to remove an effect from.
     * @param slot The slot to remove the effect from.
     */
    public EquipResult dropEffect(Player player, String slot) {
        EquipResult result = unequipEffect(player, slot);

        // Checking if an effect was removed
        if (result.type == EquipResultType.FAIL) return result;

        // Skipping if the unequip event was canceled
        if (result.type == EquipResultType.CANCELLED) {
            if (result.effect == null) {
                throw new IllegalStateException("Cancelled unequip events should still return their related effect");
            }

            return result;
        }

        // Making sure the effect is not null
        if (result.effect == null) {
            throw new IllegalStateException("Successful unequip events need to return their related effect.");
        }

        // Dropping the item
        player.getWorld().dropItem(player.getLocation(), result.effect.createItem());

        return result;
    }

    /**
     * Unequips an effect from a player.
     * Fails if the {@link EffectUnequipEvent} was canceled or if the player's inventory was full.
     *
     * @param player The {@link Player} to remove an effect from.
     * @param slot The slot to remove the effect from.
     */
    public EquipResult unequipEffect(Player player, String slot) {
        InfuseEffect effect = plugin.getDataManager().getEffect(player, slot);
        if (effect == null) return new EquipResult(EquipResultType.FAIL);

        // Calling an EffectUnequipEvent
        EffectUnequipEvent event = new EffectUnequipEvent(player, effect, slot);
        if (!event.callEvent()) return new EquipResult(EquipResultType.CANCELLED, effect);

        // Unequipping the effect and updating the player data
        effect.unequip(player);
        plugin.getDataManager().removeEffect(player, slot);

        return new EquipResult(EquipResultType.SUCCESS, effect);
    }

    /**
     * A record containing the result of an {@link EffectManager#equipEffect} or {@link EffectManager#unequipEffect} call.
     *
     * @param type The {@link EquipResultType} (Pass/Fail/Cancelled)
     * @param effect
     */
    public record EquipResult(EquipResultType type, @Nullable InfuseEffect effect) {
        public EquipResult(EquipResultType type) {
            this(type, null);
        }
    }

    public enum EquipResultType {
        FAIL,
        CANCELLED,
        SUCCESS
    }
}