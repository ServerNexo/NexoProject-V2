package me.nexo.items.artefactos;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.items.NexoItems;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

/**
 * 🎒 NexoItems - Interceptor de Artefactos (Arquitectura Enterprise Java 21)
 * Rendimiento: Cero Estáticos, O(1) PDC Reads y Folia-Ready.
 */
@Singleton
public class ArtefactoListener implements Listener {

    private final ArtefactoManager manager;
    private final CrossplayUtils crossplayUtils;

    // 🌟 LLAVE CONSTANTE: Inmutable y aislada de la instancia del plugin para máxima seguridad
    private static final NamespacedKey ARTEFACTO_ID_KEY = new NamespacedKey("nexoitems", "nexo_artefacto_id");

    // 💉 PILAR 1: Inyección de Dependencias Directa
    @Inject
    public ArtefactoListener(ArtefactoManager manager, CrossplayUtils crossplayUtils) {
        this.manager = manager;
        this.crossplayUtils = crossplayUtils;
    }

    // ==========================================
    // 🖱️ INTERCEPCIÓN DEL CLIC DERECHO
    // ==========================================
    @EventHandler(priority = EventPriority.HIGH)
    public void alUsarArtefacto(PlayerInteractEvent event) {
        // 🌟 MODERNIZACIÓN PAPER: Método isRightClick() es más rápido que Action comparisons
        if (!event.getAction().isRightClick()) return;

        var p = event.getPlayer();
        var item = p.getInventory().getItemInMainHand();

        // 🌟 GHOST-ITEM PROOF
        if (item.isEmpty() || !item.hasItemMeta()) return;

        var pdc = item.getItemMeta().getPersistentDataContainer();
        if (pdc.has(ARTEFACTO_ID_KEY, PersistentDataType.STRING)) {
            event.setCancelled(true); // Bloqueo de interacciones Vanilla

            String id = pdc.get(ARTEFACTO_ID_KEY, PersistentDataType.STRING);
            
            // Simulación de DTO (Inmutabilidad por record asumida)
            var dto = simularObtencionDeYML(id);

            if (dto != null) {
                manager.procesarUso(p, dto);
            }
        }
    }

    // ==========================================
    // 🛡️ LÓGICA DE LA CAPA ESPECTRAL (PATTERN MATCHING JAVA 21)
    // ==========================================

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void alRecibirDano(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player p && manager.invulnerables.contains(p.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void alHacerDano(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player p && manager.invulnerables.contains(p.getUniqueId())) {
            event.setCancelled(true);
            crossplayUtils.sendMessage(p, "&#FF5555[!] Sistemas ofensivos bloqueados mientras la Capa Espectral esté activa.");
        }
    }

    // Seguridad: Si se desconecta, liberación de recursos inmediata
    @EventHandler
    public void alSalir(PlayerQuitEvent event) {
        var p = event.getPlayer();
        var uuid = p.getUniqueId();
        
        manager.invulnerables.remove(uuid);

        if (manager.alasActivas.remove(uuid)) {
            p.setAllowFlight(false);
            p.setFlying(false);
        }
    }

    // ==========================================
    // 💡 SIMULADOR DE YML (Arquitectura de Transición)
    // ==========================================
    private ArtefactoDTO simularObtencionDeYML(String id) {
        if (id == null) return null;
        return switch (id.toLowerCase()) {
            case "gancho_cobre" -> new ArtefactoDTO(id, "Gancho de Cobre", ArtefactoDTO.Rareza.COMUN, 10, 3, ArtefactoDTO.HabilidadType.ACTIVA, 0);
            case "totem_crecimiento" -> new ArtefactoDTO(id, "Tótem de Crecimiento", ArtefactoDTO.Rareza.RARO, 15, 5, ArtefactoDTO.HabilidadType.ACTIVA, 0);
            case "iman_chatarra" -> new ArtefactoDTO(id, "Imán de Chatarra", ArtefactoDTO.Rareza.RARO, 10, 5, ArtefactoDTO.HabilidadType.ACTIVA, 0);
            case "hoja_vacio" -> new ArtefactoDTO(id, "Hoja del Vacío", ArtefactoDTO.Rareza.EPICO, 40, 3, ArtefactoDTO.HabilidadType.ACTIVA, 0);
            case "vara_florbifida" -> new ArtefactoDTO(id, "Vara Florbífida", ArtefactoDTO.Rareza.EPICO, 35, 8, ArtefactoDTO.HabilidadType.ACTIVA, 0);
            case "cetro_glacial" -> new ArtefactoDTO(id, "Cetro Glacial", ArtefactoDTO.Rareza.EPICO, 30, 10, ArtefactoDTO.HabilidadType.ACTIVA, 0);
            case "pico_enano" -> new ArtefactoDTO(id, "Pico del Enano Rey", ArtefactoDTO.Rareza.LEGENDARIO, 50, 15, ArtefactoDTO.HabilidadType.ACTIVA, 0);
            case "orbe_sobrecarga" -> new ArtefactoDTO(id, "Orbe de Sobrecarga", ArtefactoDTO.Rareza.LEGENDARIO, 30, 60, ArtefactoDTO.HabilidadType.DESPLIEGUE, 0);
            case "capa_espectral" -> new ArtefactoDTO(id, "Capa Espectral", ArtefactoDTO.Rareza.LEGENDARIO, 50, 30, ArtefactoDTO.HabilidadType.ACTIVA, 0);
            case "reloj_astral" -> new ArtefactoDTO(id, "Reloj de Bolsillo Astral", ArtefactoDTO.Rareza.MITICO, 80, 120, ArtefactoDTO.HabilidadType.ACTIVA, 0);
            case "alas_nexo" -> new ArtefactoDTO(id, "Alas del Nexo", ArtefactoDTO.Rareza.COSMICO, 10, 5, ArtefactoDTO.HabilidadType.TOGGLE, 0);
            default -> null;
        };
    }
}