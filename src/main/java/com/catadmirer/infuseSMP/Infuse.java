package com.catadmirer.infuseSMP;

import com.catadmirer.infuseSMP.commands.*;
import com.catadmirer.infuseSMP.effects.*;
import com.catadmirer.infuseSMP.expansions.ExpansionHelper;
import com.catadmirer.infuseSMP.extraeffects.*;
import com.catadmirer.infuseSMP.listeners.*;
import com.catadmirer.infuseSMP.managers.*;
import com.catadmirer.infuseSMP.expansions.InfusePlaceholders;
import com.catadmirer.infuseSMP.playerdata.DataManager;
import com.catadmirer.infuseSMP.playerdata.H2DataManager;
import com.catadmirer.infuseSMP.playerdata.YamlDataManager;
import com.catadmirer.infuseSMP.util.regions.BasicRegionBlocker;
import com.catadmirer.infuseSMP.util.regions.DualRegionBlocker;
import com.catadmirer.infuseSMP.util.regions.RegionBlocker;
import com.catadmirer.infuseSMP.util.trust.BetterTeamsTrustManager;
import com.catadmirer.infuseSMP.util.trust.MultiTrustManager;
import com.catadmirer.infuseSMP.util.trust.TrustManager;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Infuse extends JavaPlugin {
    public static final Logger LOGGER = LoggerFactory.getLogger("Infuse");
    public static final NamespacedKey JOIN_EFFECT_KEY = new NamespacedKey("infuse", "has_join_effects");

    private static Infuse instance;

    private DataManager dataManager;
    private final EffectManager effectManager;
    private final MainConfig mainConfig;
    private final GlobalLoop loop;
    private final RecipeManager recipeManager;
    private final HitTracker hitTracker;
    private final RitualManager ritualManager;
    private TrustManager trustManager;
    private RegionBlocker regionBlocker;

    @NonNull
    public static Infuse getInstance() {
        assert instance != null;
        return instance;
    }

    public Infuse() {
        instance = this;

        this.mainConfig = new MainConfig(this);

        // Loading the config
        mainConfig.load();

        // Applying config updates
        mainConfig.applyUpdates();

        this.effectManager = new EffectManager(this);
        this.loop = new GlobalLoop(this);
        this.recipeManager = new RecipeManager();
        this.hitTracker = new HitTracker(this);
        this.ritualManager = new RitualManager();
    }

    public void onLoad() {
        if (ExpansionHelper.canUseBetterTeams() && mainConfig.enableBetterTeams()) {
            trustManager = new MultiTrustManager(new BetterTeamsTrustManager(), dataManager);
        } else {
            trustManager = dataManager;
        }

        if (ExpansionHelper.canUseWorldGuard()) {
            regionBlocker = new DualRegionBlocker();
            LOGGER.info("WorldGuard found! Enabling region-based effect management.");
        } else {
            regionBlocker = new BasicRegionBlocker();
            LOGGER.info("WorldGuard is not installed! Using blacklisted-worlds configs");
        }
    }

    public void onEnable() {
        // Loading the message translator
        new MessageTranslator().loadAll();

        // Loading the data manager
        // If no valid data manager is found, disable the plugin
        dataManager = switch (mainConfig.storageMode().toLowerCase()) {
            case "h2" -> new H2DataManager(this);
            case "yaml" -> new YamlDataManager();
            default -> null;
        };

        if (dataManager == null) {
            LOGGER.error("Could not read a valid storage type from the config!  Disabling Infuse.");
            throw new RuntimeException("Invalid storage type");
        }

        // Registering infuse commands
        this.registerCommands();

        // Starting the passive effect loop
        loop.start();

        // Registering event listeners for the plugin
        this.registerEvents();

        // Registering the vanilla effects
        registerEffects();

        // Registering the infuse recipes
        recipeManager.reload();

        // Initializing the action bar updater
        new ActionBarUpdater(this).runTaskTimer(this, 0, 20);

        // Registering the PlaceholderAPI listener if the plugin is installed
        if (ExpansionHelper.canUsePlaceholderAPI()) {
            new InfusePlaceholders(this).register();
            LOGGER.info("Placeholders Enabled!");
        } else {
            LOGGER.warn("PlaceholderAPI is not installed, so custom placeholders won't work.");
        }

        // Logging the success message
        LOGGER.info("Infuse Plugin has been enabled!");
    }

    public MainConfig getMainConfig() {
        return mainConfig;
    }

    public RecipeManager getRecipeManager() {
        return recipeManager;
    }

    /** Registers the commands for the plugin. */
    private void registerCommands() {
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, e -> {
            e.registrar().register(SparkCommand.build(this, true));
            e.registrar().register(SparkCommand.build(this, false));

            e.registrar().register(TrustCommand.build(trustManager, true));
            e.registrar().register(TrustCommand.build(trustManager, false));

            e.registrar().register(SwapCommand.build(this));

            e.registrar().register(InfuseCommand.build(this));

            e.registrar().register(DrainCommand.build(this, true));
            e.registrar().register(DrainCommand.build(this, false));

            e.registrar().register(DrawCommand.build());
        });
    }

    public void onDisable() {
        // Stopping the passive effect loop
        loop.stop();

        // Sending the log message
        LOGGER.info("Infuse Plugin is disabling...");

        // Stopping existing rituals
        ritualManager.stopRitual();

        // Finalizing the message
        LOGGER.info("Infuse Plugin has been disabled!");
    }

    private void registerEvents() {
        // Initializing the hit tracker
        Bukkit.getPluginManager().registerEvents(hitTracker, this);

        // Registering events for all the listeners
        Bukkit.getPluginManager().registerEvents(new PlayerSwapHandItemsListener(dataManager), this);
        Bukkit.getPluginManager().registerEvents(new CrafterCraftListener(), this);
        Bukkit.getPluginManager().registerEvents(new EntityDeathListener(dataManager), this);
        Bukkit.getPluginManager().registerEvents(new EntityDropItemListener(this), this);
        Bukkit.getPluginManager().registerEvents(new EntityPickupItemListener(this), this);
        Bukkit.getPluginManager().registerEvents(new EffectCraftManager(), this);
        Bukkit.getPluginManager().registerEvents(new InventoryClickListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ItemDespawnListener(dataManager), this);
        Bukkit.getPluginManager().registerEvents(new PlayerDeathListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerItemConsumeListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerQuitListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerSwapHandItemsListener(dataManager), this);
    }

    private void registerEffects() {
        InfuseEffect.register(new Emerald());
        InfuseEffect.register(new Ender());
        InfuseEffect.register(new Feather());
        InfuseEffect.register(new Fire());
        InfuseEffect.register(new Frost());
        InfuseEffect.register(new Haste());
        InfuseEffect.register(new Heart());
        InfuseEffect.register(new Invis());
        InfuseEffect.register(new Ocean());
        InfuseEffect.register(new Regen());
        InfuseEffect.register(new Speed());
        InfuseEffect.register(new Strength());
        InfuseEffect.register(new Thunder());

        if (mainConfig.enableApophis()) InfuseEffect.register(new Apophis());
        if (mainConfig.enableThief()) InfuseEffect.register(new Thief());
    }

    public String getVersion() {
        return getPluginMeta().getVersion();
    }

    /** Checks the modrinth api for any updates to the plugin. */
    private String getLatestVersion() {
        HttpRequest request = HttpRequest.newBuilder()
            .GET()
            .header("User-Agent", "Infuse/" + getVersion())
            .uri(URI.create("https://api.modrinth.com/v2/project/infusesmp/version"))
            .build();

        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpResponse<String> response = client.send(request, BodyHandlers.ofString());

            // Handling http error codes
            if (response.statusCode() != 200) {
                LOGGER.warn("Recieved error code {} from api.modrinth.com", response.statusCode());
                return null;
            }

            // Parsing json
            Gson gson = new Gson();
            JsonArray versions = gson.fromJson(response.body(), JsonArray.class);

            // If no versions are returned, defaulting to the current version
            if (versions.isEmpty()) {
                LOGGER.warn("No versions published to modrinth, defaulting to current version");
                return getVersion();
            }

            JsonObject latestVersion = versions.get(0).getAsJsonObject();
            return latestVersion.get("verson_number").getAsString();
        } catch (JsonSyntaxException err) {
            LOGGER.error("Could not parse the json given by modrinth.", err);
        } catch (InterruptedException err) {
            LOGGER.error("Version request was interrupted", err);
        } catch (IOException err) {
            LOGGER.error("Could not get versions from modrinth", err);
        }

        return null;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public EffectManager getEffectManager() {
        return effectManager;
    }

    public HitTracker getHitTracker() {
        return hitTracker;
    }

    public RitualManager getRitualManager() {
        return ritualManager;
    }

    public TrustManager getTrustManager() {
        return trustManager;
    }

    public RegionBlocker getRegionBlocker() {
        return regionBlocker;
    }
}
