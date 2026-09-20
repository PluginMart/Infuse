package com.catadmirer.infuseSMP.managers;

import com.catadmirer.infuseSMP.Infuse;
import com.catadmirer.infuseSMP.Message;
import com.catadmirer.infuseSMP.effects.Ender;
import com.catadmirer.infuseSMP.effects.InfuseEffect;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class RitualManager {
    private final Infuse plugin;

    private boolean active;
    @Nullable private BossBar bossBar;
    @Nullable private EnderCrystal enderCrystal;
    @Nullable private InfuseEffect effect;
    @Nullable private Location location;
    @Nullable private ImmortalBrewer immortalBrewer;
    @Nullable private BukkitTask task;

    public RitualManager() {
        plugin = Infuse.getInstance();
    }

    public boolean isActive() {
        return active;
    }

    // Might be giving users too much access to the parts of a ritual here.  They can modify attributes of a ritual as it happens.
    public @Nullable BossBar getBossBar() {
        return bossBar;
    }

    public @Nullable InfuseEffect getEffect() {
        return effect;
    }

    public @Nullable Location getLocation() {
        return location;
    }

    public boolean hasBeam() {
        return active && enderCrystal != null;
    }

    public boolean isBrewerImmortal() {
        return active && immortalBrewer != null;
    }

    public void startRitual(HumanEntity player, InfuseEffect effect, Location location) {
        if (active) {
            player.sendMessage(new Message(Message.MessageType.ERROR_RITUAL_ACTIVE).toComponent());
            return;
        }

        active = true;
        this.effect = effect;
        this.location = location;

        // Creating the bossbar
        Component itemName = effect.getName().toComponent();
        Component bossBarName = Component.text("🧪 ").append(itemName.decorate(TextDecoration.BOLD)).append(Component.text(" 🧪").decoration(TextDecoration.BOLD, false)).color(itemName.color());
        bossBar = BossBar.bossBar(bossBarName, 1, effect.getRitualColor(), BossBar.Overlay.PROGRESS);

        // Adding every player online to the bossbar
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.showBossBar(bossBar);
        }

        // Spawning the ender crystal if the config allows
        if (plugin.getMainConfig().ritualBeacon()) {
            Location startLoc = location.clone().add(0.5, 0, 0.5);
            startLoc.setY(-100);
            Location targetLoc = location.clone().add(0.5, 0, 0.5);
            targetLoc.setY(500);

            enderCrystal = location.getWorld().spawn(startLoc, EnderCrystal.class);
            enderCrystal.setShowingBottom(false);
            enderCrystal.setInvulnerable(true);
            enderCrystal.setInvisible(true);
            enderCrystal.setBeamTarget(targetLoc);
            enderCrystal.setPersistent(false);
        }

        // Putting together the messages
        String x = String.valueOf(location.getBlockX());
        String y = String.valueOf(location.getBlockY());
        String z = String.valueOf(location.getBlockZ());

        World.Environment worldEnv = location.getWorld().getEnvironment();
        String worldName = switch(worldEnv) {
            case NORMAL -> "<green><b>Overworld";
            case NETHER -> "<dark_red><b>Nether";
            case THE_END -> "<dark_purple><b>End";
            default -> "<gray>" + location.getWorld().getName();
        };

        Message minecraftMessage = new Message(Message.MessageType.EFFECT_BROADCAST);
        minecraftMessage.applyPlaceholder("player", player.getName());
        minecraftMessage.applyPlaceholder("item", itemName);
        minecraftMessage.applyPlaceholder("x", x);
        minecraftMessage.applyPlaceholder("y", y);
        minecraftMessage.applyPlaceholder("z", z);
        minecraftMessage.applyPlaceholder("dimension", worldName);

        Message discordMessage = new Message(Message.MessageType.DISCORD_BROADCAST);
        discordMessage.applyPlaceholder("player", player.getName());
        discordMessage.applyPlaceholder("item", PlainTextComponentSerializer.plainText().serialize(itemName));
        discordMessage.applyPlaceholder("x", x);
        discordMessage.applyPlaceholder("y", y);
        discordMessage.applyPlaceholder("z", z);
        discordMessage.applyPlaceholder("dimension", MiniMessage.miniMessage().stripTags(worldName));

        // Broadcasting that the ritual has started
        Bukkit.broadcast(minecraftMessage.toComponent());
        if (plugin.getMainConfig().enableDiscordBroadcasts()) {
            String webhookUrl = plugin.getMainConfig().discordWebhookUrl();
            if (webhookUrl != null && !webhookUrl.isEmpty()) {
                sendToDiscord(webhookUrl, discordMessage.toString());
            }
        }

        // Preventing the brewing stand from being broken or opened
        if (plugin.getMainConfig().useImmortalBrewers()) {
            immortalBrewer = new ImmortalBrewer(location);
            Bukkit.getPluginManager().registerEvents(immortalBrewer, plugin);
        }

        // Getting the duration of the ritual
        int ritualDuration;
        if (effect.equals(new Ender(true))) {
            ritualDuration = plugin.getMainConfig().ritualDurationEnder();
        } else {
            ritualDuration = plugin.getMainConfig().ritualDuration();
        }

        // Starting the ritual progress bar
        final int period = 1;
        task = new BukkitRunnable() {
            float progress = 1;
            final float progressDecrement = period / (ritualDuration * 20f);

            @Override
            public void run() {
                // Checking if the brewing stand was broken.
                if (!isBrewerImmortal()) {
                    if (location.getBlock().getType() != Material.BREWING_STAND) {
                        Bukkit.broadcast(new Message(Message.MessageType.RITUAL_INTERRUPTED).toComponent());

                        stopRitual();
                        cancel();
                        return;
                    }
                }

                // Updating the bossbar
                assert bossBar != null;
                progress -= progressDecrement;

                if (progress <= 0) {
                    completeRitual();
                    cancel();
                    return;
                }

                bossBar.progress(progress);
            }
        }.runTaskTimer(this.plugin, 0, period);
    }

    /**
     * Handles the successful completion of a ritual.<br>
     * If no ritual is active, nothing happens.<br>
     * Drops the crafted item, broadcasts that the ritual is complete, and triggers an ender recipe update.
     */
    public void completeRitual() {
        if (!active) return;

        assert effect != null;
        assert location != null;

        // Broadcasting that the effect has been brewed
        Message msg = new Message(Message.MessageType.EFFECT_FINISHED);
        msg.applyPlaceholder("item", effect.getName());
        Bukkit.broadcast(msg.toComponent());

        // Dropping the item
        location.getWorld().dropItem(location.add(0, 1, 0), effect.createItem());

        // Finalizing the ritual ending
        stopRitual();
    }

    /**
     * Cleanup logic for rituals.  Runs regardless of whether the ritual succeeded or not.<br>
     * Hides the bossbar and end crystal and also lets player's break the brewing stand again.
     */
    public void stopRitual() {
        if (!active) return;

        assert bossBar != null;

        // Removing the bossbar from view
        for (Player audience : Bukkit.getOnlinePlayers()) {
            audience.hideBossBar(bossBar);
        }

        // Stopping the ritual task
        if (task != null) task.cancel();

        // Removing the beam
        if (enderCrystal != null) enderCrystal.remove();

        // Allowing the brewing stand to be broken
        if (immortalBrewer != null) HandlerList.unregisterAll(immortalBrewer);

        active = false;
        bossBar = null;
        effect = null;
        enderCrystal = null;
        immortalBrewer = null;
        location = null;
        task = null;
    }

    private void sendToDiscord(String webhookUrl, String message) {
        String payload = "{\"content\": \"" + message + "\"}";

        HttpRequest request = HttpRequest.newBuilder(URI.create(webhookUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload)).build();


        try(HttpClient client = HttpClient.newHttpClient()) {
            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());

            // Checking the response status code
            int status = response.statusCode();
            if (status == 200) {
                Infuse.LOGGER.info("Message sent to Discord!");
            } else {
                Infuse.LOGGER.info("Error sending message to Discord: {}", status);
            }
        } catch (IOException err) {
            Infuse.LOGGER.error("Could not send webhook message to discord.", err);
        } catch (InterruptedException err) {
            Infuse.LOGGER.error("Discord webhook request was interrupted!", err);
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

            event.setUseInteractedBlock(Event.Result.DENY);
        }
    }
}
