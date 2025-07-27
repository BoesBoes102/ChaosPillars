package com.boes.chaospillars;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
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

    private int powerupCooldown = 30;
    private int maxTimer = 600;
    private int timer = 600;
    private Map<UUID, PlayerStats> playerStats = new HashMap<>();
    private final Map<UUID, UUID> lastDamager = new HashMap<>();
    private final Set<UUID> activePlayers = new HashSet<>();
    private BukkitRunnable gameTask;
    private BukkitRunnable itemTask;
    private BukkitRunnable countdownTask;
    private Scoreboard scoreboard;
    private Objective objective;
    private World gameWorld;
    private BossBar bossBar;
    private final String timeLeftKey = ChatColor.YELLOW + "Time Left:";
    private final String playersLeftKey = ChatColor.YELLOW + "Players Left:";
    private final String powerupCooldownKey = ChatColor.YELLOW + "Powerup:";
    private final Set<UUID> frozenPlayers = new HashSet<>();
    private StatsManager statsManager;
    private List<Material> floorBlockTypes = new ArrayList<>();
    private List<Material> pillarBlockTypes = new ArrayList<>();
    private final int itemGiveIntervalTicks = 60;

    public enum GameState {
        IDLE,
        COUNTDOWN,
        RUNNING
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
        saveDefaultConfig(); // Loads config.yml if not already created
        reloadGameConfig();

        // Setup stats manager
        statsManager = new StatsManager(this); // create this field if you haven't already
        playerStats = statsManager.loadStats();

        getLogger().info("Chaos Pillars enabled.");
        gameWorld = Bukkit.getWorld("world");

        if (gameWorld == null) {
            getLogger().severe("Could not find world 'world'. Plugin disabled.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        clearArea();
        gamerule();
        bedrockPlatform();

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

    private void reloadGameConfig() {
        String worldName = getConfig().getString("game.world-name", "world");
        gameWorld = Bukkit.getWorld(worldName);

        maxTimer = getConfig().getInt("game.timer-seconds", 600);
        timer = maxTimer;

        powerupCooldown = getConfig().getInt("game.powerup-cooldown-seconds", 30);

        int itemGiveIntervalTicks = getConfig().getInt("game.item-give-interval-seconds", 4) * 20;

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
    @Override
    public boolean onCommand(@NotNull CommandSender sender,@NotNull Command command,@NotNull String label,@NotNull String @NotNull[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        switch (command.getName().toLowerCase()) {
            case "startchaos":
                if (!player.hasPermission("Chaos.start")) {
                    player.sendMessage(ChatColor.RED + "You do not have permission to start the Chaos Pillars game.");
                    return true;
                }
                if (getGameState() != GameState.IDLE) {
                    player.sendMessage(ChatColor.RED + "Chaos game is already running or counting down!");
                    return true;
                }
                int playerCount = Bukkit.getOnlinePlayers().size();

                if (playerCount < 2) {
                    player.sendMessage(ChatColor.RED + "Not enough players to start Chaos Pillars! Need at least 2.");
                    return true;
                }

                if (playerCount > 10) {
                    player.sendMessage(ChatColor.RED + "Chaos Pillars supports a maximum of 10 players!");
                    return true;
                }
                Bukkit.broadcastMessage(ChatColor.GREEN + "Chaos Pillars game starting!");
                startGame();
                return true;


            case "stopchaos":
                if (!player.hasPermission("Chaos.stop")) {
                    player.sendMessage(ChatColor.RED + "You do not have permission to stop the Chaos Pillars game.");
                    return true;
                }
                endGame();
                if (countdownTask != null) {
                    countdownTask.cancel();
                    countdownTask = null;
                }
                return true;
            case "chaosreload":
                if (!player.hasPermission("Chaos.admin")) return true;
                reloadConfig();
                reloadGameConfig();
                player.sendMessage(ChatColor.GREEN + "Chaos Pillars config reloaded!");
                return true;
            case "stats":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("This command can only be used by players.");
                    return true;
                }
                PlayerStats stats = playerStats.getOrDefault(player.getUniqueId(), new PlayerStats());

                player.sendMessage(ChatColor.GOLD + "=== Your Chaos Pillars Stats ===");
                player.sendMessage(ChatColor.YELLOW + "Wins: " + ChatColor.WHITE + stats.getWins());
                player.sendMessage(ChatColor.YELLOW + "Games Played: " + ChatColor.WHITE + stats.getGamesPlayed());
                player.sendMessage(ChatColor.YELLOW + "Win Rate: " + ChatColor.WHITE + String.format("%.1f", stats.getWinRate()) + "%");
                player.sendMessage(ChatColor.YELLOW + "Kills: " + ChatColor.WHITE + stats.getKills());
                player.sendMessage(ChatColor.YELLOW + "Deaths: " + ChatColor.WHITE + stats.getDeaths());
                player.sendMessage(ChatColor.YELLOW + "KDR: " + ChatColor.WHITE + String.format("%.2f", stats.getKDR()));
                return true;
            default:
                return false;
        }
    }

    private void stopGameTasks() {
        if (gameTask != null) gameTask.cancel();
        if (itemTask != null) itemTask.cancel();
        if (countdownTask != null) countdownTask.cancel();
        gameTask = null;
        itemTask = null;
        countdownTask = null;
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
    }

    private void startTimer() {
        timer = getConfig().getInt("game.timer-seconds", 600);
        powerupCooldown = getConfig().getInt("game.powerup-cooldown-seconds", 30);


        gameTask = new BukkitRunnable() {
            @Override
            public void run() {
                timer--;
                powerupCooldown--;

                if (objective != null) {
                    objective.getScore(timeLeftKey).setScore(timer);
                    objective.getScore(playersLeftKey).setScore(activePlayers.size());
                    objective.getScore(powerupCooldownKey).setScore(powerupCooldown);
                }

                if (bossBar != null) {
                    bossBar.setProgress(Math.max(0, timer / (double) maxTimer));
                }


                if (powerupCooldown <= 0) {
                    Randompostiveeffect();
                    powerupCooldown = getConfig().getInt("game.powerup-cooldown-seconds", 30);

                }

                if (timer <= 0) {
                    Bukkit.broadcastMessage(ChatColor.RED + "Time is up! Game over.");
                    endGame();
                }
            }
        };

        gameTask.runTaskTimer(this, 20L, 20L); // 1-second intervals
    }


    private void endGame() {
        stopGameTasks();
        resetScoreboard();

        Location spawn = gameWorld.getSpawnLocation();
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.teleport(spawn);
            player.setGameMode(GameMode.SPECTATOR);
        }
        bedrockPlatform();
        activePlayers.clear();
        clearArea();
        killAllMobs();
        setGameState(GameState.IDLE);
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar = null;
        }
    }

    private void startGame() {
        activePlayers.clear();
        killAllMobs();
        startScoreboard();
        randomFloor();

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

        List<Location> basePillarLocations = new ArrayList<>();
        int pillarCount = 10;

        for (int i = 0; i < pillarCount; i++) {
            double angle = 2 * Math.PI * i / pillarCount;
            int x = (int) (radius * Math.cos(angle));
            int z = (int) (radius * Math.sin(angle));

            for (int y = 0; y < height; y++) {
                Location loc = new Location(gameWorld, x, baseY + y, z);
                loc.getBlock().setType(currentRoundPillarMaterial);

            }

            basePillarLocations.add(new Location(gameWorld, x, baseY, z));
        }

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


        frozenPlayers.addAll(activePlayers);
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
                    startTimer();
                    setGameState(GameState.RUNNING);
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
                PotionEffectType chosen = effects.get(random.nextInt(effects.size())); // moved here
                player.addPotionEffect(new PotionEffect(
                        chosen, 20 * 60, 1, false, true
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

        // Update stats
        PlayerStats deadStats = playerStats.computeIfAbsent(deadId, k -> new PlayerStats());
        deadStats.addDeath();

        if (killerId != null && !killerId.equals(deadId)) {
            OfflinePlayer killer = Bukkit.getOfflinePlayer(killerId);
            PlayerStats killerStats = playerStats.computeIfAbsent(killerId, k -> new PlayerStats());
            killerStats.addKill();

            event.setDeathMessage(ChatColor.RED + dead.getName() + ChatColor.GRAY + " was killed by " + ChatColor.YELLOW + killer.getName());
        } else {
            event.setDeathMessage(ChatColor.RED + dead.getName() + ChatColor.GRAY + " died.");
        }

        lastDamager.remove(deadId); // Cleanup

        // Game logic
        activePlayers.remove(deadId);
        dead.setGameMode(GameMode.SPECTATOR);

        if (activePlayers.size() == 1) {
            UUID winnerId = activePlayers.iterator().next();
            Player winner = Bukkit.getPlayer(winnerId);
            if (winner != null) {
                Bukkit.broadcastMessage(ChatColor.GOLD + winner.getName() + " has won the Chaos Pillars game!");
                PlayerStats winnerStats = playerStats.computeIfAbsent(winnerId, k -> new PlayerStats());
                winnerStats.addWin();
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

        player.sendMessage(ChatColor.LIGHT_PURPLE + "Do /startchaos to start a game!");
        if (scoreboard != null && objective != null) {
            player.setScoreboard(scoreboard);
        }

        if (bossBar != null) {
            bossBar.addPlayer(player);
        }
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
        int borderRadius = (int) border.getSize() / 2;

        int minX = (int) center.getX() - borderRadius;
        int maxX = (int) center.getX() + borderRadius;
        int minZ = (int) center.getZ() - borderRadius;
        int maxZ = (int) center.getZ() + borderRadius;

        int minY = gameWorld.getMinHeight();
        int maxY = gameWorld.getMaxHeight();
        final int batchSize = 100;

        final int[] x = {minX};
        final int[] y = {minY};
        final int[] z = {minZ};

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
                        y[0]++;
                        if (y[0] > maxY) {
                            y[0] = minY;
                            x[0]++;
                            if (x[0] > maxX) {
                                cancel();
                                getLogger().info("✔ Area cleared .");
                                return;
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(this, 0L, 1L);
    }


    public void randomFloor() {
        if (floorBlockTypes.isEmpty()) {
            getLogger().warning("No valid floor block types available. Skipping floor generation.");
            return;
        }

        int radius = 18;
        int y = -64;

        // Pick one random material for the entire floor
        Material selectedMaterial = floorBlockTypes.get(new Random().nextInt(floorBlockTypes.size()));

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x * x + z * z <= radius * radius) {
                    Location loc = new Location(gameWorld, x, y, z);
                    loc.getBlock().setType(selectedMaterial, false);
                }
            }
        }

        getLogger().info("Generated circular floor with " + selectedMaterial + " at Y = -64.");
    }


    private void bedrockPlatform() {
        if (gameWorld == null) return;

        int size = 37; // World border size
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
            if (!(damager instanceof Projectile projectile)) return;
            if (!(projectile.getShooter() instanceof Player shooter)) return;

            // Knockback direction = from shooter to victim
            org.bukkit.util.@NotNull Vector direction = victim.getLocation().toVector().subtract(shooter.getLocation().toVector()).normalize();
            @NotNull Vector knockback = direction.multiply(0.8).setY(0.5); // Horizontal + small lift

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
}