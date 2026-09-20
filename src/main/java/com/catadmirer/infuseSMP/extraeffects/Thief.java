package com.catadmirer.infuseSMP.extraeffects;

import com.catadmirer.infuseSMP.EffectConstants;
import com.catadmirer.infuseSMP.EffectIds;
import com.catadmirer.infuseSMP.Message;
import com.catadmirer.infuseSMP.Message.MessageType;
import com.catadmirer.infuseSMP.effects.InfuseEffect;
import com.catadmirer.infuseSMP.managers.CooldownManager;
import com.catadmirer.infuseSMP.util.regions.RegionBlocker;
import com.destroystokyo.paper.profile.PlayerProfile;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.profile.PlayerTextures;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Thief extends InfuseEffect {
    private static final Map<UUID, DisguiseData> disguisedPlayers = new HashMap<>();

    public Thief() {
        this(false);
    }

    public Thief(boolean augmented) {
        super("thief", EffectIds.THIEF, augmented, EffectConstants.potionColor(EffectIds.THIEF), EffectConstants.ritualColor(EffectIds.THIEF));
    }

    @Override
    public void equip(Player owner) {
        if (RegionBlocker.getInstance().isEffectBlocked(owner, this)) return;

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.unlistPlayer(owner);
        }
    }

    @Override
    public void unequip(Player owner) {
        if (disguisedPlayers.containsKey(owner.getUniqueId())) {
            removeDisguise(owner);
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.listPlayer(owner);
        }
    }

    @Override
    public void activateSpark(Player owner) {
        if (!RegionBlocker.getInstance().canUseSpark(owner)) return;

        UUID playerUUID = owner.getUniqueId();
        if (CooldownManager.isOnCooldown(playerUUID, "thief")) return;
        if (CooldownManager.isOnCooldown(playerUUID, "thief_stolen")) return;

        owner.playSound(owner.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1, 1);

        // Applying cooldowns and durations for the effect
        long cooldown = plugin.getMainConfig().cooldown(this);
        long duration = plugin.getMainConfig().duration(this);

        CooldownManager.setTimes(playerUUID, "thief", duration, cooldown);
    }

    @Override
    public InfuseEffect getRegularVersion() {
        return new Thief();
    }

    @Override
    public InfuseEffect getAugmentedVersion() {
        return new Thief(true);
    }

    @Override
    public Message getName() {
        return new Message(augmented ? MessageType.AUG_THIEF_NAME : MessageType.THIEF_NAME);
    }

    @Override
    public Message getLore() {
        return new Message(augmented ? MessageType.AUG_THIEF_LORE : MessageType.THIEF_LORE);
    }

    private void activateEffect(Player player, @NotNull InfuseEffect effect, Entity victim) {
        Message msg = new Message(MessageType.THIEF_STEAL);
        msg.applyPlaceholder("victim", victim.getName());
        msg.applyPlaceholder("effect_name", effect.getName());
        player.sendMessage(msg.toComponent());

        // Activating the stolen spark.
        effect.activateSpark(player);

        UUID playerUUID = player.getUniqueId();

        // Removing cooldowns from the stolen spark
        CooldownManager.clearSpecificCooldown(playerUUID, effect.getPlainKey());
        CooldownManager.clearSpecificDuration(playerUUID, effect.getPlainKey());

        // Applying cooldowns for the thief effect
        long cooldown = plugin.getMainConfig().cooldown(effect);
        long duration = plugin.getMainConfig().duration(effect);

        CooldownManager.setTimes(playerUUID, "thief_stolen", duration, cooldown * 2);
    }

    /**
     * Disguises a thief user into another player.
     * Overrides the thief user's name and skin.
     *
     * @param thiefUser The thief user to disguise
     * @param player The player to disguise the thief as
     */
    private void disguise(Player thiefUser, Player player) {
        // Storing the killer's original skin
        disguisedPlayers.put(thiefUser.getUniqueId(),
                new DisguiseData(thiefUser.customName(),
                        thiefUser.displayName(),
                        thiefUser.isCustomNameVisible(),
                        thiefUser.getPlayerProfile().getTextures()));

        // Taking the dead player's name
        thiefUser.customName(player.customName());
        thiefUser.displayName(player.displayName());
        thiefUser.setCustomNameVisible(player.isCustomNameVisible());

        // Taking the dead player's skin
        PlayerProfile profile = thiefUser.getPlayerProfile();
        profile.setTextures(player.getPlayerProfile().getTextures());
        thiefUser.setPlayerProfile(profile);

        long disguiseEndTime = System.currentTimeMillis() + 3600 * 1000; // 1 hour

        // Showing the disguise timer bossbar
        BossBar bossBar = Bukkit.createBossBar("Disguise", BarColor.PINK, BarStyle.SOLID);
        bossBar.setProgress(1);
        bossBar.addPlayer(thiefUser);

        // Starting the task to update the bossbar and eventually revert the disguise.
        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            long timeLeft = disguiseEndTime - System.currentTimeMillis();

            if (timeLeft < 0 || timeLeft / 3600.0 < 0) {
                removeDisguise(thiefUser);
                bossBar.removePlayer(thiefUser);
                task.cancel();
                return;
            }

            bossBar.setProgress(timeLeft / 3600.0);
        }, 0, 20);
    }

    /**
     * Removes a disguise from a player.
     * Sets a player's skin and name to what they were before they disguised.
     *
     * @param player The player to remove the disguise from
     */
    private void removeDisguise(Player player) {
        if (!disguisedPlayers.containsKey(player.getUniqueId())) return;

        // Getting the original data for the player
        DisguiseData originalData = disguisedPlayers.remove(player.getUniqueId());

        // Resetting the player's name
        player.customName(originalData.customName);
        player.displayName(originalData.displayName);
        player.setCustomNameVisible(originalData.customNameVisible);

        // Resetting the player's skin
        PlayerProfile profile = player.getPlayerProfile();
        profile.setTextures(originalData.skin);
        player.setPlayerProfile(profile);
    }

    private record DisguiseData(Component customName, Component displayName, boolean customNameVisible, PlayerTextures skin) {}

    //// Listeners ////
    //// These are only registered once, so they need to be able to handle being used for every player, no matter what effects they actually have

    /**
     * Hiding thief effect users from players who recently joined.
     * <br>
     * This is one of the few abilities that bypasses the WorldGuard limitations.
     */
    @EventHandler
    public void hideThievesOnJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Hiding thief users when they join
        if (plugin.getDataManager().hasEffect(player, this)) {
            Bukkit.getOnlinePlayers().forEach(p -> p.unlistPlayer(player));
        }

        // Hiding any online thief users from the player that joined.
        for (Player otherPlayer : Bukkit.getOnlinePlayers()) {
            if (!plugin.getDataManager().hasEffect(otherPlayer, this)) continue;

            player.unlistPlayer(otherPlayer);
        }
    }

    /**
     * Removes a disguised player's disguise.
     * 
     * Unaffected by WorldGuard
     * 
     * @param event A {@link PlayerDeathEvent}
     */
    @EventHandler
    public void loseDisguise(PlayerDeathEvent event) {
        removeDisguise(event.getEntity());
    }

    /**
     * Disguises a player as the person they kill.
     * 
     * @param event A {@link PlayerDeathEvent}
     */
    @EventHandler
    public void getDisguise(PlayerDeathEvent event) {
        Player deadPlayer = event.getPlayer();
        Player killer = deadPlayer.getKiller();

        if (killer == null) return;
        if (!plugin.getDataManager().hasEffect(killer, this)) return;
        if (RegionBlocker.getInstance().isEffectBlocked(killer, this)) return;
        if (RegionBlocker.getInstance().isEffectBlocked(deadPlayer, this)) return;

        disguise(killer, deadPlayer);
    }

    @EventHandler
    public void onPlayerHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player player)) return;
        if (!plugin.getDataManager().hasEffect(player, this)) return;
        if (!RegionBlocker.getInstance().canBeTargetedBySpark(victim)) return;

        UUID playerUUID = player.getUniqueId();
        if (!CooldownManager.isEffectActive(playerUUID, "thief")) return;
        if (RegionBlocker.getInstance().isEffectBlocked(player, this)) return;

        InfuseEffect leftEffect = plugin.getDataManager().getEffect(victim, "1");
        InfuseEffect rightEffect = plugin.getDataManager().getEffect(victim, "2");

        if (leftEffect != null && rightEffect != null) {
            activateEffect(player, Math.random() > 0.5 ? leftEffect : rightEffect, victim);
        } else if (leftEffect != null) {
            activateEffect(player, leftEffect, victim);
        } else if (rightEffect != null) {
            activateEffect(player, rightEffect, victim);
        } else return;

        CooldownManager.setDuration(playerUUID, "thief", 0);
    }
}
