package com.william.hg.arena;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ArenaManager {

    private final Plugin plugin;
    private final File file;
    private final Map<String, Arena> arenas = new HashMap<>();

    public ArenaManager(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "arenas.yml");
        load();
    }

    public Arena get(String name) {
        return arenas.get(name.toLowerCase());
    }

    public Arena getDefault() {
        return arenas.values().stream().findFirst().orElse(null);
    }

    public List<String> arenaNames() {
        return List.copyOf(arenas.keySet());
    }

    public void createArena(String name, Location center, double borderRadius) {
        arenas.put(name.toLowerCase(), new Arena(name.toLowerCase(), center, borderRadius, new ArrayList<>()));
        save();
    }

    public boolean addSpawn(String name, Location location) {
        Arena arena = get(name);
        if (arena == null) return false;
        arena.spawnPoints().add(location);
        save();
        return true;
    }

    public void applyBorder(Arena arena) {
        World world = arena.center().getWorld();
        if (world == null) return;
        WorldBorder border = world.getWorldBorder();
        border.setCenter(arena.center());
        border.setSize(arena.borderRadius() * 2);
        border.setDamageAmount(2.0);
        border.setDamageBuffer(0.0);
        border.setWarningDistance(10);
    }

    private void load() {
        if (!file.exists()) return;

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yaml.getConfigurationSection("arenas");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            ConfigurationSection arenaSection = section.getConfigurationSection(key);
            if (arenaSection == null) continue;

            String worldName = arenaSection.getString("world", "world");
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                plugin.getLogger().warning("Arena '" + key + "' references unknown world: " + worldName);
                continue;
            }

            Location center = new Location(world,
                    arenaSection.getDouble("center.x"),
                    arenaSection.getDouble("center.y"),
                    arenaSection.getDouble("center.z"));

            double radius = arenaSection.getDouble("border-radius", 500);

            List<Location> spawns = new ArrayList<>();
            List<Map<?, ?>> spawnList = arenaSection.getMapList("spawns");
            for (Map<?, ?> map : spawnList) {
                spawns.add(new Location(world,
                        ((Number) map.get("x")).doubleValue(),
                        ((Number) map.get("y")).doubleValue(),
                        ((Number) map.get("z")).doubleValue(),
                        map.containsKey("yaw") ? ((Number) map.get("yaw")).floatValue() : 0f,
                        0f));
            }

            arenas.put(key, new Arena(key, center, radius, spawns));
            plugin.getLogger().info("Loaded arena '" + key + "' with " + spawns.size() + " spawns.");
        }
    }

    private void save() {
        YamlConfiguration yaml = new YamlConfiguration();

        for (Arena arena : arenas.values()) {
            String path = "arenas." + arena.name();
            Location c = arena.center();
            yaml.set(path + ".world", c.getWorld() != null ? c.getWorld().getName() : "world");
            yaml.set(path + ".center.x", c.getX());
            yaml.set(path + ".center.y", c.getY());
            yaml.set(path + ".center.z", c.getZ());
            yaml.set(path + ".border-radius", arena.borderRadius());

            List<Map<String, Object>> spawnList = new ArrayList<>();
            for (Location s : arena.spawnPoints()) {
                Map<String, Object> map = new HashMap<>();
                map.put("x", s.getX());
                map.put("y", s.getY());
                map.put("z", s.getZ());
                map.put("yaw", s.getYaw());
                spawnList.add(map);
            }
            yaml.set(path + ".spawns", spawnList);
        }

        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save arenas: " + e.getMessage());
        }
    }
}
