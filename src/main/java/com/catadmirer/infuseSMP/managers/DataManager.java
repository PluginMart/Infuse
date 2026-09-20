package com.catadmirer.infuseSMP.managers;

import com.catadmirer.infuseSMP.Infuse;
import com.catadmirer.infuseSMP.effects.InfuseEffect;
import com.catadmirer.infuseSMP.util.trust.TrustManager;
import net.kyori.adventure.key.Key;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NullMarked;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@NullMarked
public class DataManager implements TrustManager {
    private final File dataFile;
    private final YamlConfiguration config;

    public DataManager(Infuse plugin) {
        this.dataFile = new File(plugin.getDataFolder(), "data/playerdata.yml");
        this.config = YamlConfiguration.loadConfiguration(dataFile);
    }

    /**
     * Reloads the configuration.
     *
     * @return Whether the configuration was loaded successfully.
     */
    public boolean load() {
        // Creating the file if it doesn't exist.
        // If the function returns false, the load function fails too.
        if (!createFile(false)) {
            return false;
        }

        // Loading the config
        try {
            config.load(dataFile);
            Infuse.LOGGER.info("Successfully loaded {}", dataFile.getName());
            return true;
        } catch (InvalidConfigurationException err) {
            Infuse.LOGGER.warn("{} contains an invalid YAML configuration.  Verify the contents of the file.", dataFile.getName());
        } catch (IOException err) {
            Infuse.LOGGER.error("Could not find {}.  Check that it exists.", dataFile.getName());
        }

        return false;
    }

    /**
     * Writes the config to the file.
     *
     * @return Whether or not the config was successfully written.
     */
    public boolean save() {
        // Creating the file if it doesn't exist.
        // If the function returns false, the load function fails too.
        if (!createFile(false)) {
            return false;
        }

        // Saving the config
        try {
            config.save(dataFile);
            Infuse.LOGGER.info("Saved {}", dataFile.getName());
            return true;
        } catch (IOException e) {
            Infuse.LOGGER.warn("Could not save {}.  Make sure the user has write permissions.", dataFile.getName());
        }

        return false;
    }

    /**
     * Creating the config file. If it doesn't exist, it loads the default config. If the file does
     * exist, it will only replace it if the parameter is true.
     *
     * @param replace Whether or not to replace the config file with the default configs.
     * @return Whether or not the file was created successfully.
     */
    public boolean createFile(boolean replace) {
        if (dataFile.exists() && replace) {
            dataFile.delete();
        } else if (dataFile.exists()) {
            return true;
        }

        // Creating the file if it doesn't exist.
        try {
            dataFile.getParentFile().mkdirs();
            dataFile.createNewFile();
        } catch (IOException e) {
            Infuse.LOGGER.error("Could not create {}.  Make sure the user has the right permissions.", dataFile.getName());
            return false;
        }

        return true;
    }

    public int getExistingCount(InfuseEffect effect) {
        return config.getInt("existing-effects." + effect.key().asString(), 0);
    }

    public void setExistingCount(InfuseEffect effect, int crafted) {
        config.set("existing-effects." + effect.key().asString(), crafted);

        save();
    }

    @Deprecated
    @Override
    public Set<UUID> getTrusted(UUID player) {
        return config.getStringList(player + ".trust").stream().map(UUID::fromString).collect(Collectors.toSet());
    }

    @Deprecated
    @Override
    public void setTrusted(UUID player, Set<UUID> trusted) {
        config.set(player + ".trust", trusted.stream().map(UUID::toString).toList());
        save();
    }

    @Deprecated
    @Override
    public void addTrust(UUID player, UUID trusted) {
        Set<UUID> trustedPlayers = getTrusted(player);
        trustedPlayers.add(trusted);

        setTrusted(player, trustedPlayers);
    }

    @Deprecated
    @Override
    public void removeTrust(UUID player, UUID trusted) {
        Set<UUID> trustedSet = getTrusted(player);
        trustedSet.remove(trusted);

        setTrusted(player, trustedSet);
    }

    public void setEffect(UUID owner, String slot, @Nullable InfuseEffect effect) {
        String key = owner.toString() + "." + slot;
        if (effect == null) {
            config.set(key, null);
        } else {
            config.set(key, effect.key().asString());
        }
        save();
    }

    @Nullable
    public InfuseEffect getEffect(UUID playerUUID, String slot) {
        String effectKey = config.getString(playerUUID.toString() + "." + slot, null);
        if (effectKey == null) return null;

        Key key;
        if (effectKey.contains(":")) {
            key = Key.key(effectKey);
        } else {
            key = Key.key("infuse", effectKey);
        }

        InfuseEffect effect = InfuseEffect.getEffect(key);
        if (effect == null) {
            Infuse.LOGGER.warn("No valid ability found for the equipped effect.");
        }

        return effect;
    }

    public boolean hasEffect(OfflinePlayer player, InfuseEffect effect) {
        return hasEffect(player, effect, false);
    }

    public boolean hasEffect(OfflinePlayer player, InfuseEffect effect, boolean differentiateAugmented) {
        return hasEffect(player, effect, differentiateAugmented, "1") || hasEffect(player, effect, differentiateAugmented, "2");
    }

    public boolean hasEffect(OfflinePlayer player, InfuseEffect effect, String slot) {
        return hasEffect(player, effect, false, slot);
    }

    public boolean hasEffect(OfflinePlayer player, InfuseEffect effect, boolean differentiateAugmented, String slot) {
        InfuseEffect equippedEffect = getEffect(player.getUniqueId(), slot);

        if (equippedEffect == null) return false;

        if (differentiateAugmented) {
            return effect.equals(equippedEffect);
        }

        return effect.getId() == equippedEffect.getId();
    }

    public void removeEffect(UUID playerUUID, String slot) {
        config.set(playerUUID.toString() + "." + slot, null);
        save();
    }

    public void setControlMode(UUID playerUUID, String defaultMode) {
        config.set(playerUUID.toString() + ".controls", defaultMode);
        save();
    }

    public String getControlMode(UUID playerUUID) {
        return config.getString(playerUUID.toString() + ".controls", "offhand");
    }

    public void applyUpdates() {
        Object oldCrafted = config.get("effects-crafted");
        if (oldCrafted != null) {
            config.set("existing-effects", oldCrafted);
            config.set("effects-crafted", null);
        }

        save();
    }
}
