package me.nexo.items;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.items.config.ConfigManager;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * 🎒 NexoItems - Listener de Crafteos Especiales (Arquitectura Enterprise)
 * Rendimiento: Componentes Kyori Nativos, EditMeta y Dependency Injection estricta.
 */
@Singleton
public class CraftingListener implements Listener {

    // 🌟 DEPENDENCIAS PROPAGADAS
    private final ConfigManager configManager;
    private final CrossplayUtils crossplayUtils;

    // 💉 PILAR 1: Inyección de Dependencias Directa
    @Inject
    public CraftingListener(ConfigManager configManager, CrossplayUtils crossplayUtils) {
        this.configManager = configManager;
        this.crossplayUtils = crossplayUtils;
    }

    /**
     * 🌟 1. PREPARACIÓN: Reconocemos la forma en la mesa y mostramos el resultado visual
     */
    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        var matrix = event.getInventory().getMatrix();

        // Verificamos si la forma coincide con la receta del Minion
        if (esRecetaMinionCobblestone(matrix)) {
            // Creamos un "Placeholder" visual (No es el ítem final)
            var minionFalso = new ItemStack(Material.PLAYER_HEAD);
            
            // 🌟 PAPER NATIVE: editMeta es más rápido y seguro en RAM
            minionFalso.editMeta(meta -> {
                meta.displayName(crossplayUtils.parseCrossplay(null, "&#FFAA00⭐ Minion de Cobblestone (Nv. 1)"));
                meta.lore(List.of(
                        crossplayUtils.parseCrossplay(null, "&#AAAAAA¡Craftea esto para obtener"),
                        crossplayUtils.parseCrossplay(null, "&#AAAAAAun trabajador automático!")
                ));
            });

            // Lo ponemos en la casilla de resultado
            event.getInventory().setResult(minionFalso);
        }
    }

    /**
     * 🌟 2. CRAFTEO: Interceptamos cuando el jugador saca el ítem
     */
    @EventHandler
    public void alCraftear(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player jugador)) return;

        var resultado = event.getRecipe() != null ? event.getRecipe().getResult() : event.getCurrentItem();
        if (resultado == null || resultado.isEmpty() || !resultado.hasItemMeta() || !resultado.getItemMeta().hasDisplayName()) return;

        var displayNameComp = resultado.getItemMeta().displayName();
        if (displayNameComp == null) return;

        // 🌟 FIX KYORI: Serialización de Componentes a texto plano (Adiós ChatColor.stripColor)
        String nombreItem = PlainTextComponentSerializer.plainText().serialize(displayNameComp);

        // 🛡️ Lógica Original Protegida (Colecciones/Permisos)
        if (nombreItem.contains("Diamante Encantado")) {
            if (!jugador.hasPermission("nexo.coleccion.diamante1")) {
                event.setCancelled(true);
                jugador.closeInventory();

                // 🌟 LEEMOS DESDE TU NUEVA ARQUITECTURA ENTERPRISE
                String mensaje = configManager.getMessages().eventos().crafteo().ensamblajeDenegado();
                crossplayUtils.sendMessage(jugador, mensaje);

                jugador.playSound(jugador.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }
        }

        // 🐝 MAGIA: Interceptamos el crafteo del Minion
        if (nombreItem.contains("Minion de Cobblestone (Nv. 1)")) {

            // 🛡️ Prevenimos bugs Vanilla de duplicación
            if (event.isShiftClick()) {
                event.setCancelled(true);
                crossplayUtils.sendMessage(jugador, "&#FF5555⚠️ No puedes usar Shift-Click para fabricar herramientas complejas.");
                return;
            }

            // Cancelamos el crafteo vanilla para que no se lleve el "Placeholder" de la cabeza de jugador
            event.setCancelled(true);

            // 1. Consumimos 1 ítem de cada slot de la mesa de trabajo manualmente
            var matrix = event.getInventory().getMatrix();
            for (int i = 0; i < matrix.length; i++) {
                // 🌟 FIX PAPER 1.21: isEmpty() cubre nulos, aire y stacks de 0
                if (matrix[i] != null && !matrix[i].isEmpty()) {
                    matrix[i].setAmount(matrix[i].getAmount() - 1);
                }
            }
            event.getInventory().setMatrix(matrix); // Actualizamos la mesa visualmente

            // 2. Llamamos al Módulo NexoMinions en secreto usando la consola del servidor
            // (Mantenemos esta llamada sincrónica ya que modifica la data del jugador inmediatamente)
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "minion give " + jugador.getName() + " COBBLESTONE 1");

            // 3. Efectos de éxito
            crossplayUtils.sendMessage(jugador, "&#55FF55¡Has fabricado tu primer Minion!");
            jugador.playSound(jugador.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }
    }

    /**
     * 🔍 LÓGICA DE LA RECETA: Pico de madera en el centro, rodeado de 8 bloques de Cobblestone
     */
    private boolean esRecetaMinionCobblestone(ItemStack[] matrix) {
        if (matrix.length < 9) return false; // Por si están en el inventario pequeño 2x2

        // El centro (Slot 4) debe ser exactamente un Pico de Madera
        if (matrix[4] == null || matrix[4].getType() != Material.WOODEN_PICKAXE) return false;

        // Los bordes deben ser Piedra
        int[] bordes = {0, 1, 2, 3, 5, 6, 7, 8};
        for (int i : bordes) {
            if (matrix[i] == null || matrix[i].getType() != Material.COBBLESTONE) return false;
        }

        return true; // ¡La receta es correcta!
    }
}