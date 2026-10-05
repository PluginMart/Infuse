package com.catadmirer.infuseSMP.expansions;

import org.bukkit.Bukkit;

public class ExpansionHelper {
    public static boolean canUseBetterTeams() {
        return Bukkit.getPluginManager().isPluginEnabled("BetterTeams");
    }

    public static boolean canUsePlaceholderAPI() {
        return Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
    }

    public static boolean canUseWorldGuard() {
        return Bukkit.getPluginManager().isPluginEnabled("WorldGuard");
    }
}
