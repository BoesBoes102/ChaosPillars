package com.boes.chaospillars.scoreboard;

import com.boes.chaospillars.ChaosPillars;
import com.boes.chaospillars.enums.GameState;
import com.boes.chaospillars.stats.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import java.util.HashSet;

public class ChaosScoreboardManager {

    private final ChaosPillars plugin;
    private Scoreboard scoreboard;
    private Objective objective;

    public ChaosScoreboardManager(ChaosPillars plugin) {
        this.plugin = plugin;
    }

    public void startScoreboard() {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        scoreboard = manager.getNewScoreboard();

        Objective existing = scoreboard.getObjective("chaos");
        if (existing != null) {
            existing.unregister();
        }

        objective = scoreboard.registerNewObjective("chaos", "dummy", ChatColor.GOLD + "Chaos Pillars");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (plugin.getGameState() == GameState.RUNNING) {
                player.setScoreboard(scoreboard);
            } else {
                updateIdleScoreboard(player);
            }
        }
    }

    public void updateIdleScoreboard(Player player) {
        Scoreboard idleBoard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective idleObjective = idleBoard.registerNewObjective("idle", "dummy", ChatColor.GOLD + "Your Stats");
        idleObjective.setDisplaySlot(DisplaySlot.SIDEBAR);

        PlayerStats stats = plugin.playerStats.getOrDefault(player.getUniqueId(), new PlayerStats());

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

    public void updateGameScoreboard() {
        if (objective == null || scoreboard == null) return;

        int score = 6;


        for (String entry : new HashSet<>(scoreboard.getEntries())) {
            scoreboard.resetScores(entry);
        }

        objective.getScore(ScoreboardTranslator.translate("§6Chaos Pillars")).setScore(score--);
        objective.getScore(ScoreboardTranslator.translate("§7───────────────")).setScore(score--);
        objective.getScore(ScoreboardTranslator.translate("§cTime Left: §f" + plugin.getTimer() + "s")).setScore(score--);
        objective.getScore(ScoreboardTranslator.translate("§aPlayers Alive: §f" + plugin.getActivePlayers().size())).setScore(score--);
        objective.getScore(ScoreboardTranslator.translate("§bPowerup In: §f" + plugin.getPowerupCooldown() + "s")).setScore(score--);
        objective.getScore(ScoreboardTranslator.translate("§dEvent In: §f" + plugin.getEventCooldown() + "s")).setScore(score--);
        objective.getScore(ScoreboardTranslator.translate("§7─────────────── ")).setScore(score--);

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getScoreboard() != scoreboard) {
                player.setScoreboard(scoreboard);
            }
        }
    }

    public void resetScoreboard() {
        if (scoreboard != null) {
            Objective existing = scoreboard.getObjective("chaos");
            if (existing != null) {
                existing.unregister();
            }

            for (Player player : Bukkit.getOnlinePlayers()) {
                player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
            }
        }

        scoreboard = null;
        objective = null;
    }
}
