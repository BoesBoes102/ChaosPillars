package com.boes.chaospillars;

import com.boes.chaospillars.listeners.WorldListener;
import com.boes.chaospillars.tasks.GameGenerateTask;
import com.boes.chaospillars.tasks.ClearAreaTask;
import com.boes.chaospillars.tasks.KillAllEntitiesTask;
import com.boes.chaospillars.tasks.RandomPositiveEffectTask;
import com.boes.chaospillars.tasks.SetupTask;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
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
    private int powerupCooldown = 30;
    private int timer = 600;
    public Map<UUID, PlayerStats> playerStats = new HashMap<>();
    private final Map<UUID, UUID> lastDamager = new HashMap<>();
    public final Set<UUID> activePlayers = new HashSet<>();
    private BukkitRunnable gameTask;
    private BukkitRunnable itemTask;
    public BukkitRunnable countdownTask;
    private Scoreboard scoreboard;
    private Objective objective;
    private World gameWorld;
    private final Set<UUID> frozenPlayers = new HashSet<>();
    private StatsManager statsManager;
    private List<Material> floorBlockTypes = new ArrayList<>();
    private List<Material> pillarBlockTypes = new ArrayList<>();
    private int itemGiveIntervalTicks = 60;
    private BukkitTask lavaTask;
    private BossBar lavaBossBar;


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
        resetScoreboard();
        startScoreboard();

        // Setup world
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


        Objects.requireNonNull(getCommand("chaos")).setExecutor(new ChaosCommand(this));
        Objects.requireNonNull(getCommand("chaos")).setTabCompleter(new ChaosCommand(this));



        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getPluginManager().registerEvents(new WorldListener(), this);

        new ClearAreaTask(gameWorld).runTaskTimer(this, 0L, 1L);

        setGameState(GameState.IDLE);

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.teleport(gameWorld.getSpawnLocation());
            player.setGameMode(GameMode.SPECTATOR);
            updateIdleScoreboard(player);
        }
    }


    @Override
    public void onDisable() {
        stopGameTasks();
        resetScoreboard();
        statsManager.saveStats(playerStats);
    }


    public void reloadGameConfig() {
        String worldName = getConfig().getString("game.world-name", "world");
        gameWorld = Bukkit.getWorld(worldName);

        timer = getConfig().getInt("game.timer-seconds", 600);

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
                    Material.STONE,
                    Material.SLIME_BLOCK,
                    Material.AIR,
                    Material.HONEY_BLOCK,
                    Material.WHITE_WOOL,
                    Material.GLASS,
                    Material.GLASS_PANE,
                    Material.OAK_LOG,
                    Material.CHERRY_LEAVES,
                    Material.NETHERRACK,
                    Material.BEDROCK,
                    Material.DIAMOND_BLOCK,
                    Material.COBWEB,
                    Material.CRAFTING_TABLE,
                    Material.SOUL_SAND,
                    Material.OAK_TRAPDOOR,
                    Material.MAGMA_BLOCK,
                    Material.ICE,
                    Material.TNT,
                    Material.BLACK_CARPET,
                    Material.BLUE_ICE,
                    Material.POINTED_DRIPSTONE,
                    Material.SCAFFOLDING
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

    private String translate(String message){
        return ChatColor.translateAlternateColorCodes('&', message);
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

        int score = 11;
        for (String entry : scoreboard.getEntries()) {
            scoreboard.resetScores(entry);
        }
        idleObjective.getScore(translate("&6Chaos Pillars")).setScore(score--);
        idleObjective.getScore(translate("§7─────────────── ")).setScore(score--);
        idleObjective.getScore(translate("§ePlayer: §f" + player.getName())).setScore(score--);
        idleObjective.getScore(translate("§7Kills: §f" + stats.getKills())).setScore(score--);
        idleObjective.getScore(translate("§7Deaths: §f" + stats.getDeaths())).setScore(score--);
        idleObjective.getScore(translate("§7Wins: §f" + stats.getWins())).setScore(score--);
        idleObjective.getScore(translate("§7Games Played: §f" + stats.getGamesPlayed())).setScore(score--);
        idleObjective.getScore(translate("§7Win Streak: §f" + stats.getWinStreak())).setScore(score--);
        idleObjective.getScore(translate("§7Loss Streak: §f" + stats.getLossStreak())).setScore(score--);
        idleObjective.getScore(translate("§7───────────────")).setScore(score--);

        player.setScoreboard(idleBoard);
    }


    private void updateGameScoreboard() {
        if (objective == null) return;

        int score = 6;
        for (String entry : scoreboard.getEntries()) {
            scoreboard.resetScores(entry);
        }

        objective.getScore(translate("§6Chaos Pillars")).setScore(score--);
        objective.getScore(translate("§7───────────────")).setScore(score--);
        objective.getScore(translate("§cTime Left: §f" + timer + "s")).setScore(score--);
        objective.getScore(translate("§aPlayers Alive: §f" + activePlayers.size())).setScore(score--);
        objective.getScore(translate("§bPowerup In: §f" + powerupCooldown + "s")).setScore(score--);
        objective.getScore(translate("§dEvent In: §f" + eventCooldown + "s")).setScore(score--);
        objective.getScore(translate("§7─────────────── ")).setScore(score--);

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


                if (powerupCooldown <= 0) {
                    new RandomPositiveEffectTask(ChaosPillars.this, activePlayers)
                            .runTask(ChaosPillars.this);

                    powerupCooldown = getConfig().getInt("game.powerup-cooldown-seconds", 30);
                }


                if (eventCooldown <= 0) {

                    ChaosEventManager eventManager = new ChaosEventManager(ChaosPillars.this);
                    eventManager.triggerRandomEvent();

                    eventCooldown = getConfig().getInt("game.event-cooldown-seconds", 120);

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
        stopLavaRise();
        stopLavaCountdown();
        stopGameTasks();
        gameWorld.setTime(1000);
        gameWorld.setStorm(false);
        Location spawn = gameWorld.getSpawnLocation();
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.teleport(spawn);
            player.setGameMode(GameMode.SPECTATOR);
            player.getInventory().clear();
        }
        new ClearAreaTask(gameWorld).runTaskTimer(this, 0L, 1L);
        activePlayers.clear();
        new KillAllEntitiesTask(gameWorld).run();
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateIdleScoreboard(player);
        }
        setGameState(GameState.IDLE);
    }

    public void startGame() {
        activePlayers.clear();
        new KillAllEntitiesTask(gameWorld).run();

        // Create a generator instance
        GameGenerateTask generator = new GameGenerateTask(gameWorld, floorBlockTypes, pillarBlockTypes);

        // Schedule floor generation after 40 ticks
        Bukkit.getScheduler().runTaskLater(this, generator::generateRandomFloor, 40L);

        gameWorld.setTime(1000);
        gameWorld.setStorm(false);

        Random random = new Random();
        Material currentRoundPillarMaterial = pillarBlockTypes.get(random.nextInt(pillarBlockTypes.size()));
        Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "This round's pillars are made of " +
                ChatColor.YELLOW + currentRoundPillarMaterial.name().toLowerCase().replace('_', ' ') + "!");

        int radius = 12;
        int height = 50;
        int baseY = -63;
        int pillarCount = 10;

        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        Collections.shuffle(players);

        // Schedule pillar generation + player teleport after 40 ticks
        Bukkit.getScheduler().runTaskLater(this, () -> {
            List<Location> basePillarLocations = generator.generatePillars(currentRoundPillarMaterial, radius, height, baseY, pillarCount);

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

        setGameState(GameState.COUNTDOWN);

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
                    setGameState(GameState.RUNNING);
                    resetScoreboard();
                    startScoreboard();
                    startTimer();
                    startItemTask();
                    startLavaCountdown(timer);
                    for (UUID uuid : activePlayers) {
                        PlayerStats stats = playerStats.computeIfAbsent(uuid, k -> new PlayerStats());
                        stats.addGamePlayed();
                    }
                    cancel();
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
        }.runTaskTimer(this, 0L, 20L);
    }
    private final List<Material> validItems = Arrays.stream(Material.values())
            .filter(mat -> mat.isItem()
                    && mat != Material.AIR
                    && mat != Material.CAVE_AIR
                    && mat != Material.VOID_AIR)
            .collect(Collectors.toList());


    private final Random random = new Random();

    public void startItemTask() {
        stopItemTask();

        itemTask = new BukkitRunnable() {
            int secondsLeft = Math.max(1, itemGiveIntervalTicks / 20);

            @Override
            public void run() {
                if (secondsLeft <= 0) {
                    for (UUID uuid : new HashSet<>(activePlayers)) {
                        Player player = Bukkit.getPlayer(uuid);
                        if (player == null || !player.isOnline() || player.isDead()) continue;

                        Material mat = validItems.get(random.nextInt(validItems.size()));
                        player.getInventory().addItem(new ItemStack(mat));

                        player.spigot().sendMessage(
                                ChatMessageType.ACTION_BAR,
                                new TextComponent(ChatColor.GREEN + "You received an item!")
                        );
                    }
                    secondsLeft = Math.max(1, itemGiveIntervalTicks / 20);
                } else {
                    for (UUID uuid : new HashSet<>(activePlayers)) {
                        Player player = Bukkit.getPlayer(uuid);
                        if (player == null || !player.isOnline()) continue;

                        player.spigot().sendMessage(
                                ChatMessageType.ACTION_BAR,
                                new TextComponent(ChatColor.YELLOW + "Next item in " + secondsLeft + "s")
                        );
                    }
                    secondsLeft--;
                }
            }
        };

        itemTask.runTaskTimer(this, 20L, 20L);
    }


    public void stopItemTask() {
        if (itemTask != null) {
            itemTask.cancel();
            itemTask = null;
        }
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.NATURAL) {
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

        Bukkit.getScheduler().runTask(this, () -> {
            player.setGameMode(GameMode.SPECTATOR);
            player.teleport(gameWorld.getSpawnLocation());
            player.sendMessage(ChatColor.LIGHT_PURPLE + "Do /chaos start to start a game!");

            if (getGameState() == GameState.RUNNING) {
                player.setScoreboard(scoreboard);
            } else {
                updateIdleScoreboard(player);
            }
        });
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

    public void stopLavaRise() {
        if (lavaTask != null && !lavaTask.isCancelled()) {
            lavaTask.cancel();
        }
    }

    private BukkitRunnable lavaCountdownTask;

    public void startLavaCountdown(int totalGameTime) {
        int lavaStartTime = 120;
        int lavaCountdown = totalGameTime - lavaStartTime;

        if (lavaBossBar == null) {
            lavaBossBar = Bukkit.createBossBar(
                    ChatColor.RED + "Lava in " + lavaCountdown + "s",
                    BarColor.RED,
                    BarStyle.SOLID
            );
            for (UUID uuid : activePlayers) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) lavaBossBar.addPlayer(player);
            }
            lavaBossBar.setVisible(true);
        }

        lavaCountdownTask = new BukkitRunnable() {
            int countdown = lavaCountdown;

            @Override
            public void run() {
                if (countdown <= 0) {
                    lavaBossBar.setTitle(ChatColor.RED + "Lava is rising!");
                    lavaBossBar.setProgress(1.0);
                    startLavaRise();
                    cancel();
                    return;
                }

                lavaBossBar.setTitle(ChatColor.RED + "Lava in " + countdown + "s");
                lavaBossBar.setProgress((double) countdown / lavaCountdown);

                countdown--;
            }
        };
        lavaCountdownTask.runTaskTimer(this, 0L, 20L);
    }

    public void stopLavaCountdown() {
        if (lavaCountdownTask != null && !lavaCountdownTask.isCancelled()) {
            lavaCountdownTask.cancel();
        }
        if (lavaBossBar != null) {
            lavaBossBar.removeAll();
            lavaBossBar = null;
        }
    }



    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (frozenPlayers.contains(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Wait for the countdown to end!");
        }
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

        lavaTask = new BukkitRunnable() {
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