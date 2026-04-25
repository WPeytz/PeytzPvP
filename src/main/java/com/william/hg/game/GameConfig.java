package com.william.hg.game;

import org.bukkit.configuration.file.FileConfiguration;

public final class GameConfig {

    private int minPlayers;
    private int maxPlayers;
    private int countdownSeconds;
    private int gracePeriodSeconds;
    private int endingDelaySeconds;
    private int spawnRadius;
    private int feastMinMinutes;
    private int feastMaxMinutes;
    private int deathmatchMinutes;

    public GameConfig(FileConfiguration config) {
        reload(config);
    }

    public void reload(FileConfiguration config) {
        this.minPlayers = config.getInt("game.min-players", 2);
        this.maxPlayers = config.getInt("game.max-players", 24);
        this.countdownSeconds = config.getInt("game.countdown-seconds", 30);
        this.gracePeriodSeconds = config.getInt("game.grace-period-seconds", 120);
        this.endingDelaySeconds = config.getInt("game.ending-delay-seconds", 10);
        this.spawnRadius = config.getInt("game.spawn-radius", 50);
        this.feastMinMinutes = config.getInt("game.feast-min-minutes", 20);
        this.feastMaxMinutes = config.getInt("game.feast-max-minutes", 27);
        this.deathmatchMinutes = config.getInt("game.deathmatch-minutes", 50);
    }

    public int minPlayers() { return minPlayers; }
    public int maxPlayers() { return maxPlayers; }
    public int countdownSeconds() { return countdownSeconds; }
    public int gracePeriodSeconds() { return gracePeriodSeconds; }
    public int endingDelaySeconds() { return endingDelaySeconds; }
    public int spawnRadius() { return spawnRadius; }
    public int feastMinMinutes() { return feastMinMinutes; }
    public int feastMaxMinutes() { return feastMaxMinutes; }
    public int deathmatchMinutes() { return deathmatchMinutes; }
}
