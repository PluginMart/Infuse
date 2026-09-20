package com.catadmirer.infuseSMP.listeners;

import com.catadmirer.infuseSMP.Infuse;
import com.catadmirer.infuseSMP.Message;
import com.catadmirer.infuseSMP.Message.MessageType;
import com.catadmirer.infuseSMP.effects.InfuseEffect;
import com.catadmirer.infuseSMP.inventories.AugOrRegChooser;
import com.catadmirer.infuseSMP.inventories.EffectChooser;
import com.catadmirer.infuseSMP.inventories.RecipeGUI;
import com.catadmirer.infuseSMP.inventories.RecipeListGUI;

import com.catadmirer.infuseSMP.managers.RecipeManager;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class InventoryClickListener implements Listener {
    @EventHandler
    public void clickEffectChooser(InventoryClickEvent event) {
        HumanEntity player = event.getWhoClicked();
        Inventory inventory = event.getClickedInventory();

        if (inventory == null) return;
        if (!(inventory.getHolder() instanceof EffectChooser)) return;

        // Cancelling the click event to prevent the player from getting the item.
        event.setCancelled(true);

        ItemStack item = event.getCurrentItem();
        InfuseEffect effect = InfuseEffect.getEffect(item);

        // Ignoring if the player clicked on something other than an effect.
        if (effect == null) return;

        player.openInventory(new AugOrRegChooser(effect).getInventory());
    }

    @EventHandler
    public void clickAugOrRegChooser(InventoryClickEvent event) {
        Inventory inventory = event.getClickedInventory();
        if (inventory == null) return;
        if (!(inventory.getHolder() instanceof AugOrRegChooser)) return;

        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.POTION) return;

        event.setCancelled(true);
    }

    /**
     * Preventing players from clicking items in a {@link RecipeGUI} inventory.
     *
     * @param event The {@link InventoryClickEvent} to listen for.
     */
    @EventHandler
    public void recipeGUIHandler(InventoryClickEvent event) {
        Inventory inventory = event.getClickedInventory();
        if (inventory == null) return;
        if (!(inventory.getHolder() instanceof RecipeGUI)) return;

        event.setCancelled(true);
    }

    /**
     * Inventory click handler for the RecipeListGUI inventory
     *
     * @param event an InventoryClickEvent
     */
    @EventHandler
    public void recipeListGUIHandler(InventoryClickEvent event) {
        Inventory inventory = event.getClickedInventory();
        if (inventory == null) return;
        if (!(inventory.getHolder() instanceof RecipeListGUI)) return;

        event.setCancelled(true);

        // Getting the clicked item and opening the recipe menu for the item.
        ItemStack clickedItem = event.getCurrentItem();
        InfuseEffect effect = InfuseEffect.getEffect(clickedItem);
        if (effect == null) return;

        HumanEntity player = event.getWhoClicked();

        // Erroring out if the recipe is not enabled
        RecipeManager recipeManager = Infuse.getInstance().getRecipeManager();
        if (!recipeManager.isRecipeEnabled(effect)) {
            player.sendMessage(new Message(MessageType.RECIPE_DISABLED).toComponent());
            return;
        }

        if (recipeManager.getRecipe(effect).getChoiceMap().isEmpty()) {
            player.sendMessage(new Message(MessageType.RECIPE_NOT_FOUND).toComponent());
            return;
        }

        // Opening the recipe gui
        Inventory recipeGui = new RecipeGUI(recipeManager, effect).getInventory();
        player.closeInventory();
        player.openInventory(recipeGui);
    }
}
