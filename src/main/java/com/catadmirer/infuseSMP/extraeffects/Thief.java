package com.catadmirer.infuseSMP.extraeffects;

import com.catadmirer.infuseSMP.EffectConstants;
import com.catadmirer.infuseSMP.effects.InfuseEffect;
import com.catadmirer.infuseSMP.Infuse;
import com.catadmirer.infuseSMP.Message;
import com.catadmirer.infuseSMP.Message.MessageType;
import com.catadmirer.infuseSMP.managers.CooldownManager;
import com.destroystokyo.paper.profile.PlayerProfile;
import net.kyori.adventure.key.Key;
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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class Thief extends InfuseEffect {
    private static final Map<UUID, DisguiseData> disguisedPlayers = new HashMap<>();

    public Thief() {
        this(false);
    }

    public Thief(boolean augmented) {
        super("thief", EffectConstants.Id.THIEF, augmented, EffectConstants.PotionColor.THIEF, EffectConstants.RitualColor.THIEF, EffectConstants.BackgroundColor.THIEF);
    }

    @Override
    public void equip(Player owner) {
        if (plugin.getRegionBlocker().isEffectBlocked(owner, this)) return;

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
    public void activateSpark(Player owner, String slot) {
        if (!plugin.getRegionBlocker().canUseSpark(owner)) return;

        UUID playerUUID = owner.getUniqueId();

        // Because thief cooldowns are weirdly named, we need to manually check keys.
        if (CooldownManager.getCooldowns(playerUUID).stream().anyMatch(k -> k.startsWith(plainKey + "_") && k.endsWith("_" + slot))) return;

        owner.playSound(owner.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1, 1);

        // Applying cooldowns and durations for the effect
        long cooldown = plugin.getMainConfig().cooldown(this);
        long duration = plugin.getMainConfig().duration(this);

        CooldownManager.setTimes(playerUUID, plainKey + "_" + slot, duration, cooldown);
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

    private void activateEffect(Player player, @NotNull InfuseEffect effect, Entity victim, String slot) {
        Message msg = new Message(MessageType.THIEF_STEAL);
        msg.applyPlaceholder("victim", victim.getName());
        msg.applyPlaceholder("effect_name", effect.getName());
        player.sendMessage(msg.toComponent());

        // Activating the stolen spark.
        effect.activateSpark(player, slot);

        UUID playerUUID = player.getUniqueId();

        // Removing cooldowns from the stolen spark
        CooldownManager.clearSpecificCooldown(playerUUID, effect.getPlainKey() + "_" + slot);
        CooldownManager.clearSpecificDuration(playerUUID, effect.getPlainKey() + "_" + slot);

        // Applying cooldowns for the thief effect
        long cooldown = plugin.getMainConfig().cooldown(effect);
        long duration = plugin.getMainConfig().duration(effect);

        CooldownManager.setTimes(playerUUID, plainKey + "_" + effect.key() + "_" + slot, duration, cooldown * 2);
    }

    @Override
    public char getIcon(Player user, String slot) {
        UUID uuid = user.getUniqueId();

        Optional<String> cooldownKey = CooldownManager.getCooldowns(uuid).stream().filter(k -> k.startsWith(this.plainKey + "_") && k.endsWith("_" + slot)).findFirst();
        Optional<String> durationKey = CooldownManager.getDurations(uuid).stream().filter(k -> k.startsWith(this.plainKey + "_") && k.endsWith("_" + slot)).findFirst();

        // Cooldowns run while the effect is active, so check the duration first.
        String activeKey = null;
        boolean active = false;
        long magnitude = -1;
        if (durationKey.isPresent() && CooldownManager.isEffectActive(uuid, durationKey.get())) {
            active = true;
            magnitude = CooldownManager.getEffectTimeLeft(uuid, durationKey.get()) / 1000;
            activeKey = durationKey.get();
        } else if (cooldownKey.isPresent() && CooldownManager.isOnCooldown(uuid, cooldownKey.get())) {
            magnitude = CooldownManager.getCooldownTimeLeft(uuid, cooldownKey.get()) / 1000;
            activeKey = cooldownKey.get();
        }

        // If the effect is inactive or if the player hasn't stolen an effect, return the regular icon
        if (activeKey == null || activeKey.split("_").length == 2) return super.getIcon(user, slot);

        // Reformatting the active key to get the stolen effect.
        int lastUnderscore = activeKey.lastIndexOf("_");
        if (lastUnderscore > 6) activeKey = activeKey.substring(6, lastUnderscore);

        // Parsing the stolen key
        InfuseEffect stolen = InfuseEffect.getEffect(Key.key("infuse", activeKey));
        if (stolen == null) {
            Infuse.LOGGER.error("{} stole an invalid effect '{}'!", user.getName(), activeKey);
            return super.getIcon(user, slot);
        }

        // If the effect is active, return the other effect's icon.
        if (active) {
            return stolen.icon(true, (float) magnitude / plugin.getMainConfig().duration(stolen));
        }

        // If the effect is on cooldown, return the thief icon but scale the cooldown.
        return icon(active, (float) magnitude / plugin.getMainConfig().cooldown(stolen) * 2);
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
     * <p>
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
        if (plugin.getRegionBlocker().isEffectBlocked(killer, this)) return;
        if (plugin.getRegionBlocker().isEffectBlocked(deadPlayer, this)) return;

        disguise(killer, deadPlayer);
    }

    @EventHandler
    public void onPlayerHit(EntityDamageByEntityEvent event) {
        Infuse.LOGGER.debug("[Thief] Recieved EntityDamageByEntityEvent");
        if (!(event.getEntity() instanceof Player victim)) return;
        Infuse.LOGGER.debug("[Thief] Victim is player");
        if (!(event.getDamager() instanceof Player attacker)) return;
        Infuse.LOGGER.debug("[Thief] Attacker is player");
        if (!plugin.getDataManager().hasEffect(attacker, this)) return;
        Infuse.LOGGER.debug("[Thief] Attacker has thief effect");
        if (!plugin.getRegionBlocker().canBeTargetedBySpark(victim)) return;
        Infuse.LOGGER.debug("[Thief] Victim can be targeted by spark");

        UUID playerUUID = attacker.getUniqueId();
        Set<String> durations = CooldownManager.getDurations(playerUUID);
        Infuse.LOGGER.debug("[Thief] Parsing {} tracked durations for {}.", durations.size(), attacker.getName());

        // Because this event listener doesn't know anything about the effect, we need to infer the slot from the existing durations.
        // This gets any thief cooldown that isn't a stolen duration.
        Optional<String> thisDuration = durations.stream()
            .filter(k -> k.startsWith(plainKey + "_"))
            .filter(k -> k.lastIndexOf("_") == 5)
            .findFirst();
        
        Infuse.LOGGER.debug("[Thief] {} a valid active duration.", thisDuration.isPresent() ? "Found" : "Did not find");

        if (thisDuration.isEmpty()) return;
        String durationKey = thisDuration.get();

        Infuse.LOGGER.debug("[Thief] Using the {} duration.", durationKey);
        if (!CooldownManager.isEffectActive(playerUUID, durationKey)) return;
        Infuse.LOGGER.debug("[Thief] {} is still active.", durationKey);
        if (plugin.getRegionBlocker().isEffectBlocked(attacker, this)) return;
        Infuse.LOGGER.debug("[Thief] Thief effect isn't blocked in this region");

        InfuseEffect leftEffect = plugin.getDataManager().getEffect(victim, "1");
        InfuseEffect rightEffect = plugin.getDataManager().getEffect(victim, "2");

        if (leftEffect != null && rightEffect != null) {
            Infuse.LOGGER.debug("[Thief] Victim has both effects");

            if (Math.random() > 0.5) {
                Infuse.LOGGER.debug("[Thief] Activating the left effect");
                activateEffect(attacker, leftEffect, victim, "1");
            } else {
                Infuse.LOGGER.debug("[Thief] Activating the right effect");
                activateEffect(attacker, rightEffect, victim, "2");
            }
        } else if (leftEffect != null) {
            Infuse.LOGGER.debug("[Thief] Victim only has the left effect");
            activateEffect(attacker, leftEffect, victim, "1");
        } else if (rightEffect != null) {
            Infuse.LOGGER.debug("[Thief] Victim only has the right effect");
            activateEffect(attacker, rightEffect, victim, "2");
        } else return;

        Infuse.LOGGER.debug("[Thief] Updating the base thief duration");
        CooldownManager.setDuration(playerUUID, durationKey, 0);
    }
}
