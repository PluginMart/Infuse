package com.catadmirer.infuseSMP;

import com.catadmirer.infuseSMP.effects.InfuseEffect;
import net.kyori.adventure.key.Key;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class MainConfig {
    public final File file;
    public final FileConfiguration config;
    public final Infuse plugin;

    public MainConfig(Infuse plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "config.yml");
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    /**
     * Reloads the configuration.
     */
    public void load() {
        // Creating the file if it doesn't exist.
        createFile();

        // Loading the config
        try {
            config.load(file);
            Infuse.LOGGER.info("Successfully loaded {}", file.getName());
        } catch (InvalidConfigurationException e) {
            Infuse.LOGGER.warn("{} contains an invalid YAML configuration.  Verify the contents of the file.", file.getName());
        } catch (IOException e) {
            Infuse.LOGGER.error("Could not find {}.  Check that it exists.", file.getName(), e);
        }
    }

    /**
     * Writes the config to the file.
     */
    public void save() {
        // Creating the file if it doesn't exist.
        createFile();

        // Saving the config
        try {
            config.save(file);
            Infuse.LOGGER.info("Saved {}", file.getName());
        } catch (IOException e) {
            Infuse.LOGGER.warn("Could not save {}.  Make sure the user has write permissions.", file.getName());
        }
    }

    /**
     * Creates the default config file.<br>
     * Does not override any existing config.
     */
    public void createFile() {
        createFile(false);
    }

    /**
     * Creates the default config file.<br>
     *
     * @param override If true, any old config is replaced with the new version.
     */
    public void createFile(boolean override) {
        if (file.exists() && !override) return;

        plugin.saveResource(file.getName(), override);
    }

    /**
     * Creates a backup of the config file.<br>
     * Just adds ".bak" to the end of the file name.
     */
    public void backupConfig() {
        try {
            Files.copy(file.toPath(), Paths.get(file.getPath() + ".bak"), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            Infuse.LOGGER.error("Could not backup config {}", file.getName(), e);
        }
    }

    /**
     * Copies a config value from one config to another.<br>
     * If the old config doesn't have the key, nothing happens.
     *
     * @param oldKey The key of oldConfig to copy the value from.
     * @param newKey The key of newConfig to copy the value into.
     * @param oldConfig The config to copy the value from.
     * @param newConfig The config to copy the value into.
     */
    public void copyConfig(String oldKey, String newKey, FileConfiguration oldConfig, FileConfiguration newConfig) {
        if (oldConfig.get(oldKey) == null) return;

        newConfig.set(newKey, oldConfig.get(oldKey));
    }

    /** Applies changes to the config and carries across old settings. */
    public void applyUpdates() {
        if (this.config.getString("config_version") == null) {
            // Backing up the old config and loading the new one
            backupConfig();
            createFile(true);

            // Creating a second instance of MainConfig to put modified values into
            MainConfig newMainConfig = new MainConfig(plugin);
            newMainConfig.load();

            FileConfiguration newConfig = newMainConfig.config;

            Infuse.LOGGER.info("Old configuration version has been found, updating configuration. This may take a minute.");

            // Copying old configs
            newConfig.getKeys(true).forEach(key -> {
                // Skipping keys with more children
                if (newConfig.isConfigurationSection(key)) return;

                // Skipping keys not in the old config
                if (!config.contains(key)) return;

                // Copying values from the old config
                newConfig.set(key, config.get(key));
            });

            // Copying old configs whose keys changed
            copyConfig("ritual_duration", "rituals.duration", config, newConfig);
            copyConfig("ritual_duration_ender", "rituals.ender_duration", config, newConfig);
            copyConfig("regular_effect_broadcast", "rituals.broadcast_regular", config, newConfig);
            copyConfig("ritual_beacon", "rituals.beacon", config, newConfig);
            copyConfig("enable_discord_broadcasts", "rituals.send_webhooks", config, newConfig);
            copyConfig("discord_webhook_url", "rituals.webhook_url", config, newConfig);

            // effect_drops changed what it interprets, so this helps remap it.
            String oldDrops = config.getString("effect_drops", "");
            switch (oldDrops.toLowerCase()) {
                case "1" -> newConfig.set("effect_drops", "prefer_1");
                case "2" -> newConfig.set("effect_drops", "prefer_2");
                case "none", "prefer_1", "prefer_2", "only_1", "only_2" -> newConfig.set("effect_drops", oldDrops.toLowerCase());
            }

            copyConfig("emerald.lock_duration_seconds", "emerald.passive.lock_duration_seconds", config, newConfig);
            copyConfig("emerald.xp_stolen_per_hit", "emerald.passive.xp_stolen_per_hit", config, newConfig);
            copyConfig("emerald.xp_stolen_percent", "emerald.passive.xp_stolen_percent", config, newConfig);
            copyConfig("emerald.percent_xp_to_share", "emerald.passive.percent_xp_to_share", config, newConfig);
            copyConfig("emerald.enchantment.looting_level", "emerald.passive.looting_level", config, newConfig);
            copyConfig("emerald.blacklisted-worlds", "emerald.blacklisted_worlds", config, newConfig);
            copyConfig("emerald.multipler-xp.standard", "emerald.passive.xp_multiplier", config, newConfig);
            copyConfig("emerald.multipler-xp.use_effect", "emerald.spark.xp_multiplier", config, newConfig);

            copyConfig("ender.spark.max-distance", "ender.spark.max_distance", config, newConfig);
            copyConfig("ender.blacklisted-worlds", "ender.blacklisted_worlds", config, newConfig);

            copyConfig("feather.blacklisted-worlds", "feather.blacklisted_worlds", config, newConfig);

            copyConfig("fire.passive.walk-speed", "fire.passive.lava_walk_speed", config, newConfig);
            copyConfig("fire.spark.explosion-radius", "fire.spark.explosion_radius", config, newConfig);
            copyConfig("fire.blacklisted-worlds", "fire.blacklisted_worlds", config, newConfig);

            copyConfig("frost.passive.snow-changing-radius", "frost.passive.snow_changing_radius", config, newConfig);
            copyConfig("frost.passive.walk-speed", "frost.passive.powdered_snow_walk_speed", config, newConfig);
            copyConfig("frost.blacklisted-worlds", "frost.blacklisted_worlds", config, newConfig);

            copyConfig("haste.enchantment.fortune_level", "haste.passive.fortune_level", config, newConfig);
            copyConfig("haste.enchantment.efficiency_level", "haste.passive.efficiency_level", config, newConfig);
            copyConfig("haste.enchantment.unbreaking_level", "haste.passive.unbreaking_level", config, newConfig);
            copyConfig("haste.blacklisted-worlds", "haste.blacklisted_worlds", config, newConfig);

            copyConfig("heart.blacklisted-worlds", "heart.blacklisted_worlds", config, newConfig);

            copyConfig("invis.blacklisted-worlds", "invis.blacklisted_worlds", config, newConfig);

            copyConfig("ocean_pulling.pull.interval", "ocean.spark.pull_interval", config, newConfig);
            copyConfig("ocean_pulling.pull.radius", "ocean.spark.pull_radius", config, newConfig);
            copyConfig("ocean_pulling.pull.strength", "ocean.spark.pull_strength", config, newConfig);
            copyConfig("ocean.passive.drown-strength", "ocean.passive.drown_strength", config, newConfig);
            copyConfig("ocean.passive.drown-damage", "ocean.passive.drown_damage", config, newConfig);
            copyConfig("ocean.spark.drown-strength", "ocean.spark.drown_strength", config, newConfig);
            copyConfig("ocean.spark.drown-damage", "ocean.spark.drown_damage", config, newConfig);
            copyConfig("ocean.blacklisted-worlds", "ocean.blacklisted_worlds", config, newConfig);

            copyConfig("regen.spark.heal-trusted-radius", "regen.spark.heal_trusted_radius", config, newConfig);
            copyConfig("regen.blacklisted-worlds", "regen.blacklisted_worlds", config, newConfig);

            copyConfig("speed.dashMultiplier", "speed.spark.dash_multiplier", config, newConfig);
            copyConfig("speed.playerVelocityMultiplier", "speed.spark.player_velocity_multiplier", config, newConfig);
            copyConfig("speed.blacklisted-worlds", "speed.blacklisted_worlds", config, newConfig);

            copyConfig("strength.blacklisted-worlds", "strength.blacklisted_worlds", config, newConfig);

            copyConfig("thunder.spark.base-radius", "thunder.spark.base_radius", config, newConfig);
            copyConfig("thunder.spark.per-player-boost-radius", "thunder.spark.per_player_boost_radius", config, newConfig);
            copyConfig("thunder.blacklisted-worlds", "thunder.blacklisted_worlds", config, newConfig);

            copyConfig("extra_effects.Apophis", "apophis.enabled", config, newConfig);
            copyConfig("apophis.xp_stolen_per_hit", "apophis.passive.xp_stolen_per_hit", config, newConfig);
            copyConfig("apophis.xp_stolen_percent", "apophis.passive.xp_stolen_percent", config, newConfig);
            copyConfig("apophis.percent_xp_to_share", "apophis.passive.percent_xp_to_share", config, newConfig);
            copyConfig("apophis.lock_duration_seconds", "apophis.passive.lock_duration_seconds", config, newConfig);
            copyConfig("apophis.spark.explosion-radius", "apophis.spark.explosion_radius", config, newConfig);
            copyConfig("apophis.passive.walk-speed", "apophis.passive.lava_walk_speed", config, newConfig);
            copyConfig("apophis.enchantment.looting_level", "apophis.passive.looting_level", config, newConfig);
            copyConfig("apophis.multipler-xp.standard", "apophis.passive.xp_multiplier", config, newConfig);
            copyConfig("apophis.multipler-xp.use_effect", "apophis.spark.xp_multiplier", config, newConfig);
            copyConfig("apophis.blacklisted-worlds", "apophis.blacklisted_worlds", config, newConfig);

            copyConfig("extra_effects.Thief", "thief.enabled", config, newConfig);
            copyConfig("thief.blacklisted-worlds", "thief.blacklisted_worlds", config, newConfig);

            // Overwriting the config
            newMainConfig.save();
            load();
        }
    }

    //
    // General Configs
    //

    public String lang() {
        return config.getString("lang", "en_US");
    }

    public boolean allowInfiniteEffects() {
        return config.getBoolean("allow_infinite_effects", false);
    }

    public boolean emptyEffectIcon() {
        return config.getBoolean("empty_effect_icon", true);
    }

    public boolean playerHeadDrops() {
        return config.getBoolean("player_head_drops", true);
    }

    public boolean brewingGui() {
        return config.getBoolean("brewing_gui", true);
    }

    public boolean dropOnNaturalDeath() {
        return config.getBoolean("drop_on_natural_death", true);
    }

    //
    // Ritual Configs
    //

    public int ritualDuration() {
        return config.getInt("rituals.duration", 600);
    }

    public int ritualDurationEnder() {
        return config.getInt("rituals.ender_duration", 3600);
    }

    public boolean regularBroadcast() {
        return config.getBoolean("rituals.broadcast_regular", true);
    }

    public boolean enableDiscordBroadcasts() {
        return config.getBoolean("rituals.send_webhooks", false);
    }

    public String discordWebhookUrl() {
        return config.getString("rituals.webhook_url", "");
    }

    public boolean ritualBeacon() {
        return config.getBoolean("rituals.beacon", true);
    }

    public boolean useImmortalBrewers() {
        return config.getBoolean("rituals.immortal_brewing_stands", true);
    }

    //
    // General Configs part 2
    //

    public String effectDrops() {
        return config.getString("effect_drops", "random");
    }

    public int hitCounterDecaySeconds() {
        return config.getInt("hit_counter_decay_seconds", 15);
    }

    public boolean joinEffectsEnabled() {
        return config.getBoolean("join_effects_enabled", false);
    }

    public List<InfuseEffect> joinEffects() {
        return config.getStringList("join_effects").stream().map(v -> key(v)).map(InfuseEffect::getEffect).filter(Objects::nonNull).toList();
    }

    private Key key(String input) {
        if (input.contains(":")) return Key.key(input);
        return Key.key("infuse", input);
    }

    public boolean enableBetterTeams() {
        return config.getBoolean("betterteams.enabled", false);
    }

    public boolean betterTeamsTrustAllies() {
        return config.getBoolean("betterteams.trust_allies", false);
    }

    //
    // Generic Effect Configs
    //

    /**
     * Gets the amount of each effect that can be crafted
     *
     * @param effect The effect to check
     * @return The number of effects that can be crafted of the specified {@link InfuseEffect}.
     */
    public int getCraftLimit(InfuseEffect effect) {
        List<Integer> craftLimits = config.getIntegerList("craft_limits." + effect.getPlainKey());

        if (craftLimits.size() != 2) {
            Infuse.LOGGER.error("Craft limits are required to be a list of 2 integers.  Found {} entries for effect {}", craftLimits.size(), effect.getPlainKey());
            Infuse.LOGGER.error("Returning default limits");

            return effect.isAugmented() ? 1 : 3;
        }

        return craftLimits.get(effect.isAugmented() ? 0 : 1);
    }

    public List<NamespacedKey> getBlacklistedWorlds(InfuseEffect effect) {
        return config.getStringList(effect.getPlainKey() + ".blacklisted_worlds")
                .stream()
                .filter(Objects::nonNull)
                .map(NamespacedKey::fromString)
                .toList();
    }

    public long cooldown(InfuseEffect effect) {
        return config.getLong(effect.getPlainKey() + ".cooldown." + (effect.isAugmented() ? "augmented" : "default"));
    }

    public long duration(InfuseEffect effect) {
        return config.getLong(effect.getPlainKey() + ".duration." + (effect.isAugmented() ? "augmented" : "default"));
    }

    //
    // Emerald Configs
    //

    public double emeraldLockDurationSeconds() {
        return config.getDouble("emerald.lock_duration_seconds", 10);
    }

    public int emeraldExpPerHit() {
        return config.getInt("emerald.passive.xp_stolen_per_hit", 15);
    }

    public float emeraldExpPercent() {
        return Math.clamp((float) config.getDouble("emerald.passive.xp_stolen_percent", 1), 0, 1);
    }

    public float emeraldPercentExpToShare() {
        return Math.clamp((float) config.getDouble("emerald.passive.percent_xp_to_share", 0.5), 0, 1);
    }

    public int emeraldLootingLevel() {
        return config.getInt("emerald.passive.looting_level", 5);
    }

    public String storageMode() {
        return config.getString("storage_mode", "yaml");
    }

    public double emeraldMultiplierStandard() {
        return config.getDouble("emerald.passive.xp_multiplier", 2);
    }

    public double emeraldMultiplierUseEffect() {
        return config.getDouble("emerald.spark.xp_multiplier", 4);
    }

    //
    // Ender Configs
    //

    public double enderPassiveRadius() {
        return config.getDouble("ender.passive.radius", 10);
    }

    public int enderSparkMaxDistance() {
        return config.getInt("ender.spark.max_distance", 15);
    }

    //
    // Feather Configs
    //

    public double featherLandRadius() {
        return config.getDouble("feather.land.radius", 4);
    }

    public double featherLandDamage() {
        return config.getDouble("feather.land.damage", 8);
    }

    //
    // Fire Configs
    //

    public double firePassiveWalkSpeed() {
        return config.getDouble("fire.passive.lava_walk_speed", 0.6);
    }

    public double fireSparkRadius() {
        return config.getDouble("fire.spark.radius", 5);
    }

    public double fireSparkExplosionRadius() {
        return config.getDouble("fire.spark.explosion_radius", 5);
    }

    //
    // Frost Configs
    //

    public int frostPassiveSnowChangingRadius() {
        return config.getInt("frost.passive.snow_changing_radius", 3);
    }

    public double frostPassiveWalkSpeed() {
        return config.getDouble("frost.passive.powdered_snow_walk_speed", 0.6);
    }

    public double frostSparkRadius() {
        return config.getDouble("frost.spark.radius", 5);
    }

    //
    // Haste Configs
    //

    public int hasteFortuneLevel() {
        return config.getInt("haste.enchantment.fortune_level", 5);
    }

    public int hasteEfficiencyLevel() {
        return config.getInt("haste.enchantment.efficiency_level", 10);
    }

    public int hasteUnbreakingLevel() {
        return config.getInt("haste.enchantment.unbreaking_level", 5);
    }

    //
    // Heart Configs
    //

    // Nothing to configure :(

    //
    // Invis Configs
    //

    public boolean invisHideKills() {
        return config.getBoolean("invis.hide_kills", false);
    }

    public boolean invisHideDeaths() {
        return config.getBoolean("invis.hide_deaths", false);
    }

    //
    // Ocean Configs
    //

    public int oceanPullInterval() {
        return config.getInt("ocean.spark.pull_interval", 20);
    }

    public int oceanPullRadius() {
        return config.getInt("ocean.spark.pull_radius", 5);
    }

    public double oceanPullStrength() {
        return config.getDouble("ocean.spark.pull_strength", 0.3);
    }

    public int oceanPassiveDrownStrength() {
        return config.getInt("ocean.passive.drown_strength", 5);
    }

    public int oceanPassiveDrownDamage() {
        return config.getInt("ocean.passive.drown_damage", 1);
    }

    public int oceanSparkDrownStrength() {
        return config.getInt("ocean.spark.drown_strength", 20);
    }

    public int oceanSparkDrownDamage() {
        return config.getInt("ocean.spark.drown_damage", 2);
    }

    //
    // Regen Configs
    //

    public double regenSparkHealTrustedRadius() {
        return config.getDouble("regen.spark.heal_trusted_radius", 5);
    }

    //
    // Speed Configs
    //

    public int speedDashMultiplier() {
        return config.getInt("speed.spark.dash_multiplier", 2);
    }

    public int speedPlayerVelocityMultiplier() {
        return config.getInt("speed.spark.player_velocity_multiplier", 2);
    }

    //
    // Strength Configs
    //

    // Nothing to configure :(

    //
    // Thunder Configs
    //

    public double thunderSparkBaseRadius() {
        return config.getDouble("thunder.spark.base_radius", 10);
    }

    public double thunderSparkPerPlayerBoostRadius() {
        return config.getDouble("thunder.spark.per_player_boost_radius", 0.3);
    }

    public int thunderSparkStrikesPerPlayer() {
        return config.getInt("thunder.spark.strikes_per_player", 3);
    }

    //
    // Apophis Configs
    //

    public boolean enableApophis() {
        return config.getBoolean("apophis.enabled", false);
    }

    public int apophisExpPerHit() {
        return config.getInt("apophis.passive.xp_stolen_per_hit", 15);
    }

    public float apophisExpPercent() {
        return Math.clamp((float) config.getDouble("apophis.passive.xp_stolen_percent", 1), 0, 1);
    }

    public float apophisPercentExpToShare() {
        return Math.clamp((float) config.getDouble("apophis.passive.percent_xp_to_share", 0.5), 0, 1);
    }

    public double apophisLockDurationSeconds() {
        return config.getDouble("apophis.passive.lock_duration_seconds", 10);
    }

    public int apophisLootingLevel() {
        return config.getInt("apophis.passive.looting_level", 5);
    }

    public double apophisLavaWalkSpeed() {
        return config.getDouble("apophis.passive.lava_walk_speed", 0.6);
    }

    public int apophisXpMultiplierStandard() {
        return config.getInt("apophis.passive.multiplier_xp", 2);
    }

    public double apophisSparkRadius() {
        return config.getDouble("apophis.spark.radius", 5);
    }

    public double apophisSparkExplosionRadius() {
        return config.getDouble("apophis.spark.explosion_radius", 5);
    }

    public int apophisXpMultiplierSpark() {
        return config.getInt("apophis.spark.multiplier_xp", 4);
    }

    //
    // Thief Configs
    //

    public boolean enableThief() {
        return config.getBoolean("thief.enabled", false);
    }
}
