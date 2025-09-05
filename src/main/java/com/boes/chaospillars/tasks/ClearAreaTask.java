package com.boes.chaospillars.tasks;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitRunnable;

public class ClearAreaTask extends BukkitRunnable {
    private final World gameWorld;
    private final int minX;
    private final int maxX;
    private final int minY = -64;
    private final int maxY;
    private final int minZ;
    private final int maxZ;



    private int x;
    private int y;
    private int z;

    public ClearAreaTask(World world) {
        this.gameWorld = world;

        int radius = (int) (world.getWorldBorder().getSize() / 2);
        int centerX = world.getWorldBorder().getCenter().getBlockX();
        int centerZ = world.getWorldBorder().getCenter().getBlockZ();

        this.minX = centerX - radius;
        this.maxX = centerX + radius;
        this.minZ = centerZ - radius;
        this.maxZ = centerZ + radius;

        this.maxY = world.getMaxHeight();

        this.x = minX;
        this.y = maxY;
        this.z = minZ;
    }

    @Override
    public void run() {
        int changed = 0;

        int batchSize = 50000;
        while (changed < batchSize) {
            Block block = gameWorld.getBlockAt(x, y, z);

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
                        Bukkit.getLogger().info("✔ Cleared area and generated bedrock floor!");
                        return;
                    }
                }
            }
        }
    }
}
