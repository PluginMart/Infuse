package com.catadmirer.infuseSMP.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.inventory.ItemStack;

import com.catadmirer.infuseSMP.effects.InfuseEffect;
import com.catadmirer.infuseSMP.playerdata.DataManager;

public class ItemDespawnListener implements Listener {
    private final DataManager dataManager;

    public ItemDespawnListener(DataManager dataManager) {
        this.dataManager = dataManager;
    }

    @EventHandler
    public void lowerCraftLimitOnDespawn(ItemDespawnEvent event) {
        ItemStack item = event.getEntity().getItemStack();
        InfuseEffect effect = InfuseEffect.fromItem(item);
        if (effect == null) return;

        // Decrementing the number of crafted effects
        dataManager.setExistingCount(effect, dataManager.getExistingCount(effect) - 1);
    }
}
