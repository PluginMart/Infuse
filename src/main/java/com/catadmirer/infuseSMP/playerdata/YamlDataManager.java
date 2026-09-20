package com.catadmirer.infuseSMP.playerdata;

import com.catadmirer.infuseSMP.Infuse;
import com.catadmirer.infuseSMP.effects.InfuseEffect;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@NullMarked
public class YamlDataManager implements DataManager {
    private final File dataFile;
    private final YamlConfiguration config;

    public YamlDataManager() {
        this.dataFile = new File(Infuse.getInstance().getDataFolder(), "data/playerdata.yml");
        this.config = YamlConfiguration.loadConfiguration(dataFile);
    }

    @Override
    public void load() {
        // Creating the file if it doesn't exist.
        createFile();

        // Loading the config
        try {
            config.load(dataFile);
            Infuse.LOGGER.info("Successfully loaded YAML data!");
        } catch (InvalidConfigurationException err) {
            Infuse.LOGGER.warn("{} contains an invalid YAML configuration.  Verify the contents of the file.", dataFile.getName());
        } catch (IOException err) {
            Infuse.LOGGER.error("Could not find {}.  Check that it exists.", dataFile.getName());
        }

    }

    private void save() {
        // Creating the file if it doesn't exist.
        createFile();

        // Saving the config
        try {
            config.save(dataFile);
            Infuse.LOGGER.info("Saved {}", dataFile.getName());
        } catch (IOException e) {
            Infuse.LOGGER.warn("Could not save {}.  Make sure the user has write permissions.", dataFile.getName());
        }

    }

    /** Creates the config file. If it doesn't exist, it loads the default config. */
    public void createFile() {
        Infuse.getInstance().saveResource("config.yml", false);
    }

    @Override
    public int getExistingCount(InfuseEffect effect) {
        return config.getInt("effects-crafted." + effect.getKey(), 0);
    }

    @Override
    public void setExistingCount(InfuseEffect effect, int count) {
        config.set("effects-crafted." + effect.getKey(), count);
    }

    @Override
    public Set<OfflinePlayer> getTrusted(OfflinePlayer player) {
        return config.getStringList(player.getUniqueId() + ".trust").stream().map(UUID::fromString).map(Bukkit::getOfflinePlayer).collect(Collectors.toSet());
    }

    @Override
    public void setTrusted(OfflinePlayer player, Set<OfflinePlayer> allTrusted) {
        config.set(player.getUniqueId() + ".trust", allTrusted.stream().map(OfflinePlayer::getUniqueId).toList());

        save();
    }

    @Override
    public void setEffect(OfflinePlayer player, String slot, @Nullable InfuseEffect effect) {
        // Making sure slot is "1" or "2"
        if (!slot.equals("1") && !slot.equals("2")) {
            Infuse.LOGGER.warn("Slot '{}' is not a valid slot.  Please use \"1\" or \"2\"", slot);
            return;
        }

        if (effect == null) {
            config.set(player.getUniqueId() + "." + slot, null);
        } else {
            config.set(player.getUniqueId() + "." + slot, effect.getKey());
        }
        save();
    }

    @Nullable
    @Override
    public InfuseEffect getEffect(OfflinePlayer player, String slot) {
        String effectKey = config.getString(player.getUniqueId() + "." + slot, null);
        InfuseEffect effect = InfuseEffect.fromString(effectKey);
        if (effectKey != null && effect == null) {
            Infuse.LOGGER.warn("No valid ability found for the equipped effect.");
        }

        return effect;
    }

    @Override
    public void setControlMode(OfflinePlayer player, String defaultMode) {
        config.set(player.getUniqueId() + ".controls", defaultMode);
        save();
    }

    @Override
    public String getControlMode(OfflinePlayer player) {
        return config.getString(player.getUniqueId() + ".controls", "command");
    }

    @Override
    public void applyUpdates() {
        try {
            Scanner scanner = new Scanner(dataFile);
            StringBuilder inputBuffer = new StringBuilder();
            String line;

            while (scanner.hasNextLine()) {
                line = scanner.nextLine();

                // Replacing old configs
                if (line.startsWith("effects-crafted")) {
                    line = line.replace("effects-crafted", "existing-effects");
                }
                inputBuffer.append(line);
                inputBuffer.append('\n');
            }
            scanner.close();

            // Emptying the string buffer back into the file
            FileOutputStream fileOut = new FileOutputStream(dataFile);
            fileOut.write(inputBuffer.toString().getBytes());
            fileOut.close();
        } catch (IOException ignored) {}
    }
}