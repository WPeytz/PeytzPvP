package com.william.hg.listener;

import com.william.hg.game.GameManager;
import com.william.hg.game.GamePhase;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public final class CompassListener implements Listener {

    private final GameManager gameManager;

    public CompassListener(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @EventHandler
    public void onCompassUse(PlayerInteractEvent event) {
        if (!event.getAction().isRightClick()) return;
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.COMPASS) return;

        Player player = event.getPlayer();
        GamePhase phase = gameManager.phase();
        if (phase != GamePhase.GRACE && phase != GamePhase.ACTIVE) return;

        Player nearest = findNearest(player);
        if (nearest == null) {
            player.sendActionBar(Component.text("No players found", NamedTextColor.GRAY));
            return;
        }

        player.setCompassTarget(nearest.getLocation());
        player.sendActionBar(Component.text("Tracking ", NamedTextColor.GREEN)
                .append(Component.text(nearest.getName(), NamedTextColor.WHITE)));
    }

    private Player findNearest(Player player) {
        Player nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (UUID uuid : gameManager.alivePlayers()) {
            if (uuid.equals(player.getUniqueId())) continue;
            Player target = Bukkit.getPlayer(uuid);
            if (target == null || !target.getWorld().equals(player.getWorld())) continue;

            double dist = player.getLocation().distanceSquared(target.getLocation());
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = target;
            }
        }
        return nearest;
    }
}
