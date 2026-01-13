package com.boes.chaospillars.ChaosGame;

import com.boes.chaospillars.ChaosPillars;
import com.boes.chaospillars.enums.GameState;
import com.boes.chaospillars.scoreboard.ChaosScoreboardManager;
import com.boes.chaospillars.stats.PlayerStats;
import com.boes.chaospillars.tasks.*;
import com.boes.chaospillars.tasks.TimerTask;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public record StartGame(ChaosPillars plugin, World gameWorld, ChaosScoreboardManager scoreboardManager,
                        int itemGiveIntervalTicks) {

    public void startGame() {
        ResetGameTask resetTask = new ResetGameTask(
                plugin,
                plugin.getGameWorld(),
                plugin.getTimerTask(),
                plugin.getItemTask(),
                plugin.getLavaCountdownTask(),
                plugin.getLavaRiseTask()
        );
        resetTask.reset();


        new KillAllEntitiesTask(plugin.getGameWorld()).run();

        GameGenerateTask generator = new GameGenerateTask(plugin);
        int radius = 12, height = 50, baseY = -63, pillarCount = 10;

        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        Collections.shuffle(players);
        List<Location> basePillarLocations = generator.generateArena(radius, height, baseY, pillarCount);

        plugin.getActivePlayers().clear();
        plugin.getFrozenPlayers().clear();
        for (int i = 0; i < players.size() && i < basePillarLocations.size(); i++) {
            Player player = players.get(i);
            Location tp = basePillarLocations.get(i).clone().add(0.5, height, 0.5);
            player.teleport(tp);
            player.setGameMode(GameMode.SURVIVAL);
            player.setHealth(20);
            player.setFoodLevel(20);
            player.clearActivePotionEffects();
            player.getInventory().clear();

            plugin.getActivePlayers().add(player.getUniqueId());
            plugin.getFrozenPlayers().add(player.getUniqueId());
        }

        int playerCount = players.size();
        boolean useExtraRing = playerCount > 10 || plugin.isForceExtraRing();
        
        double worldBorderSize = useExtraRing ? 61 : 37;
        gameWorld.getWorldBorder().setSize(worldBorderSize);
        plugin.setGameState(GameState.COUNTDOWN);

        new BukkitRunnable() {
            int countdown = 8;

            @Override
            public void run() {
                if (countdown <= 0) {
                    Bukkit.broadcastMessage(ChatColor.GREEN + "Game Started!");
                    for (UUID uuid : plugin.getFrozenPlayers()) {
                        Player player = Bukkit.getPlayer(uuid);
                        if (player != null) {
                            player.sendTitle(ChatColor.GREEN + "Game Started!", "", 10, 40, 10);
                        }
                    }

                    plugin.getFrozenPlayers().clear();
                    plugin.setGameState(GameState.RUNNING);


                    plugin.timerTask = new TimerTask(plugin);
                    plugin.itemTask = new ItemTask(plugin, plugin.itemGiveIntervalTicks);
                    plugin.lavaCountdownTask = new LavaCountdownTask(plugin);
                    plugin.lavaCountdownTask.runTaskTimer(plugin, 20L, 20L);


                    scoreboardManager.resetScoreboard();
                    scoreboardManager.startScoreboard();

                    for (UUID uuid : plugin.getActivePlayers()) {
                        PlayerStats stats = plugin.getPlayerStats().computeIfAbsent(uuid, k -> new PlayerStats());
                        stats.addGamePlayed();
                        stats.resetRoundKills();
                    }

                    cancel();
                    return;
                }

                for (UUID uuid : plugin.getActivePlayers()) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null) {
                        player.sendTitle(ChatColor.YELLOW + "Starting in",
                                ChatColor.RED + String.valueOf(countdown), 0, 20, 0);
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
                    }
                }
                countdown--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }
}
