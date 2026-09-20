package com.catadmirer.infuseSMP.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;

import com.catadmirer.infuseSMP.Infuse;
import com.catadmirer.infuseSMP.Message;
import com.catadmirer.infuseSMP.Message.MessageType;
import com.catadmirer.infuseSMP.effects.InfuseEffect;
import com.catadmirer.infuseSMP.managers.EffectManager.EquipResult;
import com.catadmirer.infuseSMP.managers.EffectManager.EquipResultType;

public class PlayerItemConsumeListener implements Listener {
    private final Infuse plugin;

    public PlayerItemConsumeListener(Infuse plugin) {
        this.plugin = plugin;
    }

    /**
     * Handling when players drink an infuse potion.
     *
     * @param event The consume event.
     */
    @EventHandler
    public void equipEffectPotion(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // Getting the effect from the item
        InfuseEffect effect = InfuseEffect.fromItem(item);

        // Skipping if the effect is not found.
        if (effect == null) return;

        // Equipping the effect
        EquipResult result = plugin.getEffectManager().equipEffect(player, effect, "1", false);

        // Equipping the slot in the players other slot
        if (result.type() == EquipResultType.FAIL) {
            // Drain slot 2 if an effect is equipped there
            if (plugin.getDataManager().getEffect(player, "2") != null) {
                result = plugin.getEffectManager().drainEffect(player, "2");

                // If the drain failed or was cancelled, exit
                if (result.type() == EquipResultType.CANCELLED) return;
                if (result.type() == EquipResultType.FAIL) return;
            }

            result = plugin.getEffectManager().equipEffect(player, effect, "2", false);
        }

        // Skipping the rest of the logic if the equip event was cancelled
        if (result.type() == EquipResultType.CANCELLED) return;

        // Notifying the player
        Message msg = new Message(MessageType.EFFECT_EQUIPPED);
        msg.applyPlaceholder("effect_name", effect.getName());
        player.sendMessage(msg.toComponent());

        // Removing the effect from the player
        event.setItem(event.getItem().subtract(1));
    }
}
