package com.catadmirer.infuseSMP.listeners;

import com.catadmirer.infuseSMP.effects.InfuseEffect;
import com.catadmirer.infuseSMP.managers.CooldownManager;
import com.catadmirer.infuseSMP.playerdata.DataManager;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

public class PlayerSwapHandItemsListener implements Listener {
    private final DataManager dataManager;

    public PlayerSwapHandItemsListener(DataManager dataManager) {
        this.dataManager = dataManager;
    }

    /**
     * Listens for when the player swaps the items in their main and offhand.
     * When they do so, it will be used to activate their left or right spark based on whether they are crouching.
     *
     * @param event The {@link PlayerSwapHandItemsEvent} to process
     */
    @EventHandler
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();
        if (!dataManager.getControlMode(player).equals("offhand")) return;

        if (!player.isSneaking()) {
            // Activating the left effect's spark if the player was not sneaking and the effect wasn't on cooldown.
            InfuseEffect lEffect = dataManager.getEffect(player, "1");
            if (lEffect == null) return;
            if (CooldownManager.isOnCooldown(playerUUID, lEffect.getPlainKey())) return;
            event.setCancelled(true);
            lEffect.activateSpark(player);
        } else {
            // Activating the right effect's spark if the player was sneaking and the effect wasn't on cooldown.
            InfuseEffect rEffect = dataManager.getEffect(player, "2");
            if (rEffect == null) return;
            if (CooldownManager.isOnCooldown(playerUUID, rEffect.getPlainKey())) return;
            event.setCancelled(true);
            rEffect.activateSpark(player);
        }
    }
}
