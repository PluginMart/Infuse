package com.catadmirer.infuseSMP.listeners;

import com.catadmirer.infuseSMP.Infuse;
import com.catadmirer.infuseSMP.Message;
import com.catadmirer.infuseSMP.Message.MessageType;
import com.catadmirer.infuseSMP.effects.InfuseEffect;
import com.catadmirer.infuseSMP.managers.EffectCraftManager;
import com.catadmirer.infuseSMP.util.regions.RegionBlocker;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.List;

public class PlayerJoinListener implements Listener {
    private final Infuse plugin;

    public PlayerJoinListener(Infuse plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void giveRecipes(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Giving the player all the infuse recipes
        InfuseEffect.getRegisteredEffects().values().stream().map(plugin.getRecipeManager()::getRecipeKey).forEach(player::discoverRecipe);
    }

    @EventHandler
    public void tellControlMode(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Telling the player their current control mode
        String controlMode = plugin.getDataManager().getControlMode(player);
        if (controlMode == null) controlMode = "Offhand";
        boolean offhandEnabled = controlMode.equalsIgnoreCase("Offhand");
        player.addAttachment(plugin, "ability.use", !offhandEnabled);

        Message msg = new Message(MessageType.JOIN_ABILITY_NOTIFY);
        msg.applyPlaceholder("control_mode", controlMode);
        player.sendMessage(msg.toComponent());
    }

    /** Activates the player's effects and assigns them a starting effect if they haven't played before. */
    @EventHandler
    public void activateEffects(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Giving the player their starting effects if they haven't joined before
        if (plugin.getMainConfig().joinEffectsEnabled() && !player.hasPlayedBefore()) {
            List<InfuseEffect> effects = plugin.getMainConfig().joinEffects();
            if (effects.isEmpty()) return;

            InfuseEffect effect = effects.get((int) (Math.random() * effects.size()));
            plugin.getEffectManager().equipEffect(player, effect, "1", false);
            return;
        }

        // Enabling each effect
        InfuseEffect effect = plugin.getDataManager().getEffect(player, "1");
        if (effect != null && !RegionBlocker.getInstance().isEffectBlocked(player, effect)) effect.equip(player);

        effect = plugin.getDataManager().getEffect(player, "2");
        if (effect != null && !RegionBlocker.getInstance().isEffectBlocked(player, effect)) effect.equip(player);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!EffectCraftManager.isRitual()) return;

        event.getPlayer().showBossBar(EffectCraftManager.getBar());
    }
}
