package com.boes.chaospillars.tasks;

import com.boes.chaospillars.ChaosPillars;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class ClearAreaTask extends BukkitRunnable {

    private final World gameWorld;
    private final int maxX;
    private final int maxY;
    private final int minZ;
    private final int maxZ;
    private int x;
    private int y;
    private int z;


    public ClearAreaTask(JavaPlugin plugin) {
        if (!(plugin instanceof ChaosPillars)) {
            throw new IllegalArgumentException("Plugin must be an instance of ChaosPillars");
        }

        ChaosPillars chaosPlugin = (ChaosPillars) plugin;
        this.gameWorld = chaosPlugin.getGameWorld();

        if (gameWorld == null) {
            throw new IllegalStateException("Game world is not loaded yet!");
        }

        int radius = (int) (gameWorld.getWorldBorder().getSize() / 2 + 1);
        int centerX = gameWorld.getWorldBorder().getCenter().getBlockX();
        int centerZ = gameWorld.getWorldBorder().getCenter().getBlockZ();

        int minX = centerX - radius;
        this.maxX = centerX + radius;
        this.minZ = centerZ - radius;
        this.maxZ = centerZ + radius;
        this.maxY = gameWorld.getMaxHeight();

        this.x = minX;
        this.y = maxY;
        this.z = minZ;
    }

    @Override
    public void run() {
        if (gameWorld == null || !gameWorld.equals(Bukkit.getWorld(gameWorld.getName()))) {
            cancel();
            return;
        }

        int changed = 0;
        int batchSize = 50000;

        while (changed < batchSize) {
            Block block = gameWorld.getBlockAt(x, y, z);
            int minY = -64;

            if (y == minY) {
                if (block.getType() != Material.BEDROCK) {
                    block.setType(Material.BEDROCK, false);
                }
            } else {
                if (block.getType() != Material.AIR) {
                    block.setType(Material.AIR, false);
                }
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
                        cancel();
                        Bukkit.getLogger().info("Cleared area and generated bedrock floor!");
                        return;
                    }
                }
            }
        }
    }
}
