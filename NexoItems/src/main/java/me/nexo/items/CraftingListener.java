package me.nexo.items;

import com.google.inject.Inject;
import com.google.inject.Singleton; // 🌟 FIX: Faltaba importar esta línea
import me.nexo.core.crossplay.CrossplayUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

@Singleton // 🌟 Aplicado correctamente
public class CraftingListener implements Listener {

    private final NexoItems plugin;

    @Inject // 🌟 Aplicado correctamente
    public CraftingListener(NexoItems plugin) {
        this.plugin = plugin;
    }

    /**
     * 🌟 1. PREPARACIÓN: Reconocemos la forma en la mesa y mostramos el resultado visual
     */
    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        ItemStack[] matrix = event.getInventory().getMatrix();

        // Verificamos si la forma coincide con la receta del Minion
        if (esRecetaMinionCobblestone(matrix)) {
            // Creamos un "Placeholder" visual (No es el ítem final)
            ItemStack minionFalso = new ItemStack(Material.PLAYER_HEAD);
            ItemMeta meta = minionFalso.getItemMeta();
            meta.setDisplayName(ChatColor.GOLD + "⭐ Minion de Cobblestone (Nv. 1)");
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "¡Craftea esto para obtener",
                    ChatColor.GRAY + "un trabajador automático!"
            ));
            minionFalso.setItemMeta(meta);

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

        ItemStack resultado = event.getRecipe() != null ? event.getRecipe().getResult() : event.getCurrentItem();
        if (resultado == null || !resultado.hasItemMeta() || !resultado.getItemMeta().hasDisplayName()) return;

        String nombreItem = ChatColor.stripColor(resultado.getItemMeta().getDisplayName());
        if (nombreItem == null) return;

        // 🛡️ Lógica Original Protegida (Colecciones/Permisos)
        if (nombreItem.contains("Diamante Encantado")) {
            if (!jugador.hasPermission("nexo.coleccion.diamante1")) {
                event.setCancelled(true);
                jugador.closeInventory();

                // 🌟 LEEMOS DESDE TU NUEVA ARQUITECTURA ENTERPRISE
                String mensaje = plugin.getConfigManager().getMessages().eventos().crafteo().ensamblajeDenegado();
                CrossplayUtils.sendMessage(jugador, mensaje);

                jugador.playSound(jugador.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }
        }

        // 🐝 MAGIA: Interceptamos el crafteo del Minion
        if (nombreItem.contains("Minion de Cobblestone (Nv. 1)")) {

            // 🛡️ Prevenimos bugs Vanilla de duplicación
            if (event.isShiftClick()) {
                event.setCancelled(true);
                jugador.sendMessage(ChatColor.RED + "⚠️ No puedes usar Shift-Click para fabricar herramientas complejas.");
                return;
            }

            // Cancelamos el crafteo vanilla para que no se lleve el "Placeholder" de la cabeza de jugador
            event.setCancelled(true);

            // 1. Consumimos 1 ítem de cada slot de la mesa de trabajo manualmente
            ItemStack[] matrix = event.getInventory().getMatrix();
            for (int i = 0; i < matrix.length; i++) {
                if (matrix[i] != null && !matrix[i].getType().isAir()) {
                    matrix[i].setAmount(matrix[i].getAmount() - 1);
                }
            }
            event.getInventory().setMatrix(matrix); // Actualizamos la mesa visualmente

            // 2. Llamamos al Módulo NexoMinions en secreto usando la consola del servidor
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "minion give " + jugador.getName() + " COBBLESTONE 1");

            // 3. Efectos de éxito
            jugador.sendMessage(ChatColor.GREEN + "¡Has fabricado tu primer Minion!");
            jugador.playSound(jugador.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
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