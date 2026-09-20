package com.catadmirer.infuseSMP.listeners;

import com.catadmirer.infuseSMP.effects.InfuseEffect;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.inventory.ItemStack;

public class CrafterCraftListener implements Listener {
    /** Prevents infuse effects from being crafted in a crafter. */
    @EventHandler
    public void onCrafterCraft(CrafterCraftEvent event) {
        ItemStack item = event.getResult();
        InfuseEffect effect = InfuseEffect.fromItem(item);
        if (effect == null) return;

        event.setCancelled(true);
    }
}
