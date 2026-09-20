package com.catadmirer.infuseSMP.listeners;

import com.catadmirer.infuseSMP.managers.DataManager;
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
        if (!dataManager.getControlMode(player.getUniqueId()).equals("offhand")) return;

        if (!player.isSneaking()) {
            player.performCommand("/lspark");
        } else {
            player.performCommand("/rspark");
        }
    }
}
