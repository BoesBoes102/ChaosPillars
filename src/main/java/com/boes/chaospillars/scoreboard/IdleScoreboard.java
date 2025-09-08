package com.boes.chaospillars.scoreboard;

import com.boes.chaospillars.stats.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.Map;
import java.util.UUID;

public record IdleScoreboard(Map<UUID, PlayerStats> playerStats) {

    public void updateIdleScoreboard(Player player) {
        Scoreboard idleBoard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective idleObjective = idleBoard.registerNewObjective("idle", "dummy", ChatColor.GOLD + "Your Stats");
        idleObjective.setDisplaySlot(DisplaySlot.SIDEBAR);

        PlayerStats stats = playerStats.getOrDefault(player.getUniqueId(), new PlayerStats());

        int score = 11;
        idleObjective.getScore(ScoreboardTranslator.translate("&6Chaos Pillars")).setScore(score--);
        idleObjective.getScore(ScoreboardTranslator.translate("§7─────────────── ")).setScore(score--);
        idleObjective.getScore(ScoreboardTranslator.translate("§ePlayer: §f" + player.getName())).setScore(score--);
        idleObjective.getScore(ScoreboardTranslator.translate("§7Kills: §f" + stats.getKills())).setScore(score--);
        idleObjective.getScore(ScoreboardTranslator.translate("§7Deaths: §f" + stats.getDeaths())).setScore(score--);
        idleObjective.getScore(ScoreboardTranslator.translate("§7Wins: §f" + stats.getWins())).setScore(score--);
        idleObjective.getScore(ScoreboardTranslator.translate("§7Games Played: §f" + stats.getGamesPlayed())).setScore(score--);
        idleObjective.getScore(ScoreboardTranslator.translate("§7Win Streak: §f" + stats.getWinStreak())).setScore(score--);
        idleObjective.getScore(ScoreboardTranslator.translate("§7Loss Streak: §f" + stats.getLossStreak())).setScore(score--);
        idleObjective.getScore(ScoreboardTranslator.translate("§7───────────────")).setScore(score--);

        player.setScoreboard(idleBoard);
    }
}
