package com.catadmirer.infuseSMP.listeners;

import com.catadmirer.infuseSMP.Infuse;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.inventory.ItemStack;

import com.catadmirer.infuseSMP.effects.InfuseEffect;

public class ItemDespawnListener implements Listener {
    @EventHandler
    public void lowerCraftLimitOnDespawn(ItemDespawnEvent event) {
        ItemStack item = event.getEntity().getItemStack();
        InfuseEffect effect = InfuseEffect.getEffect(item);
        if (effect == null) return;

        // Decrementing the number of crafted effects
        Infuse.getInstance().getDataManager().setExistingCount(effect, Infuse.getInstance().getDataManager().getExistingCount(effect) - 1);
    }
}
