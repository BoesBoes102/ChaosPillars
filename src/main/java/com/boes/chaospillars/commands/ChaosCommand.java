package com.boes.chaospillars.commands;

import com.boes.chaospillars.ChaosPillars;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class ChaosCommand implements CommandExecutor {

    private final StartCommand start;
    private final StopCommand stop;
    private final ReloadCommand reload;
    private final StatsCommand stats;
    private final SecondRingCommand SecondRingCommand;
    private final ConfigListCommand configList;

    public ChaosCommand(ChaosPillars plugin) {
        this.start = new StartCommand(plugin);
        this.stop = new StopCommand(plugin);
        this.reload = new ReloadCommand(plugin);
        this.stats = new StatsCommand(plugin);
        this.SecondRingCommand = new SecondRingCommand(plugin);
        this.configList = new ConfigListCommand(plugin);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 0) return false;

        switch (args[0].toLowerCase()) {
            case "start" -> start.onCommand(sender, command, label, args);
            case "stop" -> stop.onCommand(sender, command, label, args);
            case "reload" -> reload.onCommand(sender, command, label, args);
            case "stats" -> stats.onCommand(sender, command, label, args);
            case "secondring" -> SecondRingCommand.onCommand(sender, command, label, args);
            case "configlist" -> configList.onCommand(sender, command, label, args);
            default -> sender.sendMessage("Unknown subcommand.");
        }
        return true;
    }
}
