package com.catadmirer.infuseSMP.effects;

import com.catadmirer.infuseSMP.EffectConstants;
import com.catadmirer.infuseSMP.Message;
import com.catadmirer.infuseSMP.events.TenHitsGivenEvent;
import com.catadmirer.infuseSMP.managers.CooldownManager;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.Particle.DustOptions;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class Thunder extends InfuseEffect {
    public Thunder() {
        this(false);
    }

    public Thunder(boolean augmented) {
        super("thunder", EffectConstants.Id.THUNDER, augmented, EffectConstants.PotionColor.THUNDER, EffectConstants.RitualColor.THUNDER, EffectConstants.BackgroundColor.THUNDER);
    }

    @Override
    public void equip(Player owner) {}

    @Override
    public void unequip(Player owner) {}

    @Override
    public void activateSpark(Player owner, String slot) {
        UUID uuid = owner.getUniqueId();

        if (CooldownManager.isOnCooldown(uuid, plainKey + "_" + slot)) return;
        if (plugin.getRegionBlocker().isEffectBlocked(owner, this)) return;
        if (!plugin.getRegionBlocker().canUseSpark(owner)) return;

        owner.getWorld().playSound(owner.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1, 1);

        // Applying cooldowns and durations for the effect
        long cooldown = plugin.getMainConfig().cooldown(this);
        long duration = plugin.getMainConfig().duration(this);

        CooldownManager.setTimes(uuid, plainKey + "_" + slot, duration, cooldown);

        long durationTicks = duration * 20;
        World world = owner.getWorld();

        final double baseRadius = plugin.getMainConfig().thunderSparkBaseRadius();
        final double radiusBoostPerPlayer = plugin.getMainConfig().thunderSparkPerPlayerBoostRadius();
        final int maxHits = plugin.getMainConfig().thunderSparkStrikesPerPlayer();

        // Starting the lightning storm
        new BukkitRunnable() {
            int ticksElapsed = 0;
            final HashMap<Integer, Integer> hitCount = new HashMap<>();

            public void run() {
                if (this.ticksElapsed >= durationTicks) {
                    this.cancel();
                    return;
                }

                // Calculating the radius
                double radius = baseRadius;
                while (true) {
                    long nearbyPlayers = world.getNearbyEntities(owner.getLocation(), radius, radius, radius).stream().filter(p -> p instanceof Player).count();
                    double tmp = baseRadius + radiusBoostPerPlayer * nearbyPlayers;
                    if (tmp == radius) break;

                    radius = tmp;
                }

                // Striking all players within the radius
                for (Entity entity : world.getNearbyEntities(owner.getLocation(), radius, radius, radius)) {
                    if (!(entity instanceof Player target)) continue;
                    if (plugin.getDataManager().doesTrust(target, owner)) continue;
                    if (!plugin.getRegionBlocker().canBeTargetedBySpark(target)) continue;

                    // Incrementing the hit count for this entity and making sure they aren't hit too many times
                    int count = hitCount.merge(entity.getEntityId(), 1, Integer::sum);
                    if (count > maxHits) continue;

                    strikeLighting(target, owner);
                }

                this.ticksElapsed += 20;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    @Override
    public InfuseEffect getRegularVersion() {
        return new Thunder();
    }

    @Override
    public InfuseEffect getAugmentedVersion() {
        return new Thunder(true);
    }

    @Override
    public Message getName() {
        return new Message(augmented ? Message.MessageType.AUG_THUNDER_NAME : Message.MessageType.THUNDER_NAME);
    }

    @Override
    public Message getLore() {
        return new Message(augmented ? Message.MessageType.AUG_THUNDER_LORE : Message.MessageType.THUNDER_LORE);
    }

    /**
     * Custom lightning bolt for the thunder effect.
     *
     * @param target The entity to hit with a lightning bolt.
     * @param attacker The entity to attribute the damage to.
     */
    public static void strikeLighting(LivingEntity target, LivingEntity attacker) {
        target.getWorld().strikeLightningEffect(target.getLocation());
        target.damage(2, DamageSource.builder(DamageType.LIGHTNING_BOLT).withDirectEntity(attacker).build());
        target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0, new DustOptions(Color.YELLOW, 1.5F));
    }

    /**
     * Chain lightning functionality.
     * This is a recursive function that runs up to 10 times to strike nearby entities with lightning.
     * The function should be called with a list containing only the attacking entity.
     *
     * @param targets The list of targets that have been hit by the lightning bolt, except for the first entry which is the attacker.
     *
     * @throws InvalidParameterException If the <code>targets</code> parameter is null or empty.
     */
    private void chainLightning(List<Player> targets) {
        if (targets == null) throw new InvalidParameterException("targets cannot be null");
        if (targets.size() == 11) return;
        if (targets.isEmpty()) throw new InvalidParameterException("targets list needs to have the attacker in the front");

        Player attacker = targets.getFirst();
        if (plugin.getRegionBlocker().isEffectBlocked(attacker, this)) return;

        // TODO: make config
        double radius = 3;

        // Finding the next target.
        for (Entity entity : targets.getLast().getNearbyEntities(radius, radius, radius)) {
            if (!(entity instanceof Player target)) continue;
            if (targets.contains(target)) continue;
            if (plugin.getDataManager().doesTrust(attacker, target)) continue;
            if (plugin.getRegionBlocker().isEffectBlocked(entity, this)) return;

            // Target found!  Striking them then searching for the next target after 1 second.
            strikeLighting(target, attacker);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                // Adding the target to the list
                targets.add(target);

                // Recursion babyyy
                chainLightning(targets);
            }, 20L);

            return;
        }
    }

    // Listeners //
    // These are only registered once, so they need to be able to handle being used for every player, no matter what effects they actually have

    /**
     * Strikes a player with lightning and chains it
     *
     * @param event A {@link TenHitsGivenEvent}.
     */
    @EventHandler
    public void onTenHitEvent(TenHitsGivenEvent event) {
        Player attacker = event.getPlayer();
        if (!plugin.getDataManager().hasEffect(attacker, this)) return;
        if (plugin.getRegionBlocker().isEffectBlocked(attacker, this)) return;

        if (!(event.getLastTarget() instanceof Player target)) return;
        if (plugin.getRegionBlocker().isEffectBlocked(target, this)) return;

        // Striking the attacked player
        strikeLighting(target, attacker);

        // Continuing the chain after 1 second
        Bukkit.getScheduler().runTaskLater(plugin, t -> chainLightning(new ArrayList<>(List.of(attacker, target))), 20L);
    }

    @EventHandler
    public void thunderAutoChanneling(EntityDamageByEntityEvent event) {
        // Ignoring non-trident damage
        if (!(event.getDamager() instanceof Trident trident)) return;

        // Making sure the shooter has the thunder effect
        if (!(trident.getShooter() instanceof Player attacker)) return;
        if (!plugin.getDataManager().hasEffect(attacker, this)) return;
        if (plugin.getRegionBlocker().isEffectBlocked(attacker, this)) return;

        // Only summoning lightning if the target is a living entity
        if (!(event.getEntity() instanceof LivingEntity target)) return;
        if (target instanceof Player p && plugin.getDataManager().doesTrust(attacker, p)) return;
        if (plugin.getRegionBlocker().isEffectBlocked(target, this)) return;

        strikeLighting(target, attacker);
    }
}
