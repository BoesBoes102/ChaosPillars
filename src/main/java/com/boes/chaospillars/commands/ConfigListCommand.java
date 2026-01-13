package com.boes.chaospillars.commands;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

public class ConfigListCommand implements CommandExecutor {

    private final org.bukkit.plugin.java.JavaPlugin plugin;

    public ConfigListCommand(org.bukkit.plugin.java.JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {

        sender.sendMessage(ChatColor.GOLD + "==== ChaosPillars Config ====");

        FileConfiguration config = plugin.getConfig();
        int timer = config.getInt("game.timer-seconds");
        int powerup = config.getInt("game.powerup-cooldown-seconds");
        int eventCd = config.getInt("game.event-cooldown-seconds");
        int itemInterval = config.getInt("game.item-give-interval-seconds");

        sender.sendMessage(ChatColor.YELLOW + "Game Settings:");
        sender.sendMessage(ChatColor.GRAY + " - timer-seconds: " + ChatColor.AQUA + timer);
        sender.sendMessage(ChatColor.GRAY + " - powerup-cooldown-seconds: " + ChatColor.AQUA + powerup);
        sender.sendMessage(ChatColor.GRAY + " - event-cooldown-seconds: " + ChatColor.AQUA + eventCd);
        sender.sendMessage(ChatColor.GRAY + " - item-give-interval-seconds: " + ChatColor.AQUA + itemInterval);

        listAndValidate(sender, config, "game.floor-block-types", "Floor Blocks");
        listAndValidate(sender, config, "game.pillar-block-types", "Pillar Blocks");

        sender.sendMessage(ChatColor.GOLD + "============================");
        return true;
    }

    private void listAndValidate(CommandSender sender, FileConfiguration config, String path, String title) {
        List<String> values = config.getStringList(path);
        sender.sendMessage(ChatColor.YELLOW + title + ":");
        if (values == null || values.isEmpty()) {
            sender.sendMessage(ChatColor.RED + " - <empty>");
            return;
        }
        for (String raw : values) {
            String name = raw == null ? "" : raw.trim();
            if (name.isEmpty()) {
                sender.sendMessage(ChatColor.RED + " - <blank entry>");
                continue;
            }
            ChatColor color = isValidBlock(name) ? ChatColor.GREEN : ChatColor.RED;
            sender.sendMessage(color + " - " + name);
        }
    }

    private boolean isValidBlock(String name) {
        try {
            Material m = Material.valueOf(name.toUpperCase(Locale.ROOT));
            return m.isBlock() || name.equalsIgnoreCase("AIR");
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
