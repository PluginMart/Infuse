package com.catadmirer.infuseSMP.listeners;

import com.catadmirer.infuseSMP.Infuse;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import com.catadmirer.infuseSMP.effects.InfuseEffect;

public class EntityDeathListener implements Listener {
    private final Infuse plugin = Infuse.getInstance();

    @EventHandler
    public void lowerCraftLimitOnDestroy(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Item itemEntity)) return;

        ItemStack item = itemEntity.getItemStack();
        InfuseEffect effect = InfuseEffect.getEffect(item);
        if (effect == null) return;

        // Decrementing the number of crafted effects
        plugin.getDataManager().setExistingCount(effect, plugin.getDataManager().getExistingCount(effect) - 1);
    }
}
