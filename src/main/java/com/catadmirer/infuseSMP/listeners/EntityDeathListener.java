package com.catadmirer.infuseSMP.listeners;

import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import com.catadmirer.infuseSMP.effects.InfuseEffect;
import com.catadmirer.infuseSMP.playerdata.DataManager;

public class EntityDeathListener implements Listener {
    private final DataManager dataManager;

    public EntityDeathListener(DataManager dataManager) {
        this.dataManager = dataManager;
    }

    @EventHandler
    public void lowerCraftLimitOnDestroy(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Item itemEntity)) return;

        ItemStack item = itemEntity.getItemStack();
        InfuseEffect effect = InfuseEffect.fromItem(item);
        if (effect == null) return;

        // Decrementing the number of crafted effects
        dataManager.setExistingCount(effect, dataManager.getExistingCount(effect) - 1);
    }
}
