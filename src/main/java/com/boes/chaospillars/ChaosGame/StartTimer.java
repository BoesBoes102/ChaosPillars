package com.boes.chaospillars.ChaosGame;

import com.boes.chaospillars.ChaosPillars;
import com.boes.chaospillars.scoreboard.ChaosScoreboardManager;
import com.boes.chaospillars.tasks.ChaosEventTask;
import com.boes.chaospillars.tasks.RandomPositiveEffectTask;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class StartTimer {

    private final ChaosPillars plugin;
    private final Set<UUID> activePlayers;
    private final Set<UUID> quitters;
    private final Map<UUID, UUID> lastDamager;
    private final Map<UUID, ?> playerStats;
    private final BukkitRunnable itemTask;
    private final World gameWorld;
    private final ChaosScoreboardManager scoreboardManager;

    private int timer;
    private int powerupCooldown;
    private int eventCooldown;

    public StartTimer(ChaosPillars plugin,
                      Set<UUID> activePlayers,
                      Set<UUID> quitters,
                      Map<UUID, UUID> lastDamager,
                      Map<UUID, ?> playerStats,
                      BukkitRunnable itemTask,
                      World gameWorld,
                      Object scoreboardManager) {
        this.plugin = plugin;
        this.activePlayers = activePlayers;
        this.quitters = quitters;
        this.lastDamager = lastDamager;
        this.playerStats = playerStats;
        this.itemTask = itemTask;
        this.gameWorld = gameWorld;
        this.scoreboardManager = (ChaosScoreboardManager) scoreboardManager;
    }

    public void startTimer() {
        timer = plugin.getConfig().getInt("game.timer-seconds", 300);
        powerupCooldown = plugin.getConfig().getInt("game.powerup-cooldown-seconds", 30);
        eventCooldown = plugin.getConfig().getInt("game.event-cooldown-seconds", 20);

        BukkitRunnable gameTask = new BukkitRunnable() {
            @Override
            public void run() {
                timer--;
                powerupCooldown--;
                eventCooldown--;

                scoreboardManager.updateGameScoreboard();

                if (powerupCooldown <= 0) {
                    new RandomPositiveEffectTask(activePlayers).runTask(plugin);
                    powerupCooldown = plugin.getConfig().getInt("game.powerup-cooldown-seconds", 30);
                }

                if (eventCooldown <= 0) {
                    ChaosEventTask eventManager = new ChaosEventTask(plugin, gameWorld);
                    eventManager.triggerRandomEvent();
                    eventCooldown = plugin.getConfig().getInt("game.event-cooldown-seconds", 20);
                }

                if (timer <= 0) {
                    Bukkit.broadcastMessage(ChatColor.RED + "Time is up! Game over.");
                    EndGame endGame = new EndGame(
                            plugin,
                            gameWorld,
                            activePlayers,
                            quitters,
                            lastDamager,
                            playerStats,
                            itemTask
                    );
                    endGame.endGame();
                    cancel();
                }
            }
        };

        gameTask.runTaskTimer(plugin, 20L, 20L);
    }
}
