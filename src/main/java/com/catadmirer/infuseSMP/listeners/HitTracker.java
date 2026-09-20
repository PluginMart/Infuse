package com.catadmirer.infuseSMP.listeners;

import com.catadmirer.infuseSMP.Infuse;
import com.catadmirer.infuseSMP.effects.InfuseEffect;
import com.catadmirer.infuseSMP.effects.Thunder;
import com.catadmirer.infuseSMP.events.TenHitEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class HitTracker implements Listener {
    private final Infuse plugin;
    private final Map<UUID,Integer> hitTracker = new HashMap<>();
    final Queue<Runnable> decayQueue = new ConcurrentLinkedQueue<>();

    public HitTracker(Infuse plugin) {
        this.plugin = plugin;
    }

    /**
     * Tracking the number of hits a player has.
     *
     * @param event A {@link EntityDamageByEntityEvent}
     */
    @EventHandler
    public void onPlayerHit(EntityDamageByEntityEvent event) {
        // Making sure the event isn't cancelled before going through with the event
        if (event.isCancelled()) return;

        // Making sure both entities are players
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof Player target)) return;

        // Skipping the hit if the attacker trusts the target
        if (plugin.getDataManager().isTrusted(attacker, target)) return;

        Infuse.LOGGER.debug("{} has hit {}", attacker.getName(), target.getName());

        // Making sure it counts as a normal hit
        // Vanilla attack cooldown needs to be at 84.8% to be a normal hit.
        if (attacker.getAttackCooldown() < 0.85) {
            Infuse.LOGGER.debug("Hit ignored due to being under attack cooldown threshold.");
            return;
        }

        // Incrementing the hit counter
        int hits = hitTracker.getOrDefault(attacker.getUniqueId(), 0) + 1;

        // Incrementing by 2 if the thunder effect is registered, the attacker has it, and if they are in the rain.
        Thunder thunder = new Thunder();
        if (InfuseEffect.isRegistered(thunder) && plugin.getDataManager().hasEffect(attacker, thunder) && attacker.isInRain()) {
            hits += 1;
        }

        Infuse.LOGGER.debug("{}'s hit counter is {}.", attacker.getName(), hits);

        if (hits >= 10) {
            // Calling the TenHitEvent
            TenHitEvent tenHit = new TenHitEvent(attacker, target);
            tenHit.callEvent();
            Infuse.LOGGER.debug("Called TenHitEvent");

            hitTracker.put(attacker.getUniqueId(), 0);

            // Removing 10 objects from the queue
            for (int i = 0; i < 10; i++) {
                if (decayQueue.isEmpty()) continue;
                decayQueue.remove();
            }
            Infuse.LOGGER.debug("Removed items from queue.");
            return;
        }

        // Saving the hit count
        hitTracker.put(attacker.getUniqueId(), hits);

        // Having the hit counter decay over time
        int hitCounterDecaySeconds = plugin.getMainConfig().hitCounterDecaySeconds();
        if (hitCounterDecaySeconds < 1) return;

        Infuse.LOGGER.debug("Adding item to decay queue");
        decayQueue.add(() -> {
            // Skipping if the attacker has left the game
            if (!attacker.isConnected()) return;

            Infuse.LOGGER.debug("Decrementing hit counter");
            int curHits = hitTracker.get(attacker.getUniqueId());

            Infuse.LOGGER.debug("{}'s hit counter is {}.", attacker.getName(), curHits - 1);
            hitTracker.put(attacker.getUniqueId(), curHits - 1);
        });

        // Running the decay task if it is still around
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Runnable decayTask = decayQueue.peek();
            if (decayTask != null) {
                decayQueue.remove();
                decayTask.run();
            }
        }, hitCounterDecaySeconds * 20L);
    }

    /**
     * Removes players from the hit tracker when they leave.
     *
     * @param event A {@link PlayerQuitEvent}
     */
    @EventHandler
    public void removeFromHitTracker(PlayerQuitEvent event) {
        hitTracker.remove(event.getPlayer().getUniqueId());
    }
}
