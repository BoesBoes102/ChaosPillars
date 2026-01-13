package com.boes.chaospillars.tasks;

import com.boes.chaospillars.ChaosPillars;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class ClearAreaTask extends BukkitRunnable {

    private final JavaPlugin plugin;
    private final World gameWorld;
    private final int clearMaxX;
    private final int clearMinX;
    private final int clearMinZ;
    private final int clearMaxZ;
    private final int maxY;
    private int x;
    private int y;
    private int z;

    public ClearAreaTask(JavaPlugin plugin) {
        if (!(plugin instanceof ChaosPillars chaosPlugin)) {
            throw new IllegalArgumentException("Plugin must be an instance of ChaosPillars");
        }

        this.plugin = plugin;

        this.gameWorld = chaosPlugin.getGameWorld();

        if (gameWorld == null) {
            throw new IllegalStateException("Game world is not loaded yet!");
        }

        double borderSize = 61; // Always clear size of a duel rings game
        int centerX = gameWorld.getWorldBorder().getCenter().getBlockX();
        int centerZ = gameWorld.getWorldBorder().getCenter().getBlockZ();

        int clearRadius = (int) (borderSize / 2) + 2;
        this.clearMinX = centerX - clearRadius;
        this.clearMaxX = centerX + clearRadius;
        this.clearMinZ = centerZ - clearRadius;
        this.clearMaxZ = centerZ + clearRadius;

        this.maxY = gameWorld.getMaxHeight();

        this.x = this.clearMinX;
        this.y = maxY;
        this.z = clearMinZ;
    }

    @Override
    public void run() {
        if (gameWorld == null || !gameWorld.equals(Bukkit.getWorld(gameWorld.getName()))) {
            cancel();
            return;
        }

        int changed = 0;
        int batchSize = 50000;
        int minY = gameWorld.getMinHeight();

        while (changed < batchSize) {
            Block block = gameWorld.getBlockAt(x, y, z);
            Material type = block.getType();
            boolean isFluid = type == Material.WATER || type == Material.LAVA;

            if (type != Material.AIR) {
                block.setType(Material.AIR, isFluid);
            }

            changed++;
            z++;
            if (z > clearMaxZ) {
                z = clearMinZ;
                y--;
                if (y < minY) {
                    y = maxY;
                    x++;
                    if (x > clearMaxX) {
                        new FLowingCleanupTask(plugin, gameWorld, clearMinX, clearMaxX, clearMinZ, clearMaxZ, minY, maxY).runTaskTimer(plugin, 1L, 1L);
                        cancel();
                        Bukkit.getLogger().info("Cleared area!");
                        return;
                    }
                }
            }
        }
    }

    private static class FLowingCleanupTask extends BukkitRunnable {

        private final JavaPlugin plugin;
        private final World world;
        private final int minX;
        private final int maxX;
        private final int minZ;
        private final int maxZ;
        private final int minY;
        private final int maxY;

        private int x;
        private int y;
        private int z;
        private int pass = 0;
        private int changesThisPass = 0;

        private FLowingCleanupTask(JavaPlugin plugin, World world, int minX, int maxX, int minZ, int maxZ, int minY, int maxY) {
            this.plugin = plugin;
            this.world = world;
            this.minX = minX;
            this.maxX = maxX;
            this.minZ = minZ;
            this.maxZ = maxZ;
            this.minY = minY;
            this.maxY = maxY;

            this.x = minX;
            this.y = maxY;
            this.z = minZ;
        }

        @Override
        public void run() {
            if (world == null || !world.equals(Bukkit.getWorld(world.getName())) || !plugin.isEnabled()) {
                cancel();
                return;
            }

            int changed = 0;
            int batchSize = 50000;

            while (changed < batchSize) {
                Block block = world.getBlockAt(x, y, z);
                Material type = block.getType();
                if (type == Material.WATER || type == Material.LAVA) {
                    block.setType(Material.AIR, true);
                    changesThisPass++;
                }

                changed++;
                z++;
                if (z > maxZ) {
                    z = minZ;
                    y--;
                    if (y < minY) {
                        y = maxY;
                        x++;
                        if (x > maxX) {
                            pass++;
                            if (changesThisPass == 0 || pass >= 5) {
                                cancel();
                                return;
                            }

                            changesThisPass = 0;
                            x = minX;
                            y = maxY;
                            z = minZ;
                            return;
                        }
                    }
                }
            }
        }
    }
}
