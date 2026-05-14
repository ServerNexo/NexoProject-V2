package me.aeroxis.dungeons.nemesis;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.aeroxis.core.crossplay.CrossplayUtils;
import me.aeroxis.core.utils.Base64Util; // 🌟 Importamos la utilidad
import me.aeroxis.dungeons.AeroxisDungeons;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 🏹 AeroxisDungeons - El Cazador de Némesis
 * Intercepta la muerte de un Jefe Némesis, decodifica asíncronamente su Base64
 * y genera una explosión física de ítems (Efecto Piñata).
 */
@Singleton
public class NemesisHuntListener implements Listener {

    private final AeroxisDungeons plugin;
    private final NemesisManager nemesisManager;
    private final CrossplayUtils crossplayUtils;
    private final Base64Util base64Util; // 🌟 NUEVA DEPENDENCIA

    // 🚀 Virtual Threads para decodificación masiva sin lag
    private final ExecutorService asyncExecutor = Executors.newVirtualThreadPerTaskExecutor();

    // 💉 Inyectamos las 4 dependencias
    @Inject
    public NemesisHuntListener(AeroxisDungeons plugin, NemesisManager nemesisManager,
                               CrossplayUtils crossplayUtils, Base64Util base64Util) {
        this.plugin = plugin;
        this.nemesisManager = nemesisManager;
        this.crossplayUtils = crossplayUtils;
        this.base64Util = base64Util;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onNemesisDeath(EntityDeathEvent event) {
        LivingEntity mob = event.getEntity();

        // 1. Filtro O(1): ¿El monstruo asesinado es un Némesis?
        if (!nemesisManager.isNemesis(mob)) return;

        Player killer = mob.getKiller();

        // Extraemos el nombre del Némesis en texto plano (Ej: "Némesis de Faust")
        String nemesisName = "Un Némesis";
        if (mob.customName() != null) {
            nemesisName = PlainTextComponentSerializer.plainText().serialize(mob.customName());
        }

        // 2. Extraer el código Base64 inyectado en el ADN del mob (Main Thread)
        String base64Loot = mob.getPersistentDataContainer().get(nemesisManager.getLootKey(), PersistentDataType.STRING);

        if (base64Loot != null) {
            final String finalNemesisName = nemesisName; // Variable effectively final para el Hilo

            // 3. 🚀 Tarea Pesada: Deserialización Asíncrona
            asyncExecutor.submit(() -> {
                try {
                    // 🌟 FIX: Usamos la instancia inyectada 'base64Util' en lugar de la clase estática
                    ItemStack[] recoveredLoot = base64Util.itemStackArrayFromBase64(base64Loot);

                    // 4. Volvemos al Main Thread para interactuar con el mundo físico de Bukkit
                    Bukkit.getScheduler().runTask(plugin, () -> {

                        // EFECTO PIÑATA: Dropeamos ítem por ítem en el suelo
                        for (ItemStack item : recoveredLoot) {
                            if (item != null && !item.getType().isAir()) {
                                mob.getWorld().dropItemNaturally(mob.getLocation(), item);
                            }
                        }

                        // Sonido Legendario de Recompensa
                        mob.getWorld().playSound(mob.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

                        // 🌟 ANUNCIO GLOBAL (Bounty Claimed)
                        if (killer != null) {
                            crossplayUtils.broadcastMessage("\n&#FFD700<bold>🏆 ¡CONTRATO DE CAZA COMPLETADO! 🏆</bold>");
                            crossplayUtils.broadcastMessage("&#E6CCFFEl jugador &#55FF55" + killer.getName() + " &#E6CCFFha ejecutado a &#FF5555" + finalNemesisName);
                            crossplayUtils.broadcastMessage("&#E6CCFFTodo el botín robado ha sido liberado.\n");
                        }
                    });

                } catch (Exception e) {
                    plugin.getLogger().severe("❌ Error decodificando botín robado del Némesis: " + e.getMessage());
                }
            });
        }
    }
}