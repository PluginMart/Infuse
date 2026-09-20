package com.catadmirer.infuseSMP.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;

import com.catadmirer.infuseSMP.Infuse;
import com.catadmirer.infuseSMP.effects.InfuseEffect;
import com.catadmirer.infuseSMP.managers.ParticleManager;

public class EntityPickupItemListener implements Listener {
    private final Infuse plugin;

    public EntityPickupItemListener(Infuse plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        ItemStack item = event.getItem().getItemStack();
        InfuseEffect effect = InfuseEffect.fromItem(item);
        if (effect == null) return;
        ParticleManager.dropEffect(plugin, true, effect, event.getItem().getLocation());
    }
}