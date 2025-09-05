package com.boes.chaospillars.tasks;

import org.bukkit.*;
import org.bukkit.block.Block;

import java.util.*;

public class GameGenerateTask {
    private final World gameWorld;
    private final List<Material> floorBlockTypes;
    private final List<Material> pillarBlockTypes;
    private final Random random = new Random();

    private Material currentRoundFloorMaterial;

    public GameGenerateTask(World gameWorld, List<Material> floorBlockTypes, List<Material> pillarBlockTypes) {
        this.gameWorld = gameWorld;
        this.floorBlockTypes = floorBlockTypes;
        this.pillarBlockTypes = pillarBlockTypes;
    }

    public List<Location> generatePillars(Material material, int radius, int height, int baseY, int count) {
        List<Location> basePillarLocations = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            double angle = 2 * Math.PI * i / count;
            int x = (int) (radius * Math.cos(angle));
            int z = (int) (radius * Math.sin(angle));

            for (int y = 0; y < height; y++) {
                Location loc = new Location(gameWorld, x, baseY + y, z);
                loc.getBlock().setType(material, false);
            }

            basePillarLocations.add(new Location(gameWorld, x, baseY, z));
        }

        return basePillarLocations;
    }

    public void generateRandomFloor() {
        if (floorBlockTypes.isEmpty()) {
            Bukkit.getLogger().warning("No valid floor block types available. Skipping floor generation.");
            return;
        }

        int radius = 18;
        int y = -64;

        Material selectedMaterial = floorBlockTypes.get(random.nextInt(floorBlockTypes.size()));
        currentRoundFloorMaterial = selectedMaterial;

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x * x + z * z <= radius * radius) {
                    Block block = gameWorld.getBlockAt(x, y, z);
                    block.setType(selectedMaterial, false);
                }
            }
        }

        Bukkit.getLogger().info("Generated circular floor with " + selectedMaterial + " at Y = " + y);

        Bukkit.broadcastMessage(ChatColor.AQUA + "This round's floor is made of " +
                ChatColor.YELLOW + selectedMaterial.name().toLowerCase().replace('_', ' ') +
                ChatColor.AQUA + "!");
    }

    public Material getCurrentRoundFloorMaterial() {
        return currentRoundFloorMaterial;
    }
}
