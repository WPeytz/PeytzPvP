package com.william.hg.listener;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public final class SoupListener implements Listener {

    private static final double HEAL_AMOUNT = 7.0; // 3.5 hearts

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getItem() == null || event.getItem().getType() != Material.MUSHROOM_STEW) return;

        Player player = event.getPlayer();
        double newHealth = Math.min(player.getHealth() + HEAL_AMOUNT, player.getMaxHealth());
        player.setHealth(newHealth);

        player.getInventory().setItem(player.getInventory().getHeldItemSlot(), new ItemStack(Material.BOWL));
    }
}
