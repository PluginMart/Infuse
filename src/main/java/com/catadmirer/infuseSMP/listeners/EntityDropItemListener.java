package com.catadmirer.infuseSMP.listeners;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

import com.catadmirer.infuseSMP.Infuse;
import com.catadmirer.infuseSMP.effects.InfuseEffect;
import com.catadmirer.infuseSMP.managers.ParticleManager;

public class EntityDropItemListener implements Listener {
    @EventHandler
    public void onEntityDrop(EntityDropItemEvent event) {
        onDrop(event.getEntity(), event.getItemDrop());
    }

    @EventHandler
    public void onEntityDrop(PlayerDropItemEvent event) {
        onDrop(event.getPlayer(), event.getItemDrop());
    }

    public void onDrop(Entity entity, Item dropped) {
        ItemStack itemStack = dropped.getItemStack();
        InfuseEffect effect = InfuseEffect.getEffect(itemStack);
        if (effect == null) return;
        ParticleManager.dropEffect(Infuse.getInstance(), false, effect, entity.getLocation());
        dropped.setGlowing(true);
    }
}