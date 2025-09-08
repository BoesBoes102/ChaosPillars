package com.boes.chaospillars.scoreboard;

import org.bukkit.ChatColor;

public class ScoreboardTranslator {
    public static String translate(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
