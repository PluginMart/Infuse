package com.catadmirer.infuseSMP.managers;

import com.catadmirer.infuseSMP.Infuse;
import com.catadmirer.infuseSMP.Message;
import com.catadmirer.infuseSMP.Message.MessageType;
import com.catadmirer.infuseSMP.effects.InfuseEffect;
import com.catadmirer.infuseSMP.inventories.StationSelectionMenu;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpRequest.BodyPublishers;
import java.util.List;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.BrewingStand;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.Event.Result;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MenuType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jspecify.annotations.Nullable;

public class EffectCraftManager implements Listener {
    private final Infuse plugin = Infuse.getInstance();
    private static BossBar ritualBossBar;
    private static EnderCrystal ritualBeam;

    public static boolean isRitual() {
        return ritualBossBar != null;
    }

    @Nullable
    public static BossBar getBar() {
        return ritualBossBar;
    }

    private void sendToDiscord(String webhookUrl, String message) {
        String payload = "{\"content\": \"" + message + "\"}";

        HttpRequest request = HttpRequest.newBuilder(URI.create(webhookUrl))
                .header("Content-Type", "application/json")
                .POST(BodyPublishers.ofString(payload)).build();

        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpResponse<Void> response = client.send(request, BodyHandlers.discarding());

            // Checking the response status code
            int status = response.statusCode();
            if (status == 200) {
                Infuse.LOGGER.info("Message sent to Discord!");
            } else {
                Infuse.LOGGER.error("Error sending message to Discord: {}", status);
            }
        } catch (IOException err) {
            Infuse.LOGGER.error("Could not send webhook message to discord.", err);
        } catch (InterruptedException err) {
            Infuse.LOGGER.error("Discord webhook request was interrupted!", err);
        }
    }

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        // Safe to assume the crafted item is the correct augmented/regular form due to the PrepareItemCraftEvent Listener
        final ItemStack craftedItem = event.getInventory().getResult();
        final InfuseEffect effect = InfuseEffect.fromItem(craftedItem);
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

        // Checking craft limits
        int craftLimit = plugin.getMainConfig().getCraftLimit(effect);
        int numCrafted = plugin.getDataManager().getExistingCount(effect);
        if (numCrafted == craftLimit) {
            player.sendMessage(Component.text("The max number of ").append(effect.getName().toComponent()).append(Component.text("effects has been reached", NamedTextColor.WHITE)));
            event.setCancelled(true);
            return;
        }

        // Incrementing the number of effects crafted.
        plugin.getDataManager().setExistingCount(effect, numCrafted + 1);

        // If the effect is not augmented, just craft it
        if (!effect.isAugmented())  {
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
            formattedMessage.applyPlaceholder("item", effect.getName().toString());
            formattedMessage.applyPlaceholder("x", brewerLocation.getBlockX());
            formattedMessage.applyPlaceholder("y", brewerLocation.getBlockY());
            formattedMessage.applyPlaceholder("z", brewerLocation.getBlockZ());
            formattedMessage.applyPlaceholder("dimension", worldName);

            Bukkit.broadcast(formattedMessage.toComponent());
            return;
        }

        // Making sure there isn't a ritual active already
        if (ritualBossBar != null) {
            player.sendMessage(new Message(MessageType.ERROR_RITUAL_ACTIVE).toComponent());
            event.setCancelled(true);
            return;
        }

        // Removing the ingredients
        event.getInventory().forEach(item -> item.subtract(1));

        // Closing the inventory
        player.closeInventory();

        // Cancelling the event
        event.setCancelled(true);

        // Starting the ritual for the augmented effect
        // Creating the bossbar
        Component itemName = effect.getName().toComponent();
        Component bossbarName = Component.text("🧪 ").append(itemName.decorate(TextDecoration.BOLD)).append(Component.text(" 🧪").decoration(TextDecoration.BOLD, false)).color(itemName.color());
        ritualBossBar = BossBar.bossBar(bossbarName, 1, effect.getRitualColor(), BossBar.Overlay.PROGRESS);

        // Adding every player online to the bossbar
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.showBossBar(ritualBossBar);
        }

        // Getting the duration of the ritual
        int ritualDuration;
        if (effect.toString().equals("aug_ender")) {
            ritualDuration = plugin.getMainConfig().ritualDurationEnder();
        } else {
            ritualDuration = plugin.getMainConfig().ritualDuration();
        }

        // Spawning the ender crystal if the config allows
        if (plugin.getMainConfig().ritualBeacon()) {
            Location startLoc = brewerLocation.clone().add(0.5, 0, 0.5);
            startLoc.setY(-100);
            Location targetLoc = brewerLocation.clone().add(0.5, 0, 0.5);
            targetLoc.setY(500);

            ritualBeam = (EnderCrystal) brewerLocation.getWorld().spawnEntity(startLoc, EntityType.END_CRYSTAL);
            ritualBeam.setShowingBottom(false);
            ritualBeam.setInvulnerable(true);
            ritualBeam.setInvisible(true);
            ritualBeam.setBeamTarget(targetLoc);
            ritualBeam.setPersistent(false);

            Bukkit.getScheduler().runTaskLater(plugin, ritualBeam::remove, ritualDuration * 20L);
        }

        Environment worldEnv = brewerLocation.getWorld().getEnvironment();
        String worldName = switch(worldEnv) {
            case NORMAL -> "<green><b>Overworld";
            case NETHER -> "<dark_red><b>Nether";
            case THE_END -> "<dark_purple><b>End";
            default -> "<gray>" + brewerLocation.getWorld().getName();
        };

        String x = String.valueOf(brewerLocation.getBlockX());
        String y = String.valueOf(brewerLocation.getBlockY());
        String z = String.valueOf(brewerLocation.getBlockZ());

        Message mcMessage = new Message(MessageType.EFFECT_BROADCAST);
        mcMessage.applyPlaceholder("player", player.getName());
        mcMessage.applyPlaceholder("item", MiniMessage.miniMessage().serialize(itemName));
        mcMessage.applyPlaceholder("x", x);
        mcMessage.applyPlaceholder("y", y);
        mcMessage.applyPlaceholder("z", z);
        mcMessage.applyPlaceholder("dimension", worldName);

        Message dscMessage = new Message(MessageType.DISCORD_BROADCAST);
        dscMessage.applyPlaceholder("player", player.getName());
        dscMessage.applyPlaceholder("item", PlainTextComponentSerializer.plainText().serialize(itemName));
        dscMessage.applyPlaceholder("x", x);
        dscMessage.applyPlaceholder("y", y);
        dscMessage.applyPlaceholder("z", z);
        dscMessage.applyPlaceholder("dimension", MiniMessage.miniMessage().stripTags(worldName));

        // Broadcasting that the ritual has started
        Bukkit.broadcast(mcMessage.toComponent());
        if (plugin.getMainConfig().enableDiscordBroadcasts()) {
            String webhookUrl = plugin.getMainConfig().discordWebhookUrl();
            if (webhookUrl != null && !webhookUrl.isEmpty()) {
                sendToDiscord(webhookUrl, dscMessage.toString());
            }
        }

        // Preventing the brewing stand from being broken or opened
        ImmortalBrewer brewerListener = new ImmortalBrewer(brewerLocation);
        Bukkit.getPluginManager().registerEvents(brewerListener, plugin);

        // Updating the ender recipe
        plugin.getRecipeManager().updateEnderRecipe();

        // Starting the ritual progress bar
        new BukkitRunnable() {
            float progress = 1.0f;
            final double progressDecrement = 1.0 / (ritualDuration * 20.0);

            @Override
            public void run() {
                progress -= (float) progressDecrement;
                if (progress <= 0) {
                    ritualBossBar.progress(0);
                    cancel();

                    // Removing the bossbar from view
                    for (Player audience : Bukkit.getOnlinePlayers()) {
                        audience.hideBossBar(ritualBossBar);
                    }

                    // Allowing the brewing stand to be broken
                    HandlerList.unregisterAll(brewerListener);

                    // Broadcasting that the effect has been brewed
                    Message msg = new Message(MessageType.EFFECT_FINISHED);
                    msg.applyPlaceholder("item", MiniMessage.miniMessage().serialize(itemName));
                    Bukkit.broadcast(msg.toComponent());

                    // Dropping the item
                    brewerLocation.getWorld().dropItem(brewerLocation.add(0, 1, 0), effect.createItem());
                    ritualBossBar = null;
                    return;
                }

                ritualBossBar.progress(progress);
            }

        }.runTaskTimer(this.plugin, 0, 1);
    }

    public static void removeBeam() {
        if (!(ritualBeam.isDead())) ritualBeam.remove();
        ritualBeam = null;
    }

    /** Consulting the recipe manager to determine what to craft */
    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        // Ignoring non-infuse items
        if (event.getRecipe() == null) return;
        if (InfuseEffect.fromItem(event.getRecipe().getResult()) == null) return;

        ItemStack toCraft = plugin.getRecipeManager().getItemToCraft(event.getRecipe());
        event.getInventory().setResult(toCraft);
    }

    public static final Component effectCraftingMenu = Component.text("Effect Crafting");

    /** Handles when players right click a brewing stand. */
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

    /**
     * Holds a {@link Location} and prevents the block there from being broken, ever.
     * Meant for brewing stands only.  Does not cover {@link BlockBurnEvent}s.
     */
    public static class ImmortalBrewer implements Listener {
        private final Location brewerLocation;

        public ImmortalBrewer(Location brewerLocation) {
            this.brewerLocation = brewerLocation;
        }

        @EventHandler
        public void onBrewingStandBreak(BlockBreakEvent event) {
            if (event.getBlock().getLocation().equals(brewerLocation)) {
                event.setCancelled(true);
            }
        }

        @EventHandler
        public void onBrewingStandExplode(EntityExplodeEvent event) {
            List<Block> blocks = event.blockList();
            blocks.removeIf(block -> block.getLocation().equals(brewerLocation));
        }

        @EventHandler(priority = EventPriority.LOW)
        public void onBrewingStandInteract(PlayerInteractEvent event) {
            Block clicked = event.getClickedBlock();
            if (clicked == null) return;
            if (!clicked.getLocation().equals(brewerLocation)) return;

            event.setUseInteractedBlock(Result.DENY);
        }
    }
}
