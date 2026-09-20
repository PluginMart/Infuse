package com.catadmirer.infuseSMP.effects;

import com.catadmirer.infuseSMP.EffectConstants;
import com.catadmirer.infuseSMP.EffectIds;
import com.catadmirer.infuseSMP.Message;
import com.catadmirer.infuseSMP.managers.CooldownManager;
import com.catadmirer.infuseSMP.managers.ParticleManager;
import com.catadmirer.infuseSMP.util.regions.RegionBlocker;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Speed extends InfuseEffect {
    private static final Map<UUID, Integer> speedLevels = new HashMap<>();
    private static final Map<UUID, Long> lastHitTime = new HashMap<>();
    private static final Map<UUID, Long> bowPullStartTime = new HashMap<>();

    public Speed() {
        this(false);
    }

    public Speed(boolean augmented) {
        super("speed", EffectIds.SPEED, augmented, EffectConstants.potionColor(EffectIds.SPEED), EffectConstants.ritualColor(EffectIds.SPEED));
    }

    @Override
    public void equip(Player owner) {
        if (RegionBlocker.getInstance().isEffectBlocked(owner, this)) return;

        speedLevels.put(owner.getUniqueId(), 0);
        owner.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, -1, 0, false, false, false));
    }

    @Override
    public void unequip(Player owner) {
        speedLevels.remove(owner.getUniqueId());

        owner.removePotionEffect(PotionEffectType.SPEED);
    }

    @Override
    public void applyPassives(Player owner) {
        if (RegionBlocker.getInstance().isEffectBlocked(owner, this)) return;

        UUID uuid = owner.getUniqueId();
        long lastHit = lastHitTime.getOrDefault(uuid, 0L);
        if (System.currentTimeMillis() - lastHit > 1000L) {
            speedLevels.put(uuid, 0);
        }

        updateSpeedEffect(owner);
    }

    @Override
    public void activateSpark(Player owner) {
        UUID playerUUID = owner.getUniqueId();

        if (CooldownManager.isOnCooldown(playerUUID, "speed")) return;
        if (RegionBlocker.getInstance().isEffectBlocked(owner, this)) return;
        if (!RegionBlocker.getInstance().canUseSpark(owner)) return;

        owner.getWorld().playSound(owner.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1, 1);
        ParticleManager.spawnEffectCloud(owner, Color.fromRGB(0xD1A44B));
        final Vector direction = owner.getEyeLocation().getDirection().normalize();
        double playerVelocityMultiplier = plugin.getMainConfig().speedDashMultiplier();
        owner.setVelocity(direction.clone().multiply(playerVelocityMultiplier));
        final Particle.DustOptions dustOptions = new Particle.DustOptions(Color.fromRGB(0xE6DCAA), 1.5F);
        final Location[] previousLocation = new Location[]{owner.getLocation().clone()};
        final int[] ticksPassed = new int[]{0};
        final Location anchor = owner.getLocation();
        Bukkit.getRegionScheduler().runAtFixedRate(plugin, anchor, (task) -> {
            if (!owner.isOnline()) {
                task.cancel();
                return;
            }

            Location currentLocation = owner.getLocation();
            double distance = previousLocation[0].distance(currentLocation);

            if (distance > 0.1) {
                Vector step = currentLocation.toVector().subtract(previousLocation[0].toVector()).normalize().multiply(0.3);
                Location particleLocation = previousLocation[0].clone();

                for (double d = 0; d <= distance; d += step.length()) {
                    particleLocation.add(step);
                    owner.getWorld().spawnParticle(Particle.DUST, particleLocation, 5, 0.1, 0.05, 0.1, 0.05, dustOptions);
                }

                previousLocation[0] = currentLocation.clone();
            }

            if (ticksPassed[0] >= 3 && owner.isOnGround()) {
                task.cancel();
                return;
            }

            ticksPassed[0]++;
        }, 1L, 1L);

        // Applying cooldowns and durations for the effect
        long cooldown = plugin.getMainConfig().cooldown(this);
        long duration = plugin.getMainConfig().duration(this);

        CooldownManager.setTimes(playerUUID, "speed", duration, cooldown);
    }

    @Override
    public InfuseEffect getRegularVersion() {
        return new Speed();
    }

    @Override
    public InfuseEffect getAugmentedVersion() {
        return new Speed(true);
    }

    @Override
    public Message getName() {
        return new Message(augmented ? Message.MessageType.AUG_SPEED_NAME : Message.MessageType.SPEED_NAME);
    }

    @Override
    public Message getLore() {
        return new Message(augmented ? Message.MessageType.AUG_SPEED_LORE : Message.MessageType.SPEED_LORE);
    }

    public void updateSpeedEffect(Player owner) {
        if (!speedLevels.containsKey(owner.getUniqueId())) return;

        int lvl = speedLevels.get(owner.getUniqueId());
        if (lvl < 0) lvl = 0;

        owner.removePotionEffect(PotionEffectType.SPEED);
        owner.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, -1, lvl * plugin.getMainConfig().speedPlayerVelocityMultiplier(), false, false, false));
    }

    //// Listeners ////
    //// These are only registered once, so they need to be able to handle being used for every player, no matter what effects they actually have

    @EventHandler
    public void onEntityShootBow(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!plugin.getDataManager().hasEffect(player, this)) return;
        if (RegionBlocker.getInstance().isEffectBlocked(player, this)) return;

        long startTime = bowPullStartTime.getOrDefault(player.getUniqueId(), 0L);
        long pullTimeMs = System.currentTimeMillis() - startTime;
        double adjustedPullTimeMs = pullTimeMs * 1.8;
        float pullFraction = (float)Math.min(adjustedPullTimeMs / 1000, 1);
        event.getProjectile().setVelocity(event.getProjectile().getVelocity().multiply(pullFraction));

        bowPullStartTime.remove(player.getUniqueId());
    }

    @EventHandler
    public void onEntityDamageByEntityEvent(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!plugin.getDataManager().hasEffect(player, this)) return;
        if (RegionBlocker.getInstance().isEffectBlocked(player, this)) return;

        UUID uuid = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        long lastHit = lastHitTime.getOrDefault(uuid, 0L);
        if (currentTime - lastHit < 50L) return;

        lastHitTime.put(uuid, currentTime);
        speedLevels.put(uuid, speedLevels.getOrDefault(uuid, 0) + 1);
        if (event.getEntity() instanceof LivingEntity target) {
            int currentNoDamageTicks = target.getNoDamageTicks();
            target.setNoDamageTicks(currentNoDamageTicks / 2);
        }
    }
}