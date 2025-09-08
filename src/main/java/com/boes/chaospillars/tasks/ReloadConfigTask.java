package com.boes.chaospillars.tasks;

import com.boes.chaospillars.ChaosPillars;
import org.bukkit.Material;

import java.util.*;
import java.util.stream.Collectors;

public record ReloadConfigTask(ChaosPillars plugin) {

    public void run() {

        plugin.timer = plugin.getConfig().getInt("game.timer-seconds", 300);
        plugin.powerupCooldown = plugin.getConfig().getInt("game.powerup-cooldown-seconds", 30);
        plugin.itemGiveIntervalTicks = plugin.getConfig().getInt("game.item-give-interval-seconds", 3) * 20;
        plugin.eventCooldown = plugin.getConfig().getInt("game.event-cooldown-seconds", 20);


        List<String> floorStrings = plugin.getConfig().getStringList("floor-block-types");
        List<Material> parsedFloorMaterials = floorStrings.stream()
                .map(name -> {
                    try {
                        return Material.valueOf(name.toUpperCase());
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


        List<String> materialNames = plugin.getConfig().getStringList("game.pillar-block-types");
        List<Material> validMaterials = new ArrayList<>();
        for (String name : materialNames) {
            try {
                Material mat = Material.valueOf(name.toUpperCase());
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

        plugin.getLogger().info("Loaded floor blocks: " + plugin.floorBlockTypes);
        plugin.getLogger().info("Loaded pillar blocks: " + plugin.pillarBlockTypes);
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
