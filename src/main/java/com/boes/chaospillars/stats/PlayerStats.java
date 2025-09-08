package com.boes.chaospillars.stats;

public class PlayerStats {
    private int wins;
    private int gamesPlayed;
    private int kills;
    private int deaths;
    private int winStreak;
    private int highestWinStreak;
    private int lossStreak = 0;
    private int highestLossStreak = 0;

    public int getWins() {
        return wins;
    }

    public void setWins(int wins) {
        this.wins = wins;
    }

    public int getGamesPlayed() {
        return gamesPlayed;
    }

    public void setGamesPlayed(int gamesPlayed) {
        this.gamesPlayed = gamesPlayed;
    }

    public int getKills() {
        return kills;
    }

    public void setKills(int kills) {
        this.kills = kills;
    }

    public int getDeaths() {
        return deaths;
    }

    public void setDeaths(int deaths) {
        this.deaths = deaths;
    }

    public void addWin() {
        wins++;
    }

    public void addGamePlayed() {
        gamesPlayed++;
    }

    public void addKill() {
        kills++;
    }

    public void addDeath() {
        deaths++;
    }

    public double getWinRate() {
        return gamesPlayed == 0 ? 0 : (wins * 100.0) / gamesPlayed;
    }

    public double getKDR() {
        return deaths == 0 ? kills : (double) kills / deaths;
    }

    public int getWinStreak() {
        return winStreak;
    }

    public void setWinStreak(int winStreak) {
        this.winStreak = winStreak;
    }

    public int getHighestWinStreak() {
        return highestWinStreak;
    }

    public void setHighestWinStreak(int highestWinStreak) {
        this.highestWinStreak = highestWinStreak;
    }

    public int getLossStreak() {
        return lossStreak;
    }

    public void setLossStreak(int lossStreak) {
        this.lossStreak = lossStreak;
    }

    public int getHighestLossStreak() {
        return highestLossStreak;
    }

    public void setHighestLossStreak(int highestLossStreak) {
        this.highestLossStreak = highestLossStreak;
    }

    public void addLoss() {
        lossStreak++;
        if (lossStreak > highestLossStreak) {
            highestLossStreak = lossStreak;
        }
    }

    public void resetLossStreak() {
        lossStreak = 0;
    }
}
