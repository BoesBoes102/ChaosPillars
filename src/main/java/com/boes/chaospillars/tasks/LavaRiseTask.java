package com.boes.chaospillars.tasks;

import com.boes.chaospillars.ChaosPillars;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitRunnable;

public class LavaRiseTask {
    private final ChaosPillars plugin;
    private final World gameWorld;
    private BukkitRunnable lavaTask;

    public LavaRiseTask(ChaosPillars plugin) {
        if (plugin == null || plugin.getGameWorld() == null) {
            throw new IllegalArgumentException("Plugin and gameWorld cannot be null");
        }
        this.plugin = plugin;
        this.gameWorld = plugin.getGameWorld();
    }

    public void start() {
        int lavaStartY = -64;
        int lavaEndY = -0;
        int lavaDurationSeconds = 80;
        long totalTicks = lavaDurationSeconds * 20L;

        WorldBorder border = gameWorld.getWorldBorder();
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
                if (!gameWorld.equals(Bukkit.getWorld(gameWorld.getName()))) {
                    cancel();
                    return;
                }

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
                            Block block = gameWorld.getBlockAt(x, currentY, z);
                            if (block.getType() != Material.LAVA) {
                                block.setType(Material.LAVA);
                            }
                        }
                    }
                }

                ticksPassed++;
            }
        };

        lavaTask.runTaskTimer(plugin, 0L, 1L);
    }

    public void stop() {
        if (lavaTask != null && !lavaTask.isCancelled()) {
            lavaTask.cancel();
            lavaTask = null;
        }
    }
}
