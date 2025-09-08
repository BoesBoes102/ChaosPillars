package com.boes.chaospillars.commands;

import com.boes.chaospillars.ChaosPillars;
import com.boes.chaospillars.enums.GameState;
import com.boes.chaospillars.ChaosGame.StartGame;
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

        StartGame chaosGame = new StartGame(plugin, plugin.getGameWorld(), plugin.getScoreboardManager(), plugin.itemGiveIntervalTicks);chaosGame.startGame();


        return true;
    }

}
