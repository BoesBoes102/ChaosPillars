package com.boes.chaospillars.scoreboard;

import com.boes.chaospillars.stats.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.Map;
import java.util.UUID;

public record IdleScoreboard(Map<UUID, PlayerStats> playerStats) {

    public void updateIdleScoreboard(Player player) {
        Scoreboard idleBoard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective idleObjective = idleBoard.registerNewObjective(
                "idle", "dummy", ScoreboardTranslator.translate("&6Your Stats")
        );
        idleObjective.setDisplaySlot(DisplaySlot.SIDEBAR);

        PlayerStats stats = playerStats.getOrDefault(player.getUniqueId(), new PlayerStats());

        String[] lines = {
                "&7─────────────── ",
                "&fPlayer: &e" + player.getName(),
                "&fKills: &f" + stats.getKills(),
                "&fDeaths: &f" + stats.getDeaths(),
                "&fWins: &f" + stats.getWins(),
                "&fGames Played: &f" + stats.getGamesPlayed(),
                "&fWin Streak: &6" + stats.getWinStreak(),
                "&fLoss Streak: &f" + stats.getLossStreak(),
                "&7───────────────"
        };

        int score = lines.length;
        for (String line : lines) {
            idleObjective.getScore(ScoreboardTranslator.translate(line)).setScore(score--);
        }

        player.setScoreboard(idleBoard);
    }
}
