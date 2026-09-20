package com.catadmirer.infuseSMP.managers;

import com.catadmirer.infuseSMP.Infuse;
import com.catadmirer.infuseSMP.Message;
import com.catadmirer.infuseSMP.Message.MessageType;
import com.catadmirer.infuseSMP.effects.InfuseEffect;
import com.catadmirer.infuseSMP.events.EffectCraftEvent;
import com.catadmirer.infuseSMP.inventories.StationSelectionMenu;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.BrewingStand;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.Event.Result;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MenuType;

@SuppressWarnings("UnstableApiUsage")
public class EffectCraftManager implements Listener {
    private final Infuse plugin = Infuse.getInstance();

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        // Safe to assume the crafted item is the correct augmented/regular form due to the PrepareItemCraftEvent Listener
        final ItemStack craftedItem = event.getInventory().getResult();
        final InfuseEffect effect = InfuseEffect.getEffect(craftedItem);
        final HumanEntity player = event.getWhoClicked();
        // Making sure the item being crafted is an Infuse effect
        if (effect == null) return;

        // Not allowing the player to shift click effects
        if (event.isShiftClick()) {
            player.sendMessage(Component.text("You cannot shift click effects", NamedTextColor.RED));
            event.setCancelled(true);
            return;
        }

        // Making sure the brewing stand is still placed
        final Location brewerLocation = event.getInventory().getLocation();
        if (brewerLocation == null || brewerLocation.getBlock().getType() != Material.BREWING_STAND) {
            player.sendMessage(new Message(MessageType.EFFECT_NO_BREWING).toComponent());
            event.setCancelled(true);
            return;
        }

        int numCrafted = plugin.getDataManager().getExistingCount(effect);

        // Checking craft limits
        if (!plugin.getMainConfig().allowInfiniteEffects()) {
            int craftLimit = plugin.getMainConfig().getCraftLimit(effect);
            if (craftLimit != -1 && numCrafted == craftLimit) {
                player.sendMessage(Component.text("The max number of ").append(effect.getName().toComponent()).append(Component.text("effects has been reached", NamedTextColor.WHITE)));
                event.setCancelled(true);
                return;
            }

            // Updating the recipe if needed
            // Recipes are only updated when infinite effects aren't used.
            if (numCrafted + 1 == craftLimit) {
                Bukkit.removeRecipe(plugin.getRecipeManager().getRecipeKey(effect));
                Bukkit.addRecipe(plugin.getRecipeManager().getRecipe(effect.getRegularVersion()));
            }
        }

        // Incrementing the number of effects crafted.
        plugin.getDataManager().setExistingCount(effect, numCrafted + 1);

        // If the effect is not augmented, just craft it
        if (!effect.isAugmented())  {
            // Calling the EffectCraftEvent
            new EffectCraftEvent(player, effect).callEvent();

            // Announcing the effect being crafted if the config is enabled
            if (!plugin.getMainConfig().regularBroadcast()) return;

            Environment worldEnv = brewerLocation.getWorld().getEnvironment();
            String worldName = switch(worldEnv) {
                case NORMAL -> "<green><b>Overworld";
                case NETHER -> "<dark_red><b>Nether";
                case THE_END -> "<dark_purple><b>End";
                default -> "<gray>" + brewerLocation.getWorld().getName();
            };

            Message formattedMessage = new Message(MessageType.REGULAR_BROADCAST);
            formattedMessage.applyPlaceholder("player", player.getName());
            formattedMessage.applyPlaceholder("item", effect.getName());
            formattedMessage.applyPlaceholder("x", brewerLocation.getBlockX());
            formattedMessage.applyPlaceholder("y", brewerLocation.getBlockY());
            formattedMessage.applyPlaceholder("z", brewerLocation.getBlockZ());
            formattedMessage.applyPlaceholder("dimension", worldName);

            Bukkit.broadcast(formattedMessage.toComponent());
            return;
        }

        // Making sure no rituals were active.
        if (plugin.getRitualManager().isActive()) {
            event.setCancelled(true);
            return;
        }

        // Starting the ritual
        plugin.getRitualManager().startRitual(player, effect, brewerLocation);

        // Calling the EffectCraftEvent
        new EffectCraftEvent(player, effect).callEvent();

        // Removing the ingredients
        event.getInventory().forEach(item -> item.subtract(1));

        // Closing the inventory
        player.closeInventory();

        // Cancelling the event
        event.setCancelled(true);
    }

    public static final Component effectCraftingMenu = Component.text("Effect Crafting");

    /** Handles when players right-click a brewing stand. */
    @EventHandler
    public void onBrewingStandInteract(PlayerInteractEvent event) {
        if (event.useInteractedBlock() == Result.DENY) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null) return;
        if (block.getType() != Material.BREWING_STAND) return;

        event.setCancelled(true);
        Player player = event.getPlayer();
        if (plugin.getMainConfig().brewingGui()) {
            player.openInventory(new StationSelectionMenu(block.getLocation()).getInventory());
        } else {
            // Opening the menu for crafting effects
            MenuType.CRAFTING.builder().location(block.getLocation()).title(effectCraftingMenu).build(player).open();
        }
    }

    /** Handles click events in the {@link StationSelectionMenu} inventory. */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getClickedInventory() == null) return;
        if (!(event.getClickedInventory().getHolder() instanceof StationSelectionMenu menu)) return;

        event.setCancelled(true);
        HumanEntity player = event.getWhoClicked();

        // Making sure the block is still a brewing stand
        Block block = menu.getStandLocation().getBlock();
        if (block.getType() != Material.BREWING_STAND)
            return;

        if (event.getSlot() == 11) {
            // Closing the StationSelectionMenu
            player.closeInventory();

            // Opening the menu for crafting effects
            MenuType.CRAFTING.builder().location(block.getLocation()).title(effectCraftingMenu).build(player).open();
        } else if (event.getSlot() == 15) {
            // Closing the StationSelectionMenu
            player.closeInventory();

            // Opening the brewing stand
            BrewingStand data = (BrewingStand) block.getState();
            player.openInventory(data.getInventory());
        }
    }
}
