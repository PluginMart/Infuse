package com.catadmirer.infuseSMP.util;

import com.destroystokyo.paper.MaterialSetTag;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class ItemUtil {
    public static boolean isSword(ItemStack item) {
        if (item == null) return false;

        return MaterialSetTag.ITEMS_SWORDS.isTagged(item.getType());
    }

    public static boolean isPickaxe(ItemStack item) {
        if (item == null) return false;

        return MaterialSetTag.ITEMS_PICKAXES.isTagged(item.getType());
    }

    public static boolean isAxe(ItemStack item) {
        if (item == null) return false;

        return MaterialSetTag.ITEMS_AXES.isTagged(item.getType());
    }

    public static boolean isShovel(ItemStack item) {
        if (item == null) return false;

        return MaterialSetTag.ITEMS_SHOVELS.isTagged(item.getType());
    }

    public static boolean isHoe(ItemStack item) {
        if (item == null) return false;

        return MaterialSetTag.ITEMS_HOES.isTagged(item.getType());
    }

    public static void applySpecialEnchantment(ItemStack item, NamespacedKey key, Enchantment enchantment, int newLevel) {
        // Skipping if the key was already applied
        if (item.getPersistentDataContainer().has(key)) return;

        int currentLevel = item.getEnchantmentLevel(enchantment);

        // Skipping if the enchantment is already higher than the new level
        if (currentLevel >= newLevel) return;

        // Storing the current level for later
        item.editPersistentDataContainer(c -> c.set(key, PersistentDataType.INTEGER, currentLevel));

        // Removing the old enchantment and applying the new one
        item.removeEnchantment(enchantment);
        item.addUnsafeEnchantment(enchantment, newLevel);
    }

    public static void removeSpecialEnchant(ItemStack item, NamespacedKey key, Enchantment enchantment) {
        // Skipping if the item doesn't have the key
        if (!item.getPersistentDataContainer().has(key)) return;

        // Getting the old level from pdc.
        //noinspection DataFlowIssue
        int oldLevel = item.getPersistentDataContainer().get(key, PersistentDataType.INTEGER);

        // Removing the old level from pdc
        item.editPersistentDataContainer(c -> c.remove(key));

        item.removeEnchantment(enchantment);
        if (oldLevel == 0) return;
        item.addEnchantment(enchantment, oldLevel);
    }
}
