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
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.boss.BossBar;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;

import java.util.*;

public class ChaosPillars extends JavaPlugin implements Listener {

    private final Set<Location> placedBlocks = new HashSet<>();
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

    @Override
    public void onEnable() {
        getLogger().info("Chaos Pillars enabled.");
        gameWorld = Bukkit.getWorld("world");
        if (gameWorld == null) {
            getLogger().severe("Could not find world 'world'. Plugin disabled.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        clearArea(); // Clears arena area at startup
        gamerule();

        // Set the permanent world border here
        gameWorld.getWorldBorder().setCenter(0, 0);
        gameWorld.getWorldBorder().setSize(36);
        gameWorld.getWorldBorder().setWarningDistance(1); // optional: show warning near border
        gameWorld.getWorldBorder().setWarningTime(5);

        Bukkit.getPluginManager().registerEvents(this, this);
        clearSquareShellBeyondWorldBorder();
        ensureBedrockFloorAtMinus64();
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

        // Require OP for all commands
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

            default:
                return false;
        }
    }

    private void startCountdown() {
        int playerCount = Bukkit.getOnlinePlayers().size();

        if (playerCount < 2) {
            Bukkit.broadcastMessage(ChatColor.RED + "Not enough players to start Chaos Pillars! Need at least 2.");
            return; // Do not start
        }

        if (playerCount > 10) {
            Bukkit.broadcastMessage(ChatColor.RED + "Chaos Pillars supports a maximum of 10 players!");
            return; // Do not start
        }

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
                    // Update time left score
                    objective.getScore(ChatColor.YELLOW + "Time Left: ").setScore(timer);
                    // Update players left score dynamically
                    objective.getScore(ChatColor.RED + "Players Left: ").setScore(activePlayers.size());
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
        clearArea();
        activePlayers.clear();
        pillarLocations.clear();
        killAllMobs();
        startScoreboard();
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar = null;
        }
        int radius = 12;
        int height = 50;
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        for (Player p : players) {
            p.getInventory().clear();
        }

        // Create pillars and store their base location (one per pillar)
        List<Location> basePillarLocations = new ArrayList<>();
        int pillarCount = 10;

        for (int i = 0; i < pillarCount; i++) {
            double angle = 2 * Math.PI * i / pillarCount;
            int x = (int) (radius * Math.cos(angle));
            int z = (int) (radius * Math.sin(angle));
            int baseY = gameWorld.getHighestBlockYAt(x, z) + 1;

            // Build pillar
            for (int y = 0; y < height; y++) {
                Location loc = new Location(gameWorld, x, baseY + y, z);
                loc.getBlock().setType(Material.BEDROCK);
                pillarLocations.add(loc);
            }

            basePillarLocations.add(new Location(gameWorld, x, baseY, z));
        }

        // Shuffle players to avoid spawning next to the same people every game
        Collections.shuffle(players);

        int playerCount = players.size();

        if (playerCount == 3) {
            // Special case: space 3 players evenly around the circle, i.e. 120 degrees apart
            for (int i = 0; i < playerCount; i++) {
                double angle = 2 * Math.PI * i / playerCount; // 0, 120, 240 degrees
                int x = (int) (radius * Math.cos(angle));
                int z = (int) (radius * Math.sin(angle));
                int baseY = gameWorld.getHighestBlockYAt(x, z) + 1;
                Location tp = new Location(gameWorld, x + 0.5, baseY + height, z + 0.5);

                Player player = players.get(i);
                player.teleport(tp);
                player.setGameMode(GameMode.SURVIVAL);
                player.setHealth(20);
                player.setFoodLevel(20);
                activePlayers.add(player.getUniqueId());
            }
        } else {
            // Otherwise, assign players to the first N pillar bases (which we already generated)
            for (int i = 0; i < playerCount && i < basePillarLocations.size(); i++) {
                Location baseLoc = basePillarLocations.get(i);
                Player player = players.get(i);
                Location tp = baseLoc.clone().add(0.5, height, 0.5);
                player.teleport(tp);
                player.setGameMode(GameMode.SURVIVAL);
                player.setHealth(20);
                player.setFoodLevel(20);
                activePlayers.add(player.getUniqueId());
            }
        }

        gameWorld.getWorldBorder().setCenter(0, 0);
        gameWorld.getWorldBorder().setSize(32);

        // Create the boss bar and add players now that activePlayers is filled
        bossBar = Bukkit.createBossBar(ChatColor.GOLD + "Time Remaining", BarColor.YELLOW, BarStyle.SEGMENTED_10);
        for (UUID uuid : activePlayers) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                bossBar.addPlayer(player);
            }
        }
        bossBar.setProgress(1.0);
        bossBar.setVisible(true);

        // Start the timer task
        startTimer();

        // Start giving random items periodically
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
    }

    private void clearArea() {
        int radius = 50;
        int minY = -63; // Start at -63 so -64 layer is kept
        int maxY = gameWorld.getMaxHeight(); // usually 319

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = minY; y < maxY; y++) {
                    if (y == -64) continue; // keep the bottom layer
                    Location loc = new Location(gameWorld, x, y, z);
                    loc.getBlock().setType(Material.AIR);
                }
            }
        }
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

    private void clearSquareShellBeyondWorldBorder() {
        if (gameWorld == null) return;

        WorldBorder border = gameWorld.getWorldBorder();
        Location center = border.getCenter();
        double borderRadius = border.getSize() / 2.0;

        int minY = gameWorld.getMinHeight();
        int maxY = gameWorld.getMaxHeight();

        int innerRadius = (int) Math.floor(borderRadius);
        int outerRadius = innerRadius + 5; // 5 blocks outside border

        int minX = (int) Math.floor(center.getX()) - outerRadius;
        int maxX = (int) Math.ceil(center.getX()) + outerRadius;
        int minZ = (int) Math.floor(center.getZ()) - outerRadius;
        int maxZ = (int) Math.ceil(center.getZ()) + outerRadius;

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                // Check if block is outside the border square but inside the outer square
                boolean outsideInner = x < (center.getX() - innerRadius) || x > (center.getX() + innerRadius)
                        || z < (center.getZ() - innerRadius) || z > (center.getZ() + innerRadius);

                boolean insideOuter = x >= (center.getX() - outerRadius) && x <= (center.getX() + outerRadius)
                        && z >= (center.getZ() - outerRadius) && z <= (center.getZ() + outerRadius);

                if (outsideInner && insideOuter) {
                    for (int y = minY; y <= maxY; y++) {
                        Location loc = new Location(gameWorld, x, y, z);
                        loc.getBlock().setType(Material.AIR);
                    }
                }
            }
        }

        getLogger().info("Cleared 5-block thick square shell outside the world border.");
    }

    private void ensureBedrockFloorAtMinus64() {
        if (gameWorld == null) return;

        WorldBorder border = gameWorld.getWorldBorder();
        Location center = border.getCenter();
        double borderRadius = border.getSize() / 2.0;

        int floorY = -64;
        int minX = (int) Math.floor(center.getX() - borderRadius);
        int maxX = (int) Math.ceil(center.getX() + borderRadius);
        int minZ = (int) Math.floor(center.getZ() - borderRadius);
        int maxZ = (int) Math.ceil(center.getZ() + borderRadius);

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                Location loc = new Location(gameWorld, x, floorY, z);
                if (loc.getBlock().getType() != Material.BEDROCK) {
                    loc.getBlock().setType(Material.BEDROCK);
                }
            }
        }
        getLogger().info("Ensured bedrock floor at Y=-64 covering the world border area.");
    }
}
