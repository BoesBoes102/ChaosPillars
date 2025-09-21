package com.boes.chaospillars.tasks;

import com.boes.chaospillars.ChaosPillars;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.scheduler.BukkitRunnable;

public class TimerTask extends BukkitRunnable {

    private final ChaosPillars plugin;

    public TimerTask(ChaosPillars plugin) {
        this.plugin = plugin;
        start();
    }

    private void start() {
        plugin.setTimer(plugin.getConfig().getInt("game.timer-seconds", 300));
        plugin.setPowerupCooldown(plugin.getConfig().getInt("game.powerup-cooldown-seconds", 30));
        plugin.setEventCooldown(plugin.getConfig().getInt("game.event-cooldown-seconds", 20));

        this.runTaskTimer(plugin, 20L, 20L);
    }

    @Override
    public void run() {
        plugin.setPowerupCooldown(Math.max(plugin.getPowerupCooldown() - 1, 0));
        plugin.setEventCooldown(Math.max(plugin.getEventCooldown() - 1, 0));
        plugin.setTimer(Math.max(plugin.getTimer() - 1, 0));

        if (plugin.getPowerupCooldown() <= 0) {
            new RandomPositiveEffectTask(plugin).runTask(plugin);
            plugin.setPowerupCooldown(plugin.getConfig().getInt("game.powerup-cooldown-seconds", 30));
        }

        if (plugin.getEventCooldown() <= 0) {
            new ChaosEventTask(plugin, plugin.getGameWorld()).triggerRandomEvent();
            plugin.setEventCooldown(plugin.getConfig().getInt("game.event-cooldown-seconds", 20));
        }

        if (plugin.getTimer() <= 0) {
            Bukkit.broadcastMessage(ChatColor.RED + "Time is up! Game over.");
            cancel();

        }

        plugin.getGameScoreboard().updateGameScoreboard(
                plugin.getTimer(),
                plugin.getPowerupCooldown(),
                plugin.getEventCooldown(),
                plugin.getActivePlayers()
        );
    }

    public void stop() {
        cancel();
    }
}
