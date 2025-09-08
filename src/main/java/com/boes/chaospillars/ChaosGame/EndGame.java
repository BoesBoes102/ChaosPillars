package com.boes.chaospillars.ChaosGame;

import com.boes.chaospillars.ChaosPillars;
import com.boes.chaospillars.enums.GameState;
import com.boes.chaospillars.scoreboard.IdleScoreboard;
import com.boes.chaospillars.stats.PlayerStats;
import com.boes.chaospillars.tasks.ClearAreaTask;
import com.boes.chaospillars.tasks.KillAllEntitiesTask;
import com.boes.chaospillars.tasks.ResetGameTask;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record EndGame(ChaosPillars plugin, World gameWorld, Set<UUID> activePlayers, Set<UUID> quitters,
                      Map<UUID, UUID> lastDamager, Map<UUID, ?> playerStats, org.bukkit.scheduler.BukkitRunnable itemTask) {

    public void endGame() {
        new ResetGameTask(
                plugin,
                gameWorld,
                plugin.gameTask,
                plugin.itemTask,
                plugin.countdownTask
        ).reset();

        Location spawn = gameWorld.getSpawnLocation();
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.teleport(spawn);
            player.setGameMode(GameMode.SPECTATOR);
            player.getInventory().clear();
            new IdleScoreboard((Map<UUID, PlayerStats>) playerStats).updateIdleScoreboard(player);
        }

        new ClearAreaTask(gameWorld).runTaskTimer(plugin, 0L, 1L);
        new KillAllEntitiesTask(gameWorld).run();

        activePlayers.clear();
        quitters.clear();
        lastDamager.clear();

        plugin.setGameState(GameState.IDLE);
    }

}
