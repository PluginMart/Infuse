package com.catadmirer.infuseSMP.effects;

import com.catadmirer.infuseSMP.EffectConstants;
import com.catadmirer.infuseSMP.EffectIds;
import com.catadmirer.infuseSMP.Message;
import com.catadmirer.infuseSMP.events.TenHitEvent;
import com.catadmirer.infuseSMP.managers.CooldownManager;
import com.catadmirer.infuseSMP.util.regions.RegionBlocker;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.UUID;

public class Fire extends InfuseEffect {
    public Fire() {
        this(false);
    }

    public Fire(boolean augmented) {
        super("fire", EffectIds.FIRE, augmented, EffectConstants.potionColor(EffectIds.FIRE), EffectConstants.ritualColor(EffectIds.FIRE));
    }

    @Override
    public void equip(Player owner) {
        if (RegionBlocker.getInstance().isEffectBlocked(owner, this)) return;
        owner.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, -1, 0, false, false));
    }

    @Override
    public void unequip(Player owner) {
        owner.removePotionEffect(PotionEffectType.FIRE_RESISTANCE);
    }

    @Override
    public void activateSpark(Player owner) {
        UUID playerUUID = owner.getUniqueId();

        if (CooldownManager.isOnCooldown(playerUUID, "fire")) return;
        if (!RegionBlocker.getInstance().canUseSpark(owner)) return;
        if (RegionBlocker.getInstance().isEffectBlocked(owner, this)) return;

        owner.getWorld().playSound(owner.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1, 1);

        final double radius = plugin.getMainConfig().fireSparkRadius();
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

        // Applying cooldowns and durations for the effect
        long cooldown = plugin.getMainConfig().cooldown(this);
        long duration = plugin.getMainConfig().duration(this);

        CooldownManager.setTimes(playerUUID, "fire", duration, cooldown);
    }

    @Override
    public InfuseEffect getRegularVersion() {
        return new Fire();
    }

    @Override
    public InfuseEffect getAugmentedVersion() {
        return new Fire(true);
    }

    @Override
    public Message getName() {
        return new Message(augmented ? Message.MessageType.AUG_FIRE_NAME : Message.MessageType.FIRE_NAME);
    }

    @Override
    public Message getLore() {
        return new Message(augmented ? Message.MessageType.AUG_FIRE_LORE : Message.MessageType.FIRE_LORE);
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
                        if (RegionBlocker.getInstance().isEffectBlocked(target, Fire.this)) continue;
                        target.damage(8, caster);
                    }
                }

                ++this.tick;
            }
        }).runTaskTimer(plugin, 0L, 1L);
    }

    private void startDarkRedDustEffect(final Location startLoc, Player caster) {
        final World world = startLoc.getWorld();
        final double explosionRadius = plugin.getMainConfig().fireSparkExplosionRadius();
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

        double boostStrength = plugin.getMainConfig().firePassiveWalkSpeed();
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
    public void fireCombustTarget(TenHitEvent event) {
        Player attacker = event.getAttacker();
        if (!plugin.getDataManager().hasEffect(attacker, this)) return;
        if (RegionBlocker.getInstance().isEffectBlocked(attacker, this)) return;
        if (RegionBlocker.getInstance().isEffectBlocked(event.getTarget(), this)) return;

        event.getTarget().setFireTicks(100);
    }
}