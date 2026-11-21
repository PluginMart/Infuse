package com.catadmirer.infuseSMP.expansions;

import org.bukkit.Bukkit;

public class ExpansionHelper {
    public static boolean canUseBetterTeams() {
        return Bukkit.getPluginManager().getPlugin("BetterTeams") != null;
    }

    public static boolean canUsePlaceholderAPI() {
        return Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
    }

    public static boolean canUseWorldGuard() {
        return Bukkit.getPluginManager().getPlugin("WorldGuard") != null;
    }
}
