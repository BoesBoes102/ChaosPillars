package com.boes.chaospillars.tasks;

import com.boes.chaospillars.ChaosGame.EndGame;
import com.boes.chaospillars.ChaosPillars;
import com.boes.chaospillars.stats.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class TimerTask extends BukkitRunnable {
    private boolean shrinkTriggered = false;

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

        if (!shrinkTriggered && plugin.getTimer() == 60) {
            var border = plugin.getGameWorld().getWorldBorder();
            border.setWarningDistance(0);
            border.setWarningTime(0);
            border.setDamageAmount(2);
            border.setSize(5, 40);
            shrinkTriggered = true;
            Bukkit.broadcastMessage(ChatColor.RED + "World border shrinking to 5! Stay inside.");
        }

        if (plugin.getPowerupCooldown() <= 0) {
            new PowerUpTask(plugin).runTask(plugin);
            plugin.setPowerupCooldown(plugin.getConfig().getInt("game.powerup-cooldown-seconds", 30));
        }

        if (plugin.getEventCooldown() <= 0) {
            new EventTask(plugin, plugin.getGameWorld()).triggerRandomEvent();
            plugin.setEventCooldown(plugin.getConfig().getInt("game.event-cooldown-seconds", 20));
        }

        if (plugin.getTimer() <= 0) {
            Bukkit.broadcastMessage(ChatColor.RED + "Time is up! Game over.");

            int topKills = plugin.getActivePlayers().stream()
                    .map(id -> plugin.getPlayerStats().computeIfAbsent(id, k -> new PlayerStats()).getRoundKills())
                    .max(Integer::compareTo)
                    .orElse(0);

            if (topKills > 0) {
                var topKillers = plugin.getActivePlayers().stream()
                        .filter(id -> plugin.getPlayerStats().computeIfAbsent(id, k -> new PlayerStats()).getRoundKills() == topKills)
                        .toList();

                if (topKillers.size() == 1) {
                    UUID winnerId = topKillers.get(0);
                    Player winner = Bukkit.getPlayer(winnerId);
                    PlayerStats winnerStats = plugin.getPlayerStats().computeIfAbsent(winnerId, k -> new PlayerStats());

                    if (winner != null) {
                        Bukkit.broadcastMessage(ChatColor.GOLD + "🎉 " + winner.getName() + " has the most kills and wins the round! 🎉");
                    } else {
                        Bukkit.broadcastMessage(ChatColor.GOLD + "Player with most kills (offline) wins the round.");
                    }

                    winnerStats.addWin();
                    winnerStats.resetLossStreak();
                    int newStreak = winnerStats.getWinStreak();
                    if (newStreak >= 3 && winner != null) {
                        Bukkit.broadcastMessage(ChatColor.YELLOW + winner.getName() + " is on a " + newStreak + " game win streak!");
                    }

                    for (UUID id : plugin.getActivePlayers()) {
                        if (!id.equals(winnerId)) {
                            PlayerStats stats = plugin.getPlayerStats().computeIfAbsent(id, k -> new PlayerStats());
                            stats.addLoss();
                            stats.addDeath();
                            stats.resetWinStreak();
                        }
                    }
                } else {
                    String names = topKillers.stream()
                            .map(Bukkit::getPlayer)
                            .filter(p -> p != null)
                            .map(Player::getName)
                            .reduce((a, b) -> a + ", " + b)
                            .orElse("Multiple players");

                    Bukkit.broadcastMessage(ChatColor.YELLOW + "It's a tie! Multiple players have " + topKills + " kills: " + names);
                    Bukkit.broadcastMessage(ChatColor.GRAY + "No winners or losers this round.");
                }
            } else {
                Bukkit.broadcastMessage(ChatColor.GRAY + "Nobody had any kills. It's a tie!");
            }

            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                EndGame endGame = new EndGame(plugin);
                endGame.endGame();
            }, 60L);

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
