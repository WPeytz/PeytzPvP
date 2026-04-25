package com.william.hg.game;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class FeastManager {

    private static final int PLATFORM_RADIUS = 15;
    private static final int CHEST_COUNT = 12;

    private final Plugin plugin;
    private final int minDelaySeconds;
    private final int maxDelaySeconds;

    private BukkitTask countdownTask;
    private Location feastLocation;
    private int secondsRemaining;
    private int platformY;
    private boolean platformBuilt;
    private boolean spawned;

    public FeastManager(Plugin plugin, int minDelayMinutes, int maxDelayMinutes) {
        this.plugin = plugin;
        this.minDelaySeconds = minDelayMinutes * 60;
        this.maxDelaySeconds = maxDelayMinutes * 60;
    }

    public void schedule(Location arenaCenter) {
        cancel();
        spawned = false;
        platformBuilt = false;

        var random = ThreadLocalRandom.current();
        // Feast spawns within 100 blocks of center
        double angle = random.nextDouble() * 2 * Math.PI;
        double dist = 20 + random.nextDouble() * 80;
        int x = (int) (arenaCenter.getX() + Math.cos(angle) * dist);
        int z = (int) (arenaCenter.getZ() + Math.sin(angle) * dist);
        int y = arenaCenter.getWorld().getHighestBlockYAt(x, z) + 1;
        feastLocation = new Location(arenaCenter.getWorld(), x, y, z);

        int totalDelay = minDelaySeconds + random.nextInt(maxDelaySeconds - minDelaySeconds + 1);
        secondsRemaining = totalDelay;

        countdownTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
        plugin.getLogger().info("Feast scheduled in " + totalDelay + "s at " + x + ", " + z);
    }

    public void cancel() {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
    }

    private void tick() {
        secondsRemaining--;

        // Build platform at 5 min mark, or immediately if total delay < 5 min
        if (!platformBuilt && (secondsRemaining == 300 || secondsRemaining <= 300)) {
            platformBuilt = true;
            buildPlatform();
            int x = feastLocation.getBlockX();
            int z = feastLocation.getBlockZ();
            broadcast(Component.text("The Feast will spawn in " + formatTime(secondsRemaining) + "!", NamedTextColor.GOLD, TextDecoration.BOLD));
            broadcast(Component.text("Coordinates: ", NamedTextColor.YELLOW)
                    .append(Component.text(x + ", " + z, NamedTextColor.WHITE, TextDecoration.BOLD)));
            return;
        }

        if (secondsRemaining == 240) {
            announceTime("4 minutes");
        } else if (secondsRemaining == 180) {
            announceTime("3 minutes");
        } else if (secondsRemaining == 120) {
            announceTime("2 minutes");
        } else if (secondsRemaining == 60) {
            announceTime("1 minute");
        } else if (secondsRemaining == 30) {
            announceTime("30 seconds");
        } else if (secondsRemaining == 10) {
            announceTime("10 seconds");
        } else if (secondsRemaining <= 5 && secondsRemaining > 0) {
            announceTime(secondsRemaining + " second" + (secondsRemaining > 1 ? "s" : ""));
        } else if (secondsRemaining <= 0) {
            spawnFeast();
            cancel();
        }
    }

    private String formatTime(int seconds) {
        if (seconds >= 60) {
            int min = seconds / 60;
            return min + " minute" + (min > 1 ? "s" : "");
        }
        return seconds + " second" + (seconds > 1 ? "s" : "");
    }

    private void announceTime(String time) {
        int x = feastLocation.getBlockX();
        int z = feastLocation.getBlockZ();
        broadcast(Component.text("Feast in " + time + "!", NamedTextColor.GOLD)
                .append(Component.text(" (" + x + ", " + z + ")", NamedTextColor.GRAY)));
        if (secondsRemaining <= 5) {
            playSound(Sound.BLOCK_NOTE_BLOCK_HAT);
        }
    }

    private void buildPlatform() {
        World world = feastLocation.getWorld();
        int cx = feastLocation.getBlockX();
        int cz = feastLocation.getBlockZ();
        int baseY = world.getHighestBlockYAt(cx, cz);
        platformY = baseY;

        for (int dx = -PLATFORM_RADIUS; dx <= PLATFORM_RADIUS; dx++) {
            for (int dz = -PLATFORM_RADIUS; dz <= PLATFORM_RADIUS; dz++) {
                if (dx * dx + dz * dz <= PLATFORM_RADIUS * PLATFORM_RADIUS) {
                    int x = cx + dx;
                    int z = cz + dz;
                    // Place grass at the platform level, clear everything above
                    world.getBlockAt(x, baseY, z).setType(Material.GRASS_BLOCK);
                    for (int clearY = baseY + 1; clearY <= baseY + 10; clearY++) {
                        world.getBlockAt(x, clearY, z).setType(Material.AIR);
                    }
                }
            }
        }

        plugin.getLogger().info("Feast platform built at " + cx + ", " + baseY + ", " + cz);
    }

    private void spawnFeast() {
        broadcast(Component.text("The Feast has spawned!", NamedTextColor.GOLD, TextDecoration.BOLD));
        playSound(Sound.ENTITY_ENDER_DRAGON_GROWL);
        spawned = true;

        World world = feastLocation.getWorld();
        int cx = feastLocation.getBlockX();
        int cz = feastLocation.getBlockZ();
        int chestY = platformY + 1;

        // Place enchantment table at center
        world.getBlockAt(cx, chestY, cz).setType(Material.ENCHANTING_TABLE);

        int[][] chestOffsets = {
                // Inner diagonals (touching enchant table)
                { 1,  1},
                { 1, -1},
                {-1,  1},
                {-1, -1},

                { 2, 2 },
                { 2, -2},
                {-2, 2 },
                {-2, -2},

                {2,  0},
                {-2, 0},
                {0,  2},
                {0, -2},
        };
        List<Block> chestBlocks = new ArrayList<>();

        for (int[] offset : chestOffsets) {
            Block block = world.getBlockAt(cx + offset[0], chestY, cz + offset[1]);
            block.setType(Material.CHEST);
            chestBlocks.add(block);
        }

        // Fill chests
        for (Block block : chestBlocks) {
            if (block.getState() instanceof Chest chest) {
                fillFeastChest(chest.getBlockInventory());
            }
        }
    }

    private void fillFeastChest(Inventory inv) {
        var random = ThreadLocalRandom.current();
        List<ItemStack> pool = new ArrayList<>();

        // Always include some food
        pool.add(new ItemStack(Material.COOKED_BEEF, 3 + random.nextInt(5)));
        pool.add(new ItemStack(Material.MUSHROOM_STEW));

        // Weapons
        if (random.nextFloat() < 0.5f) pool.add(new ItemStack(Material.DIAMOND_SWORD));
        if (random.nextFloat() < 0.3f) {
            ItemStack sharpSword = new ItemStack(Material.DIAMOND_SWORD);
            sharpSword.addEnchantment(Enchantment.SHARPNESS, 1);
            pool.add(sharpSword);
        }

        // Armor pieces (random)
        if (random.nextFloat() < 0.5f) pool.add(new ItemStack(Material.DIAMOND_HELMET));
        if (random.nextFloat() < 0.5f) pool.add(new ItemStack(Material.DIAMOND_CHESTPLATE));
        if (random.nextFloat() < 0.5f) pool.add(new ItemStack(Material.DIAMOND_LEGGINGS));
        if (random.nextFloat() < 0.5f) pool.add(new ItemStack(Material.DIAMOND_BOOTS));

        // Utility
        if (random.nextFloat() < 0.6f) pool.add(new ItemStack(Material.ENDER_PEARL, 1 + random.nextInt(3)));
        if (random.nextFloat() < 0.4f) pool.add(new ItemStack(Material.COBWEB, 2 + random.nextInt(4)));
        if (random.nextFloat() < 0.3f) pool.add(new ItemStack(Material.TNT, 1 + random.nextInt(3)));
        if (random.nextFloat() < 0.4f) pool.add(new ItemStack(Material.FLINT_AND_STEEL));
        if (random.nextFloat() < 0.3f) pool.add(new ItemStack(Material.LAVA_BUCKET));
        if (random.nextFloat() < 0.3f) pool.add(new ItemStack(Material.WATER_BUCKET));

        // Enchanting
        if (random.nextFloat() < 0.4f) pool.add(new ItemStack(Material.LAPIS_LAZULI, 4 + random.nextInt(12)));

        // Ranged
        if (random.nextFloat() < 0.5f) pool.add(new ItemStack(Material.BOW));
        if (random.nextFloat() < 0.6f) pool.add(new ItemStack(Material.ARROW, 8 + random.nextInt(16)));

        // Potions
        if (random.nextFloat() < 0.4f) pool.add(makePotion(Material.SPLASH_POTION, org.bukkit.potion.PotionType.STRONG_HEALING));
        if (random.nextFloat() < 0.3f) pool.add(makePotion(Material.POTION, org.bukkit.potion.PotionType.STRENGTH));
        if (random.nextFloat() < 0.3f) pool.add(makePotion(Material.POTION, org.bukkit.potion.PotionType.SWIFTNESS));

        // Scatter items into random slots
        Collections.shuffle(pool);
        int maxItems = 4 + random.nextInt(4);
        for (int i = 0; i < Math.min(maxItems, pool.size()); i++) {
            int slot = random.nextInt(27);
            if (inv.getItem(slot) == null) {
                inv.setItem(slot, pool.get(i));
            }
        }
    }

    private ItemStack makePotion(Material type, org.bukkit.potion.PotionType potionType) {
        ItemStack potion = new ItemStack(type);
        potion.editMeta(org.bukkit.inventory.meta.PotionMeta.class, meta ->
                meta.setBasePotionType(potionType));
        return potion;
    }

    public boolean isProtected(Location loc) {
        if (!platformBuilt || spawned || feastLocation == null) return false;
        if (!loc.getWorld().equals(feastLocation.getWorld())) return false;

        int dx = loc.getBlockX() - feastLocation.getBlockX();
        int dz = loc.getBlockZ() - feastLocation.getBlockZ();
        return dx * dx + dz * dz <= PLATFORM_RADIUS * PLATFORM_RADIUS;
    }

    private void broadcast(Component message) {
        Bukkit.getServer().sendMessage(
                Component.text("[HG] ", NamedTextColor.DARK_AQUA).append(message));
    }

    private void playSound(Sound sound) {
        for (var player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), sound, 1f, 1f);
        }
    }
}
