package com.boes.chaospillars;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StatsManager {

    private final ChaosPillars plugin;
    private final File statsFile;
    private final FileConfiguration statsConfig;

    public StatsManager(ChaosPillars plugin) {
        this.plugin = plugin;
        this.statsFile = new File(plugin.getDataFolder(), "stats.yml");

        if (!statsFile.exists()) {
            try {
                statsFile.getParentFile().mkdirs();
                statsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create stats.yml!");
                e.printStackTrace();
            }
        }

        this.statsConfig = YamlConfiguration.loadConfiguration(statsFile);
    }

    public void saveStats(Map<UUID, PlayerStats> statsMap) {
        for (Map.Entry<UUID, PlayerStats> entry : statsMap.entrySet()) {
            String path = "players." + entry.getKey();
            PlayerStats stats = entry.getValue();

            statsConfig.set(path + ".wins", stats.getWins());
            statsConfig.set(path + ".gamesPlayed", stats.getGamesPlayed());
            statsConfig.set(path + ".kills", stats.getKills());
            statsConfig.set(path + ".deaths", stats.getDeaths());
        }

        try {
            statsConfig.save(statsFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save stats.yml!");
            e.printStackTrace();
        }
    }

    public Map<UUID, PlayerStats> loadStats() {
        Map<UUID, PlayerStats> loadedStats = new HashMap<>();
        if (!statsConfig.isConfigurationSection("players")) return loadedStats;

        for (String uuidString : statsConfig.getConfigurationSection("players").getKeys(false)) {
            UUID uuid = UUID.fromString(uuidString);
            PlayerStats stats = new PlayerStats();

            stats.setWins(statsConfig.getInt("players." + uuidString + ".wins", 0));
            stats.setGamesPlayed(statsConfig.getInt("players." + uuidString + ".gamesPlayed", 0));
            stats.setKills(statsConfig.getInt("players." + uuidString + ".kills", 0));
            stats.setDeaths(statsConfig.getInt("players." + uuidString + ".deaths", 0));

            loadedStats.put(uuid, stats);
        }

        return loadedStats;
    }
}
