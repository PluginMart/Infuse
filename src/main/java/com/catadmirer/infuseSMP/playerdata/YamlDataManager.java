package com.catadmirer.infuseSMP.playerdata;

import com.catadmirer.infuseSMP.Infuse;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jspecify.annotations.NullMarked;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@NullMarked
public class YamlDataManager extends AbstractDataManager {
    private final File dataFile;

    public YamlDataManager(Infuse plugin) {
        this.dataFile = new File(plugin.getDataFolder(), "data/playerdata.yml");
    }

    public void load() {
        // Creating the file if it doesn't exist.
        // If the function returns false, the load function fails too.
        if (!createFile(false)) return;

        // Loading the config
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.load(dataFile);
            Infuse.LOGGER.info("Successfully loaded {}", dataFile.getName());
        } catch (InvalidConfigurationException err) {
            Infuse.LOGGER.warn("{} contains an invalid YAML configuration.  Verify the contents of the file.", dataFile.getName());
            return;
        } catch (IOException err) {
            Infuse.LOGGER.error("Could not find {}.  Check that it exists.", dataFile.getName());
            return;
        }

        Object oldCrafted = config.get("existing-effects");
        if (oldCrafted != null) {
            config.set("existing_effects", oldCrafted);
            config.set("existing-effects", null);
        }
        oldCrafted = config.get("effects-crafted");
        if (oldCrafted != null) {
            config.set("existing-effects", oldCrafted);
            config.set("effects-crafted", null);
        }
    }

    public void save() {
        // Creating the file if it doesn't exist.
        // If the function returns false, the load function fails too.
        if (!createFile(false)) return;

        // Creating the yaml to write
        YamlConfiguration config = new YamlConfiguration();
        existingCount.forEach((key, count) -> config.set("existing-effects." + key, count));
        allTrusts.forEach((user, trusts) -> config.set(user + ".trust", trusts.stream().map(UUID::toString).toList()));
        playerEffects.forEach((user, effects) -> effects.forEach((slot, key) -> config.set(user + "." + slot, key.asString())));
        offhandUsers.forEach((user, mode) -> config.set(user + ".controls", mode));

        // Saving the config
        try {
            config.save(dataFile);
            Infuse.LOGGER.info("Saved {}", dataFile.getName());
        } catch (IOException e) {
            Infuse.LOGGER.warn("Could not save {}.  Make sure the user has write permissions.", dataFile.getName());
        }
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
}