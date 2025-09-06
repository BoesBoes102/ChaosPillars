package com.boes.chaospillars.listeners;

import com.boes.chaospillars.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record DeathListener(
        Map<UUID, PlayerStats> playerStats,
        Map<UUID, UUID> lastDamager,
        Set<UUID> activePlayers,
        Set<UUID> quitters,
        Runnable endGameCallback,
        World gameWorld
) implements Listener {

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player dead = event.getEntity();

        if (!dead.getWorld().equals(gameWorld)) return;

        UUID deadId = dead.getUniqueId();
        UUID killerId = null;

        if (dead.getKiller() != null) {
            killerId = dead.getKiller().getUniqueId();
        } else if (lastDamager.containsKey(deadId)) {
            killerId = lastDamager.get(deadId);
        }

        PlayerStats deadStats = playerStats.computeIfAbsent(deadId, k -> new PlayerStats());
        deadStats.addDeath();
        deadStats.addLoss();
        deadStats.setWinStreak(0);

        if (quitters.contains(deadId)) {
            event.setDeathMessage(ChatColor.RED + dead.getName() + ChatColor.GRAY + " left the game.");
            quitters.remove(deadId);
        } else if (killerId != null && !killerId.equals(deadId)) {
            OfflinePlayer killer = Bukkit.getOfflinePlayer(killerId);
            PlayerStats killerStats = playerStats.computeIfAbsent(killerId, k -> new PlayerStats());
            killerStats.addKill();

            event.setDeathMessage(ChatColor.RED + dead.getName() + ChatColor.GRAY + " was killed by " + ChatColor.YELLOW + killer.getName());
        } else {
            event.setDeathMessage(ChatColor.RED + dead.getName() + ChatColor.GRAY + " died.");
        }

        lastDamager.remove(deadId);
        activePlayers.remove(deadId);
        dead.setGameMode(GameMode.SPECTATOR);

        if (activePlayers.size() == 1) {
            UUID winnerId = activePlayers.iterator().next();
            Player winner = Bukkit.getPlayer(winnerId);
            if (winner != null) {
                Bukkit.broadcastMessage(ChatColor.GOLD + winner.getName() + " has won the Chaos Pillars game!");
                PlayerStats winnerStats = playerStats.computeIfAbsent(winnerId, k -> new PlayerStats());
                winnerStats.addWin();
                winnerStats.resetLossStreak();

                int newStreak = winnerStats.getWinStreak() + 1;
                winnerStats.setWinStreak(newStreak);

                if (newStreak > winnerStats.getHighestWinStreak()) {
                    winnerStats.setHighestWinStreak(newStreak);
                }
            }
            endGameCallback.run();
        } else if (activePlayers.isEmpty()) {
            Bukkit.broadcastMessage(ChatColor.GRAY + "Nobody won the Chaos Pillars game.");
            endGameCallback.run();
        }
    }
}
