package com.catadmirer.infuseSMP.effects;

import com.catadmirer.infuseSMP.EffectConstants;
import com.catadmirer.infuseSMP.EffectIds;
import com.catadmirer.infuseSMP.Infuse;
import com.catadmirer.infuseSMP.Message;
import com.catadmirer.infuseSMP.events.EffectEquipEvent;
import com.catadmirer.infuseSMP.events.TenHitEvent;
import com.catadmirer.infuseSMP.managers.CooldownManager;
import com.catadmirer.infuseSMP.util.regions.RegionBlocker;
import com.destroystokyo.paper.MaterialSetTag;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Frost extends InfuseEffect {
    private final static Set<UUID> frozenAttackers = new HashSet<>();
    private final static Set<Location> frozenSnow = new HashSet<>();

    public Frost() {
        this(false);
    }

    public Frost(boolean augmented) {
        super("frost", EffectIds.FROST, augmented, EffectConstants.potionColor(EffectIds.FROST), EffectConstants.ritualColor(EffectIds.FROST));
    }

    @Override
    public void equip(Player owner) {
        if (RegionBlocker.getInstance().isEffectBlocked(owner, this)) return;
        changeToSnow(owner);
    }

    @Override
    public void unequip(Player owner) {}

    @Override
    public void applyPassives(Player owner) {
        if (RegionBlocker.getInstance().isEffectBlocked(owner, this)) return;

        if (!(owner.getVelocity().lengthSquared() < 0.01)) {
            if (owner.isInPowderedSnow()) {
                owner.setGliding(true);
            }

            Material blockType = owner.getLocation().subtract(0, 1, 0).getBlock().getType();
            if (MaterialSetTag.ICE.isTagged(blockType)) {
                owner.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 30, 2, false, false));
            }
        }
    }

    @Override
    public void activateSpark(Player owner) {
        UUID playerUUID = owner.getUniqueId();

        if (CooldownManager.isOnCooldown(playerUUID, "frost")) return;
        if (!RegionBlocker.getInstance().canUseSpark(owner)) return;
        if (RegionBlocker.getInstance().isEffectBlocked(owner, this)) return;


        owner.getWorld().playSound(owner.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1, 1);
        owner.addPotionEffect(new PotionEffect(PotionEffectType.UNLUCK, 300, 0));

        // Applying cooldowns and durations for the effect
        long cooldown = plugin.getMainConfig().cooldown(this);
        long duration = plugin.getMainConfig().duration(this);

        CooldownManager.setTimes(playerUUID, "frost", duration, cooldown);

        Location center = owner.getLocation();
        final double radius = plugin.getMainConfig().frostSparkRadius();
        World world = owner.getWorld();
        final Set<Player> affectedPlayers = new HashSet<>();

        for (Player player : world.getPlayers()) {
            if (player.equals(owner)) continue;
            if (plugin.getDataManager().isTrusted(player, owner)) continue;
            if (player.getLocation().distance(center) > radius) continue;
            if (!RegionBlocker.getInstance().canBeTargetedBySpark(player)) continue;
            if (RegionBlocker.getInstance().isEffectBlocked(player, Frost.this));

            affectedPlayers.add(player);
            AttributeInstance jumpAttribute = player.getAttribute(Attribute.JUMP_STRENGTH);
            if (jumpAttribute != null) {
                jumpAttribute.setBaseValue(0.1);
            }
        }

        frozenAttackers.add(owner.getUniqueId());

        new BukkitRunnable() {
            public void run() {
                for (Player player : affectedPlayers) {
                    AttributeInstance jumpAttribute = player.getAttribute(Attribute.JUMP_STRENGTH);
                    if (jumpAttribute != null) {
                        jumpAttribute.setBaseValue(0.42);
                    }
                }
                frozenAttackers.remove(owner.getUniqueId());
            }
        }.runTaskLater(plugin, duration * 20L);
    }

    @Override
    public InfuseEffect getRegularVersion() {
        return new Frost();
    }

    @Override
    public InfuseEffect getAugmentedVersion() {
        return new Frost(true);
    }

    @Override
    public Message getName() {
        return new Message(augmented ? Message.MessageType.AUG_FROST_NAME : Message.MessageType.FROST_NAME);
    }

    @Override
    public Message getLore() {
        return new Message(augmented ? Message.MessageType.AUG_FROST_LORE : Message.MessageType.FROST_LORE);
    }

    public void changeToSnow(Player player) {
        if (RegionBlocker.getInstance().isEffectBlocked(player, this)) return;

        final int frostSnowRadius = plugin.getMainConfig().frostPassiveSnowChangingRadius();
        Location center = player.getLocation();

        for (int dx = -frostSnowRadius; dx <= frostSnowRadius; dx++) {
            for (int dy = -frostSnowRadius; dy <= frostSnowRadius; dy++) {
                for (int dz = -frostSnowRadius; dz <= frostSnowRadius; dz++) {
                    // Getting the block in the radius
                    Block powderSnowBlock = center.toBlockLocation().add(dx, dy, dz).getBlock();

                    // Skipping non-powdered snow blocks
                    if (powderSnowBlock.getType() != Material.POWDER_SNOW) continue;

                    // Skipping if there is a block above this one
                    if (powderSnowBlock.getRelative(BlockFace.UP).getType() != Material.AIR) continue;

                    // Skipping if the block's location is in a blocked region.
                    if (RegionBlocker.getInstance().isEffectBlocked(powderSnowBlock.getLocation(), this)) return;

                    // Changing the block to regular snow
                    powderSnowBlock.setType(Material.SNOW_BLOCK);
                    frozenSnow.add(powderSnowBlock.getLocation());

                    Bukkit.getScheduler().runTaskTimer(plugin, task -> {
                        // Skipping if the player is too close to the block
                        if (powderSnowBlock.getLocation().distance(player.getLocation()) <= frostSnowRadius) return;
                        // Skipping if the player has broke the snow block when it has been changed
                        if (!(powderSnowBlock.getType().equals(Material.SNOW_BLOCK))) return;

                        // Resetting the block to powdered snow
                        powderSnowBlock.setType(Material.POWDER_SNOW);
                        frozenSnow.remove(powderSnowBlock.getLocation());
                        task.cancel();
                    }, 10, 10);
                }
            }
        }
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        if (!(event.getPlugin().getName().equalsIgnoreCase(plugin.getName()))) return;
        frozenSnow.forEach(snow -> snow.getBlock().setType(Material.POWDER_SNOW));
    }

    //// Listeners ////
    //// These are only registered once, so they need to be able to handle being used for every player, no matter what effects they actually have

    @EventHandler
    public void onCancelSwim(EntityToggleGlideEvent event) {
        if (event.isGliding()) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (!plugin.getDataManager().hasEffect(player, this)) return;
        if (RegionBlocker.getInstance().isEffectBlocked(player, this)) return;

        if (player.isInPowderedSnow()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getDataManager().hasEffect(player, this)) return;
        if (RegionBlocker.getInstance().isEffectBlocked(player, this)) return;

        boolean inFrost = player.getLocation().getBlock().getType() == Material.POWDER_SNOW;
        Vector direction = player.getLocation().getDirection().normalize();
        if (inFrost) {
            if (event.getFrom().distanceSquared(event.getTo()) < 0.01) return;
            final double boostStrength = plugin.getMainConfig().frostPassiveWalkSpeed();
            Vector newVelocity = direction.multiply(boostStrength);
            player.setVelocity(newVelocity);
        } else {
            changeToSnow(player);
        }
    }

    @EventHandler
    public void onPlayerInteractWithWindCharge(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getType() != Material.WIND_CHARGE) return;
        if (RegionBlocker.getInstance().isEffectBlocked(player, this)) return;
        if (player.getFreezeTicks() <= 1) return;

        event.setCancelled(true);
    }

    @EventHandler
    public void onTenthAttack(TenHitEvent event) {
        Infuse.LOGGER.debug("[Frost] Recieved TenHitEvent");
        Infuse.LOGGER.debug("[Frost] TenHitEvent Attacker: {}", event.getAttacker().getName());
        Infuse.LOGGER.debug("[Frost] TenHitEvent Target: {}", event.getTarget().getName());

        if (!plugin.getDataManager().hasEffect(event.getAttacker(), this)) return;
        if (RegionBlocker.getInstance().isEffectBlocked(event.getAttacker(), this)) return;

        Infuse.LOGGER.debug("[Frost] Attacker has frost effect");

        (new BukkitRunnable() {
            int ticksElapsed = 0;
            final int freezeDuration = 200;

            public void run() {
                if (this.ticksElapsed >= freezeDuration) {
                    event.getTarget().setFreezeTicks(0);
                    this.cancel();
                } else {
                    int currentFreezeTicks = event.getTarget().getFreezeTicks();
                    event.getTarget().setFreezeTicks(currentFreezeTicks + 2);
                    this.ticksElapsed += 2;
                }
            }
        }).runTaskTimer(plugin, 0L, 2L);
    }

    @EventHandler
    public void onPlayerJoin(EffectEquipEvent event) {
        // TODO: Give this a NamespacedKey and make it an AttributeModifier
        Player player = event.getPlayer();
        AttributeInstance jumpAttribute = player.getAttribute(Attribute.JUMP_STRENGTH);
        if (jumpAttribute != null && jumpAttribute.getBaseValue() == 0.1) {
            jumpAttribute.setBaseValue(0.42);
        }
    }

    @EventHandler
    public void onPlayerAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!attacker.hasPotionEffect(PotionEffectType.UNLUCK)) return;
        if (RegionBlocker.getInstance().isEffectBlocked(attacker, this)) return;
        PotionEffect effect = attacker.getPotionEffect(PotionEffectType.UNLUCK);
        if (effect == null) return;
        if (effect.getAmplifier() >= 0 && frozenAttackers.contains(attacker.getUniqueId()) && event.getEntity() instanceof Player target) {
            target.setFreezeTicks(200);
        }
    }
}
