package com.william.hg.kits;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import java.util.List;

public enum Kit {

    ARCHER("Archer", Material.BOW, NamedTextColor.YELLOW,
            "Start with a bow and 10 arrows",
            List.of(
                    new ItemStack(Material.BOW),
                    new ItemStack(Material.ARROW, 10)
            )),

    URGAL("Urgal", Material.POTION, NamedTextColor.DARK_RED,
            "Start with 2 strength potions",
            List.of(
                    strengthPotion(),
                    strengthPotion()
            )),

    SCOUT("Scout", Material.SPLASH_POTION, NamedTextColor.AQUA,
            "Start with 2 extended speed II splash potions",
            List.of(
                    speedPotion(),
                    speedPotion()
            )),

    MINER("Miner", Material.IRON_PICKAXE, NamedTextColor.GRAY,
            "Start with an iron pickaxe",
            List.of(
                    new ItemStack(Material.IRON_PICKAXE)
            ));

    private final String displayName;
    private final Material icon;
    private final NamedTextColor color;
    private final String description;
    private final List<ItemStack> items;

    Kit(String displayName, Material icon, NamedTextColor color,
        String description, List<ItemStack> items) {
        this.displayName = displayName;
        this.icon = icon;
        this.color = color;
        this.description = description;
        this.items = items;
    }

    public String displayName() {
        return displayName;
    }

    public Material icon() {
        return icon;
    }

    public NamedTextColor color() {
        return color;
    }

    public String description() {
        return description;
    }

    public List<ItemStack> items() {
        return items;
    }

    public Component formattedName() {
        return Component.text(displayName, color);
    }

    private static ItemStack speedPotion() {
        ItemStack potion = new ItemStack(Material.SPLASH_POTION);
        potion.editMeta(PotionMeta.class, meta ->
                meta.setBasePotionType(PotionType.LONG_SWIFTNESS));
        return potion;
    }

    private static ItemStack strengthPotion() {
        ItemStack potion = new ItemStack(Material.POTION);
        potion.editMeta(PotionMeta.class, meta ->
                meta.setBasePotionType(PotionType.STRENGTH));
        return potion;
    }
}
