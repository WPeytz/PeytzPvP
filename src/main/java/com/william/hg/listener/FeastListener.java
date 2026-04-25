package com.william.hg.listener;

import com.william.hg.game.FeastManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public final class FeastListener implements Listener {

    private final FeastManager feastManager;

    public FeastListener(FeastManager feastManager) {
        this.feastManager = feastManager;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (feastManager.isProtected(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }
}
