package com.catadmirer.infuseSMP;

import com.catadmirer.infuseSMP.effects.Heart;
import com.catadmirer.infuseSMP.effects.InfuseEffect;
import com.catadmirer.infuseSMP.extraeffects.Apophis;
import com.catadmirer.infuseSMP.managers.ParticleManager;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class GlobalLoop extends BukkitRunnable {
    private final Infuse plugin;

    private static final HashSet<UUID> lEffectDisabled = new HashSet<>();
    private static final HashSet<UUID> rEffectDisabled = new HashSet<>();

    public GlobalLoop(Infuse plugin) {
        this.plugin = plugin;
    }

    public void start() {
        this.runTaskTimer(plugin, 0, 20);
    }

    public void stop() {
        this.cancel();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            // Getting the player's equipped effects
            final InfuseEffect lEffect = plugin.getDataManager().getEffect(player.getUniqueId(), "1");
            final InfuseEffect rEffect = plugin.getDataManager().getEffect(player.getUniqueId(), "2");

            ParticleManager.spawnEffectParticles(player, lEffect);
            ParticleManager.spawnEffectParticles(player, rEffect);
            ParticleManager.spawnCursedParticles(player);

            // Applying passive effects to the player
            if (lEffect != null) {
                final boolean shouldBlock = plugin.getRegionBlocker().isEffectBlocked(player, lEffect);
                boolean isBlocked = lEffectDisabled.contains(player.getUniqueId());

                if (shouldBlock && !isBlocked) {
                    lEffect.unequip(player);
                    lEffectDisabled.add(player.getUniqueId());
                    isBlocked = true;
                } else if (!shouldBlock && isBlocked) {
                    lEffect.equip(player);
                    lEffectDisabled.remove(player.getUniqueId());
                    isBlocked = false;
                }

                if (!isBlocked) lEffect.applyPassives(player);
            }

            // Applying passive effects to the player
            if (rEffect != null) {
                final boolean shouldBlock = plugin.getRegionBlocker().isEffectBlocked(player, rEffect);
                boolean isBlocked = rEffectDisabled.contains(player.getUniqueId());
                if (shouldBlock && !isBlocked) {
                    rEffect.unequip(player);
                    rEffectDisabled.add(player.getUniqueId());
                    isBlocked = true;
                } else if (!shouldBlock && isBlocked) {
                    rEffect.equip(player);
                    rEffectDisabled.remove(player.getUniqueId());
                    isBlocked = false;
                }

                if (!isBlocked) rEffect.applyPassives(player);
            }
        }
    }
}
