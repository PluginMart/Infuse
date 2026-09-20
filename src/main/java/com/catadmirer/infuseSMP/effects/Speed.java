package com.catadmirer.infuseSMP.effects;

import com.catadmirer.infuseSMP.EffectConstants;
import com.catadmirer.infuseSMP.Message;
import com.catadmirer.infuseSMP.managers.CooldownManager;
import com.catadmirer.infuseSMP.managers.ParticleManager;

import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerMoveEvent;
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
        super("speed", EffectConstants.Id.SPEED, augmented, EffectConstants.PotionColor.SPEED, EffectConstants.RitualColor.SPEED, EffectConstants.BackgroundColor.SPEED);
    }

    @Override
    public void equip(Player owner) {
        if (plugin.getRegionBlocker().isEffectBlocked(owner, this)) return;

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
        if (plugin.getRegionBlocker().isEffectBlocked(owner, this)) return;

        UUID uuid = owner.getUniqueId();
        long lastHit = lastHitTime.getOrDefault(uuid, 0L);
        if (System.currentTimeMillis() - lastHit > 1000L) {
            speedLevels.put(uuid, 0);
        }

        updateSpeedEffect(owner);
    }

    @Override
    public void activateSpark(Player owner, String slot) {
        UUID playerUUID = owner.getUniqueId();

        if (CooldownManager.isOnCooldown(playerUUID, plainKey + "_" + slot)) return;
        if (plugin.getRegionBlocker().isEffectBlocked(owner, this)) return;
        if (!plugin.getRegionBlocker().canUseSpark(owner)) return;

        owner.getWorld().playSound(owner.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1, 1);
        ParticleManager.spawnEffectCloud(owner, Color.fromRGB(0xD1A44B));

        // Maximum angle of -15 degrees (90 is straight down, -90 straight up)
        double rotY = Math.min(owner.getPitch(), -15);
        double rotX = owner.getYaw();
        double xz = Math.cos(Math.toRadians(rotY));

        Vector boost = new Vector(-xz * Math.sin(Math.toRadians(rotX)), -Math.sin(Math.toRadians(rotY)), xz * Math.cos(Math.toRadians(rotX)));
        boost.multiply(plugin.getMainConfig().speedDashMultiplier());
        owner.setVelocity(owner.getVelocity().add(boost));

        // Applying cooldowns and durations for the effect
        long cooldown = plugin.getMainConfig().cooldown(this);
        long duration = plugin.getMainConfig().duration(this);

        CooldownManager.setTimes(playerUUID, plainKey + "_" + slot, duration, cooldown);
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

    @Override
    public char getIcon(Player user, String slot) {
        UUID uuid = user.getUniqueId();

        String key = String.format("%s_%s", this.plainKey, slot);

        // The speed effect doesn't have a great way to track duration
        if (CooldownManager.isEffectActive(uuid, key)) {
            return icon(true, 0);
        }

        return super.getIcon(user, slot);
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
        if (plugin.getRegionBlocker().isEffectBlocked(player, this)) return;

        long startTime = bowPullStartTime.getOrDefault(player.getUniqueId(), 0L);
        long pullTimeMs = System.currentTimeMillis() - startTime;
        double adjustedPullTimeMs = pullTimeMs * 1.8;
        float pullFraction = (float)Math.min(adjustedPullTimeMs / 1000, 1);
        event.getProjectile().setVelocity(event.getProjectile().getVelocity().multiply(pullFraction));

        bowPullStartTime.remove(player.getUniqueId());
    }

    @EventHandler
    public void sparkMovingParticles(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (!plugin.getDataManager().hasEffect(player, this)) return;
        if (!CooldownManager.isEffectActive(player.getUniqueId(), "speed")) return;

        final Particle.DustOptions dustOptions = new Particle.DustOptions(Color.fromRGB(0xE6DCAA), 1.5F);
        player.getWorld().spawnParticle(Particle.DUST, event.getFrom(), 5, 0.1, 0.05, 0.1, 0.05, dustOptions);
    }

    @EventHandler
    public void onEntityDamageByEntityEvent(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!plugin.getDataManager().hasEffect(player, this)) return;
        if (plugin.getRegionBlocker().isEffectBlocked(player, this)) return;

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