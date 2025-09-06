package com.boes.chaospillars.tasks;

import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class ResetGameTask {
    private final World gameWorld;
    private BukkitTask gameTask;
    private BukkitTask itemTask;
    private BukkitTask countdownTask;

    public ResetGameTask(World gameWorld, BukkitRunnable gameTask, BukkitRunnable itemTask, BukkitRunnable countdownTask) {
        this.gameWorld = gameWorld;
        this.gameTask = (BukkitTask) gameTask;
        this.itemTask = (BukkitTask) itemTask;
        this.countdownTask = (BukkitTask) countdownTask;
    }

    public void reset() {
        if (gameTask != null) {
            gameTask.cancel();
            gameTask = null;
        }
        if (itemTask != null) {
            itemTask.cancel();
            itemTask = null;
        }
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        if (gameWorld != null) {
            gameWorld.setTime(1000);
            gameWorld.setStorm(false);
            gameWorld.setThundering(false);
        }
    }
}
