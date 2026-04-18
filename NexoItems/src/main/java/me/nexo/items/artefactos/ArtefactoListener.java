package me.nexo.items.artefactos;

import me.nexo.core.utils.NexoColor;
import me.nexo.items.NexoItems;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class ArtefactoListener implements Listener {

    private final NexoItems plugin;
    private final ArtefactoManager manager;
    public static NamespacedKey llaveArtefactoId;

    public ArtefactoListener(NexoItems plugin, ArtefactoManager manager) {
        this.plugin = plugin;
        this.manager = manager;
        // Creamos la llave PDC que identificará a los artefactos
        llaveArtefactoId = new NamespacedKey(plugin, "nexo_artefacto_id");
    }

    // ==========================================
    // 🖱️ INTERCEPCIÓN DEL CLIC DERECHO
    // ==========================================
    @EventHandler
    public void alUsarArtefacto(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player p = event.getPlayer();
        ItemStack item = p.getInventory().getItemInMainHand();

        if (item.getType() == org.bukkit.Material.AIR || !item.hasItemMeta()) return;

        var pdc = item.getItemMeta().getPersistentDataContainer();
        if (pdc.has(llaveArtefactoId, PersistentDataType.STRING)) {
            event.setCancelled(true); // Evita interacciones Vanilla indeseadas

            String id = pdc.get(llaveArtefactoId, PersistentDataType.STRING);

            // Obtenemos el DTO simulado
            ArtefactoDTO dto = simularObtencionDeYML(id);

            if (dto != null) {
                manager.procesarUso(p, dto);
            }
        }
    }

    // ==========================================
    // 🛡️ LÓGICA DE LA CAPA ESPECTRAL
    // ==========================================

    // Cancela el daño que RECIBE el jugador
    @EventHandler
    public void alRecibirDano(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player p) {
            if (manager.invulnerables.contains(p.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    // Cancela el daño que CAUSA el jugador
    @EventHandler
    public void alHacerDano(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player p) {
            if (manager.invulnerables.contains(p.getUniqueId())) {
                event.setCancelled(true);
                p.sendMessage(NexoColor.parse("&#FF5555[!] Sistemas ofensivos bloqueados mientras la Capa Espectral esté activa."));
            }
        }
    }

    // Seguridad: Si se desconecta, pierde el God Mode y las Alas
    @EventHandler
    public void alSalir(PlayerQuitEvent event) {
        Player p = event.getPlayer();
        manager.invulnerables.remove(p.getUniqueId());

        if (manager.alasActivas.remove(p.getUniqueId())) {
            p.setAllowFlight(false);
            p.setFlying(false);
        }
    }

    // ==========================================
    // 💡 SIMULADOR DE YML (BASE DE DATOS EN CÓDIGO)
    // ==========================================
    private ArtefactoDTO simularObtencionDeYML(String id) {
        return switch (id.toLowerCase()) {
            case "gancho_cobre" -> new ArtefactoDTO(id, "Gancho de Cobre", ArtefactoDTO.Rareza.COMUN, 10, 3, ArtefactoDTO.HabilidadType.ACTIVA, 0);
            case "totem_crecimiento" -> new ArtefactoDTO(id, "Tótem de Crecimiento", ArtefactoDTO.Rareza.RARO, 15, 5, ArtefactoDTO.HabilidadType.ACTIVA, 0);
            case "iman_chatarra" -> new ArtefactoDTO(id, "Imán de Chatarra", ArtefactoDTO.Rareza.RARO, 10, 5, ArtefactoDTO.HabilidadType.ACTIVA, 0);
            case "hoja_vacio" -> new ArtefactoDTO(id, "Hoja del Vacío", ArtefactoDTO.Rareza.EPICO, 40, 3, ArtefactoDTO.HabilidadType.ACTIVA, 0);
            case "vara_florbifida" -> new ArtefactoDTO(id, "Vara Florbífida", ArtefactoDTO.Rareza.EPICO, 35, 8, ArtefactoDTO.HabilidadType.ACTIVA, 0);
            case "cetro_glacial" -> new ArtefactoDTO(id, "Cetro Glacial", ArtefactoDTO.Rareza.EPICO, 30, 10, ArtefactoDTO.HabilidadType.ACTIVA, 0);
            case "pico_enano" -> new ArtefactoDTO(id, "Pico del Enano Rey", ArtefactoDTO.Rareza.LEGENDARIO, 50, 15, ArtefactoDTO.HabilidadType.ACTIVA, 0);
            case "orbe_sobrecarga" -> new ArtefactoDTO(id, "Orbe de Sobrecarga", ArtefactoDTO.Rareza.LEGENDARIO, 30, 60, ArtefactoDTO.HabilidadType.DESPLIEGUE, 0); // 30% de tu Energía Máxima
            case "capa_espectral" -> new ArtefactoDTO(id, "Capa Espectral", ArtefactoDTO.Rareza.LEGENDARIO, 50, 30, ArtefactoDTO.HabilidadType.ACTIVA, 0);
            case "reloj_astral" -> new ArtefactoDTO(id, "Reloj de Bolsillo Astral", ArtefactoDTO.Rareza.MITICO, 80, 120, ArtefactoDTO.HabilidadType.ACTIVA, 0);
            case "alas_nexo" -> new ArtefactoDTO(id, "Alas del Nexo", ArtefactoDTO.Rareza.COSMICO, 10, 5, ArtefactoDTO.HabilidadType.TOGGLE, 0); // 10 Energía/segundo
            default -> null;
        };
    }
}