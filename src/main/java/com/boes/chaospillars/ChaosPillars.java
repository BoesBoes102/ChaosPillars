package com.boes.chaospillars;

import com.boes.chaospillars.ChaosGame.EndGame;
import com.boes.chaospillars.commands.ChaosCommand;
import com.boes.chaospillars.commands.ChaosCommandTab;
import com.boes.chaospillars.enums.GameState;
import com.boes.chaospillars.listeners.*;
import com.boes.chaospillars.scoreboard.ChaosScoreboardManager;
import com.boes.chaospillars.scoreboard.IdleScoreboard;
import com.boes.chaospillars.stats.*;
import com.boes.chaospillars.tasks.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class ChaosPillars extends JavaPlugin implements Listener {
    public int eventCooldown = 20;
    public int powerupCooldown = 30;
    public int timer = 300;
    public Map<UUID, PlayerStats> playerStats = new HashMap<>();
    private final Map<UUID, UUID> lastDamager = new HashMap<>();
    public final Set<UUID> activePlayers = new HashSet<>();
    private final Set<UUID> quitters = new HashSet<>();
    public BukkitRunnable gameTask;
    public ItemTask itemTask;
    public BukkitRunnable countdownTask;
    private World gameWorld;
    private final Set<UUID> frozenPlayers = new HashSet<>();
    private StatsManager statsManager;
    public List<Material> floorBlockTypes = new ArrayList<>();
    public List<Material> pillarBlockTypes = new ArrayList<>();
    public int itemGiveIntervalTicks = 60;
    private LavaCountdownTask lavaCountdownTask;
    private LavaRiseTask lavaRiseTask;
    private ChaosScoreboardManager scoreboardManager;

    private GameState gameState = GameState.IDLE;
    public GameState getGameState() {
        return gameState;
    }
    public void setGameState(GameState state) {
        this.gameState = state;
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        new ReloadConfigTask(this).run();
        scoreboardManager = new ChaosScoreboardManager(this);
        scoreboardManager.resetScoreboard();
        scoreboardManager.startScoreboard();

        SetupTask setupTask = new SetupTask(this);
        gameWorld = setupTask.setupWorld();

        if (gameWorld == null) {
            getLogger().severe("Could not create/load ChaosPillars world. Plugin disabled.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        statsManager = new StatsManager(this);
        playerStats = statsManager.loadStats();

        getLogger().info("Chaos Pillars enabled.");

        ChaosCommand dispatcher = new ChaosCommand(this);
        Objects.requireNonNull(getCommand("chaos")).setExecutor(dispatcher);
        Objects.requireNonNull(getCommand("chaos")).setTabCompleter(new ChaosCommandTab());

        EndGame endGame = new EndGame(
                this,
                gameWorld,
                activePlayers,
                quitters,
                lastDamager,
                playerStats,
                itemTask
        );

        Bukkit.getPluginManager().registerEvents(new WorldListener(gameWorld), this);
        Bukkit.getPluginManager().registerEvents(new NoMobsListener(gameWorld), this);
        Bukkit.getPluginManager().registerEvents(new HeightLimitListener(gameWorld), this);
        Bukkit.getPluginManager().registerEvents(new JoinListener(this, gameWorld), this);
        Bukkit.getPluginManager().registerEvents(new DamageListener(lastDamager, gameWorld), this);
        Bukkit.getPluginManager().registerEvents(new FrozenListener(frozenPlayers, gameWorld), this);
        Bukkit.getPluginManager().registerEvents(new LeaveListener(activePlayers, quitters, gameWorld), this);
        Bukkit.getPluginManager().registerEvents(new DeathListener(playerStats, lastDamager, activePlayers, quitters, endGame::endGame, gameWorld), this);
        Bukkit.getPluginManager().registerEvents(new WorldBorderEnforcerListener(gameWorld, 100), this);

        new ClearAreaTask(gameWorld).runTaskTimer(this, 0L, 1L);

        setGameState(GameState.IDLE);

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.teleport(gameWorld.getSpawnLocation());
            player.setGameMode(GameMode.SPECTATOR);
            new IdleScoreboard(playerStats).updateIdleScoreboard(player);

        }
    }

    public Set<UUID> getActivePlayers() {
        return activePlayers;
    }

    public Set<UUID> getQuitters() {
        return quitters;
    }

    public Map<UUID, UUID> getLastDamager() {
        return lastDamager;
    }

    public World getGameWorld() {
        return gameWorld;
    }

    public ChaosScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }
    public int getEventCooldown() {
        return eventCooldown;
    }

    public int getPowerupCooldown() {
        return powerupCooldown;
    }

    public int getTimer() {
        return timer;
    }

    public LavaCountdownTask getLavaCountdownTask() {
        return lavaCountdownTask;
    }

    public void setLavaCountdownTask(LavaCountdownTask task) {
        this.lavaCountdownTask = task;
    }

    public LavaRiseTask getLavaRiseTask() {
        return lavaRiseTask;
    }

    public void setLavaRiseTask(LavaRiseTask task) {
        this.lavaRiseTask = task;
    }

    @Override
    public void onDisable() {
        new ResetGameTask(
                this,
                gameWorld,
                gameTask,
                itemTask,
                countdownTask
        ).reset();
        scoreboardManager.resetScoreboard();
        statsManager.saveStats(playerStats);
    }
}