package com.boes.chaospillars.commands;

import com.boes.chaospillars.ChaosGame.EndGame;
import com.boes.chaospillars.ChaosPillars;
import com.boes.chaospillars.enums.GameState;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public record StopCommand(ChaosPillars plugin) implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (!player.hasPermission("chaospillars.stop")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to stop the Chaos Pillars game.");
            return true;
        }

        if (plugin.getGameState() != GameState.RUNNING) {
            player.sendMessage(ChatColor.RED + "There is no game currently running.");
            return true;
        }

        EndGame endGame = new EndGame(plugin);
        endGame.endGame();

        Bukkit.broadcastMessage(ChatColor.RED + "The Chaos Pillars game has been stopped by " + player.getName() + "!");
        return true;
    }
}
