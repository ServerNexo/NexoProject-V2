package me.nexo.items.mecanicas;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareGrindstoneEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.inventory.ItemStack;

/**
 * 🎒 NexoItems - Bloqueador de Estaciones Vanilla (Arquitectura Enterprise Java 21)
 * Rendimiento: Cero Dependencias Muertas, Ghost-Item Proof y Escaneo PDC Declarativo.
 */
@Singleton
public class VanillaStationsListener implements Listener {

    // 💉 PILAR 1: Constructor inyectado vacío (Cero dead dependencies)
    @Inject
    public VanillaStationsListener() {
        // Listo para ser instanciado por el ItemsBootstrap
    }

    // 🔨 1. Bloquear Yunques Vanilla
    @EventHandler
    public void onAnvil(PrepareAnvilEvent event) {
        var inv = event.getInventory();
        if (esItemCustom(inv.getItem(0)) || esItemCustom(inv.getItem(1))) {
            event.setResult(null); // Borra el ítem de resultado visualmente
        }
    }

    // 🪨 2. Bloquear Piedras de Afilar (Grindstones)
    @EventHandler
    public void onGrindstone(PrepareGrindstoneEvent event) {
        var inv = event.getInventory();
        if (esItemCustom(inv.getItem(0)) || esItemCustom(inv.getItem(1))) {
            event.setResult(null);
        }
    }

    // ✨ 3. Bloquear Mesas de Encantamiento
    @EventHandler
    public void onEnchant(PrepareItemEnchantEvent event) {
        if (esItemCustom(event.getItem())) {
            event.setCancelled(true); // Cancela el encantamiento
        }
    }

    // 🛡️ 4. Bloquear Mesa de Herrería (Smithing Table 1.20+)
    @EventHandler
    public void onSmithing(PrepareSmithingEvent event) {
        var inv = event.getInventory();
        // En 1.20+, hay 3 slots de entrada (Template, Base, Material)
        for (int i = 0; i < 3; i++) {
            if (esItemCustom(inv.getItem(i))) {
                event.setResult(null); // Si tan solo UNO es de Nexo, bloqueamos la mesa
                return;
            }
        }
    }

    // 🧠 MAGIA SENIOR: Detecta cualquier ítem de tu ecosistema sin importar qué sea
    private boolean esItemCustom(ItemStack item) {
        // 🌟 FIX PAPER 1.21: isEmpty() cubre nulos, aire y stacks de 0
        if (item == null || item.isEmpty() || !item.hasItemMeta()) return false;

        // 🌟 JAVA 21: Búsqueda Stream declarativa ultra rápida
        return item.getItemMeta().getPersistentDataContainer().getKeys().stream()
                .anyMatch(key -> key.getNamespace().toLowerCase().contains("nexo"));
    }
}