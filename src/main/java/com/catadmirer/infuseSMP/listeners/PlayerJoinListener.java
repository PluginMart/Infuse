package com.catadmirer.infuseSMP.listeners;

import com.catadmirer.infuseSMP.Infuse;
import com.catadmirer.infuseSMP.Message;
import com.catadmirer.infuseSMP.Message.MessageType;
import com.catadmirer.infuseSMP.effects.InfuseEffect;

import com.catadmirer.infuseSMP.managers.RecipeManager;
import net.kyori.adventure.resource.ResourcePackRequest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.persistence.PersistentDataType;

public class PlayerJoinListener implements Listener {
    private final Infuse plugin = Infuse.getInstance();

    @EventHandler
    public void giveRecipes(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Giving the player all the infuse recipes
        InfuseEffect.getRegisteredEffects().stream().map(RecipeManager::getRecipeKey).forEach(player::discoverRecipe);
    }

    @EventHandler
    public void tellControlMode(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Telling the player their current control mode
        String controlMode = plugin.getDataManager().getControlMode(player.getUniqueId());

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
        InfuseEffect effect = plugin.getDataManager().getEffect(player.getUniqueId(), "1");
        if (effect != null && !plugin.getRegionBlocker().isEffectBlocked(player, effect)) effect.equip(player);

        effect = plugin.getDataManager().getEffect(player.getUniqueId(), "2");
        if (effect != null && !plugin.getRegionBlocker().isEffectBlocked(player, effect)) effect.equip(player);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!plugin.getRitualManager().isActive()) return;

        //noinspection DataFlowIssue
        event.getPlayer().showBossBar(plugin.getRitualManager().getBossBar());
    }

    @EventHandler
    public void giveResourcePack(PlayerJoinEvent event) {
        var request = ResourcePackRequest.resourcePackRequest()
                .packs(Infuse.RESOURCE_PACK)
                .required(true)
                .build();

        event.getPlayer().sendResourcePacks(request);
    }

    @EventHandler
    public void checkForUpdate(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Not notifying operators
        if (!player.hasPermission("infuse.update_notify")) return;

        // Sending the update message (if needed)
        if (!plugin.getVersion().equals(plugin.getLatestVersion())) {
            Message msg = new Message(Message.MessageType.UPDATE_AVAILABLE);
            msg.applyPlaceholder("current_version", plugin.getVersion());
            msg.applyPlaceholder("latest_version", plugin.getLatestVersion());

            player.sendMessage(msg.toComponent());
        }
    }
}
