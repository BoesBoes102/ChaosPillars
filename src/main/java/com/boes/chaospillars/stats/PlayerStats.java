package com.boes.chaospillars.stats;

public class PlayerStats {
    private int wins;
    private int gamesPlayed;
    private int kills;
    private int roundKills;
    private int deaths;
    private int winStreak = 0;
    private int highestWinStreak = 0;
    private int lossStreak = 0;
    private int highestLossStreak = 0;

    public int getWins() {
        return wins;
    }

    public void setWins(int wins) {
        this.wins = Math.max(0, wins);
    }

    public int getGamesPlayed() {
        return gamesPlayed;
    }

    public void setGamesPlayed(int gamesPlayed) {
        this.gamesPlayed = Math.max(0, gamesPlayed);
    }

    public void addWin() {
        wins++;
        winStreak++;
        if (winStreak > highestWinStreak) {
            highestWinStreak = winStreak;
        }
    }

    public void addGamePlayed() {
        gamesPlayed++;
    }

    public void resetWinStreak() {
        winStreak = 0;
    }

    public int getKills() {
        return kills;
    }

    public void setKills(int kills) {
        this.kills = Math.max(0, kills);
    }

    public void addKill() {
        kills++;
        roundKills++;
    }

    public int getRoundKills() {
        return roundKills;
    }

    public void setRoundKills(int roundKills) {
        this.roundKills = Math.max(0, roundKills);
    }

    public void resetRoundKills() {
        this.roundKills = 0;
    }

    public int getDeaths() {
        return deaths;
    }

    public void setDeaths(int deaths) {
        this.deaths = Math.max(0, deaths);
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

    public int getHighestWinStreak() {
        return highestWinStreak;
    }

    public void setWinStreak(int winStreak) {
        this.winStreak = Math.max(0, winStreak);
    }

    public void setHighestWinStreak(int highestWinStreak) {
        this.highestWinStreak = Math.max(0, highestWinStreak);
    }

    public int getLossStreak() {
        return lossStreak;
    }

    public void setLossStreak(int lossStreak) {
        this.lossStreak = Math.max(0, lossStreak);
    }

    public int getHighestLossStreak() {
        return highestLossStreak;
    }

    public void setHighestLossStreak(int highestLossStreak) {
        this.highestLossStreak = Math.max(0, highestLossStreak);
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
