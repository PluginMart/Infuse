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
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import javax.sql.DataSource;

@NullMarked
public class H2DataManager extends AsyncDataManager {
    private DataCache cache;
    private final DataSource dataSource;

    public H2DataManager(Infuse plugin) {
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException err) {
            Infuse.LOGGER.error("Could not load the H2 driver", err);
        }

        // Creating an empty cache
        cache = new DataCache();

        // Creating the JDBC DataSource
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setUrl("jdbc:h2:./" + plugin.getDataFolder().getPath() + "/data/playerdata");
        dataSource.setUser("infuse");
        dataSource.setPassword("");
        dataSource.setDescription("Infuse PlayerData Storage");

        this.dataSource = dataSource;
    }

    @Override
    public boolean load() {
        final String createPlayerDataTable = "CREATE TABLE IF NOT EXISTS player_data(player UUID PRIMARY KEY NOT NULL, slot_1 INTEGER, slot_2 INTEGER, offhand_control BOOLEAN NOT NULL);";
        final String createTrustTable = "CREATE TABLE IF NOT EXISTS trusts(truster UUID NOT NULL, trusted UUID NOT NULL);";
        final String createCraftedTable = "CREATE TABLE IF NOT EXISTS crafted_effects(effect INTEGER PRIMARY KEY NOT NULL, crafted INTEGER NOT NULL);";

        final String getAllTrusts = "SELECT * FROM trusts;";
        final String getAllPlayerData = "SELECT * FROM player_data;";
        final String getAllCrafted = "SELECT * FROM crafted_effects;";

        try (Connection conn = dataSource.getConnection()) {
            // Creating the tables if they don't exist
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(createPlayerDataTable);
                stmt.execute(createTrustTable);
                stmt.execute(createCraftedTable);
            }

            // Commiting any changes
            conn.commit();

            // Clearing any cached data
            cache = new DataCache();

            // Loading data into the cache
            try (Statement stmt = conn.createStatement()) {
                // Mirroring trusts
                ResultSet results = stmt.executeQuery(getAllTrusts);
                while (results.next()) {
                    UUID player = results.getObject(1, UUID.class);
                    UUID trusted = results.getObject(2, UUID.class);

                    Set<UUID> playerTrusts = cache.allTrusts.computeIfAbsent(player, t -> new HashSet<>());
                    playerTrusts.add(trusted);
                    cache.allTrusts.put(player, playerTrusts);
                }

                results.close();

                // Mirroring player data
                results = stmt.executeQuery(getAllPlayerData);
                while (results.next()) {
                    UUID player = results.getObject(1, UUID.class);

                    int lEffect = results.getInt(2);
                    if (!results.wasNull()) {
                        cache.leftEffects.put(player, lEffect);
                    }

                    int rEffect = results.getInt(3);
                    if (!results.wasNull()) {
                        cache.rightEffects.put(player, rEffect);
                    }

                    boolean offhandControl = results.getBoolean(4);
                    cache.controlModes.put(player, offhandControl);
                }

                results.close();

                // Mirroring crafted effect counts
                results = stmt.executeQuery(getAllCrafted);
                while (results.next()) {
                    int effectId = results.getInt(1);
                    int crafted = results.getInt(2);

                    cache.craftedCounts.put(effectId, crafted);
                }

                results.close();
            }

            Infuse.LOGGER.info("Successfully loaded H2 database!");
        } catch (SQLException err) {
            Infuse.LOGGER.error("Could not open connection to H2 database", err);
        }
    }

    @Override
    public int getExistingCount(InfuseEffect effect) {
        return cache.getExistingCount(effect);
    }

    @Override
    protected void reallySetExistingCount(InfuseEffect effect, int count) {
        // Updating the cache
        cache.setExistingCount(effect, count);

        // Updating the database
        String sql = "INSERT OR REPLACE INTO crafted_effects(effect, crafted) VALUES (?, ?);";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, effect.serialize());
            stmt.setInt(2, count);

            stmt.executeUpdate();
        } catch (SQLException e) {
            Infuse.LOGGER.error("Database error!", e);
        }
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

    @Override
    public Set<OfflinePlayer> getTrusted(OfflinePlayer player) {
        return cache.getTrusted(player);
    }

    @Override
    protected void reallySetTrusted(OfflinePlayer player, Set<OfflinePlayer> allTrusted) {
        // Updating the cache
        cache.setTrusted(player, allTrusted);

        // Updating the database
        Set<OfflinePlayer> trustedCopy = new HashSet<>(allTrusted);

        String findUnused = "SELECT * FROM trusts WHERE truster = ?;";
        String deleteExtra = "DELETE FROM trusts WHERE truster = ? AND trusted = ?;";

        String insertTrusted = "INSERT INTO trusts (truster, trusted) VALUES (?, ?);";

        try (Connection conn = dataSource.getConnection()) {
            // Removing players who are no longer trusted and filtering out players who are already trusted from the copy of the set
            try (PreparedStatement stmt = conn.prepareStatement(findUnused);
                 PreparedStatement delStmt = conn.prepareStatement(deleteExtra)) {
                stmt.setObject(1, player.getUniqueId());
                stmt.execute();
                ResultSet results = stmt.getResultSet();

                delStmt.setObject(1, player.getUniqueId());

                // Looping through the trusted players
                while (results.next()) {
                    OfflinePlayer trusted = Bukkit.getOfflinePlayer(results.getObject(1, UUID.class));

                    // Skipping already trusted players
                    if (trustedCopy.contains(trusted)) {
                        trustedCopy.remove(trusted);
                        continue;
                    }

                    // Removing players who are no longer trusted
                    delStmt.setObject(2, trusted.getUniqueId());
                    delStmt.addBatch();
                }

                // Executing the delete statement
                delStmt.executeBatch();
            } catch (SQLException e) {
                Infuse.LOGGER.error("Database error!", e);
            }

            // Adding the rest of the players to trust
            try (PreparedStatement stmt = conn.prepareStatement(insertTrusted)) {
                stmt.setObject(1, player.getUniqueId());

                for (OfflinePlayer trusted : trustedCopy) {
                    stmt.setObject(2, trusted.getUniqueId());
                    stmt.addBatch();
                }

                stmt.executeBatch();
            } catch (SQLException err) {
                Infuse.LOGGER.error("Failed to insert players back into the database", err);
            }
        } catch (SQLException err) {
            Infuse.LOGGER.info("Failed to connect to database.", err);
        }

        throw new UnsupportedOperationException("Unimplemented method 'setTrusted'");
    }

    @Override
    protected void reallyAddTrust(OfflinePlayer player, OfflinePlayer toTrust) {
        // Updating the cache
        cache.addTrust(player, toTrust);

        // Updating the database
        String insertElem = """
                            INSERT INTO trusts (truster, trusted)
                            SELECT ?, ?
                            WHERE NOT EXISTS (
                                SELECT * FROM trusts WHERE truster = ? AND trusted = ?
                            );""";

        UUID trusterUUID = player.getUniqueId();
        UUID toTrustUUID = toTrust.getUniqueId();

        try (Connection conn = dataSource.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(insertElem)) {
                stmt.setObject(1, trusterUUID);
                stmt.setObject(2, toTrustUUID);
                stmt.setObject(3, trusterUUID);
                stmt.setObject(4, toTrustUUID);
                stmt.execute();
            } catch (SQLException err) {
                Infuse.LOGGER.error("Failed to insert data into database", err);
            }
        } catch (SQLException err) {
            Infuse.LOGGER.info("Failed to connect to database.", err);
        }
    }

    @Override
    protected void reallyRemoveTrust(OfflinePlayer player, OfflinePlayer untrusted) {
        // Updating the cache
        cache.removeTrust(player, untrusted);

        // Updating the database
        String deleteElem = "DELETE FROM trusts WHERE truster = ? AND trusted = ?";

        UUID trusterUUID = player.getUniqueId();
        UUID trustedUUID = untrusted.getUniqueId();

        try (Connection conn = dataSource.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(deleteElem)) {
                stmt.setObject(1, trusterUUID);
                stmt.setObject(2, trustedUUID);
                stmt.execute();
            } catch (SQLException err) {
                Infuse.LOGGER.error("Failed to remove data from the database", err);
            }
        } catch (SQLException err) {
            Infuse.LOGGER.info("Failed to connect to database.", err);
        }
    }

    @Override
    public boolean isTrusted(OfflinePlayer player, OfflinePlayer trusted) {
        return cache.isTrusted(player, trusted);
    }

    @Override
    protected void reallySetEffect(OfflinePlayer player, String slot, @Nullable InfuseEffect effect) {
        // Updating the cache
        cache.setEffect(player, slot, effect);

        // Updating the database
        createNewPlayer(player);

        // Making sure slot is "1" or "2"
        if (!slot.equals("1") && !slot.equals("2")) {
            Infuse.LOGGER.warn("Slot '{}' is not a valid slot.  Please use \"1\" or \"2\"", slot);
            return;
        }

        // Constructing sql based on specified slot
        final String setEffectSQL = "UPDATE player_data SET slot_1 = ? WHERE player = ?;";

        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(setEffectSQL);
            stmt.setObject(1, effect == null ? null : effect.serialize());
            stmt.setObject(2, player.getUniqueId());

            stmt.executeUpdate();
        } catch (SQLException err) {
            Infuse.LOGGER.error("Could not open connection to H2 database", err);
        }
    }

    @Nullable
    @Override
    public InfuseEffect getEffect(OfflinePlayer player, String slot) {
        return cache.getEffect(player, slot);
    }

    @Override
    protected void reallySetControlMode(OfflinePlayer player, String controlMode) {
        // Updating the cache
        cache.setControlMode(player, controlMode);

        // Updating the database
        createNewPlayer(player);

        boolean offhandControls;
        if (controlMode.equals("offhand")) {
            offhandControls = true;
        } else if (controlMode.equals("command")) {
            offhandControls = false;
        } else {
            Infuse.LOGGER.error("Invalid control mode \"{}\".  Please use \"offhand\" or \"command\"", controlMode);
            return;
        }

        String sql = "UPDATE player_data SET offhand_control = ? WHERE player = ?;";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, offhandControls);
            stmt.setObject(2, player.getUniqueId());

            stmt.executeUpdate();
        } catch (SQLException e) {
            Infuse.LOGGER.error("Database error!", e);
        }
    }

    @Override
    public String getControlMode(OfflinePlayer player) {
        return cache.getControlMode(player);
    }

    @Override
    public void applyUpdates() {}
}
