package com.catadmirer.infuseSMP.effects;

import com.catadmirer.infuseSMP.EffectConstants;
import com.catadmirer.infuseSMP.EffectIds;
import com.catadmirer.infuseSMP.Message;
import com.catadmirer.infuseSMP.managers.CooldownManager;
import com.catadmirer.infuseSMP.util.regions.RegionBlocker;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.UUID;

public class Ocean extends InfuseEffect {
    public Ocean() {
        this(false);
    }

    public Ocean(boolean augmented) {
        super("ocean", EffectIds.OCEAN, augmented, EffectConstants.potionColor(EffectIds.OCEAN), EffectConstants.ritualColor(EffectIds.OCEAN));
    }

    @Override
    public void equip(Player owner) {
        if (RegionBlocker.getInstance().isEffectBlocked(owner, this)) return;
        
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
        // Boosting the strength and damage of the passive drowning if the spark is active
        if (RegionBlocker.getInstance().isEffectBlocked(owner, this)) return;

        int drownStrength = plugin.getMainConfig().oceanPassiveDrownStrength();
        int drownDamage = plugin.getMainConfig().oceanPassiveDrownDamage();
        if (CooldownManager.isEffectActive(owner.getUniqueId(), "ocean"))  {
            drownStrength = plugin.getMainConfig().oceanSparkDrownStrength();
            drownDamage = plugin.getMainConfig().oceanSparkDrownDamage();
        }

        // TODO: Make this use packets for air bubbles
        for (Player otherPlayer : owner.getWorld().getPlayers()) {
            if (otherPlayer.equals(owner)) continue;
            if (RegionBlocker.getInstance().isEffectBlocked(otherPlayer, this)) continue;
            if (otherPlayer.getLocation().distance(owner.getLocation()) > 5) continue;

            int newAir = Math.max(otherPlayer.getRemainingAir() - drownStrength, -20);
            otherPlayer.setRemainingAir(newAir);
            if (newAir <= 0) {
                otherPlayer.damage(drownDamage);
            }
        }
    }

    @Override
    public void activateSpark(Player caster) {
        UUID playerUUID = caster.getUniqueId();

        if (CooldownManager.isOnCooldown(playerUUID, "ocean")) return;
        if (!RegionBlocker.getInstance().canUseSpark(caster)) return;
        if (RegionBlocker.getInstance().isEffectBlocked(caster, Ocean.this)) return;

        caster.playSound(caster.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1, 1);

        final double radius = 5;
        final World world = caster.getWorld();
        // Applying cooldowns and durations for the effect
        long cooldown = plugin.getMainConfig().cooldown(this);
        long duration = plugin.getMainConfig().duration(this);

        CooldownManager.setTimes(playerUUID, "ocean", duration, cooldown);

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
                    double x = caster.getLocation().getX() + radius * Math.cos(rad);
                    double z = caster.getLocation().getZ() + radius * Math.sin(rad);
                    Location particleLoc = new Location(world, x, caster.getLocation().getY(), z);
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
                if (!CooldownManager.isEffectActive(caster.getUniqueId(), "ocean")) {
                    cancel();
                    return;
                }

                World world = caster.getWorld();
                Location holderLoc = caster.getLocation();
                double radius = plugin.getMainConfig().oceanPullRadius();
                double strength = plugin.getMainConfig().oceanPullStrength();

                for (Player p : world.getPlayers()) {
                    if (p.equals(caster)) continue;
                    if (plugin.getDataManager().isTrusted(caster, p)) continue;
                    if (p.getLocation().distance(holderLoc) > radius) continue;
                    if (!RegionBlocker.getInstance().canBeTargetedBySpark(p)) continue;
                    if (RegionBlocker.getInstance().isEffectBlocked(p, Ocean.this)) continue;

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
