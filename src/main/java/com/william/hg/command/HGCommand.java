package com.william.hg.command;

import com.william.hg.game.GameManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class HGCommand implements CommandExecutor, TabCompleter {

    private final GameManager gameManager;

    public HGCommand(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("hg.admin")) {
            sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(Component.text("Usage: /hg <reset>", NamedTextColor.YELLOW));
            return true;
        }

        if (args[0].equalsIgnoreCase("reset")) {
            gameManager.reset();
            sender.sendMessage(Component.text("Game reset.", NamedTextColor.GREEN));
        } else if (args[0].equalsIgnoreCase("mapreset")) {
            sender.sendMessage(Component.text("Regenerating world... Server will restart.", NamedTextColor.YELLOW));
            Bukkit.getScheduler().runTaskLater(gameManager.plugin(), () -> {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "save-all");
                Bukkit.getScheduler().runTaskLater(gameManager.plugin(), () -> {
                    // Delete region files to force regeneration
                    var worldFolder = Bukkit.getWorlds().getFirst().getWorldFolder();
                    deleteFolder(new java.io.File(worldFolder, "region"));
                    deleteFolder(new java.io.File(worldFolder, "entities"));
                    deleteFolder(new java.io.File(worldFolder, "poi"));
                    deleteFolder(new java.io.File(worldFolder, "playerdata"));
                    deleteFolder(new java.io.File(worldFolder, "advancements"));
                    deleteFolder(new java.io.File(worldFolder, "stats"));
                    var levelDat = new java.io.File(worldFolder, "level.dat");
                    var levelDatOld = new java.io.File(worldFolder, "level.dat_old");
                    levelDat.delete();
                    levelDatOld.delete();
                    Bukkit.getServer().shutdown();
                }, 20L);
            }, 20L);
        } else if (args[0].equalsIgnoreCase("pit")) {
            if (gameManager.forceDeathmatch()) {
                sender.sendMessage(Component.text("Deathmatch starting in 10 seconds.", NamedTextColor.GREEN));
            } else {
                sender.sendMessage(Component.text("Cannot start deathmatch — game must be in ACTIVE phase.", NamedTextColor.RED));
            }
        } else if (args[0].equalsIgnoreCase("start")) {
            if (gameManager.forceStart()) {
                sender.sendMessage(Component.text("Force starting in 10 seconds.", NamedTextColor.GREEN));
            } else {
                sender.sendMessage(Component.text("Cannot start — game is already running.", NamedTextColor.RED));
            }
        } else {
            sender.sendMessage(Component.text("Unknown subcommand: " + args[0], NamedTextColor.RED));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd,
                                      @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) return List.of("reset", "start", "pit", "mapreset");
        return List.of();
    }

    private void deleteFolder(java.io.File folder) {
        if (!folder.exists()) return;
        var files = folder.listFiles();
        if (files != null) {
            for (var file : files) {
                if (file.isDirectory()) {
                    deleteFolder(file);
                } else {
                    file.delete();
                }
            }
        }
        folder.delete();
    }
}
