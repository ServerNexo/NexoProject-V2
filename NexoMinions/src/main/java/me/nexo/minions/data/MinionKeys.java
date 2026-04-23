package me.nexo.minions.data;

import org.bukkit.NamespacedKey;

/**
 * 🤖 NexoMinions - Diccionario de Llaves Persistentes (Arquitectura Enterprise)
 * Rendimiento: Constantes Estáticas Inmutables O(1), Zero-Init y Thread-Safe.
 * Nota: Al ser una clase puramente de constantes, no interviene Guice.
 */
public final class MinionKeys {

    // 🌟 El namespace siempre es el nombre de tu plugin en minúsculas
    private static final String NAMESPACE = "nexominions";

    // 🌟 FIX: Convertidas en verdaderas constantes de la JVM (Thread-Safe)
    public static final NamespacedKey OWNER = new NamespacedKey(NAMESPACE, "minion_owner");
    public static final NamespacedKey TYPE = new NamespacedKey(NAMESPACE, "minion_type");
    public static final NamespacedKey TIER = new NamespacedKey(NAMESPACE, "minion_tier");
    public static final NamespacedKey NEXT_ACTION = new NamespacedKey(NAMESPACE, "minion_next_action");
    public static final NamespacedKey STORED_ITEMS = new NamespacedKey(NAMESPACE, "minion_stored_items");
    
    public static final NamespacedKey[] UPGRADES = new NamespacedKey[4];

    // 🌟 Inicialización estática nativa O(1)
    static {
        for (int i = 0; i < 4; i++) {
            UPGRADES[i] = new NamespacedKey(NAMESPACE, "minion_upgrade_" + i);
        }
    }

    // 🛡️ Previene la instanciación de este diccionario
    private MinionKeys() {
        throw new UnsupportedOperationException("Esta es una clase de constantes y no puede ser instanciada.");
    }
}