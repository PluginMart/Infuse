package com.catadmirer.infuseSMP.playerdata;

import com.catadmirer.infuseSMP.Infuse;
import com.catadmirer.infuseSMP.managers.EffectMapping;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.h2.jdbcx.JdbcDataSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class H2DataManager implements DataManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("Infuse_Storage");
    private final DataSource dataSource;

    public H2DataManager(Infuse plugin) {
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException err) {
            LOGGER.error("Could not load the H2 driver", err);
        }

        // Creating the JDBC DataSource
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setUrl("jdbc:h2:./" + plugin.getDataFolder().getPath() + "/data/playerdata");
        dataSource.setUser("infuse");
        dataSource.setPassword("");
        dataSource.setDescription("Infuse Playerdata Storage");

        this.dataSource = dataSource;
    }

    @Override
    public boolean load() {
        final String createPlayerDataDb = "CREATE TABLE IF NOT EXISTS player_data(player UUID PRIMARY KEY, slot_1 INTEGER NOT NULL, slot_2 INTEGER NOT NULL, offhand_control BOOLEAN NOT NULL);";
        final String createTrustDb = "CREATE TABLE IF NOT EXISTS trusts(truster UUID NOT NULL, trusted UUID NOT NULL);";
        final String createCraftedDb = "CREATE TABLE IF NOT EXISTS crafted_effects(effect INTEGER PRIMARY KEY, count INTEGER NOT NULL);";

        try (Connection conn = dataSource.getConnection()) {
            // Creating the tables if they dont exist
            Statement stmt = conn.createStatement();
            stmt.execute(createPlayerDataDb);
            stmt.execute(createTrustDb);
            stmt.execute(createCraftedDb);
            stmt.close();

            // Commiting any changes
            conn.commit();
            return true;
        } catch (SQLException err) {
            LOGGER.error("Could not open connection to H2 database", err);
        }

        return false;
    }


    @Override
    public int getCrafted(EffectMapping effect) {

        return 0;
    }

    @Override
    public void setCrafted(EffectMapping effect, int crafted) {
        String mergeStr = "MERGE INTO crafted_effects (?, ?)";
        
    }

    @Override
    public @NotNull Set<OfflinePlayer> getTrusted(@NotNull OfflinePlayer truster) {
        String selectStr = "SELECT trusted FROM trusts WHERE truster = ?";
        UUID trusterUUID = truster.getUniqueId();

        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(selectStr);
            stmt.setObject(1, trusterUUID);

            Set<UUID> trustedUUIDs = new HashSet<>();
            ResultSet result = stmt.executeQuery();
            while (result.next()) {
                try {
                    trustedUUIDs.add(result.getObject(1, UUID.class));
                } catch (SQLException err) {
                    LOGGER.warn("Invalid UUID in SQL results.  Skipping value.");
                }
            }

            return trustedUUIDs.stream().map(Bukkit::getOfflinePlayer).collect(Collectors.toSet());
        } catch (SQLException err) {
            LOGGER.info("Failed to connect to database.", err);
        }
        return null;
    }

    @Override
    public void setTrusted(@NotNull OfflinePlayer truster, @NotNull Set<OfflinePlayer> trusted) {
        throw new UnsupportedOperationException("Unimplemented method 'setTrusted'");
    }

    @Override
    public void addTrust(@NotNull OfflinePlayer truster, @NotNull OfflinePlayer toTrust) {
        String insertElem = """
                            INSERT INTO trusts (truster, trusted)
                            SELECT ?, ?
                            WHERE NOT EXISTS (
                                SELECT * FROM trusts WHERE truster = ? AND trusted = ?
                            );""";
        
        UUID trusterUUID = truster.getUniqueId();
        UUID toTrustUUID = toTrust.getUniqueId();

        try (Connection conn = dataSource.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(insertElem)) {
                stmt.setObject(1, trusterUUID);
                stmt.setObject(2, toTrustUUID);
                stmt.setObject(3, trusterUUID);
                stmt.setObject(4, toTrustUUID);
                stmt.execute();
            } catch (SQLException err) {
                LOGGER.error("Failed to insert data into database", err);
            }
        } catch (SQLException err) {
            LOGGER.info("Failed to connect to database.", err);
        }
    }

    @Override
    public void removeTrust(@NotNull OfflinePlayer truster, @NotNull OfflinePlayer toRemove) {
        String deleteElem = "DELETE FROM trusts WHERE truster = ? AND trusted = ?";
        
        UUID trusterUUID = truster.getUniqueId();
        UUID trustedUUID = toRemove.getUniqueId();

        try (Connection conn = dataSource.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(deleteElem)) {
                stmt.setObject(1, trusterUUID);
                stmt.setObject(2, trustedUUID);
                stmt.execute();
            } catch (SQLException err) {
                LOGGER.error("Failed to remove data from the database", err);
            }
        } catch (SQLException err) {
            LOGGER.info("Failed to connect to database.", err);
        }
    }

    @Override
    public boolean isTrusted(@NotNull OfflinePlayer truster, @NotNull OfflinePlayer toCheck) {
        String selectStr = "SELECT trusted FROM trusts WHERE truster = ? AND trusted = ?";

        UUID trusterUUID = truster.getUniqueId();
        UUID trustedUUID = toCheck.getUniqueId();

        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(selectStr);
            stmt.setObject(1, trusterUUID);
            stmt.setObject(2, trustedUUID);

            return stmt.executeQuery().next();
        } catch (SQLException err) {
            LOGGER.info("Failed to connect to database.", err);
        }

        return false;
    }

    public void insertEmptyPlayerData(@NotNull UUID playerUUID) {
        String insertElem = """
                            INSERT INTO player_data (player, slot_1, slot_2, offhand_control)
                            SELECT ?, 0, 0, FALSE
                            WHERE NOT EXISTS (
                                SELECT * FROM player_data WHERE player = ?
                            );""";

        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(insertElem);

            stmt.close();
        } catch(SQLException err) {

        }
    }

    @Override
    public void setEffect(@NotNull UUID playerUUID, @NotNull String slot, @NotNull EffectMapping effect) {
        insertEmptyPlayerData(playerUUID);
        
        String updateSlot1 = "UPDATE player_data SET slot_1 = ? WHERE player = ?";
        String updateSlot2 = "UPDATE player_data SET slot_2 = ? WHERE player = ?";

        //CREATE TABLE IF NOT EXISTS player_data(player UUID PRIMARY KEY, slot_1 INTEGER NOT NULL, slot_2 INTEGER NOT NULL, offhand_control BOOLEAN NOT NULL);
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setEffect'");
    }

    @Override
    public @Nullable EffectMapping getEffect(@NotNull UUID playerUUID, @NotNull String slot) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getEffect'");
    }

    @Override
    public boolean hasEffect(OfflinePlayer player, EffectMapping effect, boolean differentiateAugmented, String slot) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'hasEffect'");
    }

    @Override
    public void removeEffect(@NotNull UUID playerUUID, @NotNull String slot) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'removeEffect'");
    }

    @Override
    public void setControlMode(@NotNull UUID playerUUID, @NotNull String defaultMode) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setControlMode'");
    }

    @Override
    public @NotNull String getControlMode(@NotNull UUID playerUUID) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getControlMode'");
    }
}
