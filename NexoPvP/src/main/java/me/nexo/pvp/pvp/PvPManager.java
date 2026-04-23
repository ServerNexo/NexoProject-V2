package me.nexo.pvp.pvp;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.pvp.NexoPvP;
import me.nexo.pvp.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🏛️ NexoPvP - Gestor de Combate (Arquitectura Enterprise)
 * Optimizaciones: Cero llamadas estáticas, Inyección pura y Thread-Safety funcional.
 */
@Singleton
public class PvPManager {

    private final NexoPvP plugin;
    private final ConfigManager configManager;
    private final CrossplayUtils crossplayUtils; // 🌟 FIX: Dependencia inyectada desde el Core

    public final Set<UUID> pvpActivo = ConcurrentHashMap.newKeySet();
    public final Map<UUID, Long> enCombate = new ConcurrentHashMap<>();

    public final Map<UUID, Integer> puntosHonor = new ConcurrentHashMap<>();
    public final Map<UUID, Integer> rachaAsesinatos = new ConcurrentHashMap<>();

    // 💉 PILAR 1: Inyección de Dependencias
    @Inject
    public PvPManager(NexoPvP plugin, ConfigManager configManager, CrossplayUtils crossplayUtils) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.crossplayUtils = crossplayUtils;
        
        iniciarRelojCombate();
    }

    public boolean tienePvP(Player p) {
        return pvpActivo.contains(p.getUniqueId());
    }

    public void togglePvP(Player p) {
        UUID id = p.getUniqueId();

        if (estaEnCombate(p)) {
            crossplayUtils.sendMessage(p, configManager.getMessages().mensajes().pvp().errorEnCombate());
            return;
        }

        if (pvpActivo.contains(id)) {
            pvpActivo.remove(id);
            crossplayUtils.sendMessage(p, configManager.getMessages().mensajes().pvp().protocoloPaz());
        } else {
            pvpActivo.add(id);
            crossplayUtils.sendMessage(p, configManager.getMessages().mensajes().pvp().protocoloGuerra());
        }
    }

    public void marcarEnCombate(Player p1, Player p2) {
        long expiracion = System.currentTimeMillis() + 15000L; // 15 segundos de combate

        if (!estaEnCombate(p1)) {
            crossplayUtils.sendMessage(p1, configManager.getMessages().mensajes().pvp().alertaCombate());
        }
        if (!estaEnCombate(p2)) {
            crossplayUtils.sendMessage(p2, configManager.getMessages().mensajes().pvp().alertaCombate());
        }

        enCombate.put(p1.getUniqueId(), expiracion);
        enCombate.put(p2.getUniqueId(), expiracion);
    }

    public boolean estaEnCombate(Player p) {
        return enCombate.containsKey(p.getUniqueId());
    }

    private void iniciarRelojCombate() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long ahora = System.currentTimeMillis();
            
            // 🌟 FIX: Iteración atómica y limpieza concurrente sin bloqueos
            enCombate.entrySet().removeIf(entry -> {
                if (ahora > entry.getValue()) {
                    Player p = Bukkit.getPlayer(entry.getKey());
                    if (p != null) {
                        crossplayUtils.sendMessage(p, configManager.getMessages().mensajes().pvp().finCombate());
                    }
                    return true; // Elimina el jugador del mapa automáticamente
                }
                return false;
            });
            
        }, 20L, 20L); // Chequeo asíncrono cada 1 segundo (20 ticks)
    }
}