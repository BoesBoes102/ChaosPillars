package com.boes.chaospillars.tasks;

import com.boes.chaospillars.ChaosPillars;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class LavaCountdownTask extends BukkitRunnable {

    private final ChaosPillars plugin;
    private final int lavaDelay = 150;
    private int countdown;
    private BossBar lavaBossBar;

    public LavaCountdownTask(ChaosPillars plugin) {
        this.plugin = plugin;
        this.countdown = plugin.timer - lavaDelay;

        lavaBossBar = Bukkit.createBossBar(
                ChatColor.RED + "Lava in " + countdown + "s",
                BarColor.RED,
                BarStyle.SOLID
        );

        for (UUID uuid : plugin.getActivePlayers()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) lavaBossBar.addPlayer(player);
        }
        lavaBossBar.setVisible(true);
    }

    public void start() {
        this.runTaskTimer(plugin, 0L, 20L);
    }

    @Override
    public void run() {
        for (UUID uuid : plugin.getActivePlayers()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && !lavaBossBar.getPlayers().contains(player)) {
                lavaBossBar.addPlayer(player);
            }
        }

        if (countdown <= 0) {
            plugin.lavaRiseTask = new LavaRiseTask(plugin);
            plugin.lavaRiseTask.start();
            stop();
            return;
        }

        lavaBossBar.setTitle(ChatColor.RED + "Lava in " + countdown + "s");
        int total = plugin.timer - lavaDelay;
        if (total <= 0) total = 1;
        double progress = (double) countdown / total;
        progress = Math.max(0.0, Math.min(1.0, progress));
        lavaBossBar.setProgress(progress);

        countdown--;
    }

    public void stop() {
        this.cancel();
        if (lavaBossBar != null) {
            lavaBossBar.removeAll();
            lavaBossBar = null;
        }
    }
}
