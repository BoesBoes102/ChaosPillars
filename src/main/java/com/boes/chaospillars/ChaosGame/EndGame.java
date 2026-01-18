package com.boes.chaospillars.ChaosGame;

import com.boes.chaospillars.ChaosPillars;
import com.boes.chaospillars.enums.GameState;
import com.boes.chaospillars.scoreboard.ChaosScoreboardManager;
import com.boes.chaospillars.scoreboard.IdleScoreboard;
import com.boes.chaospillars.tasks.ClearAreaTask;
import com.boes.chaospillars.tasks.KillAllEntitiesTask;
import com.boes.chaospillars.tasks.ResetGameTask;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public record EndGame(ChaosPillars plugin) {

    public void endGame() {
        var border = plugin.getGameWorld().getWorldBorder();
        border.setWarningDistance(0);
        border.setWarningTime(0);
        border.setDamageAmount(2);
        border.setSize(37, 0);

        ResetGameTask resetTask = new ResetGameTask(
                plugin,
                plugin.getGameWorld(),
                plugin.getTimerTask(),
                plugin.getItemTask(),
                plugin.getLavaCountdownTask(),
                plugin.getLavaRiseTask()
        );
        resetTask.reset();


        Location spawn = plugin.getGameWorld().getSpawnLocation();
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.teleport(spawn);
            player.setGameMode(GameMode.SPECTATOR);
            player.getInventory().clear();
            new IdleScoreboard(plugin.getPlayerStats()).updateIdleScoreboard(player);
        }

        new ClearAreaTask(this.plugin()).runTaskTimer(this.plugin(), 0L, 1L);
        new KillAllEntitiesTask(plugin.getGameWorld()).run();

        plugin.getActivePlayers().clear();
        plugin.getQuitters().clear();
        plugin.getLastDamager().clear();
        plugin.getThunderstruckPlayers().clear();

        ChaosScoreboardManager scoreboardManager = plugin.getScoreboardManager();
        scoreboardManager.resetScoreboard();
        scoreboardManager.startScoreboard();

        Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin(), () -> {
            plugin.setGameState(GameState.IDLE);
        }, 60L);
    }
}
