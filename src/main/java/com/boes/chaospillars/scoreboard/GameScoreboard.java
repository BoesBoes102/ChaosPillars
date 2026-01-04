package com.boes.chaospillars.scoreboard;

import com.boes.chaospillars.ChaosPillars;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.Set;
import java.util.UUID;

public record GameScoreboard(ChaosPillars plugin) {

    public void updateGameScoreboard(int timeRemaining, int powerupCooldown, int eventCooldown, Set<UUID> activePlayers) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
            Objective objective = board.registerNewObjective(
                    "chaos", "dummy", ScoreboardTranslator.translate("&6Chaos Pillars")
            );
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);

            String[] lines = {
                    "&7─────────────── ",
                    "&cTime: &f" + timeRemaining + "s",
                    "&aAlive: &f" + activePlayers.size(),
                    "&bPowerup: &f" + powerupCooldown + "s",
                    "&dEvent: &f" + eventCooldown + "s",
                    "&7───────────────"
            };

            int score = lines.length;
            for (String line : lines) {
                objective.getScore(ScoreboardTranslator.translate(line)).setScore(score--);
            }

            player.setScoreboard(board);
        }
    }
}
