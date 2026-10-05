package com.catadmirer.infuseSMP.effects;

import com.catadmirer.infuseSMP.EffectConstants;
import com.catadmirer.infuseSMP.Message;
import com.catadmirer.infuseSMP.Message.MessageType;
import com.catadmirer.infuseSMP.events.TenHitsGivenEvent;
import com.catadmirer.infuseSMP.managers.CooldownManager;

import io.papermc.paper.event.entity.EntityEquipmentChangedEvent;
import io.papermc.paper.event.player.PlayerClientLoadedWorldEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.Particle.DustOptions;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class Invis extends InfuseEffect {
    // Map that tracks every vanished player.
    // The key is the vanished player, the value is the user who made them invisible.
    private static final Map<UUID,UUID> vanished = new HashMap<>();
    public static final MiniMessage mm = MiniMessage.miniMessage();

    public Invis() {
        this(false);
    }

    public Invis(boolean augmented) {
        super("invis", EffectConstants.Id.INVIS, augmented, EffectConstants.PotionColor.INVIS, EffectConstants.RitualColor.INVIS, EffectConstants.BackgroundColor.INVIS);
    }

    @Override
    public void equip(Player owner) {
        if (plugin.getRegionBlocker().isEffectBlocked(owner, this)) return;
        owner.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, -1, 0, false, false));
    }

    @Override
    public void unequip(Player owner) {
        owner.removePotionEffect(PotionEffectType.INVISIBILITY);
    }

    @Override
    public void activateSpark(Player owner, String slot) {
        UUID playerUUID = owner.getUniqueId();

        if (CooldownManager.isOnCooldown(playerUUID, plainKey + "_" + slot)) return;
        if (!plugin.getRegionBlocker().canUseSpark(owner)) return;
        if (plugin.getRegionBlocker().isEffectBlocked(owner, this)) return;

        owner.playSound(owner.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1, 1);

        // Applying cooldowns and durations for the effect
        long cooldown = plugin.getMainConfig().cooldown(this);
        long duration = plugin.getMainConfig().duration(this);

        CooldownManager.setTimes(playerUUID, plainKey + "_" + slot, duration, cooldown);

        final double radius = 10;
        final long durationTicks = duration * 20;
        final World world = owner.getWorld();

        vanished.put(owner.getUniqueId(), owner.getUniqueId());
        hidePlayer(owner);

        for (Entity entity : owner.getNearbyEntities(radius, radius, radius)) {
            if (!(entity instanceof final Player player)) continue;
            if (!plugin.getTrustManager().doesTrust(owner, player)) continue;
            if (plugin.getRegionBlocker().isEffectBlocked(player, this)) continue;

            vanished.put(player.getUniqueId(), owner.getUniqueId());
            hidePlayer(player);
        }

        (new BukkitRunnable() {
            long ticksElapsed = 0L;

            public void run() {
                if (this.ticksElapsed >= durationTicks) {
                    this.cancel();

                    // Unhiding players who were hidden by the person who sparked
                    vanished.entrySet().removeIf(e -> {
                        if (!e.getValue().equals(owner.getUniqueId())) return false;

                        Player player = Bukkit.getPlayer(e.getKey());
                        if (player == null || !player.isOnline()) return true;

                        showPlayer(player);
                        return true;
                    });
                } else {
                    final Location center = owner.getLocation();

                    for(int angle = 0; angle < 360; angle += 2) {
                        double rad = Math.toRadians(angle);
                        double baseX = center.getX() + radius * Math.cos(rad);
                        double baseZ = center.getZ() + radius * Math.sin(rad);
                        final DustOptions dustOptions = new DustOptions(Color.BLACK, 4);

                        for(int i = 0; i < 1; ++i) {
                            double offsetX = (Math.random() - 0.5) * 0.3;
                            double offsetZ = (Math.random() - 0.5) * 0.3;
                            Location particleLoc = new Location(world, baseX + offsetX, center.getY(), baseZ + offsetZ);
                            world.spawnParticle(Particle.DUST, particleLoc, 1, dustOptions);
                        }
                    }

                    for (Entity other : owner.getNearbyEntities(radius, radius, radius)) {
                        if (!(other instanceof final Player plr)) continue;
                        if (plugin.getTrustManager().doesTrust(owner, plr)) continue;
                        if (!plugin.getRegionBlocker().canBeTargetedBySpark(plr)) continue;
                        if (plugin.getRegionBlocker().isEffectBlocked(plr, Invis.this)) continue;

                        plr.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0, false, false));
                        plr.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 11, 0, false, false, false));
                    }

                    this.ticksElapsed += 10L;
                }
            }
        }).runTaskTimer(plugin, 0L, 10L);
    }

    @Override
    public InfuseEffect getRegularVersion() {
        return new Invis();
    }

    @Override
    public InfuseEffect getAugmentedVersion() {
        return new Invis(true);
    }

    @Override
    public Message getName() {
        return new Message(augmented ? MessageType.AUG_INVIS_NAME : MessageType.INVIS_NAME);
    }

    @Override
    public Message getLore() {
        return new Message(augmented ? MessageType.AUG_INVIS_LORE : MessageType.INVIS_LORE);
    }

    private void spawnBlackParticles(final Entity target, final int durationInSeconds) {
        (new BukkitRunnable() {
            int ticksElapsed = 0;
            final int maxTicks = durationInSeconds * 20;

            public void run() {
                if (this.ticksElapsed >= this.maxTicks) {
                    this.cancel();
                } else {
                    target.getWorld().spawnParticle(Particle.SQUID_INK, target.getLocation().add(0, 1, 0), 3, 0.5, 0.5, 0.5, 0);
                    this.ticksElapsed += 5;
                }
            }
        }).runTaskTimer(plugin, 0L, 5L);
    }

    public void hidePlayer(Player toHide) {
        for (Player other : toHide.getWorld().getPlayers()) {
            if (plugin.getTrustManager().doesTrust(toHide, other)) continue;

            other.sendEquipmentChange(toHide, EquipmentSlot.HEAD, null);
            other.sendEquipmentChange(toHide, EquipmentSlot.CHEST, null);
            other.sendEquipmentChange(toHide, EquipmentSlot.LEGS, null);
            other.sendEquipmentChange(toHide, EquipmentSlot.FEET, null);
        }
    }

    public void showPlayer(Player toShow) {
        for (Player other : toShow.getWorld().getPlayers()) {
            other.sendEquipmentChange(toShow, EquipmentSlot.HEAD, toShow.getEquipment().getItem(EquipmentSlot.HEAD));
            other.sendEquipmentChange(toShow, EquipmentSlot.CHEST, toShow.getEquipment().getItem(EquipmentSlot.CHEST));
            other.sendEquipmentChange(toShow, EquipmentSlot.LEGS, toShow.getEquipment().getItem(EquipmentSlot.LEGS));
            other.sendEquipmentChange(toShow, EquipmentSlot.FEET, toShow.getEquipment().getItem(EquipmentSlot.FEET));
        }
    }

    //// Listeners ////
    //// These are only registered once, so they need to be able to handle being used for every player, no matter what effects they actually have

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        if (killer == null) return;
        if (plugin.getRegionBlocker().isEffectBlocked(killer, this)) return;

        String victimName;
        if (plugin.getMainConfig().invisHideDeaths() && plugin.getDataManager().hasEffect(killer, this)) {
            victimName = "<gray><obf>Someone";
        } else {
            victimName = mm.serialize(victim.displayName());
        }

        String killerName;
        if (plugin.getMainConfig().invisHideKills() && plugin.getDataManager().hasEffect(killer, this)) {
            killerName = "<gray><obf>Someone";
        } else {
            killerName = mm.serialize(killer.displayName());
        }

        Message msg = new Message(MessageType.DEATH_MESSAGE);
        msg.applyPlaceholder("victim", victimName);
        msg.applyPlaceholder("killer", killerName);

        event.deathMessage(msg.toComponent());
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player shooter)) return;
        if (!plugin.getDataManager().hasEffect(shooter, this)) return;
        if (plugin.getRegionBlocker().isEffectBlocked(shooter, this)) return;
        if (!(event.getEntity() instanceof Arrow)) return;
        if (!(event.getHitEntity() instanceof Player target)) return;
        if (plugin.getRegionBlocker().isEffectBlocked(target, this)) return;

        target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 80, 0, false, false));
        this.spawnBlackParticles(target, 4);
    }

    @EventHandler
    public void onTenHits(TenHitsGivenEvent event) {
        final Player attacker = event.getPlayer();
        if (!plugin.getDataManager().hasEffect(attacker, this)) return;
        if (plugin.getRegionBlocker().isEffectBlocked(attacker, this)) return;

        final LivingEntity target = event.getLastTarget();
        if (plugin.getRegionBlocker().isEffectBlocked(target, this)) return;

        target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 80, 0, false, false));
        this.spawnBlackParticles(target, 4);
    }

    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        if (!(event.getTarget() instanceof Player target)) return;
        if (!plugin.getDataManager().hasEffect(target, this)) return;
        if (plugin.getRegionBlocker().isEffectBlocked(target, this)) return;

        event.setCancelled(true);
    }

    @EventHandler
    public void onJoin(PlayerClientLoadedWorldEvent event) {
        Player player = event.getPlayer();

        Bukkit.getScheduler().runTask(plugin, () -> {
            vanished.keySet().forEach(id -> {
                Player hidden = Bukkit.getPlayer(id);
                if (hidden == null) return;

                if (plugin.getTrustManager().doesTrust(hidden, player)) return;

                player.sendEquipmentChange(hidden, EquipmentSlot.HEAD, null);
                player.sendEquipmentChange(hidden, EquipmentSlot.CHEST, null);
                player.sendEquipmentChange(hidden, EquipmentSlot.LEGS, null);
                player.sendEquipmentChange(hidden, EquipmentSlot.FEET, null);
            });
        });
    }

    @EventHandler
    public void onEquipmentChange(EntityEquipmentChangedEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!vanished.containsKey(player.getUniqueId())) return;

        Bukkit.getScheduler().runTask(plugin, () -> hidePlayer(player));
    }
}
