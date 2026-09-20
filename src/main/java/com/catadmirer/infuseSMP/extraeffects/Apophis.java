package com.catadmirer.infuseSMP.extraeffects;

import com.catadmirer.infuseSMP.EffectConstants;
import com.catadmirer.infuseSMP.EffectIds;
import com.catadmirer.infuseSMP.Infuse;
import com.catadmirer.infuseSMP.Message;
import com.catadmirer.infuseSMP.effects.Emerald.FoodAndExpLock;
import com.catadmirer.infuseSMP.effects.InfuseEffect;
import com.catadmirer.infuseSMP.events.TenHitEvent;
import com.catadmirer.infuseSMP.managers.CooldownManager;
import com.catadmirer.infuseSMP.util.ItemUtil;
import com.catadmirer.infuseSMP.util.regions.RegionBlocker;
import com.destroystokyo.paper.event.player.PlayerPickupExperienceEvent;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Enchantable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.enchantments.CraftEnchantment;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.inventory.view.CraftEnchantmentView;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentOffer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Scanner;
import java.util.UUID;

public class Apophis extends InfuseEffect {
    public static final NamespacedKey LOOTING_KEY = new NamespacedKey("infuse", "apophis_looting");
    public static final NamespacedKey APOPHIS_BOOST = new NamespacedKey("infuse", "apophis_boost");
    public static final NamespacedKey APOPHIS_SPARK_BOOST = new NamespacedKey("infuse", "apophis_spark_boost");

    private static final MiniMessage mm = MiniMessage.miniMessage();
    private static final Component APOPHIS_NAME = Component.text("Apophis", NamedTextColor.DARK_PURPLE);

    private static final ProfileProperty APOPHIS_SKIN = new ProfileProperty(
        "textures",
        "ewogICJ0aW1lc3RhbXAiIDogMTcxNzg4NTA2MDQwNywKICAicHJvZmlsZUlkIiA6ICJlZGUyYzdhMGFjNjM0MTNiYjA5ZDNmMGJlZTllYzhlYyIsCiAgInByb2ZpbGVOYW1lIiA6ICJ0aGVEZXZKYWRlIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2MwOTBmY2NjMjBmMWM3ZWMyMDBkNGVkMDUxMjQwNjM3ZmRmNjE5ZDg1Nzg0NWZhNWRmNWJkMzM1MWJiMjBkOCIKICAgIH0KICB9Cn0=",
        "mBgGwS28lqNz7rJCysD9SElJpA5q+34uTZK68JFXIFzuoN31KQg2VHjVDz+/nAr0yXdRwOrgL5rnRb2NbKBPyKSWdcB8A1nVHeNMpoJ5c5CzEERyOROUiTRxge/MIhYL7Fkj67fkh7Sc/l7BwDAf7/7OIgiAIleUTLZ9COnIN15gylTBldOo3JOka8TTNrI1i4QmnMsbgT0luQZzrUMRtZxIHNwx+26IevzCE+hpNdwiYqnDVZdayDLPVy1vv+i3C7AJGd9b7/2/qv0YmWxvT3uKrPR8+9fbSWltGx9ikrdXO17FrGc5u0gqmPWAaSSWw/NJmMhPenILh7/MvXA8mO2m7JeuhnM/EYzdOMB3qzvkUEVddFIngPl6LNE8XG1R+APFBsbpnpybB7dQphSud5DNfuZijqLDd735kykYlRMzw5VVGf7fONheLzSV42XRsIU+5IazHvmAZ4pxr72+r9bbS9vRW38ZgQIy6p8r4tLv9jfmqmcS9lEn1CAgDLAqZWGzIWeIgOdDsrWH4ia/1gj6oZVefRCr2dAS84NsOQUdoJDbS8G0+ArN+CWgnlcwOJCS6MB5kBmQl2FPvwLcSnnRcS66XKfH28Bu2/J3Hu5zRWbONuOLQTbYFxwftUtvS1IORKBCfWvlJTx5G/mz1KOGW89iOCpW8jdx8EmzpRI="
    );

    public Apophis() {
        this(false);
    }

    public Apophis(boolean augmented) {
        super("apophis", EffectIds.APOPHIS, augmented, EffectConstants.potionColor(EffectIds.APOPHIS), EffectConstants.ritualColor(EffectIds.APOPHIS));
    }

    @Override
    public void equip(Player owner) {
        if (RegionBlocker.getInstance().isEffectBlocked(owner, this)) return;

        // Applying the potion effect to the player
        owner.addPotionEffect(new PotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE, -1, 0));
        owner.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, -1, 0, false, false));

        if (ItemUtil.isSword(owner.getInventory().getItemInMainHand())) {
            ItemUtil.applySpecialEnchantment(owner.getInventory().getItemInMainHand(), LOOTING_KEY, Enchantment.LOOTING, plugin.getMainConfig().apophisLootingLevel());
        }

        AttributeInstance attribute = owner.getAttribute(Attribute.MAX_HEALTH);
        attribute.addModifier(new AttributeModifier(APOPHIS_BOOST, 10, Operation.ADD_NUMBER));
        owner.heal(10);

        // Disguise player
        disguise(owner);
    }

    @Override
    public void unequip(Player owner) {
        // Removing the potion effects
        owner.removePotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE);
        owner.removePotionEffect(PotionEffectType.FIRE_RESISTANCE);
        
        // Removing enchanted items from the owner's inventory
        for (ItemStack item : owner.getInventory()) {
            if (item == null || item.getType() == Material.AIR) continue;

            ItemUtil.removeSpecialEnchant(item, LOOTING_KEY, Enchantment.LOOTING);
        }

        AttributeInstance attribute = owner.getAttribute(Attribute.MAX_HEALTH);
        attribute.removeModifier(APOPHIS_BOOST);
        attribute.removeModifier(APOPHIS_SPARK_BOOST);

        // Removing the player's disguise
        removeDisguise(owner);
    }

    @Override
    public void activateSpark(Player owner) {
        UUID playerUUID = owner.getUniqueId();

        // Stopping if the spark is on cooldown
        if (CooldownManager.isOnCooldown(playerUUID, "apophis")) return;
        if (!RegionBlocker.getInstance().canUseSpark(owner)) return;
        if (RegionBlocker.getInstance().isEffectBlocked(owner, this)) return;

        owner.playSound(owner.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1, 1);

        // Applying cooldowns and durations for the effect
        long cooldown = plugin.getMainConfig().cooldown(this);
        long duration = plugin.getMainConfig().duration(this);

        CooldownManager.setTimes(playerUUID, "apophis", duration, cooldown);

        owner.addPotionEffect(new PotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE, (int) duration * 20, 4));

        AttributeInstance attribute = owner.getAttribute(Attribute.MAX_HEALTH);
        attribute.addModifier(new AttributeModifier(APOPHIS_SPARK_BOOST, 10, Operation.ADD_NUMBER));
        owner.heal(10);

        Bukkit.getScheduler().runTaskLater(plugin, () -> attribute.removeModifier(APOPHIS_SPARK_BOOST), duration * 20);

        final double radius = plugin.getMainConfig().apophisSparkRadius();
        for (Entity entity : owner.getNearbyEntities(radius, radius, radius)) {
            if (!(entity instanceof LivingEntity)) continue;
            if (entity == owner) continue;
            if (!RegionBlocker.getInstance().canBeTargetedBySpark(entity)) continue;
            if (RegionBlocker.getInstance().isEffectBlocked(entity, this)) continue;

            entity.setFireTicks(100);
        }

        spawnSparkEffect(owner);
        new BukkitRunnable() {
            public void run() {
                owner.getWorld().spawnParticle(Particle.EXPLOSION, owner.getLocation(), 1);
            }
        }.runTaskLater(plugin, 20L);
    }

    @Override
    public InfuseEffect getRegularVersion() {
        return new Apophis();
    }

    @Override
    public InfuseEffect getAugmentedVersion() {
        return new Apophis(true);
    }

    @Override
    public Message getName() {
        return new Message(augmented ? Message.MessageType.AUG_APOPHIS_NAME : Message.MessageType.APOPHIS_NAME);
    }

    @Override
    public Message getLore() {
        return new Message(augmented ? Message.MessageType.AUG_APOPHIS_LORE : Message.MessageType.APOPHIS_LORE);
    }

    private void spawnSparkEffect(final Player caster) {
        (new BukkitRunnable() {
            int tick = 0;

            public void run() {
                if (this.tick >= 100) {
                    startDarkRedDustEffect(caster.getLocation(), caster);
                    this.cancel();
                    return;
                }

                Location center = caster.getLocation();
                World world = center.getWorld();
                if (this.tick > 0 && this.tick % 20 == 0) {
                    world.playSound(center, Sound.ENTITY_PLAYER_HURT_ON_FIRE, 1, 1);

                    for(int angle = 0; angle < 360; angle += 20) {
                        double rad = Math.toRadians(angle);
                        double offsetX = 5 * Math.cos(rad);
                        double offsetZ = 5 * Math.sin(rad);
                        Location particleLoc = center.clone().add(offsetX, 0.1, offsetZ);
                        world.spawnParticle(Particle.LAVA, particleLoc, 10, 0.05, 0.05, 0.05, 0.01);
                    }

                    for (Player target : world.getPlayers()) {
                        if (target.equals(caster)) continue;
                        if (target.getLocation().distance(center) > 5) continue;
                        if (!RegionBlocker.getInstance().canBeTargetedBySpark(target)) continue;
                        if (RegionBlocker.getInstance().isEffectBlocked(target, Apophis.this)) continue;
                        target.damage(8, caster);
                    }
                }

                ++this.tick;
            }
        }).runTaskTimer(plugin, 0L, 1L);
    }

    private void startDarkRedDustEffect(final Location startLoc, Player caster) {
        final World world = startLoc.getWorld();
        final double explosionRadius = plugin.getMainConfig().apophisSparkExplosionRadius();
        for (Player target : world.getPlayers()) {
            if (!target.equals(caster) && target.getLocation().distance(startLoc) <= explosionRadius) {
                target.setVelocity(new Vector(0, 2, 0));
            }
        }

        world.playSound(startLoc, Sound.ENTITY_GENERIC_EXPLODE, 1, 1);
        (new BukkitRunnable() {
            int tick = 0;

            public void run() {
                if (this.tick >= 60) {
                    this.cancel();
                    return;
                }

                double circleRadius = explosionRadius + this.tick * 0.1;
                double particleHeightOffset = this.tick * 3;
                if (particleHeightOffset > 30) {
                    this.cancel();
                    return;
                }

                for(int angle = 0; angle < 360; ++angle) {
                    double rad = Math.toRadians(angle);
                    double offsetX = circleRadius * Math.cos(rad);
                    double offsetZ = circleRadius * Math.sin(rad);
                    Location particleLoc = startLoc.clone().add(offsetX, particleHeightOffset, offsetZ);
                    world.spawnParticle(Particle.DUST_PILLAR, particleLoc, 3, 0, 0, 0, 0, Material.REDSTONE_BLOCK.createBlockData());
                }

                ++this.tick;
            }
        }).runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * Creates the disguise file for a player.  If the file exists, nothing happens.
     *
     * @param owner A player to disguisr
     */
    public void initDisguise(Player owner) {
        UUID uuid = owner.getUniqueId();

        // Getting the disguise file for the player
        File disguiseFile = new File(plugin.getDataFolder(), "data/ApophisPlayers/" + uuid + ".yml");
        disguiseFile.getParentFile().mkdirs();

        // Skipping players who already have a disguise file
        if (disguiseFile.exists()) return;

        try {
            FileWriter writer = new FileWriter(disguiseFile);
            Optional<ProfileProperty> textures = owner.getPlayerProfile().getProperties().stream().filter(property -> "textures".equals(property.getName())).findFirst();

            // Writing the urls to disk
            writer.write(mm.serialize(owner.displayName()));
            writer.write("\n");
            if (textures.isEmpty()) {
                writer.write("null\nnull");
            } else {
                writer.write(textures.get().getValue());
                writer.write("\n");
                writer.write(String.valueOf(textures.get().getSignature()));
            }

            writer.flush();
            writer.close();
        } catch (IOException err) {
            Infuse.LOGGER.error("Failed to write to {}.  Make sure it can be created and edited by the user running the server.", disguiseFile.getPath());
        }
    }

    public void disguise(Player owner) {
        // Making sure the disguise file is created
        initDisguise(owner);

        // Changing the player's skin
        PlayerProfile profile = owner.getPlayerProfile();
        profile.setProperty(APOPHIS_SKIN);
        owner.setPlayerProfile(profile);

        // Hiding the player's name
        owner.displayName(APOPHIS_NAME);
        owner.playerListName(APOPHIS_NAME);
    }

    public void removeDisguise(Player owner) {
        UUID uuid = owner.getUniqueId();

        // Getting the player's skin info from the disguise file
        File disguiseFile = new File(plugin.getDataFolder(), "data/ApophisPlayers/" + uuid + ".yml");

        try (Scanner scanner = new Scanner(disguiseFile)) {
            PlayerProfile profile = owner.getPlayerProfile();
            String value = "";
            String signature = "";

            // Getting the player's name
            if (scanner.hasNextLine()) {
                String read = scanner.nextLine();
                owner.displayName(mm.deserialize(read));
                owner.playerListName(mm.deserialize(read));
            }

            // Getting the property value
            if (scanner.hasNextLine()) {
                value = scanner.nextLine();
            }

            // Getting the property signature
            if (scanner.hasNextLine()) {
                signature = scanner.nextLine();
                if (signature.equals("null")) {
                    signature = null;
                }
            }

            profile.setProperty(new ProfileProperty("textures", value, signature));

            owner.setPlayerProfile(profile);
        } catch (FileNotFoundException err) {}

        // Deleting the disguise file
        if (disguiseFile.exists()) {
            disguiseFile.delete();
        }

        return;
    }

    private void showAndUpdateHealthAboveEntity(Entity player) {
        Location ploc = player.getLocation().add(0, 2.5, 0);

        TextDisplay as = (TextDisplay) ploc.getWorld().spawn(ploc, TextDisplay.class);

        as.setGravity(false);
        as.setCustomNameVisible(true);
        as.customName();
        updateHealthDisplay(as, (LivingEntity) player);
        player.addPassenger(as);
        final BukkitRunnable updateTask = new BukkitRunnable() {
            public void run() {
                if (!player.isDead() && player.isValid()) {
                    Apophis.this.updateHealthDisplay(as, (LivingEntity) player);
                } else {
                    this.cancel();
                    as.setCustomNameVisible(false);
                    as.customName(null);
                }
            }
        };

        updateTask.runTaskTimer(plugin, 0L, 10L);
        (new BukkitRunnable() {
            public void run() {
                updateTask.cancel();
                as.setCustomNameVisible(false);
                as.customName(null);
                player.removePassenger(as);
            }
        }).runTaskLater(plugin, 200L);
    }

    private void updateHealthDisplay(TextDisplay entity, LivingEntity player) {
        if (player.hasPotionEffect(PotionEffectType.ABSORPTION)) {
            entity.customName(Message.toComponent(String.format("<yellow><b>%.1f ❤", player.getHealth()) + player.getAbsorptionAmount()));
        } else {
            entity.customName(Message.toComponent(String.format("<red><b>%.1f ❤", player.getHealth())));
        }
    }

    //// Listeners ////
    //// These are only registered once, so they need to be able to handle being used for every player, no matter what effects they actually have

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Vector direction = player.getLocation().getDirection().normalize();

        if (!player.isInLava()) return;
        if (!plugin.getDataManager().hasEffect(player, this)) return;
        if (RegionBlocker.getInstance().isEffectBlocked(player, this)) return;
        if (event.getFrom().distanceSquared(event.getTo()) < 0.01) return;

        double boostStrength = plugin.getMainConfig().apophisLavaWalkSpeed();
        Vector newVelocity = direction.multiply(boostStrength);
        player.setVelocity(newVelocity);
    }

    @EventHandler
    public void onEntityShootBow(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!plugin.getDataManager().hasEffect(player, this)) return;
        if (RegionBlocker.getInstance().isEffectBlocked(player, this)) return;

        if (event.getForce() >= 1 && event.getProjectile() instanceof Projectile projectile) {
            projectile.setFireTicks(100);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getCause() != DamageCause.FALL) return;
        if (!plugin.getDataManager().hasEffect(player, this)) return;
        if (RegionBlocker.getInstance().isEffectBlocked(player, this)) return;
        Material blockType = player.getLocation().getBlock().getType();
        if (blockType == Material.LAVA || blockType == Material.LAVA_CAULDRON) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void apophisCombustTarget(TenHitEvent event) {
        Player attacker = event.getAttacker();
        if (!plugin.getDataManager().hasEffect(attacker, this)) return;
        if (RegionBlocker.getInstance().isEffectBlocked(attacker, this)) return;
        if (RegionBlocker.getInstance().isEffectBlocked(event.getTarget(), this)) return;

        event.getTarget().setFireTicks(100);
    }

    @EventHandler
    public void apophisShowTargetHealth(TenHitEvent event) {
        Player attacker = event.getAttacker();
        if (!plugin.getDataManager().hasEffect(attacker, this)) return;
        if (RegionBlocker.getInstance().isEffectBlocked(attacker, this)) return;

        this.showAndUpdateHealthAboveEntity(event.getTarget());
    }

    @EventHandler
    public void onPlayerEat(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getDataManager().hasEffect(player, this)) return;
        if (RegionBlocker.getInstance().isEffectBlocked(player, this)) return;

        ItemStack item = event.getItem();
        if (item.getType() == Material.ENCHANTED_GOLDEN_APPLE) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 2400, 4));
        } else {
            player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 600, 0));
        }
    }

    @EventHandler
    public void enchantHeldItem(PlayerItemHeldEvent event) {
        Infuse.LOGGER.debug("[Apophis] PlayerItemHeldEvent triggered");

        Player player = event.getPlayer();
        if (!plugin.getDataManager().hasEffect(player, this)) return;
        if (RegionBlocker.getInstance().isEffectBlocked(player, this)) return;

        Infuse.LOGGER.debug("[Apophis] PlayerItemHeldEvent is for an apophis user");

        ItemStack item = player.getInventory().getItem(event.getNewSlot());
        if (!ItemUtil.isSword(item)) return;

        Infuse.LOGGER.debug("[Apophis] Apophis user is holding a sword.  Enchanting with looting.");

        ItemUtil.applySpecialEnchantment(item, LOOTING_KEY, Enchantment.LOOTING, plugin.getMainConfig().apophisLootingLevel());
    }

    @EventHandler
    public void removeLootingWhenStored(InventoryCloseEvent event) {
        if (event.getView().getType() == InventoryType.PLAYER) return;

        for (ItemStack item : event.getView().getTopInventory().getContents()) {
            if (item == null || item.getType() == Material.AIR) continue;

            ItemUtil.removeSpecialEnchant(item, LOOTING_KEY, Enchantment.LOOTING);
        }
    }

    @EventHandler
    public void removeLootingWhenDropped(PlayerDropItemEvent event) {
        ItemUtil.removeSpecialEnchant(event.getItemDrop().getItemStack(), LOOTING_KEY, Enchantment.LOOTING);
    }

    @EventHandler
    public void tenHitEvent(TenHitEvent event) {
        Infuse.LOGGER.debug("[Apophis] Received TenHitEvent");
        Infuse.LOGGER.debug("[Apophis] Attacker: {}", event.getAttacker().getName());
        Infuse.LOGGER.debug("[Apophis] Target: {}", event.getTarget().getName());

        if (!plugin.getDataManager().hasEffect(event.getTarget(), this)) return;
        if (RegionBlocker.getInstance().isEffectBlocked(event.getTarget(), this)) return;
        if (RegionBlocker.getInstance().isEffectBlocked(event.getAttacker(), this)) return;

        Infuse.LOGGER.debug("[Apophis] Target has apophis effect");
        Infuse.LOGGER.debug("[Apophis] Locking attacker's food and Exp");

        new FoodAndExpLock(plugin, event.getAttacker(), plugin.getMainConfig().apophisLockDurationSeconds());
    }

    @EventHandler
    public void apophisExpMultiplier(PlayerPickupExperienceEvent event) {
        Player player = event.getPlayer();

        if (!plugin.getDataManager().hasEffect(player, this)) return;
        if (RegionBlocker.getInstance().isEffectBlocked(player, this)) return;

        ExperienceOrb orb = event.getExperienceOrb();
        int amount = orb.getExperience();

        double multiplier = plugin.getMainConfig().apophisXpMultiplierStandard();
        if (CooldownManager.isEffectActive(player.getUniqueId(), getPlainKey())) {
            multiplier = plugin.getMainConfig().apophisXpMultiplierSpark();
        }

        int newAmount = (int) Math.round(amount * multiplier);
        orb.setExperience(newAmount);
    }

    @SuppressWarnings({ "UnstableApiUsage", "unchecked" })
    @EventHandler
    public void apophisEnchantBonus(PrepareItemEnchantEvent event) {
        ItemStack item = event.getItem();

        // Skipping non-enchantable items
        if (!item.hasData(DataComponentTypes.ENCHANTABLE)) return;

        // Skipping already enchanted items
        if (!item.getEnchantments().isEmpty()) return;

        // Making sure the enchanter has the apophis effect
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

                    Holder<net.minecraft.world.item.enchantment.Enchantment> enchantment = null;
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
        int expPerHit = plugin.getMainConfig().apophisExpPerHit();

        // Updating the xp of the players
        damaged.setTotalExperience(Math.max(exp - expPerHit, 0));

        int toGain = (int) (Math.min(expPerHit, exp) * plugin.getMainConfig().apophisExpPercent());
        attacker.setTotalExperience(attacker.getTotalExperience() + toGain);

        // Calling the exp change event to allow for sharing if the spark is active
        new PlayerExpChangeEvent(attacker, toGain).callEvent();
    }

    @EventHandler
    public void apophisPreserveConsumables(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();

        // Making sure the player has the apophis effect
        if (!plugin.getDataManager().hasEffect(player, this)) return;
        if (RegionBlocker.getInstance().isEffectBlocked(player, this)) return;

        ItemStack consumedItem = event.getItem();

        // Not allowing potions to be be preserved
        if (consumedItem.getType() == Material.POTION) return;

        // Getting the chance for the item to not be consumed
        double chance = 0.5;
        if (CooldownManager.isEffectActive(player.getUniqueId(), getPlainKey())) chance = 0.75;

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
        if (!CooldownManager.isEffectActive(player.getUniqueId(), getPlainKey())) return;
        if (RegionBlocker.getInstance().isEffectBlocked(player, this)) return;

        for (OfflinePlayer trusted : plugin.getDataManager().getTrusted(player)) {
            Player trustedPlayer = trusted.getPlayer();

            if (trustedPlayer == null) continue;
            if (RegionBlocker.getInstance().isEffectBlocked(trustedPlayer, this)) continue;

            int toGain = (int) (event.getAmount() * plugin.getMainConfig().apophisPercentExpToShare());
            trustedPlayer.setTotalExperience(trustedPlayer.getTotalExperience() + toGain);

            // Not calling PlayerExpChangeEvent to prevent infinite looping
        }
    }

    @EventHandler
    public void onPlayerHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        UUID attackerUUID = attacker.getUniqueId();

        if (event.getEntity() instanceof Player target) {
            if (CooldownManager.isEffectActive(attackerUUID, "apophis")) {
                target.showTitle(Title.title(Component.text("\uE090"), Component.empty(), Title.Times.times(Duration.ZERO, Duration.ofSeconds(3), Duration.ZERO)));
            }
        }
    }
}
