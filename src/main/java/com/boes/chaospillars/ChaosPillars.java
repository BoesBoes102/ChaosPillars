package com.boes.chaospillars;

import com.boes.chaospillars.commands.ChaosCommand;
import com.boes.chaospillars.commands.ChaosCommandTab;
import com.boes.chaospillars.config.ConfigValidator;
import com.boes.chaospillars.config.ReloadConfigTask;
import com.boes.chaospillars.enums.GameState;
import com.boes.chaospillars.listeners.*;
import com.boes.chaospillars.scoreboard.ChaosScoreboardManager;
import com.boes.chaospillars.scoreboard.GameScoreboard;
import com.boes.chaospillars.scoreboard.IdleScoreboard;
import com.boes.chaospillars.stats.PlayerStats;
import com.boes.chaospillars.stats.StatsManager;
import com.boes.chaospillars.tasks.*;
import com.boes.chaospillars.tasks.TimerTask;
import org.bukkit.*;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class ChaosPillars extends JavaPlugin implements Listener {

    public int eventCooldown = 20;
    public int powerupCooldown = 30;
    public int timer = 300;
    public int itemGiveIntervalTicks = 60;
    public List<Material> floorBlockTypes = new ArrayList<>();
    public List<Material> pillarBlockTypes = new ArrayList<>();

    public Map<UUID, PlayerStats> playerStats = new HashMap<>();
    public Set<UUID> activePlayers = new HashSet<>();
    public Set<UUID> frozenPlayers = new HashSet<>();
    public Set<UUID> quitters = new HashSet<>();
    public Map<UUID, UUID> lastDamager = new HashMap<>();
    private GameState gameState = GameState.IDLE;
    private GameScoreboard gameScoreboard;
    private IdleScoreboard idleScoreboard;

    public TimerTask timerTask;
    public ItemTask itemTask;
    public LavaCountdownTask lavaCountdownTask;
    public LavaRiseTask lavaRiseTask;

    private StatsManager statsManager;
    private ChaosScoreboardManager scoreboardManager;
    private World gameWorld;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        ConfigValidator validator = new ConfigValidator(this);
        if (!validator.validateConfig()) {
            getLogger().severe("Configuration validation failed! Plugin disabled.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        new ReloadConfigTask(this).run();
        gameScoreboard = new GameScoreboard(this);
        scoreboardManager = new ChaosScoreboardManager(this);
        idleScoreboard = new IdleScoreboard(playerStats);
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
        statsManager.loadStats();

        getLogger().info("Chaos Pillars enabled successfully!");

        ChaosCommand dispatcher = new ChaosCommand(this);
        Objects.requireNonNull(getCommand("chaos")).setExecutor(dispatcher);
        Objects.requireNonNull(getCommand("chaos")).setTabCompleter(new ChaosCommandTab());

        registerEventListeners();

        new ClearAreaTask(this).runTaskTimer(this, 0L, 1L);


        gameState = GameState.IDLE;
    }

    private void registerEventListeners() {
        Bukkit.getPluginManager().registerEvents(new WorldListener(this), this);
        Bukkit.getPluginManager().registerEvents(new NoMobsListener(this), this);
        Bukkit.getPluginManager().registerEvents(new HeightLimitListener(this), this);
        Bukkit.getPluginManager().registerEvents(new JoinListener(this), this);
        Bukkit.getPluginManager().registerEvents(new DamageListener(this), this);
        Bukkit.getPluginManager().registerEvents(new FrozenListener(this), this);
        Bukkit.getPluginManager().registerEvents(new LeaveListener(this), this);
        Bukkit.getPluginManager().registerEvents(new DeathListener(this), this);
        Bukkit.getPluginManager().registerEvents(new WorldBorderEnforcerListener(this, 100), this);
        Bukkit.getPluginManager().registerEvents(new PlayerRespawnListener(this), this);
    }

    public TimerTask getTimerTask() {
        return timerTask;
    }

    public ItemTask getItemTask() {
        return itemTask;
    }

    public LavaCountdownTask getLavaCountdownTask() {
        return lavaCountdownTask;
    }

    public LavaRiseTask getLavaRiseTask() {
        return lavaRiseTask;
    }

    public GameState getGameState() {
        return gameState;
    }

    public void setGameState(GameState state) {
        this.gameState = state;
    }

    public Set<UUID> getActivePlayers() {
        return activePlayers;
    }

    public Set<UUID> getFrozenPlayers() {
        return frozenPlayers;
    }

    public Set<UUID> getQuitters() {
        return quitters;
    }

    public Map<UUID, UUID> getLastDamager() {
        return lastDamager;
    }

    public Map<UUID, PlayerStats> getPlayerStats() {
        return playerStats;
    }

    public int getTimer() {
        return timer; }

    public void setTimer(int timer) {
        this.timer = timer; }

    public int getPowerupCooldown() {
        return powerupCooldown; }

    public void setPowerupCooldown(int cooldown) {
        this.powerupCooldown = cooldown; }

    public int getEventCooldown() {
        return eventCooldown; }

    public void setEventCooldown(int cooldown) {
        this.eventCooldown = cooldown; }

    public World getGameWorld() {
        return gameWorld;
    }

    public ChaosScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }

    public GameScoreboard getGameScoreboard() {
        return gameScoreboard;
    }

    public IdleScoreboard getIdleScoreboard() {
        return idleScoreboard;
    }

    @Override
    public void onDisable() {
        getLogger().info("Disabling Chaos Pillars...");

        if (scoreboardManager != null) {
            scoreboardManager.resetScoreboard();
        }

        if (statsManager != null && playerStats != null) {
            try {
                statsManager.saveStats();
                getLogger().info("Player stats saved successfully.");
            } catch (Exception e) {
                getLogger().severe("Failed to save player stats: " + e.getMessage());
                e.printStackTrace();
            }
        }

        getLogger().info("Chaos Pillars disabled successfully.");
    }
}
