package com.catadmirer.infuseSMP.util;

import com.destroystokyo.paper.MaterialSetTag;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.HumanEntity;
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

    public static void giveOrDropItem(HumanEntity player, ItemStack... items) {
        player.getInventory().addItem(items).forEach((i, extra) -> player.getWorld().dropItem(player.getLocation(), extra));
    }

    public static void applySpecialEnchantment(ItemStack item, NamespacedKey key, Enchantment enchantment, int newLevel) {
        // Skipping if the key was already applied
        if (item.getPersistentDataContainer().has(key)) return;

        // Skipping if the enchantment is already higher than the new level
        if (item.getEnchantmentLevel(enchantment) >= newLevel) return;

        item.editMeta(meta -> {
            meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, item.getEnchantmentLevel(enchantment));
            meta.removeEnchant(enchantment);
            meta.addEnchant(enchantment, newLevel, true);
        });
    }

    public static void removeSpecialEnchant(ItemStack item, NamespacedKey key, Enchantment enchantment) {
        // Skipping if the item doesn't have the key
        if (!item.getPersistentDataContainer().has(key)) return;

        item.editMeta(meta -> {
            int oldLevel = meta.getPersistentDataContainer().get(key, PersistentDataType.INTEGER);
            meta.getPersistentDataContainer().remove(key);

            meta.removeEnchant(enchantment);
            meta.addEnchant(enchantment, oldLevel, true);
        });
    }
}
