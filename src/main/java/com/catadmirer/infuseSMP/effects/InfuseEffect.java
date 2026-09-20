package com.catadmirer.infuseSMP.effects;

import com.catadmirer.infuseSMP.Infuse;
import com.catadmirer.infuseSMP.Message;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import io.papermc.paper.datacomponent.item.PotionContents;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

public abstract class InfuseEffect implements Listener {
    private static final Map<Integer,InfuseEffect> REGISTERED_EFFECTS = new HashMap<>();

    public static final NamespacedKey EFFECT_KEY = new NamespacedKey("infuse", "effect_key");
    public static final NamespacedKey AUG_KEY = new NamespacedKey("infuse", "aug");

    protected final String key;
    protected final int id;
    protected final boolean augmented;
    protected final Color potionColor;
    protected final BossBar.Color ritualColor;
    protected final Infuse plugin = Infuse.getInstance();

    public InfuseEffect(String key, int id, boolean augmented, Color potionColor, BossBar.Color ritualColor) {
        this.key = key;
        this.id = id;
        this.augmented = augmented;
        this.potionColor = potionColor;
        this.ritualColor = ritualColor;
    }

    public static boolean isRegistered(InfuseEffect effect) {
        return REGISTERED_EFFECTS.containsKey(effect.id);
    }

    public static boolean register(InfuseEffect effect) {
        if (effect.id > 100) {
            Infuse.LOGGER.warn("Effect id {} for {} is invalid.  Effect ids cannot be >100.", effect.id, effect.key);
            return false;
        }

        InfuseEffect existing = REGISTERED_EFFECTS.get(effect.id);
        if (existing != null) {
            Infuse.LOGGER.warn("Effect id {} has already been taken by {}.  Cannot assign it to {}.", effect.id, existing.key, effect.key);
            return false;
        }

        REGISTERED_EFFECTS.put(effect.id, effect);
        return true;
    }

    @NonNull
    @Unmodifiable
    public static Map<Integer,InfuseEffect> getRegisteredEffects() {
        return Map.copyOf(REGISTERED_EFFECTS);
    }

    public int getId() {
        return id;
    }

    public String getPlainKey() {
        return key;
    }

    public String getKey() {
        return toString();
    }

    public boolean isAugmented() {
        return augmented;
    }

    public Color getPotionColor() {
        return potionColor;
    }

    public BossBar.Color getRitualColor() {
        return ritualColor;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof InfuseEffect effect)) return false;

        return effect.augmented == this.augmented && effect.id == this.id;
    }

    @Override
    public String toString() {
        return (augmented ? "aug_" : "") + key;
    }

    public abstract void equip(Player owner);
    public abstract void unequip(Player owner);

    @Deprecated()
    public void applyPassives(Player owner) {}
    public abstract void activateSpark(Player owner);

    public abstract InfuseEffect getRegularVersion();
    public abstract InfuseEffect getAugmentedVersion();

    public abstract Message getName();
    public abstract Message getLore();

    public char getIcon() {
        return (char) Integer.parseInt("E" + (augmented ? 2 : 0) + String.format("%02d", id), 16);
    }

    public char getActiveIcon() {
        return (char) Integer.parseInt("E" + (augmented ? 3 : 1) + String.format("%02d", id), 16);
    }

    public static InfuseEffect fromString(@Nullable String key) {
        if (key == null) return null;

        // Checking if the effect is augmented
        boolean augmented = key.startsWith("aug_");
        if (augmented) {
            key = key.substring(4);
        }

        // Searching for a matching registered effect
        for (InfuseEffect effect : REGISTERED_EFFECTS.values()) {
            if (!effect.getPlainKey().equals(key)) continue;

            return augmented ? effect.getAugmentedVersion() : effect.getRegularVersion();
        }

        Infuse.LOGGER.warn("No effect found for string '{}'.", key);
        return null;
    }

    /**
     * Creates an {@link ItemStack} representation of the effect for a player to consume.
     *
     * @return The corresponding {@link ItemStack}
     */
    public ItemStack createItem() {
        ItemStack item = new ItemStack(Material.POTION);

        // Adjusting item data
        item.setData(DataComponentTypes.CUSTOM_NAME, getName().toComponent());
        item.setData(DataComponentTypes.LORE, ItemLore.lore(getLore().toComponentList()));
        item.editPersistentDataContainer(c -> {
            c.set(EFFECT_KEY, PersistentDataType.STRING, toString());
        });

        item.setData(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay().addHiddenComponents(DataComponentTypes.POTION_CONTENTS));
        item.setData(DataComponentTypes.POTION_CONTENTS, PotionContents.potionContents().customColor(org.bukkit.Color.fromARGB(potionColor.getRGB())));

        if (augmented) {
            item.setData(DataComponentTypes.ITEM_MODEL, AUG_KEY);
        }

        return item;
    }

    /**
     * Checks if an {@link ItemStack} was created by this effect.
     *
     * @param item The item to check.
     *
     * @return Whether the item was created by this effect.
     */
    public boolean itemMatches(@Nullable ItemStack item) {
        if (item == null) return false;
        if (item.getType() != Material.POTION) return false;

        return key.equals(item.getPersistentDataContainer().get(EFFECT_KEY, PersistentDataType.STRING));
    }

    public static InfuseEffect fromItem(ItemStack item) {
        if (item == null) return null;
        if (item.getType() != Material.POTION) return null;

        String key = item.getPersistentDataContainer().get(EFFECT_KEY, PersistentDataType.STRING);
        if (key == null) return null;

        return fromString(key);
    }

    /** Serializes an InfuseEffect into an int */
    public int serialize() {
        return (augmented ? 100 : 0) + id;
    }

    /**
     * Deserializes an InfuseEffect from an int
     * <br>
     * The first two digits of an infuse effect are the effect id.  IDs 0-12 are taken by the base Effects.
     * If the number is >= 100, then the effect will be converted to its augmented form.
     *
     * @param serialized The serialized int
     */
    public static InfuseEffect deserialize(int serialized) {
        if (!REGISTERED_EFFECTS.containsKey(serialized % 100)) {
            Infuse.LOGGER.warn("Could not find an effect registered to id {}", serialized % 100);
            return null;
        }

        boolean augmented = serialized > 99;
        int id = serialized % 100;
        InfuseEffect effect = REGISTERED_EFFECTS.get(id);

        return augmented ? effect.getAugmentedVersion() : effect.getRegularVersion();
    }
}
