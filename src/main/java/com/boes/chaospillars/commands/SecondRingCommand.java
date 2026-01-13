package com.boes.chaospillars.commands;

import com.boes.chaospillars.ChaosPillars;
import com.boes.chaospillars.enums.GameState;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SecondRingCommand implements CommandExecutor {

    private final ChaosPillars plugin;

    public SecondRingCommand(ChaosPillars plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can execute this command.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("chaospillars.admin")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to execute this command.");
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.YELLOW + "Usage: /chaos secondring <on|off>");
            return true;
        }

        String mode = args[1].toLowerCase();
        boolean enable = mode.equals("on");

        if (!mode.equals("on") && !mode.equals("off")) {
            player.sendMessage(ChatColor.RED + "Invalid argument. Use 'on' or 'off'.");
            return true;
        }

        plugin.setForceExtraRing(enable);

        if (enable) {
            player.sendMessage(ChatColor.GREEN + "Extra ring mode enabled! The next game will use dual rings even with 10 or fewer players.");
        } else {
            player.sendMessage(ChatColor.GREEN + "Extra ring mode disabled! Dual rings will only be used when there are more than 10 players.");
        }

        return true;
    }
}
