package com.boes.chaospillars.commands;

import com.boes.chaospillars.ChaosPillars;
import com.boes.chaospillars.ChaosGame.StartGame;
import com.boes.chaospillars.enums.GameState;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public record StartCommand(ChaosPillars plugin) implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (!player.hasPermission("chaospillars.start")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to start the Chaos Pillars game.");
            return true;
        }

        if (plugin.getGameState() != GameState.IDLE) {
            player.sendMessage(ChatColor.RED + "A game is already running!");
            return true;
        }

        if (Bukkit.getOnlinePlayers().size() < 2) {
            player.sendMessage(ChatColor.RED + "Not enough players to start the game. Minimum required: 2");
            return true;
        }

        StartGame startGame = new StartGame(
                plugin,
                plugin.getGameWorld(),
                plugin.getScoreboardManager(),
                plugin.itemGiveIntervalTicks
        );
        startGame.startGame();

        player.sendMessage(ChatColor.GREEN + "Starting Chaos Pillars game...");
        return true;
    }
}
