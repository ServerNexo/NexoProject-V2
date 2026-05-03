package me.nexo.pvp.classes;

import com.google.inject.Singleton;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * ⚖️ NexoPvP - Motor de Peso de Armaduras
 * Clasifica a los jugadores en Ligero (Mago/Asesino), Medio (DPS) o Pesado (Tanque).
 */
@Singleton
public class ArmorWeightManager {

    public enum ArmorClass {
        LIGHT("§b☁ Ligera (Mago/Asesino)"),
        MEDIUM("§e⚖ Media (Luchador)"),
        HEAVY("§c🛡 Pesada (Tanque)");

        private final String displayName;
        ArmorClass(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }

    /**
     * Calcula la clase actual del jugador basado en lo que lleva puesto.
     */
    public ArmorClass calculatePlayerClass(Player player) {
        int totalWeight = 0;
        
        for (ItemStack item : player.getInventory().getArmorContents()) {
            if (item == null || item.isEmpty()) continue;
            totalWeight += getItemWeight(item);
        }

        // Rangos de Peso:
        // 0 a 10 = LIGERO (Ej: Full Cuero/Malla = 4 a 8 de peso)
        // 11 a 20 = MEDIO (Ej: Full Hierro = 16 de peso)
        // 21+ = PESADO (Ej: Full Diamante/Netherite = 24 a 28 de peso)
        if (totalWeight <= 10) return ArmorClass.LIGHT;
        if (totalWeight <= 20) return ArmorClass.MEDIUM;
        return ArmorClass.HEAVY;
    }

    /**
     * Asigna un peso numérico a cada pieza de armadura.
     * TODO: Leer Data Components de NexoItems si es una armadura Custom.
     */
    private int getItemWeight(ItemStack item) {
        String materialName = item.getType().name();
        
        // Armaduras Pesadas
        if (materialName.contains("NETHERITE") || materialName.contains("DIAMOND")) {
            if (materialName.contains("CHESTPLATE")) return 8;
            if (materialName.contains("LEGGINGS")) return 7;
            if (materialName.contains("BOOTS")) return 6;
            if (materialName.contains("HELMET")) return 5;
        }
        // Armaduras Medias
        if (materialName.contains("IRON") || materialName.contains("GOLDEN")) {
            if (materialName.contains("CHESTPLATE")) return 5;
            if (materialName.contains("LEGGINGS")) return 4;
            if (materialName.contains("BOOTS")) return 4;
            if (materialName.contains("HELMET")) return 3;
        }
        // Armaduras Ligeras (Cuero, Malla, Élitros)
        if (materialName.contains("LEATHER") || materialName.contains("CHAINMAIL")) {
            return 2; // Muy ligeras
        }
        if (materialName.equals("ELYTRA")) return 3;

        return 1; // Fallback
    }
}