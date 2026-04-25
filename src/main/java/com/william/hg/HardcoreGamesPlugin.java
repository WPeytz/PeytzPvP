package com.william.hg;

import com.william.hg.arena.ArenaManager;
import com.william.hg.command.ArenaCommand;
import com.william.hg.command.HGCommand;
import com.william.hg.command.KitCommand;
import com.william.hg.kits.KitManager;
import com.william.hg.game.GameConfig;
import com.william.hg.game.GameManager;
import com.william.hg.listener.GameListener;
import com.william.hg.listener.CompassListener;
import com.william.hg.listener.FeastListener;
import com.william.hg.listener.SoupListener;
import org.bukkit.plugin.java.JavaPlugin;

public final class HardcoreGamesPlugin extends JavaPlugin {

    private GameManager gameManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        var config = new GameConfig(getConfig());
        var arenaManager = new ArenaManager(this);
        var kitManager = new KitManager();
        gameManager = new GameManager(this, config, arenaManager, kitManager);

        getServer().getPluginManager().registerEvents(new GameListener(gameManager), this);
        getServer().getPluginManager().registerEvents(new CompassListener(gameManager), this);
        getServer().getPluginManager().registerEvents(new FeastListener(gameManager.feastManager()), this);
        getServer().getPluginManager().registerEvents(new SoupListener(), this);

        var arenaCmd = new ArenaCommand(arenaManager);
        getCommand("arena").setExecutor(arenaCmd);
        getCommand("arena").setTabCompleter(arenaCmd);

        var hgCmd = new HGCommand(gameManager);
        getCommand("hg").setExecutor(hgCmd);
        getCommand("hg").setTabCompleter(hgCmd);

        var kitCmd = new KitCommand(gameManager, kitManager);
        getCommand("kit").setExecutor(kitCmd);
        getCommand("kit").setTabCompleter(kitCmd);

        getLogger().info("HardcoreGames enabled.");
    }

    @Override
    public void onDisable() {
        if (gameManager != null) {
            gameManager.reset();
        }
        getLogger().info("HardcoreGames disabled.");
    }

    public GameManager gameManager() {
        return gameManager;
    }
}
