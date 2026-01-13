package com.boes.chaospillars.scoreboard;

import com.boes.chaospillars.ChaosPillars;
import com.boes.chaospillars.enums.GameState;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

public class ChaosScoreboardManager {

    private final ChaosPillars plugin;
    private final GameScoreboard gameScoreboard;
    private final IdleScoreboard idleScoreboard;

    public ChaosScoreboardManager(ChaosPillars plugin) {
        this.plugin = plugin;
        this.gameScoreboard = new GameScoreboard(plugin);
        this.idleScoreboard = new IdleScoreboard(plugin.playerStats);
    }

    public void startScoreboard() {
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!player.isOnline()) continue;

                if (plugin.getGameState() == GameState.RUNNING) {
                    gameScoreboard.updateGameScoreboard(
                            plugin.getTimer(),
                            plugin.getPowerupCooldown(),
                            plugin.getEventCooldown(),
                            plugin.getActivePlayers()
                    );
                } else {
                    idleScoreboard.updateIdleScoreboard(player);
                }
            }
        });
    }

    public void resetScoreboard() {
        if (!plugin.isEnabled()) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                Scoreboard board = player.getScoreboard();
                for (Objective obj : board.getObjectives()) {
                    obj.unregister();
                }
                player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
            }
        });
    }
}
