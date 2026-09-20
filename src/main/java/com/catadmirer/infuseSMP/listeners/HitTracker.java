package com.catadmirer.infuseSMP.listeners;

import com.catadmirer.infuseSMP.Infuse;
import com.catadmirer.infuseSMP.effects.InfuseEffect;
import com.catadmirer.infuseSMP.effects.Thunder;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import com.catadmirer.infuseSMP.events.TenHitsGivenEvent;
import com.catadmirer.infuseSMP.events.TenHitsTakenEvent;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class HitTracker implements Listener {
    private final Infuse plugin;

    private final Thunder thunder = new Thunder();

    private final Map<UUID,Integer> trackedHitsGiven = new HashMap<>();
    private final Map<UUID,Integer> trackedHitsTaken = new HashMap<>();

    private final Queue<ScheduledTask> hitsGivenDecayQueue = new ConcurrentLinkedQueue<>();
    private final Queue<ScheduledTask> hitsTakenDecayQueue = new ConcurrentLinkedQueue<>();

    public HitTracker(Infuse plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void trackHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player target)) return;

        // If the attacker isn't a living entity, don't count it as a hit
        if (!(event.getDamageSource().getCausingEntity() instanceof LivingEntity attacker)) return;

        if (event.getDamageSource().getCausingEntity() instanceof Player attackingPlayer) {
            // If the attacker is trusted, don't count it as a hit
            if (plugin.getTrustManager().doesTrust(target, attackingPlayer)) return;

            // If the attacker's cooldown is under 85%, don't count it as a hit.
            if (attackingPlayer.getAttackCooldown() < 0.85) return;
        }

        // Incrementing the hit counter
        int hits = trackedHitsTaken.merge(target.getUniqueId(), 1, Integer::sum);

        // Handling when 10 hits are reached
        if (hits == 10) {
            trackedHitsTaken.put(target.getUniqueId(), 0);

            // Canceling upcoming decay tasks
            hitsTakenDecayQueue.forEach(ScheduledTask::cancel);
            hitsTakenDecayQueue.clear();

            // Calling the TenHitsTakenEvent
            TenHitsTakenEvent e = new TenHitsTakenEvent(target, attacker);
            e.callEvent();
        } else {
            int decay = plugin.getMainConfig().hitCounterDecaySeconds();

            // Scheduling a decay task
            hitsTakenDecayQueue.add(Bukkit.getAsyncScheduler().runDelayed(plugin, t -> {
                trackedHitsTaken.computeIfPresent(target.getUniqueId(), (k, v) -> --v);
            }, decay, TimeUnit.SECONDS));
        }
    }

    @EventHandler
    public void trackAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamageSource().getCausingEntity() instanceof Player attacker)) return;

        // If the target isn't a living entity, don't count it as a hit
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        // If the attacker's cooldown is under 85%, don't count it as a hit.
        if (attacker.getAttackCooldown() < 0.85) return;

        // If the target is trusted, don't count it as a hit
        if (event.getDamageSource().getCausingEntity() instanceof Player targetPlayer && plugin.getTrustManager().doesTrust(attacker, targetPlayer)) return;

        // Skipping the hit if it was lightning from a thunder effect user.
        if (event.getDamageSource().getDamageType() == DamageType.LIGHTNING_BOLT) return;

        // Incrementing the hit counter
        int hits = trackedHitsGiven.merge(attacker.getUniqueId(), 1, Integer::sum);

        // Incrementing by 2 if the thunder effect is registered, the attacker has it, and if they are in the rain.
        if (InfuseEffect.isRegistered(thunder.key()) && plugin.getDataManager().hasEffect(attacker, thunder) && attacker.isInRain()) hits++;

        // Handling when 10 hits are reached
        if (hits >= 10) {
            trackedHitsGiven.put(attacker.getUniqueId(), 0);

            // Canceling upcoming decay tasks
            hitsGivenDecayQueue.forEach(ScheduledTask::cancel);
            hitsGivenDecayQueue.clear();

            // Calling the TenHitsTakenEvent
            TenHitsGivenEvent e = new TenHitsGivenEvent(attacker, target);
            e.callEvent();
        } else {
            int decay = plugin.getMainConfig().hitCounterDecaySeconds();

            // Scheduling a decay task
            hitsGivenDecayQueue.add(Bukkit.getAsyncScheduler().runDelayed(plugin, t -> {
                trackedHitsGiven.computeIfPresent(attacker.getUniqueId(), (k, v) -> --v);
            }, decay, TimeUnit.SECONDS));
        }
    }

    /**
     * Removes players from the hit tracker when they leave.
     *
     * @param event A {@link PlayerQuitEvent}
     */
    @EventHandler
    public void removeFromHitTracker(PlayerQuitEvent event) {
        trackedHitsGiven.remove(event.getPlayer().getUniqueId());
        trackedHitsTaken.remove(event.getPlayer().getUniqueId());
    }
}
