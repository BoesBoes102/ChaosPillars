package com.boes.chaospillars.tasks;

import com.boes.chaospillars.ChaosPillars;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameGenerateTask {

    private final World gameWorld;
    private final List<Material> floorBlockTypes;
    private final List<Material> pillarBlockTypes;
    private final Random random = new Random();

    public GameGenerateTask(ChaosPillars plugin) {
        this.gameWorld = plugin.getGameWorld();
        this.floorBlockTypes = plugin.floorBlockTypes;
        this.pillarBlockTypes = plugin.pillarBlockTypes;
    }

    public List<Location> generateArena(int pillarRadius, int pillarHeight, int baseY, int pillarCount) {
        if (gameWorld == null || !gameWorld.equals(Bukkit.getWorld(gameWorld.getName()))) {
            return new ArrayList<>();
        }
        generateFloor(baseY);
        return generatePillars(pillarRadius, pillarHeight, baseY, pillarCount);
    }

    private void generateFloor(int baseY) {
        if (floorBlockTypes.isEmpty()) {
            Bukkit.getLogger().warning("No valid floor block types available. Skipping floor generation.");
            return;
        }

        double worldBorderRadius = gameWorld.getWorldBorder().getSize() / 2;
        int minX = (int) -worldBorderRadius;
        int maxX = (int) worldBorderRadius;
        int minZ = (int) -worldBorderRadius;
        int maxZ = (int) worldBorderRadius;

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                gameWorld.getBlockAt(x, baseY - 1, z).setType(Material.AIR, false);
            }
        }

        int floorRadius = 18;
        Material currentRoundFloorMaterial = floorBlockTypes.get(random.nextInt(floorBlockTypes.size()));

        for (int x = -floorRadius; x <= floorRadius; x++) {
            for (int z = -floorRadius; z <= floorRadius; z++) {
                if (x * x + z * z <= floorRadius * floorRadius) {
                    gameWorld.getBlockAt(x, baseY, z).setType(currentRoundFloorMaterial, false);
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

        Material currentRoundPillarMaterial;
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
