package me.nexo.minions.data;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

public class MinionKeys {
    public static NamespacedKey OWNER;
    public static NamespacedKey TYPE;
    public static NamespacedKey TIER;
    public static NamespacedKey NEXT_ACTION;
    public static NamespacedKey STORED_ITEMS;
    public static NamespacedKey[] UPGRADES; // 🌟 NUEVO: Arreglo de 4 llaves para las mejoras

    public static void init(Plugin plugin) {
        OWNER = new NamespacedKey(plugin, "minion_owner");
        TYPE = new NamespacedKey(plugin, "minion_type");
        TIER = new NamespacedKey(plugin, "minion_tier");
        NEXT_ACTION = new NamespacedKey(plugin, "minion_next_action");
        STORED_ITEMS = new NamespacedKey(plugin, "minion_stored_items");

        UPGRADES = new NamespacedKey[4];
        for (int i = 0; i < 4; i++) {
            UPGRADES[i] = new NamespacedKey(plugin, "minion_upgrade_" + i);
        }
    }
}