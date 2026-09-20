package com.catadmirer.infuseSMP.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import com.catadmirer.infuseSMP.Infuse;
import com.catadmirer.infuseSMP.effects.InfuseEffect;

public class PlayerQuitListener implements Listener {
    private final Infuse plugin = Infuse.getInstance();

    /** Unequips a player's effects when they leave the game. */
    @EventHandler
    public void deactivateEffects(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Deactivating the player's effects
        InfuseEffect effect = plugin.getDataManager().getEffect(player.getUniqueId(), "1");
        if (effect != null) effect.unequip(player);

        effect = plugin.getDataManager().getEffect(player.getUniqueId(), "2");
        if (effect != null) effect.unequip(player);
    }
}
