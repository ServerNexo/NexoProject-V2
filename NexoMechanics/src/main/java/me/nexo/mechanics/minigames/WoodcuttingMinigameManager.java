package me.nexo.mechanics.minigames;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.user.UserManager;
import me.nexo.mechanics.NexoMechanics;
import me.nexo.mechanics.config.ConfigManager;
import me.nexo.protections.managers.ClaimManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ⚙️ NexoMechanics - Minijuego de Tala (Arquitectura Enterprise)
 * Rendimiento: Tags Nativos O(1) de Paper, Cero Strings Allocations y ConcurrentHashMap.
 */
@Singleton
public class WoodcuttingMinigameManager implements Listener {

    private final NexoMechanics plugin;
    private final ConfigManager configManager;
    
    // 🌟 Dependencias propagadas por Guice
    private final UserManager userManager;
    private final CrossplayUtils crossplayUtils;
    private final ClaimManager claimManager;

    private final Map<UUID, NucleoActivo> nucleos = new ConcurrentHashMap<>();

    private record NucleoActivo(Block bloque, long expiracion, Material tipoOriginal) {}

    // 💉 PILAR 1: Inyección de Dependencias Directa
    @Inject
    public WoodcuttingMinigameManager(NexoMechanics plugin, ConfigManager configManager, 
                                      UserManager userManager, CrossplayUtils crossplayUtils,
                                      ClaimManager claimManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.userManager = userManager;
        this.crossplayUtils = crossplayUtils;
        this.claimManager = claimManager;
        
        iniciarLimpiador();
    }

    @EventHandler
    public void alGolpearMadera(BlockDamageEvent event) {
        var p = event.getPlayer();
        var b = event.getBlock();
        var id = p.getUniqueId();

        // 🌟 Verificación Inyectada
        if (claimManager != null) {
            var stone = claimManager.getStoneAt(b.getLocation());
            if (stone != null && !stone.hasPermission(id, me.nexo.protections.core.ClaimAction.BREAK)) {
                event.setCancelled(true);
            }
        }

        if (event.isCancelled()) return;

        if (nucleos.containsKey(id)) {
            var nucleo = nucleos.get(id);
            if (b.getLocation().equals(nucleo.bloque().getLocation())) {
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);
                p.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, b.getLocation().add(0.5, 0.5, 0.5), 20);
                talarArbol(b);
                b.getWorld().dropItemNaturally(b.getLocation(), new ItemStack(Material.HONEYCOMB, 2));

                var user = userManager.getUserOrNull(id);

                if (user != null) {
                    int maxEnergia = 100 + ((user.getNexoNivel() - 1) * 20) + user.getEnergiaExtraAccesorios();
                    int nuevaEnergia = Math.min(user.getEnergiaMineria() + 10, maxEnergia);
                    user.setEnergiaMineria(nuevaEnergia);

                    crossplayUtils.sendActionBar(p, configManager.getMessages().mensajes().minijuegos().tala().nucleoDestruidoEnergia());
                } else {
                    crossplayUtils.sendActionBar(p, configManager.getMessages().mensajes().minijuegos().tala().nucleoDestruido());
                }
                
                p.sendBlockChange(b.getLocation(), Bukkit.createBlockData(Material.AIR));
                nucleos.remove(id);
                return;
            }
        }

        // 🌟 PAPER 1.21 FIX: Tag.LOGS es ultra rápido O(1) comparado con toString().contains()
        if (Tag.LOGS.isTagged(b.getType()) && !nucleos.containsKey(id)) {
            if (Math.random() <= 0.05) {
                activarNucleo(p, b);
            }
        }
    }

    private void activarNucleo(Player p, Block origen) {
        var objetivo = origen.getRelative(BlockFace.UP);
        
        if (!Tag.LOGS.isTagged(objetivo.getType())) {
            objetivo = origen.getRelative(BlockFace.DOWN);
        }
        
        if (Tag.LOGS.isTagged(objetivo.getType())) {
            p.sendBlockChange(objetivo.getLocation(), Bukkit.createBlockData(Material.CRIMSON_STEM));
            p.playSound(objetivo.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);
            p.getWorld().spawnParticle(Particle.WAX_ON, objetivo.getLocation().add(0.5, 0.5, 0.5), 15);

            crossplayUtils.sendActionBar(p, configManager.getMessages().mensajes().minijuegos().tala().anomaliaBotanica());
            nucleos.put(p.getUniqueId(), new NucleoActivo(objetivo, System.currentTimeMillis() + 3000L, objetivo.getType()));
        }
    }

    private void talarArbol(Block inicio) {
        var actual = inicio;
        // 🌟 PAPER 1.21 FIX: Soporte nativo para Tags de hojas y troncos
        while (Tag.LOGS.isTagged(actual.getType()) || Tag.LEAVES.isTagged(actual.getType())) {
            actual.breakNaturally();
            actual = actual.getRelative(BlockFace.UP);
        }
    }

    private void iniciarLimpiador() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long ahora = System.currentTimeMillis();
            
            // 🌟 FIX: removeIf es más limpio y seguro para evitar ConcurrentModificationException
            nucleos.entrySet().removeIf(entry -> {
                if (ahora > entry.getValue().expiracion()) {
                    var p = Bukkit.getPlayer(entry.getKey());
                    if (p != null) {
                        p.sendBlockChange(entry.getValue().bloque().getLocation(), Bukkit.createBlockData(entry.getValue().tipoOriginal()));
                        p.playSound(p.getLocation(), Sound.BLOCK_CANDLE_EXTINGUISH, 0.5f, 1f);
                        crossplayUtils.sendActionBar(p, configManager.getMessages().mensajes().minijuegos().tala().biomasaEndurecida());
                    }
                    return true; // Elimina el núcleo del mapa
                }
                return false;
            });
        }, 10L, 10L);
    }
}