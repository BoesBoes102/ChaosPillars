package com.boes.chaospillars;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.boss.BossBar;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.*;

public class ChaosPillars extends JavaPlugin implements Listener {

    private final Set<Location> placedBlocks = new HashSet<>();
    private final Set<UUID> activePlayers = new HashSet<>();
    private final List<Location> pillarLocations = new ArrayList<>();
    private final Map<UUID, Long> startCooldown = new HashMap<>();
    private BukkitRunnable gameTask;
    private BukkitRunnable itemTask;
    private BukkitRunnable countdownTask;
    private int timer = 600;
    private Scoreboard scoreboard;
    private Objective objective;
    private World gameWorld;
    private BossBar bossBar;

    @Override
    public void onEnable() {
        getLogger().info("Chaos Pillars enabled.");
        Bukkit.getPluginManager().registerEvents(this, this);

        // Your existing onEnable setup here...
        Bukkit.getScheduler().runTask(this, () -> {
            // No more voidworld creation or loading here - you said to run in main world
            gameWorld = Bukkit.getWorlds().get(0); // Get main world

            if (gameWorld == null) {
                getLogger().severe("Main world could not be loaded.");
                return;
            }

            Bukkit.getScheduler().runTaskLater(this, () -> {
                clearArea();
                gamerule();

            }, 80L);
        });
    }
    private void clearOutsideBorder(int extraRadius) {
        if (gameWorld == null) return;

        Location center = new Location(gameWorld,
                gameWorld.getWorldBorder().getCenter().getX(),
                0,
                gameWorld.getWorldBorder().getCenter().getZ()
        );

        double borderRadius = gameWorld.getWorldBorder().getSize() / 2.0;
        int minY = gameWorld.getMinHeight();
        int maxY = gameWorld.getMaxHeight();

        int startRadius = (int) Math.ceil(borderRadius);
        int endRadius = startRadius + extraRadius;

        int floorY = -64;  // your fixed floor level

        for (int x = -endRadius; x <= endRadius; x++) {
            for (int z = -endRadius; z <= endRadius; z++) {
                double dist = Math.sqrt(x * x + z * z);

                if (dist > borderRadius && dist <= endRadius) {
                    for (int y = minY; y < maxY; y++) {
                        Location loc = new Location(gameWorld, center.getX() + x, y, center.getZ() + z);
                        Material type = loc.getBlock().getType();

                        if (y == floorY) {
                            // At floor level, place bedrock always (add floor)
                            loc.getBlock().setType(Material.BEDROCK, false);
                        } else {
                            // For other levels, clear blocks except bedrock floor
                            if (type == Material.BEDROCK) continue; // keep any other bedrock untouched

                            loc.getBlock().setType(Material.AIR, false);
                        }
                    }
                }
            }
        }
    }







    private void gamerule() {
        gameWorld.setDifficulty(Difficulty.NORMAL);
        gameWorld.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        gameWorld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        gameWorld.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        gameWorld.setGameRule(GameRule.SHOW_DEATH_MESSAGES, false);
        gameWorld.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
    }

    @Override
    public void onDisable() {
        stopGameTasks();
        resetScoreboard();
        resetWorldBorder();
    }



    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        // 🔒 Require OP for all commands
        if (!player.isOp()) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        switch (command.getName().toLowerCase()) {
            case "startchaos":
                if (countdownTask != null) {
                    player.sendMessage(ChatColor.RED + "Chaos game is already starting or running!");
                    return true;
                }
                startCountdown();
                return true;

            case "stopchaos":
                endGame();
                if (countdownTask != null) {
                    countdownTask.cancel();
                    countdownTask = null;
                }
                return true;

            // Add more commands here if needed

            default:
                return false;
        }
    }


    private void startCountdown() {
        countdownTask = new BukkitRunnable() {
            int count = 10;

            @Override
            public void run() {
                if (count <= 0) {
                    Bukkit.broadcastMessage(ChatColor.GREEN + "Chaos Pillars game started!");
                    startGame();
                    cancel();
                    countdownTask = null;
                    return;
                }

                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.sendTitle(ChatColor.GREEN + "" + count, ChatColor.YELLOW + "Starting in...", 0, 20, 10);
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 1f);
                }

                count--;
            }
        };
        countdownTask.runTaskTimer(this, 0L, 20L);
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

    private void resetWorldBorder() {
        if (gameWorld != null) {
            gameWorld.getWorldBorder().reset();
        }
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
        if (manager == null) return;

        scoreboard = manager.getNewScoreboard();

        Objective existing = scoreboard.getObjective("chaos");
        if (existing != null) {
            existing.unregister();
        }

        objective = scoreboard.registerNewObjective("chaos", "dummy", ChatColor.GOLD + "Chaos Timer");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setScoreboard(scoreboard);
        }
    }

    private void startTimer() {
        timer = 600; // 10 minutes in seconds

        gameTask = new BukkitRunnable() {
            @Override
            public void run() {
                timer--;

                // Update scoreboard time
                if (objective != null) {
                    objective.getScore(ChatColor.YELLOW + "Time Left").setScore(timer);
                    objective.getScore(ChatColor.RED + "Players Left").setScore(activePlayers.size());
                }

                // Update boss bar progress
                if (bossBar != null) {
                    bossBar.setProgress(Math.max(0, timer / 600.0));
                    bossBar.setTitle(ChatColor.GOLD + "Time Remaining: " + formatTime(timer));
                }

                // When timer runs out
                if (timer <= 0) {
                    Bukkit.broadcastMessage(ChatColor.RED + "Time is up! Game over.");
                    endGame();
                }
            }
        };

        // Run every second (20 ticks)
        gameTask.runTaskTimer(this, 20L, 20L);
    }


    private void endGame() {
        stopGameTasks();
        resetScoreboard();
        resetWorldBorder();

        Location spawn = gameWorld.getSpawnLocation();
        for (UUID uuid : activePlayers) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.teleport(spawn);
                player.setGameMode(GameMode.SPECTATOR);
            }
        }


        activePlayers.clear();
        clearArea();
        killAllMobs();
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar = null;
        }



        for (Player player : Bukkit.getOnlinePlayers()) {
            player.teleport(spawn);
            player.setGameMode(GameMode.SPECTATOR);
            player.getInventory().clear();
            player.setHealth(20);
            player.setFoodLevel(20);
        }
    }

    public void createFloorAsync(int floorY, int radius, Runnable callback) {
        new BukkitRunnable() {
            int x = -radius;
            int z = -radius;
            int blocksPerTick = 100;

            @Override
            public void run() {
                int blocksSet = 0;

                while (x <= radius) {
                    while (z <= radius) {
                        gameWorld.getBlockAt(x, floorY, z).setType(Material.BEDROCK, false);

                        blocksSet++;
                        z++;

                        if (blocksSet >= blocksPerTick) {
                            return; // pause this task, continue next tick
                        }
                    }
                    z = -radius;
                    x++;
                }
                // Finished
                cancel();
                if (callback != null) callback.run();
            }
        }.runTaskTimer(this, 0L, 1L);
    }

    public void createPillarsAsync(int totalPillars, int radius, int height, int floorY, Runnable callback) {
        new BukkitRunnable() {
            int pillarIndex = 0;
            int pillarHeight = 0;

            @Override
            public void run() {
                if (pillarIndex >= totalPillars) {
                    cancel();
                    if (callback != null) callback.run();
                    return;
                }

                double angle = 2 * Math.PI * pillarIndex / totalPillars;
                int x = (int) (radius * Math.cos(angle));
                int z = (int) (radius * Math.sin(angle));
                int baseY = floorY + 1;

                // Set one block of the current pillar per tick to avoid lag
                Location loc = new Location(gameWorld, x, baseY + pillarHeight, z);
                loc.getBlock().setType(Material.BEDROCK, false);
                pillarLocations.add(loc);

                pillarHeight++;

                if (pillarHeight >= height) {
                    pillarHeight = 0;
                    pillarIndex++;
                }
            }
        }.runTaskTimer(this, 0L, 1L);
    }

    private void startGame() {
        clearArea();

        if (gameWorld == null) {
            getLogger().warning("Game world is not loaded!");
            return;
        }

        activePlayers.clear();
        pillarLocations.clear();
        killAllMobs();
        startScoreboard();

        int totalPillars = 10;
        int radius = 12;
        int height = 50;

        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        Collections.shuffle(players);
        for (Player p : players) {
            p.getInventory().clear();
        }

        int floorY = -64;
        double borderSize = gameWorld.getWorldBorder().getSize();
        int floorRadius = (int) Math.ceil(borderSize / 2.0);

        // Step 1: Create floor asynchronously
        createFloorAsync(floorY, floorRadius, () -> {
            // Step 2: After floor done, create pillars asynchronously
            createPillarsAsync(totalPillars, radius, height, floorY, () -> {
                // Step 3: After pillars are done, teleport players and finish setup
                teleportPlayersToPillars(players, totalPillars, radius, height, floorY);

                gameWorld.getWorldBorder().setCenter(0, 0);
                gameWorld.getWorldBorder().setSize(32);

                bossBar = Bukkit.createBossBar(ChatColor.GOLD + "Time Remaining", BarColor.YELLOW, BarStyle.SEGMENTED_10);
                for (UUID uuid : activePlayers) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null) {
                        bossBar.addPlayer(player);
                    }
                }
                bossBar.setVisible(true);

                startTimer();

                startItemTask();
            });
        });
    }

    private void teleportPlayersToPillars(List<Player> players, int totalPillars, int radius, int height, int floorY) {
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            int pillarIndex = i * totalPillars / players.size();
            double angle = 2 * Math.PI * pillarIndex / totalPillars;
            int x = (int) (radius * Math.cos(angle));
            int z = (int) (radius * Math.sin(angle));
            int baseY = floorY + height;

            Location tp = new Location(gameWorld, x + 0.5, baseY, z + 0.5);
            player.teleport(tp);
            player.setGameMode(GameMode.SURVIVAL);
            player.setHealth(20);
            player.setFoodLevel(20);
            activePlayers.add(player.getUniqueId());
        }
    }

    private void startItemTask() {
        itemTask = new BukkitRunnable() {
            Random rand = new Random();

            @Override
            public void run() {
                for (UUID uuid : activePlayers) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player == null) continue;
                    Material[] materials = Material.values();
                    Material mat = materials[rand.nextInt(materials.length)];
                    try {
                        player.getInventory().addItem(new ItemStack(mat));
                    } catch (Exception ignored) {
                    }
                }
            }
        };
        itemTask.runTaskTimer(this, 80L, 80L);
    }







    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.SPAWNER_EGG) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID uuid = player.getUniqueId();

        if (!activePlayers.contains(uuid)) return;

        activePlayers.remove(uuid);
        player.setGameMode(GameMode.SPECTATOR);
        Bukkit.broadcastMessage(ChatColor.RED + player.getName() + " has been eliminated!");

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

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        player.setGameMode(GameMode.SPECTATOR);

        // Add boss bar to new player if game running
        if (bossBar != null && activePlayers.contains(player.getUniqueId())) {
            bossBar.addPlayer(player);
        }
    }

    private void clearArea() {
        if (gameWorld == null) return;

        int radius = 40;  // fixed 40 block radius as requested
        int minY = gameWorld.getMinHeight();
        int maxY = gameWorld.getMaxHeight();

        int floorY = -64;  // floor level to keep intact

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = minY; y < maxY; y++) {
                    if (y == floorY) continue;  // skip the floor layer

                    gameWorld.getBlockAt(x, y, z).setType(Material.AIR, false);
                }
            }
        }
    }



    private String formatTime(int seconds) {
        int minutes = seconds / 60;
        int secs = seconds % 60;
        return String.format("%02d:%02d", minutes, secs);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!activePlayers.contains(uuid)) return;

        activePlayers.remove(uuid);

        // Check how many players remain after one leaves
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

}