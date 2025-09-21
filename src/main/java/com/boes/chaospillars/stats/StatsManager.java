package com.boes.chaospillars.stats;

import com.boes.chaospillars.ChaosPillars;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
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

    public void saveStats() {
        for (Map.Entry<UUID, PlayerStats> entry : plugin.playerStats.entrySet()) {
            String path = "players." + entry.getKey();
            PlayerStats stats = entry.getValue();

            statsConfig.set(path + ".wins", stats.getWins());
            statsConfig.set(path + ".gamesPlayed", stats.getGamesPlayed());
            statsConfig.set(path + ".kills", stats.getKills());
            statsConfig.set(path + ".deaths", stats.getDeaths());
            statsConfig.set(path + ".winStreak", stats.getWinStreak());
            statsConfig.set(path + ".highestWinStreak", stats.getHighestWinStreak());
            statsConfig.set(path + ".lossStreak", stats.getLossStreak());
            statsConfig.set(path + ".highestLossStreak", stats.getHighestLossStreak());
        }

        try {
            statsConfig.save(statsFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save stats.yml!");
            e.printStackTrace();
        }
    }

    public void loadStats() {
        plugin.playerStats.clear();

        if (!statsConfig.isConfigurationSection("players")) return;

        for (String uuidString : Objects.requireNonNull(statsConfig.getConfigurationSection("players")).getKeys(false)) {
            UUID uuid = UUID.fromString(uuidString);
            PlayerStats stats = new PlayerStats();

            stats.setWins(statsConfig.getInt("players." + uuidString + ".wins", 0));
            stats.setGamesPlayed(statsConfig.getInt("players." + uuidString + ".gamesPlayed", 0));
            stats.setKills(statsConfig.getInt("players." + uuidString + ".kills", 0));
            stats.setDeaths(statsConfig.getInt("players." + uuidString + ".deaths", 0));
            stats.setWinStreak(statsConfig.getInt("players." + uuidString + ".winStreak", 0));
            stats.setHighestWinStreak(statsConfig.getInt("players." + uuidString + ".highestWinStreak", 0));
            stats.setLossStreak(statsConfig.getInt("players." + uuidString + ".lossStreak", 0));
            stats.setHighestLossStreak(statsConfig.getInt("players." + uuidString + ".highestLossStreak", 0));

            plugin.playerStats.put(uuid, stats);
        }
    }
}
