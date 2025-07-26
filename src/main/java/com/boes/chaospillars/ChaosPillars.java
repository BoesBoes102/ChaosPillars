package com.boes.chaospillars;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.boss.BossBar;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.*;

public class ChaosPillars extends JavaPlugin implements Listener {

    private static final Material[] PILLAR_BLOCK_TYPES = {
            Material.BEDROCK,
            Material.SAND,
            Material.NETHERRACK,
            Material.END_STONE
    };


    private final Set<UUID> activePlayers = new HashSet<>();
    private final List<Location> pillarLocations = new ArrayList<>();
    private BukkitRunnable gameTask;
    private BukkitRunnable itemTask;
    private BukkitRunnable countdownTask;
    private int timer = 600;
    private Scoreboard scoreboard;
    private Objective objective;
    private World gameWorld;
    private BossBar bossBar;
    private String timeLeftKey = ChatColor.YELLOW + "Time Left:";
    private String playersLeftKey = ChatColor.YELLOW + "Players Left:";
    private final Set<UUID> frozenPlayers = new HashSet<>();
    private Material currentRoundPillarMaterial;

    @Override
    public void onEnable() {
        getLogger().info("Chaos Pillars enabled.");
        gameWorld = Bukkit.getWorld("world");
        if (gameWorld == null) {
            getLogger().severe("Could not find world 'world'. Plugin disabled.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        clearArea();
        gamerule();
        ensureBedrockFloorAtMinus64();
        gameWorld.getWorldBorder().setCenter(0, 0);
        gameWorld.getWorldBorder().setSize(40);
        gameWorld.getWorldBorder().setWarningDistance(1);
        gameWorld.getWorldBorder().setWarningTime(5);

        Bukkit.getPluginManager().registerEvents(this, this);


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
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.isOp()) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        switch (command.getName().toLowerCase()) {
            case "startchaos":
                if (gameTask != null || countdownTask != null) {
                    player.sendMessage(ChatColor.RED + "Chaos game is already running!");
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
                endGame();
                if (countdownTask != null) {
                    countdownTask.cancel();
                    countdownTask = null;
                }
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
        timer = 600;

        gameTask = new BukkitRunnable() {
            @Override
            public void run() {
                timer--;
                if (objective != null) {
                    // Use consistent keys
                    objective.getScore(timeLeftKey).setScore(timer);
                    objective.getScore(playersLeftKey).setScore(activePlayers.size());
                }

                if (bossBar != null) {
                    bossBar.setProgress(Math.max(0, timer / 600.0));
                }

                if (timer <= 0) {
                    Bukkit.broadcastMessage(ChatColor.RED + "Time is up! Game over.");
                    endGame();
                }
            }
        };
        gameTask.runTaskTimer(this, 20L, 20L);
    }

    private void endGame() {
        stopGameTasks();
        resetScoreboard();

        Location spawn = gameWorld.getSpawnLocation();
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.teleport(spawn);
            player.setGameMode(GameMode.SPECTATOR);
        }

        activePlayers.clear();
        clearArea();
        killAllMobs();

        if (bossBar != null) {
            bossBar.removeAll();
            bossBar = null;
        }
    }

    private void startGame() {


        activePlayers.clear();
        pillarLocations.clear();
        killAllMobs();
        startScoreboard();

        if (bossBar != null) {
            bossBar.removeAll();
            bossBar = null;
        }

        Random random = new Random();
        currentRoundPillarMaterial = PILLAR_BLOCK_TYPES[random.nextInt(PILLAR_BLOCK_TYPES.length)];
        Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "This round's pillars are made of " + ChatColor.YELLOW + currentRoundPillarMaterial.name().toLowerCase().replace('_', ' ') + "!");

        int radius = 12;
        int height = 50;
        int baseY = -63;

        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        Collections.shuffle(players); // Randomize pillar assignment

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
                pillarLocations.add(loc);
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
            activePlayers.add(player.getUniqueId());
        }

        gameWorld.getWorldBorder().setCenter(0, 0);
        gameWorld.getWorldBorder().setSize(40);

        bossBar = Bukkit.createBossBar(ChatColor.GOLD + "Time Remaining", BarColor.YELLOW, BarStyle.SEGMENTED_10);
        for (UUID uuid : activePlayers) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                bossBar.addPlayer(player);
            }
        }
        bossBar.setProgress(1.0);
        bossBar.setVisible(true);

        // Freeze all players
        for (UUID uuid : activePlayers) {
            frozenPlayers.add(uuid);
        }

        // Countdown before enabling movement and starting game
        new BukkitRunnable() {
            int countdown = 10;

            @Override
            public void run() {
                if (countdown == 0) {
                    Bukkit.broadcastMessage(ChatColor.GREEN + "Go!");
                    for (UUID uuid : frozenPlayers) {
                        Player player = Bukkit.getPlayer(uuid);
                        if (player != null) {
                            player.sendTitle(ChatColor.GREEN + "GO!", "", 10, 40, 10);
                        }
                    }
                    frozenPlayers.clear();
                    startTimer();

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
                                } catch (Exception ignored) {}
                            }
                        }
                    };
                    itemTask.runTaskTimer(ChaosPillars.this, 80L, 80L);
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

        // Re-attach scoreboard and bossbar if game is active
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

    private BukkitRunnable clearTask;

    private void clearArea() {
        int radius = 18;
        int minY = -63;
        int maxY = gameWorld.getMaxHeight();

        // Start coordinates
        final int[] x = {-radius};
        final int[] y = {minY};
        final int[] z = {-radius};
        final int batchSize = 100;

        clearTask = new BukkitRunnable() {
            @Override
            public void run() {
                int cleared = 0;

                while (cleared < batchSize) {
                    // Skip the floor layer at Y = -64
                    if (y[0] == -64) {
                        y[0]++;
                        continue;
                    }

                    // Only clear blocks within a circular radius
                    if (x[0] * x[0] + z[0] * z[0] <= radius * radius) {
                        Block block = gameWorld.getBlockAt(x[0], y[0], z[0]);
                        if (block.getType() != Material.AIR) {
                            block.setType(Material.AIR, false); // Set to AIR without physics
                            cleared++;
                        }
                    }

                    // Advance Z
                    z[0]++;
                    if (z[0] > radius) {
                        z[0] = -radius;
                        y[0]++;
                        if (y[0] >= maxY) {
                            y[0] = minY;
                            x[0]++;
                            if (x[0] > radius) {
                                cancel();
                                getLogger().info("✔ Area cleared!");
                                return;
                            }
                        }
                    }
                }
            }
        };

        clearTask.runTaskTimer(this, 0L, 1L);
    }




    private void ensureBedrockFloorAtMinus64() {
        if (gameWorld == null) return;

        int radius = 18;
        int floorY = -64;
        int centerX = 0;
        int centerZ = 0;

        int minX = centerX - radius;
        int maxX = centerX + radius;
        int minZ = centerZ - radius;
        int maxZ = centerZ + radius;

        double radiusSquared = radius * radius;

        new BukkitRunnable() {
            int x = minX;
            int z = minZ;

            @Override
            public void run() {
                int placed = 0;
                int blocksPerTick = 100;

                while (x <= maxX && placed < blocksPerTick) {
                    while (z <= maxZ && placed < blocksPerTick) {
                        double dx = x + 0.5 - centerX;
                        double dz = z + 0.5 - centerZ;

                        if ((dx * dx + dz * dz) <= radiusSquared) {
                            Location loc = new Location(gameWorld, x, floorY, z);
                            if (loc.getBlock().getType() != Material.BEDROCK) {
                                loc.getBlock().setType(Material.BEDROCK);
                            }
                            placed++;
                        }
                        z++;
                    }
                    if (z > maxZ) {
                        z = minZ;
                        x++;
                    }
                }

                if (x > maxX) {
                    cancel();
                    getLogger().info("Finished generating circular bedrock floor with radius 18 at Y = -64.");
                }
            }
        }.runTaskTimer(this, 0L, 1L);
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