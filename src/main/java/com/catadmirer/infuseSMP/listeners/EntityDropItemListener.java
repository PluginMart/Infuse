package com.catadmirer.infuseSMP.listeners;

import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.inventory.ItemStack;

import com.catadmirer.infuseSMP.Infuse;
import com.catadmirer.infuseSMP.effects.InfuseEffect;
import com.catadmirer.infuseSMP.managers.ParticleManager;

public class EntityDropItemListener implements Listener {
    private final Infuse plugin;

    public EntityDropItemListener(Infuse plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDrop(EntityDropItemEvent event) {
        final Item droppedItem = event.getItemDrop();
        ItemStack itemStack = droppedItem.getItemStack();
        InfuseEffect effect = InfuseEffect.fromItem(itemStack);
        if (effect == null) return;
        ParticleManager.dropEffect(plugin, false, effect, droppedItem.getLocation());
        droppedItem.setGlowing(true);
    }
}