package com.catadmirer.infuseSMP.effects;

import com.catadmirer.infuseSMP.EffectConstants;
import com.catadmirer.infuseSMP.Message;
import com.catadmirer.infuseSMP.managers.CooldownManager;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class Ocean extends InfuseEffect {

    public Ocean() {
        this(false);
    }

    private static final ArrayList<UUID> oceanTasks = new ArrayList<>();

    public Ocean(boolean augmented) {
        super("ocean", EffectConstants.Id.OCEAN, augmented, EffectConstants.PotionColor.OCEAN, EffectConstants.RitualColor.OCEAN, EffectConstants.BackgroundColor.OCEAN);
    }

    @Override
    public void equip(Player owner) {
        if (plugin.getRegionBlocker().isEffectBlocked(owner, this)) return;
        
        owner.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, -1, 0, false, false));
        owner.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, -1, 0, false, false));
    }

    @Override
    public void unequip(Player owner) {
        owner.removePotionEffect(PotionEffectType.WATER_BREATHING);
        owner.removePotionEffect(PotionEffectType.DOLPHINS_GRACE);
    }

    @Override
    public void applyPassives(Player owner) {
        // Checking if the owner of the effect already has the task
        if (oceanTasks.contains(owner.getUniqueId())) return;
        oceanTasks.add(owner.getUniqueId());

        Bukkit.getAsyncScheduler().runAtFixedRate(plugin, task -> {
            if (!(owner.isOnline()) || !(plugin.getDataManager().hasEffect(owner, this))) {
                oceanTasks.remove(owner.getUniqueId());
                task.cancel();
                return;
            }

            if (plugin.getRegionBlocker().isEffectBlocked(owner, this)) return;

            // Boosting the strength and damage of the passive drowning if the spark is active
            int drownStrength = plugin.getMainConfig().oceanPassiveDrownStrength();
            int drownDamage = plugin.getMainConfig().oceanPassiveDrownDamage();
            if (CooldownManager.isEffectActive(owner.getUniqueId(), "ocean"))  {
                drownStrength = plugin.getMainConfig().oceanSparkDrownStrength();
                drownDamage = plugin.getMainConfig().oceanSparkDrownDamage();
            }

            final int drownStrengthFinal = drownStrength;
            final int drownDamageFinal = drownDamage;
            final double drownRadius = plugin.getMainConfig().oceanSparkDrownRadius();

            Bukkit.getScheduler().runTask(plugin, () -> {
                for (Player otherPlayer : owner.getWorld().getPlayers()) {
                    if (otherPlayer.equals(owner)) continue;
                    if (plugin.getRegionBlocker().isEffectBlocked(otherPlayer, this)) continue;
                    if (otherPlayer.getLocation().distance(owner.getLocation()) > drownRadius) continue;

                    int newAir = Math.max(otherPlayer.getRemainingAir() - drownStrengthFinal, -20);
                    otherPlayer.setRemainingAir(newAir);
                    if (newAir <= 0) otherPlayer.damage(drownDamageFinal);
                }
            });

        }, 0L, plugin.getMainConfig().oceanSparkDrownInterval() * 50L, TimeUnit.MILLISECONDS);

    }

    @Override
    public void activateSpark(Player owner, String slot) {
        UUID playerUUID = owner.getUniqueId();

        if (CooldownManager.isOnCooldown(playerUUID, plainKey + "_" + slot)) return;
        if (!plugin.getRegionBlocker().canUseSpark(owner)) return;
        if (plugin.getRegionBlocker().isEffectBlocked(owner, Ocean.this)) return;

        owner.playSound(owner.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1, 1);

        final double radius = 5;
        final World world = owner.getWorld();
        // Applying cooldowns and durations for the effect
        long cooldown = plugin.getMainConfig().cooldown(this);
        long duration = plugin.getMainConfig().duration(this);

        CooldownManager.setTimes(playerUUID, plainKey + "_" + slot, duration, cooldown);

        final long durationTicks = duration * 20L;

        new BukkitRunnable() {
            long ticksElapsed = 0L;

            public void run() {
                if (this.ticksElapsed >= durationTicks) {
                    this.cancel();
                    return;
                }

                for (int angle = 0; angle < 360; angle += 10) {
                    double rad = Math.toRadians(angle);
                    double x = owner.getLocation().getX() + radius * Math.cos(rad);
                    double z = owner.getLocation().getZ() + radius * Math.sin(rad);
                    Location particleLoc = new Location(world, x, owner.getLocation().getY(), z);
                    world.spawnParticle(Particle.FALLING_WATER, particleLoc, 1);
                }

                this.ticksElapsed += 10L;
            }
        }.runTaskTimer(plugin, 0L, 10L);

        // Ocean pull runnable
        new BukkitRunnable() {
            @Override
            public void run() {
                // Stopping when the spark has run out
                if (!CooldownManager.isEffectActive(owner.getUniqueId(), "ocean")) {
                    cancel();
                    return;
                }

                World world = owner.getWorld();
                Location holderLoc = owner.getLocation();
                double radius = plugin.getMainConfig().oceanPullRadius();
                double strength = plugin.getMainConfig().oceanPullStrength();

                for (Player p : world.getPlayers()) {
                    if (p.equals(owner)) continue;
                    if (plugin.getTrustManager().doesTrust(owner, p)) continue;
                    if (p.getLocation().distance(holderLoc) > radius) continue;
                    if (!plugin.getRegionBlocker().canBeTargetedBySpark(p)) continue;
                    if (plugin.getRegionBlocker().isEffectBlocked(p, Ocean.this)) continue;

                    Vector direction = holderLoc.toVector().subtract(p.getLocation().toVector());
                    if (direction.lengthSquared() > 0.0001) {
                        Vector pullVector = direction.normalize().multiply(strength);
                        if (Double.isFinite(pullVector.getX()) && Double.isFinite(pullVector.getY()) && Double.isFinite(pullVector.getZ())) {
                            p.setVelocity(pullVector);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0, plugin.getMainConfig().oceanPullInterval());
    }

    @Override
    public InfuseEffect getRegularVersion() {
        return new Ocean();
    }

    @Override
    public InfuseEffect getAugmentedVersion() {
        return new Ocean(true);
    }

    @Override
    public Message getName() {
        return new Message(augmented ? Message.MessageType.AUG_OCEAN_NAME : Message.MessageType.OCEAN_NAME);
    }

    @Override
    public Message getLore() {
        return new Message(augmented ? Message.MessageType.AUG_OCEAN_LORE : Message.MessageType.OCEAN_LORE);
    }
}
