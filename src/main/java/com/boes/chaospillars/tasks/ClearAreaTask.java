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
    private final int clearMaxX;
    private final int clearMinZ;
    private final int clearMaxZ;
    private final int bedrockMinX;
    private final int bedrockMaxX;
    private final int bedrockMinZ;
    private final int bedrockMaxZ;
    private final int maxY;
    private int x;
    private int y;
    private int z;

    public ClearAreaTask(JavaPlugin plugin) {
        if (!(plugin instanceof ChaosPillars chaosPlugin)) {
            throw new IllegalArgumentException("Plugin must be an instance of ChaosPillars");
        }

        this.gameWorld = chaosPlugin.getGameWorld();

        if (gameWorld == null) {
            throw new IllegalStateException("Game world is not loaded yet!");
        }

        double borderSize = gameWorld.getWorldBorder().getSize();
        int centerX = gameWorld.getWorldBorder().getCenter().getBlockX();
        int centerZ = gameWorld.getWorldBorder().getCenter().getBlockZ();

        int clearRadius = (int) (borderSize / 2) + 2;
        int clearMinX = centerX - clearRadius;
        this.clearMaxX = centerX + clearRadius;
        this.clearMinZ = centerZ - clearRadius;
        this.clearMaxZ = centerZ + clearRadius;

        int bedrockRadius = (int) (borderSize / 2);
        this.bedrockMinX = centerX - bedrockRadius;
        this.bedrockMaxX = centerX + bedrockRadius;
        this.bedrockMinZ = centerZ - bedrockRadius;
        this.bedrockMaxZ = centerZ + bedrockRadius;

        this.maxY = gameWorld.getMaxHeight();

        this.x = clearMinX;
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
        int minY = -64;

        while (changed < batchSize) {
            Block block = gameWorld.getBlockAt(x, y, z);

            if (y == minY) {
                if (x >= bedrockMinX && x <= bedrockMaxX && z >= bedrockMinZ && z <= bedrockMaxZ) {
                    if (block.getType() != Material.BEDROCK) {
                        block.setType(Material.BEDROCK, false);
                    }
                } else {
                    if (block.getType() != Material.AIR) {
                        block.setType(Material.AIR, false);
                    }
                }
            } else {
                if (block.getType() != Material.AIR) {
                    block.setType(Material.AIR, false);
                }
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
                        cancel();
                        Bukkit.getLogger().info("Cleared area and generated bedrock floor!");
                        return;
                    }
                }
            }
        }
    }
}
