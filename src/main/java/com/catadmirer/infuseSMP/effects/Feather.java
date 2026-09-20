package com.catadmirer.infuseSMP.effects;

import com.catadmirer.infuseSMP.EffectConstants;
import com.catadmirer.infuseSMP.EffectIds;
import com.catadmirer.infuseSMP.Message;
import com.catadmirer.infuseSMP.events.TenHitEvent;
import com.catadmirer.infuseSMP.managers.CooldownManager;
import com.catadmirer.infuseSMP.managers.ParticleManager;
import com.catadmirer.infuseSMP.util.regions.RegionBlocker;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.WindCharge;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.UUID;

public class Feather extends InfuseEffect {
    public Feather() {
        this(false);
    }

    public Feather(boolean augmented) {
        super("feather", EffectIds.FEATHER, augmented, EffectConstants.potionColor(EffectIds.FEATHER), EffectConstants.ritualColor(EffectIds.FEATHER));
    }

    @Override
    public void equip(Player owner) {}

    @Override
    public void unequip(Player owner) {}

    @Override
    public void activateSpark(Player owner) {
        UUID playerUUID = owner.getUniqueId();

        if (CooldownManager.isOnCooldown(playerUUID, "feather")) return;
        if (!RegionBlocker.getInstance().canUseSpark(owner)) return;
        if (RegionBlocker.getInstance().isEffectBlocked(owner, this)) return;

        owner.getWorld().playSound(owner.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1, 1);
        ParticleManager.spawnEffectCloud(owner, Color.fromRGB(0xBEA3CA));
        Vector dashDirection = owner.getEyeLocation().getDirection().normalize();
        Vector launchVector = dashDirection.multiply(0).setY(1);
        owner.setVelocity(launchVector);
        owner.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 20, 10));

        // Applying cooldowns and durations for the effect
        long cooldown = plugin.getMainConfig().cooldown(this);
        long duration = plugin.getMainConfig().duration(this);

        CooldownManager.setTimes(playerUUID, "feather", duration, cooldown);

        owner.getScheduler().runDelayed(plugin, t -> CooldownManager.setDuration(playerUUID, "feathermace", 5L), null, 10);
    }

    @Override
    public InfuseEffect getRegularVersion() {
        return new Feather();
    }

    @Override
    public InfuseEffect getAugmentedVersion() {
        return new Feather(true);
    }

    @Override
    public Message getName() {
        return new Message(augmented ? Message.MessageType.AUG_FEATHER_NAME : Message.MessageType.FEATHER_NAME);
    }

    @Override
    public Message getLore() {
        return new Message(augmented ? Message.MessageType.AUG_FEATHER_LORE : Message.MessageType.FEATHER_LORE);
    }

    //// Listeners ////
    //// These are only registered once, so they need to be able to handle being used for every player, no matter what effects they actually have

    @EventHandler
    public void FeatherLand(PlayerMoveEvent event) {
        final Player player = event.getPlayer();
        final double radius = plugin.getMainConfig().featherLandRadius();

        if (!player.isOnGround()) return;
        if (!CooldownManager.isEffectActive(player.getUniqueId(), "feathermace")) return;
        if (RegionBlocker.getInstance().isEffectBlocked(player, this)) return;

        CooldownManager.setDuration(player.getUniqueId(), "feathermace", 0L);
        Location loc = player.getLocation();
        World world = player.getWorld();

        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (!(entity instanceof LivingEntity target)) continue;
            if (RegionBlocker.getInstance().isEffectBlocked(target, this)) continue;
            if (!RegionBlocker.getInstance().canBeTargetedBySpark(target)) continue;
            if (target instanceof Player targetPlayer && plugin.getDataManager().isTrusted(player, targetPlayer)) continue;

            final double damage = plugin.getMainConfig().featherLandDamage();
            target.damage(damage);

            Vector knockback = new Vector(0, 1, 0);
            target.setVelocity(target.getVelocity().add(knockback));
            Location anchor = target.getLocation();
            Bukkit.getRegionScheduler().run(plugin, anchor, (task) -> {
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 80, 0, false, false, false));
            });
        }

        world.spawnParticle(Particle.CLOUD, loc, 50, 0, 0, 0, 2);
        world.playSound(loc, Sound.ITEM_MACE_SMASH_GROUND_HEAVY, 1.5F, 1);
        Location anchor = player.getLocation();
        Bukkit.getRegionScheduler().runDelayed(plugin, anchor, (task) -> {
            if (player.isOnline()) {
                Vector dashDirection = player.getEyeLocation().getDirection().normalize();
                Vector launchVector = dashDirection.multiply(5);
                player.setVelocity(launchVector);
            }
        }, 1L);
    }

    @EventHandler
    public void onTenthHit(TenHitEvent event) {
        Player player = event.getTarget();
        Player target = event.getAttacker();

        if (!plugin.getDataManager().hasEffect(player, this)) return;
        if (RegionBlocker.getInstance().isEffectBlocked(player, this)) return;

        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 100, 2));
        Location chargeLocation = player.getLocation().add(0, 1, 0);
        WindCharge windCharge = player.getWorld().spawn(chargeLocation, WindCharge.class);
        Location targetLocation = player.getLocation().subtract(0, 1, 0);
        Vector direction = targetLocation.toVector().subtract(chargeLocation.toVector()).normalize();
        windCharge.setVelocity(direction.multiply(1));
        windCharge.setShooter(player);
        player.setVelocity(new Vector(0, 0.5, 0));
    }

    @EventHandler
    public void onPlayerFallDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getCause() != DamageCause.FALL) return;
        if (!plugin.getDataManager().hasEffect(player, this)) return;
        if (RegionBlocker.getInstance().isEffectBlocked(player, this)) return;

        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerRightClickWindcharge(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getDataManager().hasEffect(player, this)) return;
        if (RegionBlocker.getInstance().isEffectBlocked(player, this)) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.WIND_CHARGE) return;
        if (player.hasCooldown(Material.WIND_CHARGE)) return;
        if (!event.getAction().isRightClick()) return;

        Location anchor = player.getLocation();
        Bukkit.getRegionScheduler().runDelayed(plugin, anchor, t -> player.setCooldown(Material.WIND_CHARGE, 5), 1L);
    }

    @EventHandler
    public void onWindChargeLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof WindCharge windCharge)) return;
        if (!(windCharge.getShooter() instanceof Player player)) return;

        Vector direction = player.getEyeLocation().getDirection().normalize().multiply(2);
        windCharge.setVelocity(direction);
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!plugin.getDataManager().hasEffect(attacker, this)) return;
        if (RegionBlocker.getInstance().isEffectBlocked(attacker, this)) return;

        double fallDistance = attacker.getFallDistance();
        if (fallDistance < 7) return;

        attacker.getWorld().playSound(attacker.getLocation(), Sound.ITEM_MACE_SMASH_AIR, 1, 1);
        Location startLoc = attacker.getLocation();
        World world = startLoc.getWorld();
        Location particleLoc = event.getDamager().getLocation();
        world.spawnParticle(Particle.GUST_EMITTER_SMALL, particleLoc, 1, 0, 0, 0, 0);
        attacker.setVelocity(new Vector(0, 1.8, 0));
        double multiplier = 1.1;
        event.setDamage(event.getDamage() * multiplier);
    }
}
