package com.catadmirer.infuseSMP.listeners;

import com.catadmirer.infuseSMP.Infuse;
import com.catadmirer.infuseSMP.Message;
import com.catadmirer.infuseSMP.Message.MessageType;
import com.catadmirer.infuseSMP.effects.InfuseEffect;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.persistence.PersistentDataType;

public class PlayerJoinListener implements Listener {
    private final Infuse plugin;

    public PlayerJoinListener(Infuse plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void giveRecipes(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Giving the player all the infuse recipes
        InfuseEffect.getRegisteredEffects().stream().map(plugin.getRecipeManager()::getRecipeKey).forEach(player::discoverRecipe);
    }

    @EventHandler
    public void tellControlMode(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Telling the player their current control mode
        String controlMode = plugin.getDataManager().getControlMode(player);
        if (controlMode == null) controlMode = "Offhand";

        Message msg = new Message(MessageType.CONTROL_MODE_NOTIFY);
        msg.applyPlaceholder("control_mode", controlMode);
        player.sendMessage(msg.toComponent());
    }

    /** Activates the player's effects and assigns them a starting effect if they haven't played before. */
    @EventHandler
    public void activateEffects(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Giving the player their starting effects if they haven't joined before
        if (plugin.getMainConfig().joinEffectsEnabled() && !player.getPersistentDataContainer().has(Infuse.JOIN_EFFECT_KEY)) {
            plugin.getEffectManager().giveJoinEffect(player);
            player.getPersistentDataContainer().set(Infuse.JOIN_EFFECT_KEY, PersistentDataType.BOOLEAN, true);
            return;
        }

        // Enabling each effect
        InfuseEffect effect = plugin.getDataManager().getEffect(player, "1");
        if (effect != null && !plugin.getRegionBlocker().isEffectBlocked(player, effect)) effect.equip(player);

        effect = plugin.getDataManager().getEffect(player, "2");
        if (effect != null && !plugin.getRegionBlocker().isEffectBlocked(player, effect)) effect.equip(player);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!plugin.getRitualManager().isActive()) return;

        //noinspection DataFlowIssue
        event.getPlayer().showBossBar(plugin.getRitualManager().getBossBar());
    }
}
