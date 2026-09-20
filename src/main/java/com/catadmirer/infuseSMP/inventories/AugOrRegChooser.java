package com.catadmirer.infuseSMP.inventories;

import com.catadmirer.infuseSMP.Message;
import com.catadmirer.infuseSMP.effects.InfuseEffect;
import com.catadmirer.infuseSMP.util.InventoryUtils;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public class AugOrRegChooser implements InventoryHolder {
    private final Inventory inventory;

    public AugOrRegChooser(InfuseEffect effect) {
        inventory = Bukkit.createInventory(this, 27, Message.toComponent("<yellow>Choose"));

        // Filling the inventory with a filler item.
        InventoryUtils.fillInventory(inventory, InventoryUtils.createNoTooltip(effect.getBackgroundMaterial()));

        // Adding the effects to the inventory
        inventory.setItem(11, effect.getRegularVersion().createItem());
        inventory.setItem(15, effect.getAugmentedVersion().createItem());

        // Locking the inventory
        InventoryUtils.lockInventory(inventory);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
