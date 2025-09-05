package com.boes.chaospillars.tasks;

import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.World.Environment;


import java.util.Random;

public class SetupTask {

    private final JavaPlugin plugin;

    public SetupTask(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public World setupWorld() {
        String worldName = "Chaospillars";
        World gameWorld = Bukkit.getWorld(worldName);

        if (gameWorld == null) {
            plugin.getLogger().info("World '" + worldName + "' not found. Creating a fully empty void world...");

            WorldCreator creator = WorldCreator.name(worldName);
            creator.generator(new VoidGenerator());
            gameWorld = creator.createWorld();

            if (gameWorld == null) {
                plugin.getLogger().severe("Failed to create ChaosPillars void world!");
                return null;
            }

            plugin.getLogger().info("Void world '" + worldName + "' created successfully.");
        } else {
            plugin.getLogger().info("World '" + worldName + "' already exists.");
        }

        gameWorld.setDifficulty(Difficulty.NORMAL);
        gameWorld.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        gameWorld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        gameWorld.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        gameWorld.setGameRule(GameRule.SHOW_DEATH_MESSAGES, true);
        gameWorld.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
        gameWorld.setGameRule(GameRule.LOCATOR_BAR, false);

        gameWorld.getWorldBorder().setCenter(0.5, 0.5);
        gameWorld.getWorldBorder().setSize(37);
        gameWorld.getWorldBorder().setWarningDistance(0);
        gameWorld.getWorldBorder().setWarningTime(0);

        disableWorld("world_nether");
        disableWorld("world_the_end");

        plugin.getLogger().info("ChaosPillars world setup completed!");
        return gameWorld;
    }

    private void disableWorld(String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            Bukkit.unloadWorld(world, false);
            plugin.getLogger().info("World '" + worldName + "' has been disabled.");
        }
    }

    public static class VoidGenerator extends ChunkGenerator {
        @Override
        public ChunkData generateChunkData(World world, Random random, int chunkX, int chunkZ, org.bukkit.generator.ChunkGenerator.BiomeGrid biome) {
            return createChunkData(world);
        }
    }
}
