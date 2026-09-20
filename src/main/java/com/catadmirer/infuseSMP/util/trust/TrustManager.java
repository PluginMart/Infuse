package com.catadmirer.infuseSMP.util.trust;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jspecify.annotations.NullMarked;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@NullMarked
public interface TrustManager {
    Set<UUID> getTrusted(UUID player);

    default Set<OfflinePlayer> getTrusted(OfflinePlayer player) {
        return getTrusted(player.getUniqueId()).stream().map(Bukkit::getOfflinePlayer).collect(Collectors.toSet());
    }

    void setTrusted(UUID player, Set<UUID> trusted);

    default void setTrusted(OfflinePlayer player, Set<OfflinePlayer> trusted) {
        setTrusted(player.getUniqueId(), trusted.stream().map(OfflinePlayer::getUniqueId).collect(Collectors.toSet()));
    }

    void addTrust(UUID player, UUID trusted);

    default void addTrust(OfflinePlayer player, OfflinePlayer trusted) {
        addTrust(player.getUniqueId(), trusted.getUniqueId());
    }

    void removeTrust(UUID player, UUID trusted);

    default void removeTrust(OfflinePlayer player, OfflinePlayer trusted) {
        removeTrust(player.getUniqueId(), trusted.getUniqueId());
    }

    default boolean doesTrust(UUID player, UUID trusted) {
        if (player.equals(trusted)) return true;

        return getTrusted(player).contains(trusted);
    }

    default boolean doesTrust(OfflinePlayer player, OfflinePlayer trusted) {
        return doesTrust(player.getUniqueId(), trusted.getUniqueId());
    }
}
