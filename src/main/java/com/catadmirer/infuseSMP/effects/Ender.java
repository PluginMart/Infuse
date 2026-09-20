package com.catadmirer.infuseSMP.effects;

import com.catadmirer.infuseSMP.EffectConstants;
import com.catadmirer.infuseSMP.EffectIds;
import com.catadmirer.infuseSMP.Message;
import com.catadmirer.infuseSMP.managers.CooldownManager;
import com.catadmirer.infuseSMP.util.regions.RegionBlocker;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.DragonFireball;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Ender extends InfuseEffect {
    public static final Component fireballName = Component.text("Cursing Projectile");
    public static final Set<UUID> cursedPlayers = new HashSet<>();

    public Ender() {
        this(false);
    }

    public Ender(boolean augmented) {
        super("ender", EffectIds.ENDER, augmented, EffectConstants.potionColor(EffectIds.ENDER), EffectConstants.ritualColor(EffectIds.ENDER));
    }

    public void equip(Player owner) {

    }

    public void unequip(Player owner) {

    }

    @Deprecated()
    public void applyPassives(Player owner) {
        final double radius = plugin.getMainConfig().enderPassiveRadius();

        if (RegionBlocker.getInstance().isEffectBlocked(owner, this)) return;
        Collection<Entity> nearbyEntities = owner.getWorld().getNearbyEntities(owner.getLocation(), radius, radius, radius);
        for (Entity entity : nearbyEntities) {
            if (!(entity instanceof Player nearby)) continue;
            if (nearby.getUniqueId().equals(owner.getUniqueId())) continue;
            if (plugin.getDataManager().isTrusted(nearby, owner)) continue;
            nearby.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 40, 1, false, false));
        }
    }

    public void activateSpark(Player owner) {
        UUID playerUUID = owner.getUniqueId();

        if (CooldownManager.isOnCooldown(playerUUID, "ender")) return;
        if (!RegionBlocker.getInstance().canUseSpark(owner)) return;

        // Applying cooldowns and durations for the effect
        long cooldown = plugin.getMainConfig().cooldown(this);
        long duration = plugin.getMainConfig().duration(this);

        CooldownManager.setTimes(playerUUID, "ender", duration, cooldown);

        owner.playSound(owner.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1, 1);

        // Teleporting the player in the direction they're looking
        Location startLoc = owner.getEyeLocation();
        Vector direction = startLoc.getDirection().normalize();
        final int maxDistance = plugin.getMainConfig().enderSparkMaxDistance();

        Location targetLoc = null;

        for (int i = 1; i <= maxDistance; i++) {
            Location checkLoc = startLoc.clone().add(direction.clone().multiply(i));
            if (isSafeTeleportLocation(checkLoc)) {
                targetLoc = checkLoc;
            } else {
                break;
            }
        }

        if (targetLoc != null) {
            Location finalLoc = targetLoc.clone();
            finalLoc.setYaw(owner.getLocation().getYaw());
            finalLoc.setPitch(owner.getLocation().getPitch());
            owner.teleport(finalLoc);
        }
    }

    private boolean isSafeTeleportLocation(Location loc) {
        return loc.getBlock().getType().isAir() && loc.clone().add(0, 1, 0).getBlock().getType().isAir();
    }

    public InfuseEffect getRegularVersion() {
        return new Ender();
    }

    public InfuseEffect getAugmentedVersion() {
        return new Ender(true);
    }

    @Override
    public Message getName() {
        return new Message(augmented ? Message.MessageType.AUG_ENDER_NAME : Message.MessageType.ENDER_NAME);
    }

    @Override
    public Message getLore() {
        return new Message(augmented ? Message.MessageType.AUG_ENDER_LORE : Message.MessageType.ENDER_LORE);
    }

    public void shootCursingFireball(Player player) {
        UUID uuid = player.getUniqueId();

        if (CooldownManager.isOnCooldown(player.getUniqueId(), "ender_fireball")) return;
        if (RegionBlocker.getInstance().isEffectBlocked(player, this)) return;

        ItemStack handItem = player.getInventory().getItemInMainHand();
        if (handItem.getAmount() > 1) {
            handItem.setAmount(handItem.getAmount() - 1);
        } else {
            // Is this necessary?
            player.getInventory().setItemInMainHand(null);
        }

        Fireball fireball = player.launchProjectile(DragonFireball.class);
        fireball.setIsIncendiary(false);
        fireball.customName(fireballName);

        CooldownManager.setCooldown(uuid, "ender_fireball", 30);

        Vector velocity = fireball.getVelocity();
        velocity.multiply(2.0);
        fireball.setVelocity(velocity);
    }

    public void cursePlayer(UUID playerUUID, long delayTicks) {
        cursedPlayers.add(playerUUID);

        Bukkit.getScheduler().runTaskLater(plugin, t -> cursedPlayers.remove(playerUUID), delayTicks);
    }

    //// Listeners ////
    //// These are only registered once, so they need to be able to handle being used for every player, no matter what effects they actually have

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player damagedPlayer)) return;

        // Making sure the damaged player is cursed
        UUID damagedUUID = damagedPlayer.getUniqueId();
        if (!cursedPlayers.contains(damagedUUID)) return;

        // Making sure the damage source isn't the one made by this plugin (prevents looping curse damage)
        if (event.getDamageSource().getDamageType() == DamageType.CAMPFIRE && event.getDamageSource().getDirectEntity() != null) return;
        if (RegionBlocker.getInstance().isEffectBlocked(damagedPlayer, this)) return;

        // Making the fake damageSource
        DamageSource fakeSource = DamageSource.builder(DamageType.CAMPFIRE).withDirectEntity(damagedPlayer).build();

        // Sharing curse damage with all other cursed players
        for (UUID cursedUUID : cursedPlayers) {
            // Skipping the player who was hit
            if (cursedUUID == damagedUUID) continue;

            Player player = Bukkit.getPlayer(cursedUUID);
            if (player == null) {
                cursedPlayers.remove(cursedUUID);
                continue;
            }

            if (RegionBlocker.getInstance().isEffectBlocked(player, this)) continue;

            player.damage(event.getDamage(), fakeSource);
        }
    }

    @EventHandler
    public void enderOnehitMobs(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof LivingEntity mob)) return;
        if (event.getEntity() instanceof Player) return;

        UUID attackerUUID = attacker.getUniqueId();
        if (CooldownManager.isEffectActive(attackerUUID, "ender") && !RegionBlocker.getInstance().isEffectBlocked(attacker, this)) {
            mob.setHealth(0);
        }
    }

    @EventHandler
    public void onUseDragonBreath(PlayerInteractEvent event) {
        // Listening for right clicks
        Action action = event.getAction();
        if (!(action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK)) return;

        // Making sure the player has the ender effect
        Player player = event.getPlayer();
        if (!plugin.getDataManager().hasEffect(player, this)) return;

        // Making sure the player used a bottle of dragons breath
        ItemStack item = event.getItem();
        if (item == null) return;
        if (item.getType() != Material.DRAGON_BREATH) return;

        // Making sure the cursing fireball isn't on cooldown
        if (CooldownManager.isOnCooldown(player.getUniqueId(), "ender_fireball")) return;
        if (RegionBlocker.getInstance().isEffectBlocked(player, this)) return;

        shootCursingFireball(player);
        event.setCancelled(true);
    }

    @EventHandler
    public void enderCurseHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player target)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;

        if (!plugin.getDataManager().hasEffect(attacker, this)) return;
        if (RegionBlocker.getInstance().isEffectBlocked(attacker, this)) return;

        cursePlayer(target.getUniqueId(), 1200);
    }

    @EventHandler
    public void onFireballDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof DragonFireball fireball)) return;
        if (!fireballName.equals(fireball.customName())) return;
        if (!(event.getEntity() instanceof Player target)) return;
        if (!(fireball.getShooter() instanceof Player shooter)) return;
        if (plugin.getDataManager().isTrusted(target, shooter)) return;

        if (RegionBlocker.getInstance().isEffectBlocked(shooter, this)) return;
        if (RegionBlocker.getInstance().isEffectBlocked(target, this)) return;

        cursePlayer(target.getUniqueId(), 1200);
        event.setDamage(0);
    }

    @EventHandler
    public void onFireballHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof DragonFireball fireball)) return;
        if (!fireballName.equals(fireball.customName())) return;
        if (!(event.getHitEntity() instanceof Player target)) return;
        if (!(fireball.getShooter() instanceof Player shooter)) return;
        if (plugin.getDataManager().isTrusted(target, shooter)) return;

        if (RegionBlocker.getInstance().isEffectBlocked(shooter, this)) return;
        if (RegionBlocker.getInstance().isEffectBlocked(target, this)) return;

        cursePlayer(target.getUniqueId(), 1200);
    }
}
