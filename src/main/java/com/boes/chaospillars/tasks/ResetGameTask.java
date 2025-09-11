package com.boes.chaospillars.tasks;

import com.boes.chaospillars.ChaosPillars;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;

public class ResetGameTask {

    private final ChaosPillars plugin;
    private final World gameWorld;
    private BukkitRunnable gameTask;
    private ItemTask itemTask;
    private BukkitRunnable countdownTask;

    public ResetGameTask(ChaosPillars plugin,
                         World gameWorld,
                         BukkitRunnable gameTask,
                         ItemTask itemTask,
                         BukkitRunnable countdownTask) {
        this.plugin = plugin;
        this.gameWorld = gameWorld;
        this.gameTask = gameTask;
        this.itemTask = itemTask;
        this.countdownTask = countdownTask;
    }

    public void reset() {
        if (gameTask != null) {
            gameTask.cancel();
            gameTask = null;
            plugin.gameTask = null;
        }

        if (itemTask != null) {
            itemTask.stop();
            itemTask = null;
            plugin.itemTask = null;
        }

        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
            plugin.countdownTask = null;
        }

        if (plugin.getLavaCountdownTask() != null) {
            plugin.getLavaCountdownTask().stop();
            plugin.setLavaCountdownTask(null);
        }

        if (plugin.getLavaRiseTask() != null) {
            plugin.getLavaRiseTask().stop();
            plugin.setLavaRiseTask(null);
        }

        if (gameWorld != null) {
            gameWorld.setTime(1000);
            gameWorld.setStorm(false);
            gameWorld.setThundering(false);
        }
    }
}
