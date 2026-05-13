package me.nexo.mechanics.gathering.progression;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.mechanics.NexoMechanics;
import me.nexo.mechanics.gathering.data.GatheringProfile;
import me.nexo.mechanics.gathering.data.GatheringRepository; // 🌟 IMPORTAMOS EL REPOSITORIO
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 💾 Gestor de Sesiones y Memoria de NexoGathering.
 */
@Singleton
public class GatheringProfileManager implements Listener {

    private final NexoMechanics plugin;
    private final GatheringRepository repository; // 🌟 AÑADIDO
    private final Map<UUID, GatheringProfile> activeProfiles = new ConcurrentHashMap<>();

    @Inject
    public GatheringProfileManager(NexoMechanics plugin, GatheringRepository repository) {
        this.plugin = plugin;
        this.repository = repository;
        startDecayTicker();
    }

    public GatheringProfile getProfile(UUID uuid) {
        return activeProfiles.get(uuid);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        GatheringProfile profile = new GatheringProfile(uuid);
        activeProfiles.put(uuid, profile);

        // 🌟 PEDIMOS LOS DATOS A NEXO CORE ASÍNCRONAMENTE
        repository.loadProgress(uuid).thenAccept(progressData -> {
            if (!progressData.isEmpty()) {
                profile.setAllProgress(progressData);
                // (Opcional) Puedes poner un log debug aquí
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        GatheringProfile profile = activeProfiles.remove(uuid);

        if (profile != null) {
            profile.hideAllMeters(player); // Prevenimos Memory Leaks visuales

            // 🌟 MANDAMOS A GUARDAR AL DATABASE MANAGER DE NEXO CORE
            repository.saveProgress(uuid, profile.getAllProgress());
        }
    }

    // ==========================================
    // ⏱️ MOTOR DE DECADENCIA ASÍNCRONO
    // ==========================================
    private void startDecayTicker() {
        Bukkit.getAsyncScheduler().runAtFixedRate(plugin, task -> {
            for (Map.Entry<UUID, GatheringProfile> entry : activeProfiles.entrySet()) {
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player != null && player.isOnline()) {
                    entry.getValue().tickAllMeters(player);
                }
            }
        }, 1, 1, TimeUnit.SECONDS);
    }
}