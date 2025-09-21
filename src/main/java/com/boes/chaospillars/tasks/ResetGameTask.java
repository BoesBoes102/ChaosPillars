package com.boes.chaospillars.tasks;

import com.boes.chaospillars.ChaosPillars;
import org.bukkit.World;

public class ResetGameTask {

    private final World gameWorld;

    private TimerTask timerTask;
    private ItemTask itemTask;
    private LavaCountdownTask lavaCountdownTask;
    private LavaRiseTask lavaRiseTask;

    public ResetGameTask(ChaosPillars plugin, World gameWorld,
                         TimerTask timerTask,
                         ItemTask itemTask,
                         LavaCountdownTask lavaCountdownTask,
                         LavaRiseTask lavaRiseTask) {
        if (plugin == null || gameWorld == null) {
            throw new IllegalArgumentException("Plugin and gameWorld cannot be null");
        }

        this.gameWorld = gameWorld;
        this.timerTask = timerTask;
        this.itemTask = itemTask;
        this.lavaCountdownTask = lavaCountdownTask;
        this.lavaRiseTask = lavaRiseTask;
    }

    public void reset() {
        if (timerTask != null) {
            timerTask.stop();
            timerTask = null;
        }

        if (itemTask != null) {
            itemTask.stop();
            itemTask = null;
        }

        if (lavaCountdownTask != null) {
            lavaCountdownTask.stop();
            lavaCountdownTask = null;
        }

        if (lavaRiseTask != null) {
            lavaRiseTask.stop();
            lavaRiseTask = null;
        }

        gameWorld.setTime(1000);
        gameWorld.setStorm(false);
        gameWorld.setThundering(false);
    }
}
