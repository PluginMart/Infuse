package com.catadmirer.infuseSMP.managers;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CooldownManager {
    private static final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<String, Long>> durations = new ConcurrentHashMap<>();

    public static void setTimes(UUID playerUUID, String key, long durationSeconds, long cooldownSeconds) {
        setCooldown(playerUUID, key, cooldownSeconds + durationSeconds);
        setDuration(playerUUID, key, durationSeconds);
    }

    public static void setDuration(UUID playerUUID, String key, long seconds) {
        durations.computeIfAbsent(playerUUID, k -> new ConcurrentHashMap<>()).put(key, System.currentTimeMillis() + seconds * 1000);
    }

    public static boolean isEffectActive(UUID playerUUID, String key) {
        return getEffectTimeLeft(playerUUID, key) > 0L;
    }

    public static long getEffectTimeLeft(UUID playerUUID, String key) {
        Map<String, Long> playerDurations = durations.get(playerUUID);
        if (playerDurations != null && playerDurations.containsKey(key)) {
            long timeLeft = playerDurations.get(key) - System.currentTimeMillis();
            return timeLeft > 0 ? timeLeft : 0;
        }

        return 0;
    }

    public static void clearSpecificDuration(UUID playerUUID, String key) {
        Map<String, Long> playerDurations = durations.get(playerUUID);
        if (playerDurations != null) {
            playerDurations.remove(key);
        }
    }

    public static void cleanupExpiredDurations() {
        long currentTime = System.currentTimeMillis();
        for (UUID playerUUID : durations.keySet()) {
            Map<String, Long> playerDurations = durations.get(playerUUID);
            if (playerDurations != null) {
                playerDurations.entrySet().removeIf(entry -> entry.getValue() <= currentTime);
            }
        }

    }

    public static boolean isOnCooldown(UUID playerUUID, String key) {
        return getCooldownTimeLeft(playerUUID, key) > 0L;
    }

    public static void setCooldown(UUID playerUUID, String key, long seconds) {
        cooldowns.computeIfAbsent(playerUUID, k -> new ConcurrentHashMap<>()).put(key, System.currentTimeMillis() + seconds * 1000);
    }

    public static long getCooldownTimeLeft(UUID playerUUID, String key) {
        Map<String, Long> playerCooldowns = cooldowns.get(playerUUID);
        if (playerCooldowns != null && playerCooldowns.containsKey(key)) {
            long timeLeft = playerCooldowns.get(key) - System.currentTimeMillis();
            return timeLeft > 0 ? timeLeft : 0;
        }

        return 0;
    }

    public static void clearSpecificCooldown(UUID playerUUID, String key) {
        Map<String, Long> playerCooldowns = cooldowns.get(playerUUID);
        if (playerCooldowns != null) {
            playerCooldowns.remove(key);
        }
    }

    public static void cleanupExpiredCooldowns() {
        long currentTime = System.currentTimeMillis();

        for (UUID playerUUID : cooldowns.keySet()) {
            Map<String, Long> playerCooldowns = cooldowns.get(playerUUID);
            if (playerCooldowns != null) {
                playerCooldowns.entrySet().removeIf(entry -> entry.getValue() <= currentTime);
            }
        }
    }

    public static void removeAllCooldowns(UUID playerUUID) {
        cooldowns.remove(playerUUID);
    }
}