package com.catadmirer.infuseSMP.inventories;

import com.catadmirer.infuseSMP.effects.InfuseEffect;
import com.catadmirer.infuseSMP.managers.RecipeManager;
import com.catadmirer.infuseSMP.util.InventoryUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.RecipeChoice.MaterialChoice;
import org.jetbrains.annotations.NotNull;

public class RecipeGUI implements InventoryHolder {
    private final Inventory inventory;

    public RecipeGUI(RecipeManager manager, InfuseEffect effect) {
        inventory = Bukkit.createInventory(this, 45, Component.text("Recipes"));

        ShapedRecipe recipe = manager.getRecipe(effect);

        // Loading the ingredients into the gui
        int[] ingredientSlots = {10, 11, 12, 19, 20, 21, 28, 29, 30};
        int slotIndex = 0;
        for (String row : recipe.getShape()) {
            for (char ch : row.toCharArray()) {
                RecipeChoice recipeChoice = recipe.getChoiceMap().get(ch);
                if (!(recipeChoice instanceof MaterialChoice matChoice)) continue;

                ItemStack ingredient = matChoice.getItemStack();
                inventory.setItem(ingredientSlots[slotIndex], ingredient);
                slotIndex++;
            }
        }

        // Loading the result of the recipe into the output slot.
        inventory.setItem(25, recipe.getResult());

        // Filling the rest of the slots with red glass panes
        InventoryUtils.fillRemainingSlots(inventory);

        // Locking the inventory
        InventoryUtils.lockInventory(inventory);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
