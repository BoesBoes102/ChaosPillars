package com.boes.chaospillars.config;

import com.boes.chaospillars.ChaosPillars;
import org.bukkit.Material;

import java.util.*;
import java.util.stream.Collectors;

public record ReloadConfigTask(ChaosPillars plugin) {

    public void run() {
        plugin.getLogger().info("Reloading configuration...");

        plugin.reloadConfig();

        plugin.timer = plugin.getConfig().getInt("game.timer-seconds", 300);
        plugin.powerupCooldown = plugin.getConfig().getInt("game.powerup-cooldown-seconds", 30);
        plugin.itemGiveIntervalTicks = plugin.getConfig().getInt("game.item-give-interval-seconds", 3) * 20;
        plugin.eventCooldown = plugin.getConfig().getInt("game.event-cooldown-seconds", 20);

        loadFloorBlockTypes();

        loadPillarBlockTypes();

        plugin.getLogger().info("Configuration reloaded successfully!");
        plugin.getLogger().info("Loaded " + plugin.floorBlockTypes.size() + " floor block types");
        plugin.getLogger().info("Loaded " + plugin.pillarBlockTypes.size() + " pillar block types");
    }
    
    private void loadFloorBlockTypes() {
        List<String> floorStrings = plugin.getConfig().getStringList("game.floor-block-types");

        if (floorStrings.isEmpty()) {
            floorStrings = plugin.getConfig().getStringList("floor-block-types");
        }
        
        List<Material> parsedFloorMaterials = floorStrings.stream()
                .map(name -> {
                    try {
                        Material material = Material.valueOf(name.toUpperCase());
                        if (!material.isBlock() && material != Material.AIR) {
                            plugin.getLogger().warning("Floor block type '" + name + "' is not a block material. Skipping.");
                            return null;
                        }
                        return material;
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid floor block type in config: " + name);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (!parsedFloorMaterials.isEmpty()) {
            plugin.floorBlockTypes = parsedFloorMaterials;
        } else {
            plugin.getLogger().warning("No valid floor block types in config. Using defaults.");
            plugin.floorBlockTypes = getDefaultFloorMaterials();
        }
    }
    
    private void loadPillarBlockTypes() {
        List<String> materialNames = plugin.getConfig().getStringList("game.pillar-block-types");
        List<Material> validMaterials = new ArrayList<>();
        
        for (String name : materialNames) {
            try {
                Material mat = Material.valueOf(name.toUpperCase());
                if (!mat.isBlock()) {
                    plugin.getLogger().warning("Pillar block type '" + name + "' is not a block material. Skipping.");
                    continue;
                }
                validMaterials.add(mat);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid pillar block type in config: " + name);
            }
        }

        if (!validMaterials.isEmpty()) {
            plugin.pillarBlockTypes = validMaterials;
        } else {
            plugin.getLogger().warning("No valid pillar block types in config. Using defaults.");
            plugin.pillarBlockTypes = getDefaultPillarMaterials();
        }
    }

    private List<Material> getDefaultFloorMaterials() {
        return Arrays.asList(
                Material.END_STONE,
                Material.STONE,
                Material.SLIME_BLOCK,
                Material.AIR,
                Material.HONEY_BLOCK,
                Material.WHITE_WOOL,
                Material.GLASS,
                Material.GLASS_PANE,
                Material.MOSS_BLOCK,
                Material.CHERRY_LEAVES,
                Material.NETHERRACK,
                Material.BEDROCK,
                Material.DIAMOND_BLOCK,
                Material.COBWEB,
                Material.CRAFTING_TABLE,
                Material.SOUL_SAND,
                Material.OAK_TRAPDOOR,
                Material.MAGMA_BLOCK,
                Material.TNT,
                Material.BLACK_CARPET,
                Material.BLUE_ICE,
                Material.POINTED_DRIPSTONE,
                Material.SCAFFOLDING,
                Material.LAVA,
                Material.WATER
        );
    }

    private List<Material> getDefaultPillarMaterials() {
        return Arrays.asList(
                Material.BEDROCK,
                Material.SAND,
                Material.NETHERRACK,
                Material.END_STONE,
                Material.CHERRY_LOG,
                Material.CRAFTING_TABLE,
                Material.CHERRY_LEAVES,
                Material.COBBLESTONE,
                Material.OBSIDIAN,
                Material.REDSTONE_BLOCK,
                Material.TNT,
                Material.NOTE_BLOCK,
                Material.SEA_LANTERN,
                Material.MELON,
                Material.PUMPKIN,
                Material.AMETHYST_BLOCK,
                Material.GLASS,
                Material.GLASS_PANE
        );
    }
}
