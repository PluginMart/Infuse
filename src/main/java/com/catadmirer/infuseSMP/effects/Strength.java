package com.catadmirer.infuseSMP.effects;

import com.catadmirer.infuseSMP.EffectConstants;
import com.catadmirer.infuseSMP.Message;
import com.catadmirer.infuseSMP.events.TenHitsGivenEvent;
import com.catadmirer.infuseSMP.managers.CooldownManager;
import com.catadmirer.infuseSMP.util.ItemUtil;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;

import java.util.UUID;

public class Strength extends InfuseEffect {
    public Strength() {
        this(false);
    }

    public Strength(boolean augmented) {
        super("strength", EffectConstants.Id.STRENGTH, augmented, EffectConstants.PotionColor.STRENGTH, EffectConstants.RitualColor.STRENGTH, EffectConstants.BackgroundColor.STRENGTH);
    }

    @Override
    public void equip(Player owner) {}

    @Override
    public void unequip(Player owner) {}

    @Override
    public void activateSpark(Player owner, String slot) {
        UUID uuid = owner.getUniqueId();

        // Skipping players on cooldown
        if (CooldownManager.isOnCooldown(uuid, plainKey + "_" + slot)) return;
        if (plugin.getRegionBlocker().isEffectBlocked(owner, this)) return;
        if (!plugin.getRegionBlocker().canUseSpark(owner)) return;

        // Playing sounds
        owner.getWorld().playSound(owner.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1, 1);

        // Applying cooldowns and durations for the effect
        long cooldown = plugin.getMainConfig().cooldown(this);
        long duration = plugin.getMainConfig().duration(this);

        CooldownManager.setTimes(uuid, plainKey + "_" + slot, duration, cooldown);
    }

    @Override
    public InfuseEffect getRegularVersion() {
        return new Strength();
    }

    @Override
    public InfuseEffect getAugmentedVersion() {
        return new Strength(true);
    }

    @Override
    public Message getName() {
        return new Message(augmented ? Message.MessageType.AUG_STRENGTH_NAME : Message.MessageType.STRENGTH_NAME);
    }

    @Override
    public Message getLore() {
        return new Message(augmented ? Message.MessageType.AUG_STRENGTH_LORE : Message.MessageType.STRENGTH_LORE);
    }

    //// Listeners ////
    //// These are only registered once, so they need to be able to handle being used for every player, no matter what effects they actually have

    @EventHandler
    public void extraDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!plugin.getDataManager().hasEffect(attacker, this)) return;
        if (plugin.getRegionBlocker().isEffectBlocked(attacker, this)) return;

        // Damage boost
        double damage = event.getDamage();
        damage += (attacker.getAttribute(Attribute.MAX_HEALTH).getValue() - attacker.getHealth()) * 0.3;

        // Spark auto-crit
        if (!event.isCritical() && CooldownManager.isEffectActive(attacker.getUniqueId(), "strength") && !plugin.getRegionBlocker().isEffectBlocked(event.getEntity(), this)) {
            // crit dmg boost
            damage *= 1.35;

            // Playing the crit noise and spawning crit particles.
            Entity hitEntity = event.getEntity();
            hitEntity.getWorld().playSound(hitEntity.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1, 1);
            hitEntity.getWorld().spawnParticle(Particle.CRIT, hitEntity.getLocation().add(0, hitEntity.getHeight() / 2, 0), 10);
        }

        // non-player target double dmg
        if (!(event.getEntity() instanceof Player)) damage *= 2;

        // Storing the adjusted damage
        event.setDamage(damage);
    }

    @EventHandler
    public void handleShieldDisable(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!plugin.getDataManager().hasEffect(attacker, this)) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (!player.isBlocking()) return;
        if (!ItemUtil.isAxe(attacker.getInventory().getItemInMainHand())) return;

        // Cancelling the event
        event.setCancelled(true);

        // Playing noise and updating the shield
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_SHIELD_BREAK, 1, 1);
        player.setCooldown(Material.SHIELD, 200);
        player.getInventory().getItemInMainHand().damage(20, attacker);
        player.clearActiveItem();

        // Halving the damage
        player.damage(event.getDamage() / 2, event.getDamageSource());
    }

    @EventHandler
    public void preventExtraShieldDamage(PlayerItemDamageEvent event) {
        // The only time a strength user damages a shield is when they are attacking someone using a shield.
        // This prevents strength users from damaging a shield unless it is exactly 20 damage, which is how much damage they are meant to do when disabling a shield.
        // You also don't have to worry about the damage the shield would normally recieve because the original damage event is not only cancelled, but the shield is lowered too.
        if (event.getItem().getType() != Material.SHIELD) return;
        if (!plugin.getDataManager().hasEffect(event.getPlayer(), this)) return;
        if (event.getDamage() == 20) return;

        event.setCancelled(true);
    }

    /** Boosts the piercing level of any arrow to 1 for players with the strength effect. */
    @EventHandler
    public void strengthHighPiercing(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        // Making sure the shooter has the strength effect
        if (!plugin.getDataManager().hasEffect(player, this)) return;
        if (plugin.getRegionBlocker().isEffectBlocked(player, this)) return;

        // Increasing the piercing level of the shot arrow.
        if (event.getProjectile() instanceof Arrow arrow) {
            arrow.setPierceLevel(1);
        }
    }

    @EventHandler
    public void strengthTenHitEvent(TenHitsGivenEvent event) {
        Player attacker = event.getPlayer();

        if (!plugin.getDataManager().hasEffect(attacker, this)) return;
        if (plugin.getRegionBlocker().isEffectBlocked(attacker, this)) return;

        // TODO: Reveal armor durability
    }
}
