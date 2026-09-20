package com.catadmirer.infuseSMP.inventories;

import com.catadmirer.infuseSMP.Message;
import com.catadmirer.infuseSMP.util.InventoryUtils;
import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class StationSelectionMenu implements InventoryHolder {
    private final Inventory inventory;
    private final Location standLocation;

    public StationSelectionMenu(Location standLocation) {
        inventory = Bukkit.createInventory(this, 27, Component.text("Station Selection"));
        this.standLocation = standLocation;

        // Filling the inventory with a filler item.
        InventoryUtils.fillInventory(inventory, InventoryUtils.createNoTooltip(Material.GRAY_STAINED_GLASS_PANE));

        // Creating the crafting table option
        ItemStack craftingTable = new ItemStack(Material.CRAFTING_TABLE);
        craftingTable.setData(DataComponentTypes.CUSTOM_NAME, Message.toComponent("<dark_red>Crafting Table"));

        // Creating the brewing stand option
        ItemStack brewingStand = new ItemStack(Material.BREWING_STAND);
        brewingStand.setData(DataComponentTypes.CUSTOM_NAME, Message.toComponent("<dark_red>Brewing Stand"));

        // Putting the options into the inventory
        inventory.setItem(11, craftingTable);
        inventory.setItem(15, brewingStand);

        // Locking the inventory
        InventoryUtils.lockInventory(inventory);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public Location getStandLocation() {
        return standLocation;
    }
}