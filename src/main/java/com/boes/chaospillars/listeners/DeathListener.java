package com.boes.chaospillars.listeners;

import com.boes.chaospillars.ChaosGame.EndGame;
import com.boes.chaospillars.ChaosPillars;
import com.boes.chaospillars.enums.GameState;
import com.boes.chaospillars.stats.PlayerStats;
import org.bukkit.*;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.UUID;

public record DeathListener(ChaosPillars plugin) implements Listener {

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (plugin.getGameState() != GameState.RUNNING) return;

        Player dead = event.getEntity();
        if (!dead.getWorld().equals(plugin.getGameWorld())) return;

        UUID deadId = dead.getUniqueId();
        UUID killerId = null;
        if (dead.getKiller() != null) {
            killerId = dead.getKiller().getUniqueId();
        } else if (plugin.getLastDamager().containsKey(deadId)) {
            killerId = plugin.getLastDamager().get(deadId);
        }

        if (killerId != null && !killerId.equals(deadId)) {
            OfflinePlayer killer = Bukkit.getOfflinePlayer(killerId);
            String killerName = (killer.getName() != null) ? killer.getName() : "Unknown";
            PlayerStats killerStats = plugin.getPlayerStats().computeIfAbsent(killerId, k -> new PlayerStats());
            killerStats.addKill();
            event.setDeathMessage(ChatColor.RED + dead.getName() + ChatColor.GRAY + " was killed by " + ChatColor.YELLOW + killerName);
        } else if (plugin.getQuitters().contains(deadId)) {
            event.setDeathMessage(ChatColor.RED + dead.getName() + ChatColor.GRAY + " left the game.");
            plugin.getQuitters().remove(deadId);
        } else {
            event.setDeathMessage(ChatColor.RED + dead.getName() + ChatColor.GRAY + " died.");
        }

        PlayerStats deadStats = plugin.getPlayerStats().computeIfAbsent(deadId, k -> new PlayerStats());
        deadStats.addDeath();
        deadStats.addLoss();
        deadStats.resetWinStreak();

        plugin.getLastDamager().remove(deadId);
        plugin.getActivePlayers().remove(deadId);

        dead.teleport(plugin.getGameWorld().getSpawnLocation());
        dead.setGameMode(GameMode.SPECTATOR);

        plugin.getServer().getScheduler().runTaskLater(plugin, this::checkWinCondition, 5L);
    }

    private void checkWinCondition() {
        if (plugin.getActivePlayers().size() == 1) {
            UUID winnerId = plugin.getActivePlayers().iterator().next();
            announceWinner(winnerId);
        } else if (plugin.getActivePlayers().isEmpty()) {
            Bukkit.broadcastMessage(ChatColor.GRAY + "Nobody won the Chaos Pillars game.");
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                EndGame endGame = new EndGame(plugin);
                endGame.endGame();
            }, 20L);
        }
    }

    private void announceWinner(UUID winnerId) {
        Player winner = Bukkit.getPlayer(winnerId);
        if (winner != null) {
            Bukkit.broadcastMessage(ChatColor.GOLD + "🎉 " + winner.getName() + " has won the Chaos Pillars game! 🎉");

            PlayerStats winnerStats = plugin.getPlayerStats().computeIfAbsent(winnerId, k -> new PlayerStats());
            winnerStats.addWin();
            winnerStats.resetLossStreak();
            int newStreak = winnerStats.getWinStreak() + 1;
            winnerStats.setWinStreak(newStreak);

            if (newStreak > winnerStats.getHighestWinStreak()) {
                winnerStats.setHighestWinStreak(newStreak);
                if (newStreak >= 3) {
                    Bukkit.broadcastMessage(ChatColor.YELLOW + winner.getName() + " is on a " + newStreak + " game win streak!");
                }
            }

            winner.teleport(plugin.getGameWorld().getSpawnLocation());
            winner.setGameMode(GameMode.SPECTATOR);
            winner.sendTitle(ChatColor.GOLD + "🎉 VICTORY! 🎉", ChatColor.YELLOW + "You won the Chaos Pillars!", 10, 60, 20);
        } else {
            Bukkit.broadcastMessage(ChatColor.GOLD + "The game was won by a player (offline).");
        }


        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            EndGame endGame = new EndGame(plugin);
            endGame.endGame();
        }, 60L);
    }
}
