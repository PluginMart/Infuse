package com.catadmirer.infuseSMP.util;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.TooltipDisplay;

public class InventoryUtils {
    /**
     * Creates a decorative item with no name.
     *
     * @param material The material to make the item with.
     *
     * @return A decorative item with no name.
     */
    @SuppressWarnings("UnstableApiUsage")
    public static ItemStack createNoTooltip(Material material) {
        ItemStack pane = new ItemStack(material);
        pane.setData(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay().hideTooltip(true));
        return pane;
    }

    /**
     * Fills an inventory with a certain item.
     *
     * @param inventory The inventory to fill.
     * @param item The item to fill the inventory with.
     */
    public static void fillInventory(Inventory inventory, ItemStack item) {
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, item);
        }
    }

    /**
     * Putting an item into multiple slots of an inventory.
     *
     * @param inventory The inventory to place the item into.
     * @param slots The list of slots to place the item.
     * @param item The item to put into the inventory
     */
    public static void setItems(Inventory inventory, int[] slots, ItemStack item) {
        for (int slot : slots) inventory.setItem(slot, item);
    }

    /**
     * Utility function that fills all empty slots of an inventory with red stained glass panes with
     * empty names.
     *
     * @param inventory The inventory to fill with panes.
     */
    public static void fillRemainingSlots(Inventory inventory) {
        ItemStack stainedGlassPane = createNoTooltip(Material.RED_STAINED_GLASS_PANE);

        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, stainedGlassPane);
            }
        }
    }

    /**
     * "Locks" an inventory by setting the stack size for each item to 1.
     *
     * @param inventory The inventory to lock.
     */
    public static void lockInventory(Inventory inventory) {
        for (ItemStack item : inventory.getContents()) {
            if (item == null) continue;
            item.setData(DataComponentTypes.MAX_STACK_SIZE, 1);
        }
    }
}
