package me.nexo.mechanics.minigames;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.user.UserManager;
import me.nexo.mechanics.config.ConfigManager;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;

/**
 * 🎣 NexoMechanics - Gestor de Sinergias de Pesca (Arquitectura Enterprise)
 * Rendimiento: Stream anyMatch, Dependencias Segregadas y Cero Estáticos.
 */
@Singleton
public class FishingHookManager implements Listener {

    // 🌟 Dependencias segregadas: Solo exigimos lo que realmente usamos
    private final ConfigManager configManager;
    private final UserManager userManager;
    private final CrossplayUtils crossplayUtils;

    // 💉 PILAR 1: Inyección de Dependencias Directa
    @Inject
    public FishingHookManager(ConfigManager configManager, UserManager userManager, CrossplayUtils crossplayUtils) {
        this.configManager = configManager;
        this.userManager = userManager;
        this.crossplayUtils = crossplayUtils;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void alPescar(PlayerFishEvent event) {
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {

            if (event.getCaught() instanceof Item itemCapturado) {
                var pescado = itemCapturado.getItemStack();
                
                // 🌟 PAPER 1.21 FIX: isEmpty() previene procesar stacks fantasmas
                if (pescado.isEmpty() || !pescado.hasItemMeta()) return;

                var pdc = pescado.getItemMeta().getPersistentDataContainer();

                // 🌟 OPTIMIZACIÓN: anyMatch de Java detiene el bucle instantáneamente al hallar la coincidencia (Cortocircuito O(N))
                boolean esPezCustom = pdc.getKeys().stream()
                        .anyMatch(key -> key.getNamespace().equalsIgnoreCase("evenmorefish") || key.getKey().contains("emf"));

                if (esPezCustom) {
                    var p = event.getPlayer();
                    var user = userManager.getUserOrNull(p.getUniqueId());

                    if (user != null) {
                        int energiaAct = user.getEnergiaMineria();
                        int maxEnergia = 100 + ((user.getNexoNivel() - 1) * 20) + user.getEnergiaExtraAccesorios();

                        user.setEnergiaMineria(Math.min(energiaAct + 5, maxEnergia));
                        crossplayUtils.sendMessage(p, configManager.getMessages().mensajes().minijuegos().pesca().extraccionAcuatica());
                    }
                }
            }
        }
    }
}