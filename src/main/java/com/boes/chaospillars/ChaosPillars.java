package com.boes.chaospillars;

import org.bukkit.*;
import org.bukkit.block.Block;
import com.boes.chaospillars.enums.GameState;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.boss.BossBar;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class ChaosPillars extends JavaPlugin implements Listener {
    private int eventCooldown;
    public Material currentRoundFloorMaterial;
    private int powerupCooldown = 30;
    private int maxTimer = 600;
    private int timer = 600;
    public Map<UUID, PlayerStats> playerStats = new HashMap<>();
    private final Map<UUID, UUID> lastDamager = new HashMap<>();
    private final Set<UUID> activePlayers = new HashSet<>();
    private BukkitRunnable gameTask;
    private BukkitRunnable itemTask;
    public BukkitRunnable countdownTask;
    private Scoreboard scoreboard;
    private Objective objective;
    private World gameWorld;
    private BossBar bossBar;
    private final Set<UUID> frozenPlayers = new HashSet<>();
    private StatsManager statsManager;
    private List<Material> floorBlockTypes = new ArrayList<>();
    private List<Material> pillarBlockTypes = new ArrayList<>();
    private int itemGiveIntervalTicks = 60;
    private boolean lavaStarted = false;

    public Set<UUID> getActivePlayers() {
        return activePlayers;
    }

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
        reloadGameConfig();
        startScoreboard();

        statsManager = new StatsManager(this);
        playerStats = statsManager.loadStats();

        getLogger().info("Chaos Pillars enabled.");
        gameWorld = Bukkit.getWorld("world");

        if (gameWorld == null) {
            getLogger().severe("Could not find world 'world'. Plugin disabled.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        Objects.requireNonNull(getCommand("chaos")).setExecutor(new ChaosCommand(this));
        Objects.requireNonNull(getCommand("chaos")).setTabCompleter(new ChaosCommand(this));

        clearArea();
        gamerule();
        Bukkit.getScheduler().runTaskLater(this, this::bedrockPlatform, 40L);

        gameWorld.getWorldBorder().setCenter(0.5, 0.5);
        gameWorld.getWorldBorder().setSize(37);
        gameWorld.getWorldBorder().setWarningDistance(0);
        gameWorld.getWorldBorder().setWarningTime(5);


        setGameState(GameState.IDLE);
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        stopGameTasks();
        resetScoreboard();
        statsManager.saveStats(playerStats);
    }

    private void gamerule() {
        gameWorld.setDifficulty(Difficulty.NORMAL);
        gameWorld.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        gameWorld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        gameWorld.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        gameWorld.setGameRule(GameRule.SHOW_DEATH_MESSAGES, true);
        gameWorld.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
    }

    public void reloadGameConfig() {
        String worldName = getConfig().getString("game.world-name", "world");
        gameWorld = Bukkit.getWorld(worldName);

        maxTimer = getConfig().getInt("game.timer-seconds", 600);
        timer = maxTimer;

        powerupCooldown = getConfig().getInt("game.powerup-cooldown-seconds", 30);

        itemGiveIntervalTicks = getConfig().getInt("game.item-give-interval-seconds", 4) * 20;

        List<String> floorStrings = getConfig().getStringList("floor-block-types");
        List<Material> parsedFloorMaterials = floorStrings.stream()
                .map(name -> {
                    try {
                        return Material.valueOf(name.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        getLogger().warning("Invalid floor block type in config: " + name);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (!parsedFloorMaterials.isEmpty()) {
            floorBlockTypes = parsedFloorMaterials;
        } else {
            getLogger().warning("No valid floor block types in config. Using default.");
            floorBlockTypes = Arrays.asList(
                    Material.END_STONE,
                    Material.SLIME_BLOCK,
                    Material.AIR,
                    Material.HONEY_BLOCK,
                    Material.GRASS_BLOCK,
                    Material.OAK_LOG,
                    Material.CHERRY_LEAVES,
                    Material.NETHERRACK,
                    Material.BEDROCK,
                    Material.DIAMOND_BLOCK,
                    Material.COBWEB
            );
        }


        List<String> materialNames = getConfig().getStringList("game.pillar-block-types");
        List<Material> validMaterials = new ArrayList<>();

        for (String name : materialNames) {
            try {
                Material mat = Material.valueOf(name.toUpperCase());
                validMaterials.add(mat);
            } catch (IllegalArgumentException e) {
                getLogger().warning("Invalid pillar block type in config: " + name);
            }
        }

        if (!validMaterials.isEmpty()) {
            pillarBlockTypes = validMaterials;
        } else {
            getLogger().warning("No valid pillar block types in config. Using default.");
            pillarBlockTypes = Arrays.asList(
                    Material.BEDROCK, Material.SAND, Material.NETHERRACK, Material.END_STONE
            );
        }
        getLogger().info("Loaded floor blocks: " + floorBlockTypes);
        getLogger().info("Loaded pillar blocks: " + pillarBlockTypes);
    }


    private void stopGameTasks() {
        if (gameTask != null) gameTask.cancel();
        if (itemTask != null) itemTask.cancel();
        if (countdownTask != null) countdownTask.cancel();
        gameTask = null;
        itemTask = null;
        countdownTask = null;
    }

    private void killAllMobs() {
        if (gameWorld == null) return;
        for (Entity e : gameWorld.getEntities()) {
            if (e instanceof Player) continue;
            e.remove();
        }
    }

    private void startScoreboard() {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        scoreboard = manager.getNewScoreboard();

        Objective existing = scoreboard.getObjective("chaos");
        if (existing != null) {
            existing.unregister();
        }

        objective = scoreboard.registerNewObjective("chaos", "dummy", ChatColor.GOLD + "Chaos Pillars");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setScoreboard(scoreboard);
        }

        if (gameState == GameState.RUNNING) {
            updateGameScoreboard();
        } else {
            for (Player player : Bukkit.getOnlinePlayers()) {
                updateIdleScoreboard(player);
            }
        }
    }

    private void updateIdleScoreboard(Player player) {
        Scoreboard idleBoard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective idleObjective = idleBoard.registerNewObjective("idle", "dummy", ChatColor.GOLD + "Your Stats");
        idleObjective.setDisplaySlot(DisplaySlot.SIDEBAR);

        PlayerStats stats = playerStats.getOrDefault(player.getUniqueId(), new PlayerStats());

        int score = 10;

        idleObjective.getScore("§6Chaos Pillars").setScore(score--);
        idleObjective.getScore("§7─────────────── ").setScore(score--);
        idleObjective.getScore("§ePlayer: §f" + player.getName()).setScore(score--);
        idleObjective.getScore("§7Kills: §f" + stats.getKills()).setScore(score--);
        idleObjective.getScore("§7Deaths: §f" + stats.getDeaths()).setScore(score--);
        idleObjective.getScore("§7Wins: §f" + stats.getWins()).setScore(score--);
        idleObjective.getScore("§7Games Played: §f" + stats.getGamesPlayed()).setScore(score--);
        idleObjective.getScore("§7Win Streak: §f" + stats.getWinStreak()).setScore(score--);
        idleObjective.getScore("§7Loss Streak: §f" + stats.getLossStreak()).setScore(score--);
        idleObjective.getScore("§7───────────────").setScore(score--);

        player.setScoreboard(idleBoard);
    }


    private void updateGameScoreboard() {
        if (objective == null) return;

        int score = 6;
        for (String entry : scoreboard.getEntries()) {
            scoreboard.resetScores(entry);
        }

        objective.getScore("§6Chaos Pillars").setScore(score--);
        objective.getScore("§7───────────────").setScore(score--);
        objective.getScore("§cTime Left: §f" + timer + "s").setScore(score--);
        objective.getScore("§aPlayers Alive: §f" + activePlayers.size()).setScore(score--);
        objective.getScore("§bPowerup In: §f" + powerupCooldown + "s").setScore(score--);
        objective.getScore("§dEvent In: §f" + eventCooldown + "s").setScore(score--);


        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setScoreboard(scoreboard);
        }
    }


    private void resetScoreboard() {
        if (scoreboard != null) {
            Objective existing = scoreboard.getObjective("chaos");
            if (existing != null) {
                existing.unregister();
            }

            for (Player player : Bukkit.getOnlinePlayers()) {
                player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
            }
        }

        scoreboard = null;
        objective = null;
    }
    private void startTimer() {
        timer = getConfig().getInt("game.timer-seconds", 600);
        powerupCooldown = getConfig().getInt("game.powerup-cooldown-seconds", 30);
        eventCooldown = getConfig().getInt("game.event-cooldown-seconds", 120);


        gameTask = new BukkitRunnable() {
            @Override
            public void run() {
                timer--;
                powerupCooldown--;
                eventCooldown--;

                updateGameScoreboard();

                if (bossBar != null) {
                    bossBar.setProgress(Math.max(0, timer / (double) maxTimer));
                }


                if (powerupCooldown <= 0) {
                    Randompostiveeffect();
                    powerupCooldown = getConfig().getInt("game.powerup-cooldown-seconds", 30);
                }


                if (eventCooldown <= 0) {

                    ChaosEventManager eventManager = new ChaosEventManager(ChaosPillars.this);
                    eventManager.triggerRandomEvent();

                    eventCooldown = getConfig().getInt("game.event-cooldown-seconds", 120);

                }

                if (timer <= 120 && !lavaStarted) {
                    lavaStarted = true;
                    Bukkit.broadcastMessage(ChatColor.GOLD + "Lava starting to rise!");
                    startLavaRise();
                }


                if (timer <= 0) {
                    Bukkit.broadcastMessage(ChatColor.RED + "Time is up! Game over.");
                    endGame();
                }
            }
        };

        gameTask.runTaskTimer(this, 20L, 20L);
    }



    public void endGame() {
        stopGameTasks();
        gameWorld.setTime(1000);
        gameWorld.setStorm(false);
        lavaStarted = false;
        Location spawn = gameWorld.getSpawnLocation();
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.teleport(spawn);
            player.setGameMode(GameMode.SPECTATOR);
        }
        clearArea();
        activePlayers.clear();
        Bukkit.getScheduler().runTaskLater(this, this::bedrockPlatform, 60L);
        killAllMobs();
        Bukkit.getScheduler().runTaskLater(this, () -> setGameState(GameState.IDLE), 80L);

        if (bossBar != null) {
            bossBar.removeAll();
            bossBar = null;
        }
        resetScoreboard();
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateIdleScoreboard(player);
        }


    }

    public void startGame() {
        activePlayers.clear();
        killAllMobs();
        clearArea();
        Bukkit.getScheduler().runTaskLater(this, this::randomFloor, 40L);


        if (bossBar != null) {
            bossBar.removeAll();
            bossBar = null;
        }

        Random random = new Random();
        Material currentRoundPillarMaterial = pillarBlockTypes.get(random.nextInt(pillarBlockTypes.size()));
        Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "This round's pillars are made of " + ChatColor.YELLOW + currentRoundPillarMaterial.name().toLowerCase().replace('_', ' ') + "!");


        int radius = 12;
        int height = 50;
        int baseY = -63;

        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        Collections.shuffle(players);

        for (Player p : players) {
            p.getInventory().clear();
        }

        int pillarCount = 10;
        Bukkit.getScheduler().runTaskLater(this, () -> {
                    List<Location> basePillarLocations = generatePillars(currentRoundPillarMaterial, radius, height, baseY, pillarCount);


                    for (int i = 0; i < players.size() && i < basePillarLocations.size(); i++) {
                        Location baseLoc = basePillarLocations.get(i);
                        Player player = players.get(i);
                        Location tp = baseLoc.clone().add(0.5, height, 0.5);
                        player.teleport(tp);
                        player.setGameMode(GameMode.SURVIVAL);
                        player.setHealth(20);
                        player.setFoodLevel(20);
                        player.clearActivePotionEffects();
                        activePlayers.add(player.getUniqueId());
                    }
                    frozenPlayers.addAll(activePlayers);
        }, 40L);

        gameWorld.getWorldBorder().setSize(37);

        bossBar = Bukkit.createBossBar(ChatColor.GOLD + "Time Remaining", BarColor.YELLOW, BarStyle.SEGMENTED_10);
        for (UUID uuid : activePlayers) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                bossBar.addPlayer(player);
            }
        }
        bossBar.setProgress(1.0);
        bossBar.setVisible(true);



        setGameState(GameState.COUNTDOWN);

        new BukkitRunnable() {
            int countdown = 10;

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
                    setGameState(GameState.RUNNING);
                    resetScoreboard();
                    startScoreboard();
                    startTimer();
                    for (UUID uuid : activePlayers) {
                        PlayerStats stats = playerStats.computeIfAbsent(uuid, k -> new PlayerStats());
                        stats.addGamePlayed();
                    }

                    itemTask = new BukkitRunnable() {
                        final Random rand = new Random();
                        @Override
                        public void run() {
                            for (UUID uuid : activePlayers) {
                                Player player = Bukkit.getPlayer(uuid);
                                if (player == null) continue;
                                Material[] materials = Material.values();
                                Material mat = materials[rand.nextInt(materials.length)];
                                try {
                                    player.getInventory().addItem(new ItemStack(mat));
                                } catch (Exception ignored) {}
                            }
                        }
                    };
                    itemTask.runTaskTimer(ChaosPillars.this, itemGiveIntervalTicks, itemGiveIntervalTicks);
                    cancel();
                    return;
                }

                for (UUID uuid : activePlayers) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null) {
                        player.sendTitle(ChatColor.YELLOW + "Starting in", ChatColor.RED.toString() + countdown, 0, 20, 0);
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
                    }
                }

                countdown--;
            }
        }.runTaskTimer(this, 0L, 20L);
    }

    public void Randompostiveeffect() {
        List<PotionEffectType> effects = List.of(
                PotionEffectType.SPEED,
                PotionEffectType.HASTE,
                PotionEffectType.STRENGTH,
                PotionEffectType.REGENERATION,
                PotionEffectType.RESISTANCE,
                PotionEffectType.FIRE_RESISTANCE,
                PotionEffectType.HEALTH_BOOST,
                PotionEffectType.ABSORPTION,
                PotionEffectType.JUMP_BOOST,
                PotionEffectType.NIGHT_VISION,
                PotionEffectType.SATURATION,
                PotionEffectType.CONDUIT_POWER,
                PotionEffectType.INVISIBILITY,
                PotionEffectType.LUCK,
                PotionEffectType.WATER_BREATHING,
                PotionEffectType.SLOW_FALLING

        );

        Random random = new Random();

        for (UUID uuid : activePlayers) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline() && !player.isDead()) {
                PotionEffectType chosen = effects.get(random.nextInt(effects.size()));
                player.addPotionEffect(new PotionEffect(
                        chosen, 20 * powerupCooldown, 1, false, true
                ));
                player.sendMessage(ChatColor.AQUA + "✨ You received " + chosen.getName() + "!");
            }
        }

    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.SPAWNER_EGG) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player dead = event.getEntity();
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

        if (killerId != null && !killerId.equals(deadId)) {
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
            endGame();
        } else if (activePlayers.isEmpty()) {
            Bukkit.broadcastMessage(ChatColor.GRAY + "Nobody won the Chaos Pillars game.");
            endGame();
        }
    }


    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player damaged && event.getDamager() instanceof Player damager) {
            lastDamager.put(damaged.getUniqueId(), damager.getUniqueId());
        }
    }


    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        player.setGameMode(GameMode.SPECTATOR);

        player.sendMessage(ChatColor.LIGHT_PURPLE + "Do /chas start to start a game!");
        if (scoreboard != null && objective != null) {
            player.setScoreboard(scoreboard);
        }

        if (bossBar != null) {
            bossBar.addPlayer(player);
        }
        if (getGameState() == GameState.RUNNING) {

            player.setScoreboard(scoreboard);
        } else {
            updateIdleScoreboard(player);
        }
    }
    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        activePlayers.remove(uuid);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!activePlayers.contains(uuid)) return;

        activePlayers.remove(uuid);

        if (activePlayers.size() == 1) {
            UUID winnerId = activePlayers.iterator().next();
            Player winner = Bukkit.getPlayer(winnerId);
            if (winner != null) {
                Bukkit.broadcastMessage(ChatColor.GOLD + winner.getName() + " has won the Chaos Pillars game!");
            }
            endGame();
        } else if (activePlayers.isEmpty()) {
            Bukkit.broadcastMessage(ChatColor.GRAY + "Nobody won the Chaos Pillars game.");
            endGame();
        }
    }

    private void clearArea() {
        if (gameWorld == null) return;

        WorldBorder border = gameWorld.getWorldBorder();
        Location center = border.getCenter();
        int radius = ((int) border.getSize() / 2) + 2;

        int minX = (int) center.getBlockX() - radius - 1;
        int maxX = (int) center.getBlockX() + radius + 1;
        int minZ = (int) center.getBlockZ() - radius - 1;
        int maxZ = (int) center.getBlockZ() + radius + 1;


        int maxY = gameWorld.getMaxHeight();
        int minY = gameWorld.getMinHeight();

        final int[] x = {minX};
        final int[] y = {maxY};
        final int[] z = {minZ};

        final int batchSize = 500;

        new BukkitRunnable() {
            @Override
            public void run() {
                int cleared = 0;

                while (cleared < batchSize) {
                    Block block = gameWorld.getBlockAt(x[0], y[0], z[0]);
                    if (block.getType() != Material.AIR) {
                        block.setType(Material.AIR, false);
                        cleared++;
                    }

                    z[0]++;
                    if (z[0] > maxZ) {
                        z[0] = minZ;
                        x[0]++;
                        if (x[0] > maxX) {
                            x[0] = minX;
                            y[0]--;
                            if (y[0] < minY) {
                                cancel();
                                getLogger().info("✔ Entire area cleared.");
                                return;
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(this, 0L, 1L);
    }


    private List<Location> generatePillars(Material material, int radius, int height, int baseY, int count) {
        List<Location> basePillarLocations = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            double angle = 2 * Math.PI * i / count;
            int x = (int) (radius * Math.cos(angle));
            int z = (int) (radius * Math.sin(angle));

            for (int y = 0; y < height; y++) {
                Location loc = new Location(gameWorld, x, baseY + y, z);
                loc.getBlock().setType(material, false);
            }

            basePillarLocations.add(new Location(gameWorld, x, baseY, z));
        }

        return basePillarLocations;
    }


    public void randomFloor() {
        if (floorBlockTypes.isEmpty()) {
            getLogger().warning("No valid floor block types available. Skipping floor generation.");
            return;
        }

        int radius = 18;
        int y = -64;


        Material selectedMaterial = floorBlockTypes.get(new Random().nextInt(floorBlockTypes.size()));
        currentRoundFloorMaterial = selectedMaterial;

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x * x + z * z <= radius * radius) {
                    Location loc = new Location(gameWorld, x, y, z);
                    loc.getBlock().setType(selectedMaterial, false);
                }
            }
        }

        getLogger().info("Generated circular floor with " + selectedMaterial + " at Y = " + y);


        Bukkit.broadcastMessage(ChatColor.AQUA + "This round's floor is made of " + ChatColor.YELLOW +
                selectedMaterial.name().toLowerCase().replace('_', ' ') + ChatColor.AQUA + "!");
    }



    private void bedrockPlatform() {
        if (gameWorld == null) return;

        int size = 37;
        int halfSize = size / 2;
        int floorY = -64;
        int centerX = 0;
        int centerZ = 0;

        int minX = centerX - halfSize;
        int maxX = centerX + halfSize;
        int minZ = centerZ - halfSize;
        int maxZ = centerZ + halfSize;

        new BukkitRunnable() {
            int x = minX;
            int z = minZ;

            @Override
            public void run() {
                int placed = 0;
                int blocksPerTick = 100;

                while (x <= maxX && placed < blocksPerTick) {
                    while (z <= maxZ && placed < blocksPerTick) {
                        Location loc = new Location(gameWorld, x, floorY, z);
                        if (loc.getBlock().getType() != Material.BEDROCK) {
                            loc.getBlock().setType(Material.BEDROCK);
                        }
                        placed++;
                        z++;
                    }
                    if (z > maxZ) {
                        z = minZ;
                        x++;
                    }
                }

                if (x > maxX) {
                    cancel();
                    getLogger().info("Finished generating square bedrock floor (" + size + "x" + size + ") at Y = -64.");
                }
            }
        }.runTaskTimer(this, 0L, 1L);
    }

    public void startLavaRise() {
        int lavaStartY = -64;
        int lavaEndY = -12;
        int lavaDurationSeconds = 80;
        long totalTicks = lavaDurationSeconds * 20L;

        World world = gameWorld;
        WorldBorder border = world.getWorldBorder();

        Location center = border.getCenter();
        double size = border.getSize();

        int minX = (int) Math.floor(center.getX() - size / 2);
        int maxX = (int) Math.ceil(center.getX() + size / 2);
        int minZ = (int) Math.floor(center.getZ() - size / 2);
        int maxZ = (int) Math.ceil(center.getZ() + size / 2);

        new BukkitRunnable() {
            int currentY = lavaStartY;
            long ticksPassed = 0;

            @Override
            public void run() {
                if (currentY >= lavaEndY || ticksPassed >= totalTicks) {
                    cancel();
                    return;
                }

                double progress = (double) ticksPassed / totalTicks;
                int newLavaLevel = lavaStartY + (int) ((lavaEndY - lavaStartY) * progress);

                if (newLavaLevel > currentY) {
                    currentY = newLavaLevel;

                    for (int x = minX; x <= maxX; x++) {
                        for (int z = minZ; z <= maxZ; z++) {
                            Block block = world.getBlockAt(x, currentY, z);
                            if (block.getType() != Material.LAVA) {
                                block.setType(Material.LAVA);
                            }
                        }
                    }
                }

                ticksPassed++;
            }
        }.runTaskTimer(this, 0L, 1L);
    }


    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (frozenPlayers.contains(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Wait for the countdown to end!");
        }
    }

    @EventHandler
    public void onProjectileHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;

        Entity damager = event.getDamager();

        if (damager instanceof Snowball || damager instanceof Egg) {
            Projectile projectile = (Projectile) damager;
            if (!(projectile.getShooter() instanceof Player shooter)) return;

            org.bukkit.util.@NotNull Vector direction = victim.getLocation().toVector().subtract(shooter.getLocation().toVector()).normalize();
            @NotNull Vector knockback = direction.multiply(0.8).setY(0.5);

            victim.setVelocity(knockback);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (frozenPlayers.contains(player.getUniqueId())) {
            if (!event.getFrom().toVector().equals(event.getTo().toVector())) {
                event.setTo(event.getFrom());
            }
        }
    }
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        if (block.getY() >=2 ) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "HEIGHT LIMIT!");
        }
    }
}