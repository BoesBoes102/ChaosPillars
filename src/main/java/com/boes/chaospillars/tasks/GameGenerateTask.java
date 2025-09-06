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
    private Material currentRoundPillarMaterial;

    public GameGenerateTask(World gameWorld, List<Material> floorBlockTypes, List<Material> pillarBlockTypes) {
        this.gameWorld = gameWorld;
        this.floorBlockTypes = floorBlockTypes;
        this.pillarBlockTypes = pillarBlockTypes;
    }

    public List<Location> generateArena(int pillarRadius, int pillarHeight, int baseY, int pillarCount) {
        generateFloor(baseY);
        return generatePillars(pillarRadius, pillarHeight, baseY, pillarCount);
    }


    private void generateFloor(int baseY) {
        if (floorBlockTypes.isEmpty()) {
            Bukkit.getLogger().warning("No valid floor block types available. Skipping floor generation.");
            return;
        }

        int radius = 18;
        currentRoundFloorMaterial = floorBlockTypes.get(random.nextInt(floorBlockTypes.size()));

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x * x + z * z <= radius * radius) {
                    Block block = gameWorld.getBlockAt(x, baseY, z);
                    block.setType(currentRoundFloorMaterial, false);
                }
            }
        }

        Bukkit.getLogger().info("Generated circular floor with " + currentRoundFloorMaterial + " at Y = " + baseY);

        Bukkit.broadcastMessage(ChatColor.AQUA + "This round's floor is made of " +
                ChatColor.YELLOW + currentRoundFloorMaterial.name().toLowerCase().replace('_', ' ') +
                ChatColor.AQUA + "!");
    }

    private List<Location> generatePillars(int radius, int height, int baseY, int count) {
        List<Location> basePillarLocations = new ArrayList<>();

        if (pillarBlockTypes.isEmpty()) {
            Bukkit.getLogger().warning("No valid pillar block types available. Defaulting to stone.");
            currentRoundPillarMaterial = Material.STONE;
        } else {
            currentRoundPillarMaterial = pillarBlockTypes.get(random.nextInt(pillarBlockTypes.size()));
        }


        Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "This round's pillars are made of " +
                ChatColor.YELLOW + currentRoundPillarMaterial.name().toLowerCase().replace('_', ' ') + "!");


        for (int i = 0; i < count; i++) {
            double angle = 2 * Math.PI * i / count;
            int x = (int) (radius * Math.cos(angle));
            int z = (int) (radius * Math.sin(angle));

            for (int y = 0; y < height; y++) {
                Location loc = new Location(gameWorld, x, baseY + y, z);
                loc.getBlock().setType(currentRoundPillarMaterial, false);
            }

            basePillarLocations.add(new Location(gameWorld, x, baseY, z));
        }

        return basePillarLocations;
    }
}
