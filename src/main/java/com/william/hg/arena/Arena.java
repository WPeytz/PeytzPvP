package com.william.hg.arena;

import org.bukkit.Location;

import java.util.List;

public record Arena(String name, Location center, double borderRadius, List<Location> spawnPoints) {

    public int maxPlayers() {
        return spawnPoints.size();
    }
}
