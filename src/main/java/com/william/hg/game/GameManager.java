package com.william.hg.game;

import com.william.hg.arena.Arena;
import com.william.hg.arena.ArenaManager;
import com.william.hg.kits.KitManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class GameManager {

    private final Plugin plugin;
    private final GameConfig config;
    private final ArenaManager arenaManager;
    private final FeastManager feastManager;
    private final KitManager kitManager;
    private final Set<UUID> alivePlayers = new HashSet<>();

    private static final int PIT_RADIUS = 7;
    private static final int PIT_DEPTH = 5;

    private GamePhase phase = GamePhase.WAITING;
    private BukkitTask timerTask;
    private BukkitTask deathmatchTask;
    private int countdown;
    private Arena activeArena;

    public GameManager(Plugin plugin, GameConfig config, ArenaManager arenaManager, KitManager kitManager) {
        this.plugin = plugin;
        this.config = config;
        this.arenaManager = arenaManager;
        this.kitManager = kitManager;
        this.feastManager = new FeastManager(plugin,
                config.feastMinMinutes(), config.feastMaxMinutes());
    }

    public Plugin plugin() {
        return plugin;
    }

    public KitManager kitManager() {
        return kitManager;
    }

    public GamePhase phase() {
        return phase;
    }

    public FeastManager feastManager() {
        return feastManager;
    }

    public Set<UUID> alivePlayers() {
        return alivePlayers;
    }

    public boolean isAlive(UUID uuid) {
        return alivePlayers.contains(uuid);
    }

    public void handleJoin(Player player) {
        if (phase == GamePhase.WAITING || phase == GamePhase.COUNTDOWN) {
            alivePlayers.add(player.getUniqueId());
            broadcast(Component.text(player.getName() + " joined! ", NamedTextColor.GREEN)
                    .append(Component.text("(" + alivePlayers.size() + "/" + config.minPlayers() + ")", NamedTextColor.GRAY)));

            if (phase == GamePhase.WAITING && alivePlayers.size() >= config.minPlayers()) {
                startCountdown();
            }
        }
    }

    public void handleQuit(UUID uuid) {
        alivePlayers.remove(uuid);

        if ((phase == GamePhase.WAITING || phase == GamePhase.COUNTDOWN)
                && alivePlayers.size() < config.minPlayers()) {
            cancelCountdown();
        }

        if (phase == GamePhase.GRACE || phase == GamePhase.ACTIVE || phase == GamePhase.DEATHMATCH) {
            checkWinCondition();
        }
    }

    public void eliminate(Player player) {
        if (!alivePlayers.remove(player.getUniqueId())) return;

        broadcast(Component.text(player.getName() + " has been eliminated!", NamedTextColor.RED));
        player.getInventory().clear();
        checkWinCondition();
    }

    public boolean forceStart() {
        if (phase == GamePhase.GRACE || phase == GamePhase.ACTIVE || phase == GamePhase.ENDING) {
            return false;
        }
        cancelTimer();
        setPhase(GamePhase.COUNTDOWN);
        countdown = 10;

        timerTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (countdown <= 0) {
                cancelTimer();
                startGracePeriod();
                return;
            }

            if (countdown <= 5 || countdown == 10) {
                broadcast(Component.text("Game starts in " + countdown + "s", NamedTextColor.YELLOW));
                playSound(Sound.BLOCK_NOTE_BLOCK_HAT);
            }

            countdown--;
        }, 0L, 20L);
        return true;
    }

    private void startCountdown() {
        setPhase(GamePhase.COUNTDOWN);
        countdown = config.countdownSeconds();

        timerTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (countdown <= 0) {
                cancelTimer();
                startGracePeriod();
                return;
            }

            if (countdown <= 5 || countdown == 10 || countdown == 20 || countdown == 30) {
                broadcast(Component.text("Game starts in " + countdown + "s", NamedTextColor.YELLOW));
                playSound(Sound.BLOCK_NOTE_BLOCK_HAT);
            }

            countdown--;
        }, 0L, 20L);
    }

    private void cancelCountdown() {
        cancelTimer();
        setPhase(GamePhase.WAITING);
        broadcast(Component.text("Not enough players. Countdown cancelled.", NamedTextColor.RED));
    }

    private void startGracePeriod() {
        setPhase(GamePhase.GRACE);

        activeArena = arenaManager.getDefault();
        if (activeArena != null) {
            arenaManager.applyBorder(activeArena);
            activeArena.center().getWorld().setTime(0);
            activeArena.center().getWorld().setStorm(false);
            activeArena.center().getWorld().setThundering(false);
            scatterPlayers(activeArena);
            feastManager.schedule(activeArena.center());
        }

        showTitle(
                Component.text("GO!", NamedTextColor.GREEN),
                Component.text("Grace period: " + config.gracePeriodSeconds() + "s", NamedTextColor.GRAY)
        );
        playSound(Sound.ENTITY_ENDER_DRAGON_GROWL);

        timerTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            setPhase(GamePhase.ACTIVE);
            showTitle(
                    Component.text("PvP ENABLED", NamedTextColor.RED),
                    Component.text("Fight!", NamedTextColor.GRAY)
            );
            playSound(Sound.ENTITY_WITHER_SPAWN);
            scheduleDeathmatch();
        }, config.gracePeriodSeconds() * 20L);
    }

    private void scheduleDeathmatch() {
        int totalSeconds = config.deathmatchMinutes() * 60;
        int[] warnings = {300, 240, 180, 120, 60, 30, 10, 5, 4, 3, 2, 1};
        int secondsLeft = totalSeconds;

        deathmatchTask = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            private int remaining = totalSeconds;

            @Override
            public void run() {
                remaining--;

                for (int w : warnings) {
                    if (remaining == w) {
                        String time = w >= 60 ? (w / 60) + " minute" + (w / 60 > 1 ? "s" : "") : w + " second" + (w > 1 ? "s" : "");
                        broadcast(Component.text("Deathmatch in " + time + "!", NamedTextColor.LIGHT_PURPLE));
                        if (remaining <= 5) playSound(Sound.BLOCK_NOTE_BLOCK_HAT);
                        break;
                    }
                }

                if (remaining <= 0) {
                    cancelDeathmatchTimer();
                    startDeathmatch();
                }
            }
        }, 20L, 20L);
    }

    public boolean forceDeathmatch() {
        if (phase != GamePhase.ACTIVE) return false;
        cancelDeathmatchTimer();

        final int[] remaining = {10};
        deathmatchTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (remaining[0] <= 0) {
                cancelDeathmatchTimer();
                startDeathmatch();
                return;
            }
            if (remaining[0] <= 5 || remaining[0] == 10) {
                broadcast(Component.text("Deathmatch in " + remaining[0] + "s!", NamedTextColor.LIGHT_PURPLE));
                playSound(Sound.BLOCK_NOTE_BLOCK_HAT);
            }
            remaining[0]--;
        }, 0L, 20L);
        return true;
    }

    private void startDeathmatch() {
        if (phase != GamePhase.ACTIVE) return;
        if (activeArena == null) return;

        setPhase(GamePhase.DEATHMATCH);

        Location pitCenter = buildPit(activeArena.center());

        showTitle(
                Component.text("DEATHMATCH", NamedTextColor.LIGHT_PURPLE),
                Component.text("Fight to the death!", NamedTextColor.GRAY)
        );
        playSound(Sound.ENTITY_WITHER_SPAWN);

        int i = 0;
        int playerCount = (int) alivePlayers.stream().filter(u -> Bukkit.getPlayer(u) != null).count();
        for (UUID uuid : alivePlayers) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) continue;

            double angle = (2 * Math.PI / playerCount) * i;
            double dist = PIT_RADIUS - 2;
            double x = pitCenter.getX() + Math.cos(angle) * dist;
            double z = pitCenter.getZ() + Math.sin(angle) * dist;
            player.teleport(new Location(pitCenter.getWorld(), x + 0.5, pitCenter.getY(), z + 0.5));
            i++;
        }
    }

    private Location buildPit(Location arenaCenter) {
        World world = arenaCenter.getWorld();
        int cx = arenaCenter.getBlockX();
        int cz = arenaCenter.getBlockZ();
        int surfaceY = world.getHighestBlockYAt(cx, cz);
        int floorY = surfaceY - PIT_DEPTH;

        // Dig the pit and build walls
        for (int dx = -PIT_RADIUS; dx <= PIT_RADIUS; dx++) {
            for (int dz = -PIT_RADIUS; dz <= PIT_RADIUS; dz++) {
                boolean isWall = dx * dx + dz * dz >= (PIT_RADIUS - 1) * (PIT_RADIUS - 1);
                boolean isInside = dx * dx + dz * dz < PIT_RADIUS * PIT_RADIUS;

                if (!isInside) continue;

                int x = cx + dx;
                int z = cz + dz;

                // Floor
                world.getBlockAt(x, floorY, z).setType(Material.STONE_BRICKS);

                // Interior and walls
                for (int y = floorY + 1; y <= surfaceY + 3; y++) {
                    if (isWall) {
                        world.getBlockAt(x, y, z).setType(Material.STONE_BRICKS);
                    } else {
                        world.getBlockAt(x, y, z).setType(Material.AIR);
                    }
                }
            }
        }

        return new Location(world, cx + 0.5, floorY + 1, cz + 0.5);
    }

    private void checkWinCondition() {
        if (phase != GamePhase.GRACE && phase != GamePhase.ACTIVE && phase != GamePhase.DEATHMATCH) return;
        if (alivePlayers.size() > 1) return;

        cancelTimer();
        setPhase(GamePhase.ENDING);

        if (alivePlayers.size() == 1) {
            Player winner = Bukkit.getPlayer(alivePlayers.iterator().next());
            String name = winner != null ? winner.getName() : "Unknown";
            showTitle(
                    Component.text(name + " WINS!", NamedTextColor.GOLD),
                    Component.text("Hardcore Games", NamedTextColor.GRAY)
            );
            playSound(Sound.UI_TOAST_CHALLENGE_COMPLETE);
        } else {
            broadcast(Component.text("No winner — all players eliminated.", NamedTextColor.GRAY));
        }

        timerTask = Bukkit.getScheduler().runTaskLater(plugin, this::reset, config.endingDelaySeconds() * 20L);
    }

    public void reset() {
        cancelTimer();
        cancelDeathmatchTimer();
        feastManager.cancel();
        kitManager.clear();
        alivePlayers.clear();
        setPhase(GamePhase.WAITING);
        broadcast(Component.text("Game reset. Waiting for players...", NamedTextColor.AQUA));
    }

    private void scatterPlayers(Arena arena) {
        Location center = arena.center();
        int radius = config.spawnRadius();

        for (UUID uuid : alivePlayers) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) continue;

            Location spawn = findSafeSpawn(center, radius);
            player.teleport(spawn);
            player.setGameMode(GameMode.SURVIVAL);
            player.getInventory().clear();
            player.setHealth(20);
            player.setFoodLevel(20);
            player.setSaturation(5f);
            player.getInventory().addItem(createTracker());
            kitManager.applyKit(player);
        }
    }

    private ItemStack createTracker() {
        ItemStack compass = new ItemStack(Material.COMPASS);
        compass.editMeta(meta -> {
            meta.displayName(Component.text("Player Tracker", NamedTextColor.GREEN));
            meta.lore(List.of(Component.text("Right-click to track nearest player", NamedTextColor.GRAY)));
        });
        return compass;
    }

    private Location findSafeSpawn(Location center, int radius) {
        var random = ThreadLocalRandom.current();
        var world = center.getWorld();

        for (int attempt = 0; attempt < 20; attempt++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double dist = radius * 0.4 + random.nextDouble() * radius * 0.6;
            int x = (int) (center.getX() + Math.cos(angle) * dist);
            int z = (int) (center.getZ() + Math.sin(angle) * dist);
            int y = world.getHighestBlockYAt(x, z) + 1;

            var block = world.getBlockAt(x, y - 1, z);
            if (!block.isLiquid()) {
                return new Location(world, x + 0.5, y, z + 0.5);
            }
        }

        int y = world.getHighestBlockYAt(center.getBlockX(), center.getBlockZ()) + 1;
        return new Location(world, center.getX(), y, center.getZ());
    }

    private void setPhase(GamePhase newPhase) {
        this.phase = newPhase;
        plugin.getLogger().info("Phase -> " + newPhase);
    }

    private void cancelTimer() {
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
    }

    private void cancelDeathmatchTimer() {
        if (deathmatchTask != null) {
            deathmatchTask.cancel();
            deathmatchTask = null;
        }
    }

    private void broadcast(Component message) {
        Bukkit.getServer().sendMessage(Component.text("[HG] ", NamedTextColor.DARK_AQUA).append(message));
    }

    private void showTitle(Component title, Component subtitle) {
        Title t = Title.title(title, subtitle,
                Title.Times.times(Duration.ofMillis(200), Duration.ofSeconds(2), Duration.ofMillis(500)));
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.showTitle(t);
        }
    }

    private void playSound(Sound sound) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), sound, 1f, 1f);
        }
    }
}
