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
    private final ChaosPillars plugin;
    private final Random random = new Random();

    public GameGenerateTask(ChaosPillars plugin) {
        this.plugin = plugin;
        this.gameWorld = plugin.getGameWorld();
        this.floorBlockTypes = plugin.floorBlockTypes;
        this.pillarBlockTypes = plugin.pillarBlockTypes;
    }

    public List<Location> generateArena(int pillarRadius, int pillarHeight, int baseY, int pillarCount) {
        if (gameWorld == null || !gameWorld.equals(Bukkit.getWorld(gameWorld.getName()))) {
            return new ArrayList<>();
        }
        generateFloor(baseY);
        
        int playerCount = Bukkit.getOnlinePlayers().size();
        boolean useExtraRing = playerCount > 10 || plugin.isForceExtraRing();
        
        if (useExtraRing) {
            return generateTwoRings(pillarRadius, pillarHeight, baseY, pillarCount);
        } else {
            return generatePillars(pillarRadius, pillarHeight, baseY, pillarCount);
        }
    }
    
    private List<Location> generateTwoRings(int innerRadius, int innerHeight, int baseY, int innerCount) {
        List<Location> allLocations = new ArrayList<>();
        
        Material currentRoundPillarMaterial;
        if (pillarBlockTypes.isEmpty()) {
            Bukkit.getLogger().warning("No valid pillar block types available. Defaulting to stone.");
            currentRoundPillarMaterial = Material.STONE;
        } else {
            currentRoundPillarMaterial = pillarBlockTypes.get(random.nextInt(pillarBlockTypes.size()));
        }
        
        Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "This round's pillars are made of " +
                ChatColor.YELLOW + currentRoundPillarMaterial.name().toLowerCase().replace('_', ' ') + "!");
        
        int outerRadius = innerRadius + 6;
        int outerCount = calcPillarSpacing(outerRadius, 6.0);
        
        for (int i = 0; i < innerCount; i++) {
            double angle = 2 * Math.PI * i / innerCount;
            int x = (int) (innerRadius * Math.cos(angle));
            int z = (int) (innerRadius * Math.sin(angle));
            
            for (int y = 0; y < innerHeight; y++) {
                Location loc = new Location(gameWorld, x, baseY + y, z);
                loc.getBlock().setType(currentRoundPillarMaterial, false);
            }
            
            allLocations.add(new Location(gameWorld, x, baseY, z));
        }
        
        for (int i = 0; i < outerCount; i++) {
            double angle = 2 * Math.PI * i / outerCount;
            int x = (int) (outerRadius * Math.cos(angle));
            int z = (int) (outerRadius * Math.sin(angle));
            
            for (int y = 0; y < innerHeight; y++) {
                Location loc = new Location(gameWorld, x, baseY + y, z);
                loc.getBlock().setType(currentRoundPillarMaterial, false);
            }
            
            allLocations.add(new Location(gameWorld, x, baseY, z));
        }
        
        return allLocations;
    }
    
    private int calcPillarSpacing(int radius, double desiredSpacing) {
        if (desiredSpacing <= 0 || desiredSpacing >= 2 * radius) {
            return 10;
        }
        double sinValue = desiredSpacing / (2.0 * radius);
        if (sinValue > 1.0) {
            return 10;
        }
        int count = (int) Math.round(Math.PI / Math.asin(sinValue));
        return Math.max(count, 4);
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

        int playerCount = Bukkit.getOnlinePlayers().size();
        boolean useExtraRing = playerCount > 10 || plugin.isForceExtraRing();
        int floorRadius = useExtraRing ? 28 : 18;
        
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
