package me.nexo.items.api.lategame;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * 🛠️ Contrato Arquitectónico para Herramientas de Late-Game.
 * Implementado por clases como AnchorGrappleTool o AxePioletTool.
 */
public interface ILateGameTool {
    
    /**
     * @return El ID interno de la herramienta (Debe coincidir con la config de NexoItems/Oraxen).
     */
    String getToolId();

    /**
     * Verifica si el jugador cumple los requisitos (Nivel, Energía, etc.) para usar la herramienta.
     */
    boolean canUse(Player player, ItemStack item);

    /**
     * Aplica el desgaste personalizado (Ej: Consumir combustible en lugar de durabilidad vanilla).
     */
    void consumeDurability(Player player, ItemStack item, int amount);
    
    /**
     * @return TRUE si esta herramienta es capaz de anclarse a superficies.
     */
    default boolean isGrapplingTool() {
        return false;
    }
}