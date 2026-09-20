package com.catadmirer.infuseSMP.listeners;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import com.catadmirer.infuseSMP.Infuse;
import com.catadmirer.infuseSMP.managers.EffectManager.EquipResult;
import com.catadmirer.infuseSMP.managers.EffectManager.EquipResultType;

public class PlayerDeathListener implements Listener {
    private final Infuse plugin;

    public PlayerDeathListener(Infuse plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings("UnstableApiUsage")
    @EventHandler
    public void dropPlayerHeads(PlayerDeathEvent event) {
        Player player = event.getEntity();
        boolean dropHead = plugin.getMainConfig().playerHeadDrops();

        if (!dropHead) return;

        ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
        playerHead.setData(DataComponentTypes.PROFILE, ResolvableProfile.resolvableProfile(player.getPlayerProfile()));

        player.getWorld().dropItem(player.getLocation(), playerHead);
    }

    /**
     * Event handler to remove an effect from the players inventory if they die.
     *
     * @param event The server PlayerDeathEvent
     */
    @EventHandler
    public void dropEffect(PlayerDeathEvent event) {
        Player player = event.getEntity();

        // Skipping natural deaths if the config allows
        if (player.getKiller() == null && !plugin.getMainConfig().dropOnNaturalDeath()) return;

        EquipResult result;
        String dropMode = plugin.getMainConfig().effectDrops();
        switch (dropMode.toLowerCase()) {
            case "random" -> {
                String slot = (Math.random() > 0.5) ? "1" : "2";

                result = plugin.getEffectManager().dropEffect(player, slot);
                if (result.type() == EquipResultType.FAIL) {
                    plugin.getEffectManager().dropEffect(player, slot.equals("1") ? "2" : "1");
                }
            }
            case "prefer_1" -> {
                result = plugin.getEffectManager().dropEffect(player, "1");
                if (result.type() == EquipResultType.FAIL) {
                    plugin.getEffectManager().dropEffect(player, "2");
                }
            }
            case "prefer_2" -> {
                result = plugin.getEffectManager().dropEffect(player, "2");
                if (result.type() == EquipResultType.FAIL) {
                    plugin.getEffectManager().dropEffect(player, "1");
                }
            }
            case "only_1" -> plugin.getEffectManager().dropEffect(player, "1");
            case "only_2" -> plugin.getEffectManager().dropEffect(player, "2");
            case "both" -> {
                plugin.getEffectManager().dropEffect(player, "1");
                plugin.getEffectManager().dropEffect(player, "2");
            }
            case "none" -> {}
        }
    }
}
