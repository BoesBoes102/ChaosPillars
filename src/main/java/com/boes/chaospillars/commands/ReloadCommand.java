package com.boes.chaospillars.commands;

import com.boes.chaospillars.ChaosPillars;
import com.boes.chaospillars.tasks.ReloadConfigTask;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public record ReloadCommand(ChaosPillars plugin) implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (!player.hasPermission("chaospillars.reload")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to reload the Chaos Pillars config.");
            return true;
        }

        plugin.reloadConfig();
        new ReloadConfigTask(plugin).run();
        player.sendMessage(ChatColor.GREEN + "Chaos Pillars config reloaded!");
        return true;
    }
}
