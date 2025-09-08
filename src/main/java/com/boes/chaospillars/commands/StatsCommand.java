package com.boes.chaospillars.commands;

import com.boes.chaospillars.ChaosPillars;
import com.boes.chaospillars.stats.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;

public record StatsCommand(ChaosPillars plugin) implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Map<UUID, PlayerStats> statsMap = plugin.playerStats;
        PlayerStats stats;

        if (args.length == 2) {
            String targetName = args[1];
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

            if (!target.hasPlayedBefore() && !target.isOnline()) {
                player.sendMessage(ChatColor.RED + "Player '" + targetName + "' not found.");
                return true;
            }

            stats = statsMap.getOrDefault(target.getUniqueId(), new PlayerStats());
            player.sendMessage(ChatColor.GOLD + "=== Chaos Pillars Stats for " + target.getName() + " ===");
        } else {
            stats = statsMap.getOrDefault(player.getUniqueId(), new PlayerStats());
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
    }
}
