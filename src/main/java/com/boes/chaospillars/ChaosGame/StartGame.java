package com.boes.chaospillars.ChaosGame;

import com.boes.chaospillars.ChaosPillars;
import com.boes.chaospillars.enums.GameState;
import com.boes.chaospillars.scoreboard.ChaosScoreboardManager;
import com.boes.chaospillars.stats.PlayerStats;
import com.boes.chaospillars.tasks.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class StartGame {

    private final ChaosPillars plugin;
    private final World gameWorld;
    private final ChaosScoreboardManager scoreboardManager;
    private final int itemGiveIntervalTicks;

    private final Set<UUID> activePlayers = new HashSet<>();
    private final Set<UUID> frozenPlayers = new HashSet<>();
    private final Map<UUID, PlayerStats> playerStats = new HashMap<>();
    private final Set<UUID> quitters = new HashSet<>();
    private final Map<UUID, UUID> lastDamager = new HashMap<>();


    private ItemTask itemTask;

    public StartGame(ChaosPillars plugin, World gameWorld,
                     ChaosScoreboardManager scoreboardManager, int itemGiveIntervalTicks) {
        this.plugin = plugin;
        this.gameWorld = gameWorld;
        this.scoreboardManager = scoreboardManager;
        this.itemGiveIntervalTicks = itemGiveIntervalTicks;
    }

    public void startGame() {
        activePlayers.clear();

        new ResetGameTask(
                plugin,
                gameWorld,
                plugin.gameTask,
                plugin.itemTask,
                plugin.countdownTask
        ).reset();
        new KillAllEntitiesTask(gameWorld).run();

        GameGenerateTask generator = new GameGenerateTask(gameWorld,
                plugin.floorBlockTypes, plugin.pillarBlockTypes);

        int radius = 12;
        int height = 50;
        int baseY = -63;
        int pillarCount = 10;

        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        Collections.shuffle(players);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            List<Location> basePillarLocations = generator.generateArena(radius, height, baseY, pillarCount);

            for (int i = 0; i < players.size() && i < basePillarLocations.size(); i++) {
                Location baseLoc = basePillarLocations.get(i);
                Player player = players.get(i);

                Location tp = baseLoc.clone().add(0.5, height, 0.5);
                player.teleport(tp);
                player.setGameMode(GameMode.SURVIVAL);
                player.setHealth(20);
                player.setFoodLevel(20);
                player.clearActivePotionEffects();
                player.getInventory().clear();

                activePlayers.add(player.getUniqueId());
            }

            frozenPlayers.addAll(activePlayers);
        }, 40L);

        gameWorld.getWorldBorder().setSize(37);
        plugin.setGameState(GameState.COUNTDOWN);

        new BukkitRunnable() {
            int countdown = 8;

            @Override
            public void run() {
                if (countdown == 0) {
                    Bukkit.broadcastMessage(ChatColor.GREEN + "Game Started!");
                    for (UUID uuid : frozenPlayers) {
                        Player player = Bukkit.getPlayer(uuid);
                        if (player != null) {
                            player.sendTitle(ChatColor.GREEN + "Game Started!", "", 10, 40, 10);
                        }
                    }
                    frozenPlayers.clear();
                    plugin.setGameState(GameState.RUNNING);
                    scoreboardManager.resetScoreboard();
                    scoreboardManager.startScoreboard();
                    new StartTimer(plugin, activePlayers, quitters, lastDamager, playerStats, itemTask, gameWorld, plugin.getScoreboardManager());
                    new LavaCountdownTask(plugin, gameWorld);

                    itemTask = new ItemTask(plugin, activePlayers, itemGiveIntervalTicks);
                    itemTask.start();

                    for (UUID uuid : activePlayers) {
                        PlayerStats stats = playerStats.computeIfAbsent(uuid, k -> new PlayerStats());
                        stats.addGamePlayed();
                    }
                    cancel();
                    return;
                }

                for (UUID uuid : activePlayers) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null) {
                        player.sendTitle(ChatColor.YELLOW + "Starting in",
                                ChatColor.RED.toString() + countdown, 0, 20, 0);
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
                    }
                }
                countdown--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }
}
