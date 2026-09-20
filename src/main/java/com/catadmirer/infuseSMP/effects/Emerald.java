package com.catadmirer.infuseSMP.effects;

import com.catadmirer.infuseSMP.EffectConstants;
import com.catadmirer.infuseSMP.EffectIds;
import com.catadmirer.infuseSMP.Infuse;
import com.catadmirer.infuseSMP.Message;
import com.catadmirer.infuseSMP.Message.MessageType;
import com.catadmirer.infuseSMP.events.TenHitEvent;
import com.catadmirer.infuseSMP.managers.CooldownManager;
import com.catadmirer.infuseSMP.util.ItemUtil;
import com.catadmirer.infuseSMP.util.regions.RegionBlocker;
import com.destroystokyo.paper.event.player.PlayerPickupExperienceEvent;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Enchantable;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.enchantments.CraftEnchantment;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.inventory.view.CraftEnchantmentView;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentOffer;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class Emerald extends InfuseEffect {
    public static final NamespacedKey LOOTING_KEY = new NamespacedKey("infuse", "emerald_looting");

    public Emerald() {
        this(false);
    }

    public Emerald(boolean augmented) {
        super("emerald", EffectIds.EMERALD, augmented, EffectConstants.potionColor(EffectIds.EMERALD), EffectConstants.ritualColor(EffectIds.EMERALD));
    }

    @Override
    public void equip(Player owner) {
        if (RegionBlocker.getInstance().isEffectBlocked(owner, this)) return;
        // Applying the potion effect to the player
        owner.addPotionEffect(new PotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE, -1, 0));

        if (!(ItemUtil.isSword(owner.getInventory().getItemInMainHand()))) return;
        ItemUtil.applySpecialEnchantment(owner.getInventory().getItemInMainHand(), LOOTING_KEY, Enchantment.LOOTING, plugin.getMainConfig().emeraldLootingLevel());
    }

    @Override
    public void unequip(Player owner) {
        // Removing the potion effect
        owner.removePotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE);

        // Removing enchanted items from the owner's inventory
        for (ItemStack item : owner.getInventory()) {
            if (item == null || item.getType() == Material.AIR) continue;

            ItemUtil.removeSpecialEnchant(item, Emerald.LOOTING_KEY, Enchantment.LOOTING);
        }
    }

    @Override
    public void activateSpark(Player owner) {
        UUID playerUUID = owner.getUniqueId();

        // Making sure the player isn't on cooldown
        if (CooldownManager.isOnCooldown(playerUUID, "emerald")) return;
        if (!RegionBlocker.getInstance().canUseSpark(owner)) return;
        if (RegionBlocker.getInstance().isEffectBlocked(owner, this)) return;

        // Applying effects for the emerald spark
        owner.playSound(owner.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1, 1);

        // Applying cooldowns and durations for the effect
        long cooldown = plugin.getMainConfig().cooldown(this);
        long duration = plugin.getMainConfig().duration(this);

        owner.addPotionEffect(new PotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE, (int) duration * 20, 4));

        CooldownManager.setTimes(playerUUID, "emerald", duration, cooldown);
    }

    @Override
    public InfuseEffect getRegularVersion() {
        return new Emerald();
    }

    @Override
    public InfuseEffect getAugmentedVersion() {
        return new Emerald(true);
    }

    @Override
    public Message getName() {
        return new Message(augmented ? MessageType.AUG_EMERALD_NAME : MessageType.EMERALD_NAME);
    }

    @Override
    public Message getLore() {
        return new Message(augmented ? MessageType.AUG_EMERALD_LORE : MessageType.EMERALD_LORE);
    }

    //// Listeners ////
    //// These are only registered once, so they need to be able to handle being used for every player, no matter what effects they actually have

    @EventHandler
    public void enchantHeldItem(PlayerItemHeldEvent event) {
        Infuse.LOGGER.debug("[Emerald] PlayerItemHeldEvent triggered");

        Player player = event.getPlayer();
        if (!plugin.getDataManager().hasEffect(player, this)) return;
        if (RegionBlocker.getInstance().isEffectBlocked(player, this)) return;

        Infuse.LOGGER.debug("[Emerald] PlayerItemHeldEvent is for an emerald user");

        ItemStack item = player.getInventory().getItem(event.getNewSlot());
        if (!ItemUtil.isSword(item)) return;

        Infuse.LOGGER.debug("[Emerald] Emerald user is holding a sword.  Enchanting with looting.");

        ItemUtil.applySpecialEnchantment(item, LOOTING_KEY, Enchantment.LOOTING, plugin.getMainConfig().emeraldLootingLevel());
    }

    @EventHandler
    public void removeLootingWhenStored(InventoryCloseEvent event) {
        if (event.getView().getType() == InventoryType.PLAYER) return;

        for (ItemStack item : event.getView().getTopInventory().getContents()) {
            if (item == null || item.getType() == Material.AIR) continue;

            ItemUtil.removeSpecialEnchant(item, Emerald.LOOTING_KEY, Enchantment.LOOTING);
        }
    }

    @EventHandler
    public void removeLootingWhenDropped(PlayerDropItemEvent event) {
        ItemUtil.removeSpecialEnchant(event.getItemDrop().getItemStack(), Emerald.LOOTING_KEY, Enchantment.LOOTING);
    }

    @EventHandler
    public void tenHitEvent(TenHitEvent event) {
        Infuse.LOGGER.debug("[Emerald] Received TenHitEvent");
        Infuse.LOGGER.debug("[Emerald] Attacker: {}", event.getAttacker().getName());
        Infuse.LOGGER.debug("[Emerald] Target: {}", event.getTarget().getName());

        if (!plugin.getDataManager().hasEffect(event.getTarget(), this)) return;
        if (RegionBlocker.getInstance().isEffectBlocked(event.getTarget(), this)) return;
        if (RegionBlocker.getInstance().isEffectBlocked(event.getAttacker(), this)) return;

        Infuse.LOGGER.debug("[Emerald] Target has emerald effect");
        Infuse.LOGGER.debug("[Emerald] Locking attacker's food and Exp");

        new FoodAndExpLock(plugin, event.getAttacker(), plugin.getMainConfig().emeraldLockDurationSeconds());
    }

    public static class FoodAndExpLock implements Listener {
        private final Player player;

        public FoodAndExpLock(Infuse plugin, Player player, double durationSeconds) {
            this.player = player;

            Bukkit.getPluginManager().registerEvents(this, plugin);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                HandlerList.unregisterAll(this);
                Infuse.LOGGER.debug("[Emerald] Exp lock for {} has been lifted", player.getName());
            }, (long) (durationSeconds * 20));
        }

        /** Preventing the player's food level from changing. */
        @EventHandler
        public void preventFoodChange(FoodLevelChangeEvent event) {
            if (event.getEntity().getUniqueId().equals(player.getUniqueId())) {
                event.setCancelled(true);
            }
        }

        /** Preventing the player's exp level from changing. */
        @EventHandler
        public void preventExpChange(PlayerExpChangeEvent event) {
            if (event.getPlayer().getUniqueId().equals(player.getUniqueId())) {
                event.setAmount(0);
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof FoodAndExpLock other)) return false;

            return other.player.getUniqueId().equals(player.getUniqueId());
        }
    }

    @EventHandler
    public void emeraldExpMultiplier(PlayerPickupExperienceEvent event) {
        Player player = event.getPlayer();

        if (!plugin.getDataManager().hasEffect(player, this)) return;
        if (RegionBlocker.getInstance().isEffectBlocked(player, this)) return;

        ExperienceOrb orb = event.getExperienceOrb();
        int amount = orb.getExperience();

        double multiplier = plugin.getMainConfig().emeraldMultiplierStandard();
        if (CooldownManager.isEffectActive(player.getUniqueId(), "emerald")) {
            multiplier = plugin.getMainConfig().emeraldMultiplierUseEffect();
        }

        int newAmount = (int) Math.round(amount * multiplier);
        orb.setExperience(newAmount);
    }

    @SuppressWarnings({ "UnstableApiUsage", "unchecked" })
    @EventHandler
    public void emeraldEnchantBonus(PrepareItemEnchantEvent event) {
        ItemStack item = event.getItem();

        // Skipping non-enchantable items
        if (!item.hasData(DataComponentTypes.ENCHANTABLE)) return;

        // Skipping already enchanted items
        if (!item.getEnchantments().isEmpty()) return;

        // Making sure the enchanter has the emerald effect
        Player player = event.getEnchanter();
        if (!plugin.getDataManager().hasEffect(player, this)) return;
        if (RegionBlocker.getInstance().isEffectBlocked(player, this)) return;

        EnchantmentOffer[] offers = event.getOffers();
        Random random = new Random(player.getEnchantmentSeed());

        // Calculating the costs
        for (int    k = 0; k < 3; k++) {
            int cost;

            Enchantable enchantable = item.getData(DataComponentTypes.ENCHANTABLE);
            if (enchantable == null) {
                offers[k] = null;
                continue;
            }

            int i = random.nextInt(1, 9) + 7 + random.nextInt(0, 16);

            // Calculating cose
            if (k == 0) {
                cost = Math.max(i / 3, 1);
            } else if (k == 1) {
                cost = i * 2 / 3 + 1;
            } else {
                cost = Math.max(i, 30);
            }

            if (cost < k + 1) {
                offers[k] = null;
                continue;
            }

            try {
                EnchantmentMenu menu = (EnchantmentMenu) ((CraftEnchantmentView) event.getView()).getHandle();

                Method getEnchantmentList = menu.getClass().getDeclaredMethod("getEnchantmentList", RegistryAccess.class, net.minecraft.world.item.ItemStack.class, int.class, int.class);
                getEnchantmentList.setAccessible(true);

                List<?> list = (List<?>) getEnchantmentList.invoke(menu, ((CraftWorld) player.getWorld()).getHandle().registryAccess(), CraftItemStack.asNMSCopy(item), k, cost);
                if (!list.isEmpty()) {
                    EnchantmentInstance enchantmentinstance = (EnchantmentInstance) list.get(random.nextInt(list.size()));

                    Holder<net.minecraft.world.item.enchantment.Enchantment> enchantment;
                    int level;

                    Class<EnchantmentInstance> clazz = EnchantmentInstance.class;

                    if (!clazz.isRecord()) {
                        // Handling pre-1.21.5
                        enchantment = (Holder<net.minecraft.world.item.enchantment.Enchantment>) clazz.getField("enchantment").get(enchantmentinstance);
                        level = (int) clazz.getField("level").get(enchantmentinstance);
                    } else {
                        RecordComponent[] components = clazz.getRecordComponents();
                        enchantment = (Holder<net.minecraft.world.item.enchantment.Enchantment>) components[0].getAccessor().invoke(enchantmentinstance);
                        level = (int) components[1].getAccessor().invoke(enchantmentinstance);
                    }
                    offers[k] = new EnchantmentOffer(CraftEnchantment.minecraftHolderToBukkit(enchantment), level, cost);
                }
                getEnchantmentList.setAccessible(false);
            } catch (NoSuchMethodException e) {
                Infuse.LOGGER.error("Could not find the \"getEnchantmentList\" method in the EnchantmentMenu class");
            } catch (Exception e) {
                Infuse.LOGGER.error("Error while calculating enchantments:", e);
            }
        }
    }

    @EventHandler
    public void stealExp(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player damaged)) return;
        if (!(event.getDamageSource().getCausingEntity() instanceof Player attacker)) return;
        if (!plugin.getDataManager().hasEffect(attacker, this)) return;

        if (RegionBlocker.getInstance().isEffectBlocked(attacker, this)) return;
        if (RegionBlocker.getInstance().isEffectBlocked(damaged, this)) return;

        // Getting configs
        int exp = damaged.getTotalExperience();
        int expPerHit = plugin.getMainConfig().emeraldExpPerHit();

        // Updating the xp of the players
        damaged.setTotalExperience(Math.max(exp - expPerHit, 0));

        int toGain = (int) (Math.min(expPerHit, exp) * plugin.getMainConfig().emeraldExpPercent());
        attacker.setTotalExperience(attacker.getTotalExperience() + toGain);

        // Calling the exp change event to allow for sharing if the spark is active
        new PlayerExpChangeEvent(attacker, toGain).callEvent();
    }

    @EventHandler
    public void emeraldPreserveConsumables(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();

        // Making sure the player has the emerald effect
        if (!plugin.getDataManager().hasEffect(player, this)) return;
        if (RegionBlocker.getInstance().isEffectBlocked(player, this)) return;

        ItemStack consumedItem = event.getItem();

        // Not allowing potions to be be preserved
        if (consumedItem.getType() == Material.POTION) return;

        // Getting the chance for the item to not be consumed
        double chance = 0.5;
        if (CooldownManager.isEffectActive(player.getUniqueId(), "emerald")) chance = 0.75;

        // Rolling the dice
        if (Math.random() > chance) return;

        // Refunding the item
        consumedItem.add(1);
        event.setItem(consumedItem);

        // Playing a noise
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
        player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation(), 3, 1.5, 0.5, 0.5, 0.01);
    }

    @EventHandler
    public void expShare(PlayerExpChangeEvent event) {
        Player player = event.getPlayer();
        if (!CooldownManager.isEffectActive(player.getUniqueId(), "emerald")) return;
        if (RegionBlocker.getInstance().isEffectBlocked(player, this)) return;

        for (OfflinePlayer trusted : plugin.getDataManager().getTrusted(player)) {
            Player trustedPlayer = trusted.getPlayer();

            if (trustedPlayer == null) continue;
            if (RegionBlocker.getInstance().isEffectBlocked(trustedPlayer, this)) continue;

            int toGain = (int) (event.getAmount() * plugin.getMainConfig().emeraldPercentExpToShare());
            trustedPlayer.setTotalExperience(trustedPlayer.getTotalExperience() + toGain);

            // Not calling PlayerExpChangeEvent to prevent infinite looping
        }
    }
}