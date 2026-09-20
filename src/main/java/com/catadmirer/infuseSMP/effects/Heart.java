package com.catadmirer.infuseSMP.effects;

import com.catadmirer.infuseSMP.EffectConstants;
import com.catadmirer.infuseSMP.EffectIds;
import com.catadmirer.infuseSMP.Message;
import com.catadmirer.infuseSMP.events.TenHitEvent;
import com.catadmirer.infuseSMP.managers.CooldownManager;
import com.catadmirer.infuseSMP.util.regions.RegionBlocker;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class Heart extends InfuseEffect {
    public static final NamespacedKey heartBoost = new NamespacedKey("infuse", "heart_boost");
    public static final NamespacedKey heartSparkBoost = new NamespacedKey("infuse", "heart_spark_boost");

    public Heart() {
        this(false);
    }

    public Heart(boolean augmented) {
        super("heart", EffectIds.HEART, augmented, EffectConstants.potionColor(EffectIds.HEART), EffectConstants.ritualColor(EffectIds.HEART));
    }

    @Override
    public void equip(Player owner) {
        if (RegionBlocker.getInstance().isEffectBlocked(owner, this)) return;

        AttributeInstance attribute = owner.getAttribute(Attribute.MAX_HEALTH);
        attribute.addModifier(new AttributeModifier(heartBoost, 10, Operation.ADD_NUMBER));
        owner.heal(10);
    }

    @Override
    public void unequip(Player owner) {
        AttributeInstance attribute = owner.getAttribute(Attribute.MAX_HEALTH);
        attribute.removeModifier(heartBoost);
        attribute.removeModifier(heartSparkBoost);
    }

    @Override
    public void activateSpark(Player owner) {
        UUID playerUUID = owner.getUniqueId();

        if (CooldownManager.isOnCooldown(playerUUID, "heart")) return;
        if (!RegionBlocker.getInstance().canUseSpark(owner)) return;
        if (RegionBlocker.getInstance().isEffectBlocked(owner, this)) return;

        owner.playSound(owner.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1, 1);

        AttributeInstance attribute = owner.getAttribute(Attribute.MAX_HEALTH);
        attribute.addModifier(new AttributeModifier(heartSparkBoost, 10, Operation.ADD_NUMBER));
        owner.heal(10);

        // Applying cooldowns and durations for the effect
        long cooldown = plugin.getMainConfig().cooldown(this);
        long duration = plugin.getMainConfig().duration(this);

        CooldownManager.setTimes(playerUUID, "heart", duration, cooldown);

        Bukkit.getScheduler().runTaskLater(plugin, () -> attribute.removeModifier(heartSparkBoost), duration * 20);
    }

    @Override
    public InfuseEffect getRegularVersion() {
        return new Heart();
    }

    @Override
    public InfuseEffect getAugmentedVersion() {
        return new Heart(true);
    }

    @Override
    public Message getName() {
        return new Message(augmented ? Message.MessageType.AUG_HEART_NAME : Message.MessageType.HEART_NAME);
    }

    @Override
    public Message getLore() {
        return new Message(augmented ? Message.MessageType.AUG_HEART_LORE : Message.MessageType.HEART_LORE);
    }

    private void showAndUpdateHealthAboveEntity(Entity player) {
        Location ploc = player.getLocation().add(0, 2.5, 0);

        TextDisplay as = ploc.getWorld().spawn(ploc, TextDisplay.class);

        as.setGravity(false);
        as.setCustomNameVisible(true);
        as.customName();
        updateHealthDisplay(as, (LivingEntity) player);
        player.addPassenger(as);
        final BukkitRunnable updateTask = new BukkitRunnable() {
            public void run() {
                if (!player.isDead() && player.isValid()) {
                    Heart.this.updateHealthDisplay(as, (LivingEntity) player);
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
    public void heartShowTargetHealth(TenHitEvent event) {
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
}