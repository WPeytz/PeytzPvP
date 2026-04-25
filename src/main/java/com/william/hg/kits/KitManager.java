package com.william.hg.kits;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class KitManager {

    private final Map<UUID, Kit> selectedKits = new HashMap<>();

    public void selectKit(Player player, Kit kit) {
        selectedKits.put(player.getUniqueId(), kit);
        player.sendMessage(Component.text("[HG] ", NamedTextColor.DARK_AQUA)
                .append(Component.text("Kit selected: ", NamedTextColor.GREEN))
                .append(kit.formattedName()));
    }

    public Kit getSelectedKit(UUID uuid) {
        return selectedKits.get(uuid);
    }

    public void applyKit(Player player) {
        Kit kit = selectedKits.get(player.getUniqueId());
        if (kit == null) return;

        for (var item : kit.items()) {
            player.getInventory().addItem(item.clone());
        }
    }

    public void clear() {
        selectedKits.clear();
    }
}
