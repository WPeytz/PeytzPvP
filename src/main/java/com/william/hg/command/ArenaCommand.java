package com.william.hg.command;

import com.william.hg.arena.Arena;
import com.william.hg.arena.ArenaManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class ArenaCommand implements CommandExecutor, TabCompleter {

    private final ArenaManager arenaManager;

    public ArenaCommand(ArenaManager arenaManager) {
        this.arenaManager = arenaManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (!player.hasPermission("hg.admin")) {
            player.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(Component.text("Usage:", NamedTextColor.YELLOW));
            player.sendMessage(Component.text("/arena create <name> [radius]", NamedTextColor.GRAY));
            player.sendMessage(Component.text("/arena addspawn <name>", NamedTextColor.GRAY));
            player.sendMessage(Component.text("/arena info <name>", NamedTextColor.GRAY));
            player.sendMessage(Component.text("/arena list", NamedTextColor.GRAY));
            return true;
        }

        String sub = args[0].toLowerCase();
        String name = args[1];

        switch (sub) {
            case "create" -> {
                double radius = args.length >= 3 ? Double.parseDouble(args[2]) : 500;
                arenaManager.createArena(name, player.getLocation().clone(), radius);
                player.sendMessage(Component.text("Arena '" + name + "' created at your location (border: ±" + (int) radius + " blocks).", NamedTextColor.GREEN));
            }
            case "addspawn" -> {
                if (arenaManager.addSpawn(name, player.getLocation())) {
                    Arena arena = arenaManager.get(name);
                    player.sendMessage(Component.text("Spawn #" + arena.spawnPoints().size() + " added to '" + name + "'.", NamedTextColor.GREEN));
                } else {
                    player.sendMessage(Component.text("Arena '" + name + "' not found.", NamedTextColor.RED));
                }
            }
            case "info" -> {
                Arena arena = arenaManager.get(name);
                if (arena == null) {
                    player.sendMessage(Component.text("Arena '" + name + "' not found.", NamedTextColor.RED));
                } else {
                    player.sendMessage(Component.text("Arena: " + arena.name(), NamedTextColor.AQUA));
                    player.sendMessage(Component.text("  Spawns: " + arena.spawnPoints().size(), NamedTextColor.GRAY));
                    player.sendMessage(Component.text("  Border radius: " + arena.borderRadius(), NamedTextColor.GRAY));
                }
            }
            case "list" -> {
                List<String> names = arenaManager.arenaNames();
                if (names.isEmpty()) {
                    player.sendMessage(Component.text("No arenas configured.", NamedTextColor.GRAY));
                } else {
                    player.sendMessage(Component.text("Arenas: " + String.join(", ", names), NamedTextColor.AQUA));
                }
            }
            default -> player.sendMessage(Component.text("Unknown subcommand: " + sub, NamedTextColor.RED));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd,
                                      @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("create", "addspawn", "info", "list");
        }
        if (args.length == 2 && !args[0].equalsIgnoreCase("create")) {
            return arenaManager.arenaNames();
        }
        return List.of();
    }
}
