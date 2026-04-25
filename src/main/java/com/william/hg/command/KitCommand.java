package com.william.hg.command;

import com.william.hg.game.GameManager;
import com.william.hg.game.GamePhase;
import com.william.hg.kits.Kit;
import com.william.hg.kits.KitManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public final class KitCommand implements CommandExecutor, TabCompleter {

    private final GameManager gameManager;
    private final KitManager kitManager;

    public KitCommand(GameManager gameManager, KitManager kitManager) {
        this.gameManager = gameManager;
        this.kitManager = kitManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (args.length == 0) {
            showKitList(player);
            return true;
        }

        GamePhase phase = gameManager.phase();
        if (phase != GamePhase.WAITING && phase != GamePhase.COUNTDOWN) {
            player.sendMessage(Component.text("[HG] ", NamedTextColor.DARK_AQUA)
                    .append(Component.text("You can only select a kit before the game starts.", NamedTextColor.RED)));
            return true;
        }

        String kitName = args[0].toUpperCase();
        Kit kit;
        try {
            kit = Kit.valueOf(kitName);
        } catch (IllegalArgumentException e) {
            player.sendMessage(Component.text("[HG] ", NamedTextColor.DARK_AQUA)
                    .append(Component.text("Unknown kit: " + args[0], NamedTextColor.RED)));
            return true;
        }

        kitManager.selectKit(player, kit);
        return true;
    }

    private void showKitList(Player player) {
        player.sendMessage(Component.text("[HG] ", NamedTextColor.DARK_AQUA)
                .append(Component.text("Available Kits:", NamedTextColor.GREEN)));

        Kit selected = kitManager.getSelectedKit(player.getUniqueId());

        for (Kit kit : Kit.values()) {
            Component line = Component.text(" - ", NamedTextColor.GRAY)
                    .append(kit.formattedName())
                    .append(Component.text(" — " + kit.description(), NamedTextColor.GRAY));

            if (kit == selected) {
                line = line.append(Component.text(" [SELECTED]", NamedTextColor.GREEN));
            }

            player.sendMessage(line);
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            return Arrays.stream(Kit.values())
                    .map(k -> k.name().toLowerCase())
                    .filter(name -> name.startsWith(prefix))
                    .toList();
        }
        return List.of();
    }
}
