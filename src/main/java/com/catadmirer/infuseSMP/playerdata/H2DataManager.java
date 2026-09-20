package com.catadmirer.infuseSMP.playerdata;

import com.catadmirer.infuseSMP.Infuse;
import com.catadmirer.infuseSMP.effects.InfuseEffect;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.h2.jdbcx.JdbcDataSource;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;
import javax.sql.DataSource;

@NullMarked
public class H2DataManager extends AbstractDataManager {
    private final Infuse plugin;
    private final DataSource dataSource;

    public H2DataManager(Infuse plugin) {
        this.plugin = plugin;
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException err) {
            Infuse.LOGGER.error("Could not load the H2 driver", err);
        }

        // Creating the JDBC DataSource
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setUrl("jdbc:h2:./" + plugin.getDataFolder().getPath() + "/data/playerdata");
        dataSource.setUser("infuse");
        dataSource.setPassword("");
        dataSource.setDescription("Infuse PlayerData Storage");

        this.dataSource = dataSource;
    }

    @Override
    public void load() {
        final String createPlayerDataTable = "CREATE TABLE IF NOT EXISTS player_data(player UUID PRIMARY KEY NOT NULL, slot_1 VARCHAR(100), slot_2 VARCHAR(100), offhand_control BOOLEAN NOT NULL);";
        final String createTrustTable = "CREATE TABLE IF NOT EXISTS trusts(truster UUID NOT NULL, trusted UUID NOT NULL);";
        final String createCraftedTable = "CREATE TABLE IF NOT EXISTS crafted_effects(effect VARCHAR(100) PRIMARY KEY NOT NULL, crafted INTEGER NOT NULL);";

        final String getAllTrusts = "SELECT * FROM trusts;";
        final String getAllPlayerData = "SELECT * FROM player_data;";
        final String getAllCrafted = "SELECT * FROM crafted_effects;";

        try (Connection conn = dataSource.getConnection()) {
            // Creating the tables if they don't exist
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(createPlayerDataTable);
                stmt.execute(createTrustTable);
                stmt.execute(createCraftedTable);
            } catch (SQLException err) {
                Infuse.LOGGER.error("Could not create tables", err);
            }

            // Loading data into the cache
            try (Statement stmt = conn.createStatement()) {
                // Mirroring trusts
                ResultSet results = stmt.executeQuery(getAllTrusts);

                while (results.next()) {
                    UUID player = results.getObject(1, UUID.class);
                    UUID trusted = results.getObject(2, UUID.class);

                    // Remember to initialize data with super.<method> so we don't execute sql while initializing.
                    allTrusts.computeIfAbsent(player, k -> new HashSet<>()).add(trusted);
                }

                results.close();

                // Mirroring player data
                results = stmt.executeQuery(getAllPlayerData);
                while (results.next()) {
                    UUID player = results.getObject(1, UUID.class);
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(player);

                    String lEffect = results.getString(2);
                    if (!results.wasNull()) {
                        InfuseEffect effect = InfuseEffect.getEffect(key(lEffect));
                        if (effect == null) {
                            Infuse.LOGGER.warn("Invalid effect in {}'s slot 1: {}", offlinePlayer.getName(), lEffect);
                        } else {
                            playerEffects.computeIfAbsent(offlinePlayer, k -> new HashMap<>()).put("1", effect.key());
                        }
                    }

                    String rEffect = results.getString(3);
                    if (!results.wasNull()) {
                        InfuseEffect effect = InfuseEffect.getEffect(key(rEffect));
                        if (effect == null) {
                            Infuse.LOGGER.warn("Invalid effect in {}'s slot 2: {}", offlinePlayer.getName(), rEffect);
                        } else {
                            playerEffects.computeIfAbsent(offlinePlayer, k -> new HashMap<>()).put("2", effect.key());
                        }
                    }

                    boolean offhandControl = results.getBoolean(4);
                    offhandUsers.put(offlinePlayer, offhandControl);
                }

                results.close();

                // Mirroring crafted effect counts
                results = stmt.executeQuery(getAllCrafted);
                while (results.next()) {
                    String effectKey = results.getString(1);
                    int crafted = results.getInt(2);

                    existingCount.put(key(effectKey), crafted);
                }

                results.close();
            }

            Infuse.LOGGER.info("Successfully loaded H2 database!");
        } catch (SQLException err) {
            Infuse.LOGGER.error("Could not open connection to H2 database", err);
        }
    }

    // Data is saved when a setter runs
    @Override
    public void save() {}

    @Override
    public void setExistingCount(InfuseEffect effect, int count) {
        super.setExistingCount(effect, count);

        Bukkit.getAsyncScheduler().runNow(Infuse.getInstance(), t -> {
            // Updating the database
            String sql = "INSERT OR REPLACE INTO crafted_effects(effect, crafted) VALUES (?, ?);";

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, effect.key().toString());
                stmt.setInt(2, count);

                stmt.executeUpdate();
            } catch (SQLException e) {
                Infuse.LOGGER.error("Database error!", e);
            }
        });
    }

    @Override
    public void addTrust(UUID player, UUID trusted) {
        super.addTrust(player, trusted);

        Bukkit.getAsyncScheduler().runNow(plugin, t -> {
            String addTrust = "INSERT INTO trusts (truster, trusted) VALUES (?, ?);";
            try (Connection conn = dataSource.getConnection()) {
                // Adding the rest of the players to trust
                try (PreparedStatement stmt = conn.prepareStatement(addTrust)) {
                    stmt.setObject(1, player);
                    stmt.setObject(2, trusted);

                    stmt.executeUpdate();
                } catch (SQLException err) {
                    Infuse.LOGGER.error("Failed to insert a trust relationship into the table.", err);
                }
            } catch (SQLException err) {
                Infuse.LOGGER.info("Failed to connect to database.", err);
            }
        });
    }

    @Override
    public void removeTrust(UUID player, UUID trusted) {
        super.addTrust(player, trusted);

        Bukkit.getAsyncScheduler().runNow(plugin, t -> {
            String removeTrusted = "DELETE FROM trusts WHERE truster = ? AND trusted = ?;";
            try (Connection conn = dataSource.getConnection()) {
                // Adding the rest of the players to trust
                try (PreparedStatement stmt = conn.prepareStatement(removeTrusted)) {
                    stmt.setObject(1, player);
                    stmt.setObject(2, trusted);

                    stmt.executeUpdate();
                } catch (SQLException err) {
                    Infuse.LOGGER.error("Failed to remove a trust relationship from the table.", err);
                }
            } catch (SQLException err) {
                Infuse.LOGGER.info("Failed to connect to database.", err);
            }
        });
    }

    @Override
    public void setEffect(OfflinePlayer player, String slot, @Nullable InfuseEffect effect) {
        super.setEffect(player, slot, effect);

        // Making sure slot is "1" or "2"
        // Logging is already done in AbstractDataManager
        if (!slot.equals("1") && !slot.equals("2")) return;

        Bukkit.getAsyncScheduler().runNow(plugin, t -> {
            // Updating the database
            createNewPlayer(player);

            // Constructing sql based on specified slot
            final String setEffectSQL = "UPDATE player_data SET slot_" + slot + " = ? WHERE player = ?;";

            try (Connection conn = dataSource.getConnection()) {
                try (PreparedStatement stmt = conn.prepareStatement(setEffectSQL)) {
                    stmt.setString(1, effect == null ? null : effect.key().toString());
                    stmt.setObject(2, player.getUniqueId());

                    stmt.executeUpdate();
                } catch (SQLException err) {
                    Infuse.LOGGER.error("Could not set player {}'s effect in slot {} to {}", player.getName(), slot, effect == null ? "null" : effect.key(), err);
                }
            } catch (SQLException err) {
                Infuse.LOGGER.error("Could not open connection to H2 database", err);
            }
        });
    }

    @Override
    public void setControlMode(OfflinePlayer player, String controlMode) {
        super.setControlMode(player, controlMode);

        Bukkit.getAsyncScheduler().runNow(plugin, t -> {
            // Updating the database
            createNewPlayer(player);

            boolean offhandControls;
            if (controlMode.equals("offhand")) {
                offhandControls = true;
            } else if (controlMode.equals("command")) {
                offhandControls = false;
            } else {
                // Logging handled in AbstractDataManager
                return;
            }

            String sql = "UPDATE player_data SET offhand_control = ? WHERE player = ?;";
            try (Connection conn = dataSource.getConnection()) {

                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setObject(1, offhandControls);
                    stmt.setObject(2, player.getUniqueId());

                    stmt.executeUpdate();
                } catch (SQLException err) {
                    Infuse.LOGGER.error("Could not set player {}'s offhand control to {}", player.getName(), offhandControls, err);
                }
            } catch (SQLException e) {
                Infuse.LOGGER.error("Database error!", e);
            }
        });
    }

    /**
     * Creates a new player object in the database.
     * Both effects default to null, and the offhand_control defaults to false.
     * If a row for the player already exists, nothing happens.
     *
     * @param player The player to create an entry for.
     */
    private void createNewPlayer(OfflinePlayer player) {
        // All calls are already async.  This does not need to be run through the service.
        String newPlayer = "INSERT OR IGNORE INTO player_data (player, offhand_control) VALUES (?, FALSE);";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(newPlayer)) {
            stmt.setObject(1, player.getUniqueId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            Infuse.LOGGER.error("Database error!", e);
        }
    }
}
