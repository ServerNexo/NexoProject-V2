package me.nexo.items.mecanicas;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.aurelium.auraskills.api.AuraSkillsApi;
import dev.aurelium.auraskills.api.skill.Skills;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.items.NexoItems;
import me.nexo.items.dtos.ArmorDTO;
import me.nexo.items.managers.FileManager;
import me.nexo.items.managers.ItemManager;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.Random;

/**
 * 🎒 NexoItems - Eventos de Pesca y Criaturas Marinas (Arquitectura Enterprise)
 */
@Singleton
public class FishingListener implements Listener {

    private final NexoItems plugin;
    private final FileManager fileManager;
    private final Random random = new Random();

    // 💉 PILAR 3: Inyección de Dependencias
    @Inject
    public FishingListener(NexoItems plugin) {
        this.plugin = plugin;
        this.fileManager = plugin.getFileManager();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void alPescar(PlayerFishEvent event) {
        Player p = event.getPlayer();

        double probCriaturaTotal = 0.0;

        // 🛡️ Leer probabilidad de las armaduras puestas
        for (ItemStack item : p.getInventory().getArmorContents()) {
            if (item == null || !item.hasItemMeta()) continue;
            var pdc = item.getItemMeta().getPersistentDataContainer();

            if (pdc.has(ItemManager.llaveArmaduraId, PersistentDataType.STRING)) {
                // Usamos el fileManager cacheado (Más rápido)
                ArmorDTO dto = fileManager.getArmorDTO(pdc.get(ItemManager.llaveArmaduraId, PersistentDataType.STRING));
                if (dto != null) {
                    probCriaturaTotal += dto.criaturaMarina();
                }
            }
        }

        // 🎣 Momento exacto de la captura
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {

            // Tirada de dados para ver si sale monstruo
            if (random.nextDouble() * 100 <= probCriaturaTotal) {
                if (event.getCaught() != null) {
                    event.getCaught().remove(); // Borramos el pez normal
                }

                spawnearMonstruoMarino(p);

                // 🌟 FIX: Mensaje Directo (Adiós error de getMessage)
                CrossplayUtils.sendActionBar(p, "&#FF5555[!] ¡Alerta de Seguridad! Criatura marina detectada.");
                p.playSound(p.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.5f, 1.5f);

            } else {
                // 🌟 FIX: Mensaje Directo
                CrossplayUtils.sendActionBar(p, "&#55FF55[✓] Extracción exitosa.");
                p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 2.0f);
            }
        }
    }

    private void spawnearMonstruoMarino(Player p) {
        int nivelPesca = 1;
        try {
            nivelPesca = (int) AuraSkillsApi.get().getUser(p.getUniqueId()).getSkillLevel(Skills.FISHING);
        } catch (Exception ignored) {}

        // 👾 Spawneo según el nivel de AuraSkills
        if (nivelPesca < 10) {
            p.getWorld().spawnEntity(p.getLocation(), EntityType.ZOMBIE)
                    .customName(CrossplayUtils.parseCrossplay(p, "&#00E5FFZombie Ahogado"));

        } else if (nivelPesca < 25) {
            p.getWorld().spawnEntity(p.getLocation(), EntityType.GUARDIAN)
                    .customName(CrossplayUtils.parseCrossplay(p, "&#00E5FFGuardián del Arrecife"));

        } else {
            p.getWorld().spawnEntity(p.getLocation(), EntityType.ELDER_GUARDIAN)
                    .customName(CrossplayUtils.parseCrossplay(p, "&#FF5555Leviatán Anciano"));
        }
    }
}