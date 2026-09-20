package com.catadmirer.infuseSMP.effects;

import com.catadmirer.infuseSMP.Infuse;
import com.catadmirer.infuseSMP.Message;
import com.catadmirer.infuseSMP.managers.CooldownManager;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import io.papermc.paper.datacomponent.item.PotionContents;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.KeyPattern;
import net.kyori.adventure.key.Keyed;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public abstract class InfuseEffect implements Listener, Keyed {
    private static final Map<Key,InfuseEffect> REGISTERED = new HashMap<>();

    public static final NamespacedKey EFFECT_KEY = new NamespacedKey("infuse", "effect_key");
    public static final NamespacedKey AUG_KEY = new NamespacedKey("infuse", "aug");

    @KeyPattern.Value
    protected final String plainKey;
    protected final int id;
    protected final boolean augmented;
    protected final Color potionColor;
    protected final BossBar.Color ritualColor;
    protected final Material backgroundMaterial;
    protected final Infuse plugin = Infuse.getInstance();

    public InfuseEffect(@KeyPattern.Value String key, int id, boolean augmented, Color potionColor, BossBar.Color ritualColor, Material backgroundMaterial) {
        this.plainKey = key;
        this.id = id;
        this.augmented = augmented;
        this.potionColor = potionColor;
        this.ritualColor = ritualColor;
        this.backgroundMaterial = backgroundMaterial;
    }

    public static boolean isRegistered(InfuseEffect effect) {
        return isRegistered(effect.key());
    }

    public static boolean isRegistered(Key key) {
        return REGISTERED.containsKey(key);
    }

    public static boolean register(InfuseEffect effect) {
        effect = effect.getRegularVersion();

        // Enforcing the id limit
        if (effect.id > 100) {
            Infuse.LOGGER.warn("Effect id {} for {} is invalid.  Effect ids cannot be >100.", effect.id, effect.key());
            return false;
        }

        if (isRegistered(effect.key())) {
            InfuseEffect existing = REGISTERED.get(effect.key());
            Infuse.LOGGER.warn("Effect key {} has already been taken by {}.  Cannot assign it to {}.", effect.key(), existing.key(), effect.key());
            return false;
        }

        // Attempting to register the effect
        REGISTERED.put(effect.getRegularVersion().key(), effect.getRegularVersion());
        REGISTERED.put(effect.getAugmentedVersion().key(), effect.getAugmentedVersion());

        // Registering event listeners in the effect
        Bukkit.getPluginManager().registerEvents(effect, Infuse.getInstance());

        return true;
    }

    /**
     * Gets a registered effect.
     * 
     * @param key The key of the effect.
     * 
     * @return The registered effect or null if no effect is registered under the specified key.
     */
    @Nullable
    public static InfuseEffect getEffect(Key key) {
        return REGISTERED.get(key);
    }

    /**
     * Gets a registered effect.
     * 
     * @param item An item created by an effect.
     * 
     * @return The registered effect or null if the item does not come from a registered effect.
     */
    public static InfuseEffect getEffect(@Nullable ItemStack item) {
        if (item == null) return null;
        if (item.getType() != Material.POTION) return null;

        String key = item.getPersistentDataContainer().get(EFFECT_KEY, PersistentDataType.STRING);
        if (key == null) return null;

        return getEffect(parseKey(key));
    }

    /** Gets the list of registered effects. */
    @NonNull
    @Unmodifiable
    public static List<InfuseEffect> getRegisteredEffects() {
        return List.copyOf(REGISTERED.values());
    }

    private static Key parseKey(String input) {
        if (input.contains(":")) return Key.key(input);
        return Key.key("infuse", input);
    }

    public int getId() {
        return id;
    }

    public String getPlainKey() {
        return plainKey;
    }

    public Key key() {
        return Key.key(plugin, toString());
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

    public Material getBackgroundMaterial() {
        return backgroundMaterial;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof InfuseEffect effect)) return false;

        return effect.augmented == this.augmented && effect.id == this.id;
    }

    @KeyPattern.Value
    @Override
    public String toString() {
        return (augmented ? "aug_" : "") + plainKey;
    }

    public abstract void equip(Player owner);
    public abstract void unequip(Player owner);

    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated()
    public void applyPassives(Player owner) {}
    public abstract void activateSpark(Player owner, String slot);

    public abstract InfuseEffect getRegularVersion();
    public abstract InfuseEffect getAugmentedVersion();

    public abstract Message getName();
    public abstract Message getLore();

    /**
     * Calculates the character for the effect's icon based on a player's cooldown/duration.
     * 
     * @param user The player who is being shown the icon.
     * @param slot The slot the effect is equipped in.
     */
    public char getIcon(Player user, String slot) {
        UUID uuid = user.getUniqueId();

        String key = String.format("%s_%s", this.plainKey, slot);

        if (CooldownManager.isEffectActive(uuid, key)) {
            long maxDuration = plugin.getMainConfig().duration(this);
            long duration = CooldownManager.getEffectTimeLeft(uuid, key) / 1000;

            return icon(true, (float) duration / maxDuration);
        } else if (CooldownManager.isOnCooldown(uuid, key)) {
            long maxCooldown = plugin.getMainConfig().cooldown(this);
            long cooldown = CooldownManager.getCooldownTimeLeft(uuid, key) / 1000;

            return icon(false, (float) cooldown / maxCooldown);
        }

        return baseIcon();
    }

    /**
     * Calculates the character for the effect's icon.
     * 
     * @param active If true, it will apply the glowing border to the icon.
     * @param fill Between 0 and 1.  0 is no darkness, 1 is completely dark
     * 
     * @return The unicode char for the effect's icon.
     */
    public char icon(boolean active, float fill) {
        fill = Math.clamp(fill, 0, 1);

        // Getting the 
        char icon = baseIcon();

        icon += ((int) (fill * 21) << 8);
        if (active) icon += (1 << 6);

        return icon;
    }

    /**
     * Gives the base icon for an effect.
     * 
     * The icon is for an inactive effect with no cooldown.
     */
    public char baseIcon() {
        return (char) (0xe001 + (augmented ? 0x40 : 0x0) + id);
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
        item.editPersistentDataContainer(c -> c.set(EFFECT_KEY, PersistentDataType.STRING, key().toString()));

        item.setData(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay().addHiddenComponents(DataComponentTypes.POTION_CONTENTS));
        item.setData(DataComponentTypes.POTION_CONTENTS, PotionContents.potionContents().customColor(org.bukkit.Color.fromARGB(potionColor.getRGB())));

        if (augmented) {
            item.setData(DataComponentTypes.ITEM_MODEL, AUG_KEY);
        }

        return item;
    }

    @Nullable
    public ItemStack createItemWithLimits() {
        // Only regular effects should be put here
        if (isAugmented()) return null;

        // Creating the potion from the effect
        ItemStack potionItem = createItem();

        // Getting an instance of the plugin to read configs
        Infuse plugin = Infuse.getInstance();

        int augLeft = plugin.getMainConfig().getCraftLimit(getAugmentedVersion()) - plugin.getDataManager().getExistingCount(getAugmentedVersion());
        int regLeft = plugin.getMainConfig().getCraftLimit(getRegularVersion()) - plugin.getDataManager().getExistingCount(getRegularVersion());

        List<Component> lore = new ArrayList<>();
        lore.add(Message.toComponent("<gray>Augmented Limit: <aqua>" + augLeft));
        lore.add(Message.toComponent("<gray>Regular Limit: <aqua>" + regLeft));
        potionItem.setData(DataComponentTypes.LORE, ItemLore.lore(lore));

        return potionItem;
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

        return key().equals(parseKey(item.getPersistentDataContainer().get(EFFECT_KEY, PersistentDataType.STRING)));
    }
}
