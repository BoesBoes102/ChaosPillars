package com.boes.chaospillars;

public class PlayerStats {
    private int wins;
    private int gamesPlayed;
    private int kills;
    private int deaths;

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
}
