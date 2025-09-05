package com.boes.chaospillars;


import com.boes.chaospillars.enums.GameState;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChaosCommand implements CommandExecutor, TabCompleter {

    private final ChaosPillars plugin;

    public ChaosCommand(ChaosPillars plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(ChatColor.YELLOW + "/chaos <start|stop|reload|stats>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "start":
                if (!player.hasPermission("chaospillars.start")) {
                    player.sendMessage(ChatColor.RED + "You do not have permission to start the Chaos Pillars game.");
                    return true;
                }
                if (plugin.getGameState() != GameState.IDLE) {
                    player.sendMessage(ChatColor.RED + "Chaos game is already running or counting down!");
                    return true;
                }
                int playerCount = Bukkit.getOnlinePlayers().size();
                if (playerCount < 2) {
                    player.sendMessage(ChatColor.RED + "Not enough players to start Chaos Pillars! Need at least 2.");
                    return true;
                }
                if (playerCount > 10) {
                    player.sendMessage(ChatColor.RED + "Chaos Pillars supports a maximum of 10 players!");
                    return true;
                }
                Bukkit.broadcastMessage(ChatColor.GREEN + "Chaos Pillars game starting!");
                plugin.startGame();
                return true;

            case "stop":
                if (!player.hasPermission("chaospillars.stop")) {
                    player.sendMessage(ChatColor.RED + "You do not have permission to stop the Chaos Pillars game.");
                    return true;
                }
                if (plugin.getGameState() != GameState.RUNNING) {
                    player.sendMessage(ChatColor.RED + "There is no game running (yet)!");
                    return true;
                }
                plugin.endGame();
                if (plugin.countdownTask != null) {
                    plugin.countdownTask.cancel();
                    plugin.countdownTask = null;
                }
                return true;

            case "reload":
                if (!player.hasPermission("chaospillars.reload")) {
                    player.sendMessage(ChatColor.RED + "You do not have permission to reload the Chaos Pillars config.");
                    return true;
                }
                plugin.reloadConfig();
                plugin.reloadGameConfig();
                player.sendMessage(ChatColor.GREEN + "Chaos Pillars config reloaded!");
                return true;

            case "stats":
                PlayerStats stats;
                if (args.length == 2) {
                    String targetName = args[1];
                    OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);


                    if (!target.hasPlayedBefore() && !target.isOnline()) {
                        player.sendMessage(ChatColor.RED + "Player '" + targetName + "' not found.");
                        return true;
                    }

                    stats = plugin.playerStats.getOrDefault(target.getUniqueId(), new PlayerStats());
                    player.sendMessage(ChatColor.GOLD + "=== Chaos Pillars Stats for " + target.getName() + " ===");
                } else {

                    stats = plugin.playerStats.getOrDefault(player.getUniqueId(), new PlayerStats());
                    player.sendMessage(ChatColor.GOLD + "=== Your Chaos Pillars Stats ===");
                }

                player.sendMessage(ChatColor.YELLOW + "Wins: " + ChatColor.WHITE + stats.getWins());
                player.sendMessage(ChatColor.YELLOW + "Games Played: " + ChatColor.WHITE + stats.getGamesPlayed());
                player.sendMessage(ChatColor.YELLOW + "Win Rate: " + ChatColor.WHITE + String.format("%.1f", stats.getWinRate()) + "%");
                player.sendMessage(ChatColor.YELLOW + "Kills: " + ChatColor.WHITE + stats.getKills());
                player.sendMessage(ChatColor.YELLOW + "Deaths: " + ChatColor.WHITE + stats.getDeaths());
                player.sendMessage(ChatColor.YELLOW + "KDR: " + ChatColor.WHITE + String.format("%.2f", stats.getKDR()));
                player.sendMessage(ChatColor.YELLOW + "Win Streak: " + ChatColor.WHITE + stats.getWinStreak());
                player.sendMessage(ChatColor.YELLOW + "Highest Win Streak: " + ChatColor.WHITE + stats.getHighestWinStreak());
                player.sendMessage(ChatColor.YELLOW + "Loss Streak: " + ChatColor.WHITE + stats.getLossStreak());
                player.sendMessage(ChatColor.YELLOW + "Highest Loss Streak: " + ChatColor.WHITE + stats.getHighestLossStreak());
                return true;



            default:
                player.sendMessage(ChatColor.RED + "Unknown subcommand. Use /chaos <start|stop|reload|stats>");
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String @NotNull [] args) {
        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>();
            if (sender.hasPermission("chaospillars.start")) suggestions.add("start");
            if (sender.hasPermission("chaospillars.stop")) suggestions.add("stop");
            if (sender.hasPermission("chaospillars.reload")) suggestions.add("reload");
            suggestions.add("stats");
            return suggestions;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("stats")) {
            String partialName = args[1].toLowerCase();
            List<String> playerNames = new ArrayList<>();
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                String name = onlinePlayer.getName();
                if (name.toLowerCase().startsWith(partialName)) {
                    playerNames.add(name);
                }
            }
            return playerNames;
        }

        return Collections.emptyList();
    }

}
