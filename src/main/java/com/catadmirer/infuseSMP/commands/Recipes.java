package com.catadmirer.infuseSMP.commands;

import com.catadmirer.infuseSMP.Infuse;
import com.catadmirer.infuseSMP.Message;
import com.catadmirer.infuseSMP.effects.InfuseEffect;
import com.catadmirer.infuseSMP.inventories.RecipeListGUI;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;

import java.util.List;

public class Recipes implements CommandExecutor {
    private static Infuse plugin;

    public Recipes(Infuse plugin) {
        Recipes.plugin = plugin;
    }

    @NullMarked
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player) {
            player.openInventory(new RecipeListGUI().getInventory());
            return true;
        }

        return true;
    }

    /**
     * Create a potion effect with the effect limits for lore rather than the default lore.
     *
     * @param effect The {@link InfuseEffect} to create.
     *
     * @return The effect item with modified lore.
     */
    public static ItemStack createPotionWithModifiedLore(InfuseEffect effect) {
        // Only regular effects should be put here
        if (effect.isAugmented()) return null;

        // Creating the potion from the effect
        ItemStack potionItem = effect.createItem();

        int augLeft = plugin.getMainConfig().getCraftLimit(effect.getAugmentedVersion()) - plugin.getDataManager().getExistingCount(effect.getAugmentedVersion());
        int regLeft = plugin.getMainConfig().getCraftLimit(effect.getRegularVersion()) - plugin.getDataManager().getExistingCount(effect.getRegularVersion());

        potionItem.setData(DataComponentTypes.LORE, ItemLore.lore(List.of(
            Message.toComponent("<gray>Augmented Limit: <aqua>" + augLeft),
            Message.toComponent("<gray>Regular Limit: <aqua>" + regLeft)
        )));

        return potionItem;
    }
}
