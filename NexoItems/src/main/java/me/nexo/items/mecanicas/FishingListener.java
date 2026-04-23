package me.nexo.items.mecanicas;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.aurelium.auraskills.api.AuraSkillsApi;
import dev.aurelium.auraskills.api.skill.Skills;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.items.managers.FileManager;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.ElderGuardian;
import org.bukkit.entity.Guardian;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 🎒 NexoItems - Eventos de Pesca y Criaturas Marinas (Arquitectura Enterprise Java 21)
 * Rendimiento: Cero Estáticos, Paper Native Spawning (Consumer API) y ThreadLocalRandom.
 */
@Singleton
public class FishingListener implements Listener {

    // 🌟 DEPENDENCIAS PROPAGADAS
    private final FileManager fileManager;
    private final CrossplayUtils crossplayUtils;
    private final AuraSkillsApi auraSkillsApi;

    // 🌟 LLAVE CONSTANTE (Evita llamadas estáticas y acoplamiento)
    private static final NamespacedKey ARMOR_ID_KEY = new NamespacedKey("nexoitems", "armor_id");

    // 💉 PILAR 1: Inyección de Dependencias Directa
    @Inject
    public FishingListener(FileManager fileManager, CrossplayUtils crossplayUtils) {
        this.fileManager = fileManager;
        this.crossplayUtils = crossplayUtils;
        
        // Obtenemos la API externa en el arranque para evitar llamadas estáticas continuas
        this.auraSkillsApi = AuraSkillsApi.get();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void alPescar(PlayerFishEvent event) {
        var p = event.getPlayer();
        double probCriaturaTotal = 0.0;

        // 🛡️ Leer probabilidad de las armaduras puestas
        for (var item : p.getInventory().getArmorContents()) {
            // 🌟 GHOST-ITEM PROOF
            if (item == null || item.isEmpty() || !item.hasItemMeta()) continue;
            var pdc = item.getItemMeta().getPersistentDataContainer();

            if (pdc.has(ARMOR_ID_KEY, PersistentDataType.STRING)) {
                var dto = fileManager.getArmorDTO(pdc.get(ARMOR_ID_KEY, PersistentDataType.STRING));
                if (dto != null) {
                    probCriaturaTotal += dto.criaturaMarina();
                }
            }
        }

        // 🎣 Momento exacto de la captura
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {

            // 🌟 ThreadLocalRandom es mucho más rápido y ligero que instanciar java.util.Random
            if (ThreadLocalRandom.current().nextDouble() * 100 <= probCriaturaTotal) {
                if (event.getCaught() != null) {
                    event.getCaught().remove(); // Borramos el pez normal
                }

                spawnearMonstruoMarino(p);

                // 🌟 FIX: Dependencia inyectada para mensajes
                crossplayUtils.sendActionBar(p, "&#FF5555[!] ¡Alerta de Seguridad! Criatura marina detectada.");
                p.playSound(p.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.5f, 1.5f);

            } else {
                crossplayUtils.sendActionBar(p, "&#55FF55[✓] Extracción exitosa.");
                p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 2.0f);
            }
        }
    }

    private void spawnearMonstruoMarino(Player p) {
        int nivelPesca = 1;
        try {
            // Utilizamos la instancia pre-cargada
            nivelPesca = (int) auraSkillsApi.getUser(p.getUniqueId()).getSkillLevel(Skills.FISHING);
        } catch (Exception ignored) {
            // Ignorado intencionalmente (Fallback a nivel 1 si AuraSkills falla)
        }

        var loc = p.getLocation();
        var world = p.getWorld();

        // 🌟 PAPER NATIVE: Uso de la API de Consumers para Spawning seguro
        // Configura la entidad ANTES de que el servidor la inserte en el mundo
        if (nivelPesca < 10) {
            world.spawn(loc, Zombie.class, zombie -> {
                zombie.customName(crossplayUtils.parseCrossplay(p, "&#00E5FFZombie Ahogado"));
                zombie.setCustomNameVisible(true);
            });

        } else if (nivelPesca < 25) {
            world.spawn(loc, Guardian.class, guardian -> {
                guardian.customName(crossplayUtils.parseCrossplay(p, "&#00E5FFGuardián del Arrecife"));
                guardian.setCustomNameVisible(true);
            });

        } else {
            world.spawn(loc, ElderGuardian.class, elder -> {
                elder.customName(crossplayUtils.parseCrossplay(p, "&#FF5555Leviatán Anciano"));
                elder.setCustomNameVisible(true);
            });
        }
    }
}