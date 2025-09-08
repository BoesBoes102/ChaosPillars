package com.boes.chaospillars.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChaosCommandTab implements TabCompleter {

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {

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
                if (onlinePlayer.getName().toLowerCase().startsWith(partialName)) {
                    playerNames.add(onlinePlayer.getName());
                }
            }
            return playerNames;
        }

        return Collections.emptyList();
    }
}
