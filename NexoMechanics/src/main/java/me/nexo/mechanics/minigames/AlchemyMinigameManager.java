package me.nexo.mechanics.minigames;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.mechanics.NexoMechanics;
import me.nexo.mechanics.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.BrewingStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ⚗️ NexoMechanics - Minijuego de Alquimia (Arquitectura Enterprise)
 * Rendimiento: Folia-Ready RegionScheduler, Modificación Segura de Bloques y Cero Estáticos.
 */
@Singleton
public class AlchemyMinigameManager implements Listener {

    private final NexoMechanics plugin;
    private final ConfigManager configManager;
    private final CrossplayUtils crossplayUtils; // 🌟 Sinergia inyectada

    private final Map<Location, MezclaVolatil> mezclas = new ConcurrentHashMap<>();

    private static class MezclaVolatil {
        int bombeos = 0;
        int tiempoRestante = 5;
        final ItemStack[] pocionesOriginales;

        public MezclaVolatil(ItemStack[] originales) {
            this.pocionesOriginales = originales;
        }
    }

    // 💉 PILAR 1: Inyección de Dependencias Directa
    @Inject
    public AlchemyMinigameManager(NexoMechanics plugin, ConfigManager configManager, CrossplayUtils crossplayUtils) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.crossplayUtils = crossplayUtils;
    }

    @EventHandler
    public void alDestilar(BrewEvent event) {
        var b = event.getBlock();

        // 10% de probabilidad de que ocurra el minijuego
        if (Math.random() <= 0.10) {
            if (!(b.getState() instanceof BrewingStand stand)) return;

            var resultados = new ItemStack[3];
            for (int i = 0; i < 3; i++) {
                var item = stand.getInventory().getItem(i);
                if (item != null && !item.isEmpty()) resultados[i] = item.clone();
            }

            mezclas.put(b.getLocation(), new MezclaVolatil(resultados));

            b.getWorld().playSound(b.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1f, 0.5f);

            for (Player p : b.getWorld().getPlayers()) {
                if (p.getLocation().distance(b.getLocation()) <= 5) {
                    crossplayUtils.sendTitle(p,
                            configManager.getMessages().mensajes().minijuegos().alquimia().peligroTitulo(),
                            configManager.getMessages().mensajes().minijuegos().alquimia().peligroSubtitulo()
                    );
                }
            }

            // 🌟 FOLIA NATIVE: Iniciamos el temporizador atado exclusivamente a la región de este bloque
            Bukkit.getRegionScheduler().runAtFixedRate(plugin, b.getLocation(), task -> {
                var mezcla = mezclas.get(b.getLocation());
                
                // Si la mezcla ya no está en el mapa (se completó o rompió), cancelamos la tarea
                if (mezcla == null) {
                    task.cancel();
                    return;
                }

                b.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, b.getLocation().clone().add(0.5, 1, 0.5), 2, 0.1, 0.1, 0.1, 0.01);

                mezcla.tiempoRestante--;
                if (mezcla.tiempoRestante <= 0) {
                    aplicarCastigo(b.getLocation());
                    mezclas.remove(b.getLocation());
                    task.cancel();
                }
            }, 20L, 20L); // 1 segundo de delay inicial, 1 segundo de repetición
        }
    }

    @EventHandler
    public void alAgacharse(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) return;

        var p = event.getPlayer();
        var pLoc = p.getLocation();

        for (var entry : mezclas.entrySet()) {
            var locSoporte = entry.getKey();

            // Verifica que estén en el mismo mundo y cerca
            if (locSoporte.getWorld().equals(pLoc.getWorld()) && locSoporte.distance(pLoc) <= 3) {

                var mezcla = entry.getValue();
                mezcla.bombeos++;

                p.playSound(locSoporte, Sound.ITEM_BUCKET_FILL, 0.5f, 1f + (mezcla.bombeos * 0.2f));
                locSoporte.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, locSoporte.clone().add(0.5, 1, 0.5), 5);

                // Si se completa el minijuego
                if (mezcla.bombeos >= 3) {
                    p.playSound(locSoporte, Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);

                    crossplayUtils.sendMessage(p, configManager.getMessages().mensajes().minijuegos().alquimia().mezclaEstabilizada());
                    
                    // Eliminamos la mezcla de inmediato para que el RegionScheduler cancele el castigo
                    mezclas.remove(locSoporte);

                    // 🌟 FOLIA NATIVE: Agendamos la modificación del inventario en el hilo de su propio bloque
                    Bukkit.getRegionScheduler().run(plugin, locSoporte, task -> aplicarPremio(locSoporte, mezcla.pocionesOriginales));
                }
                break; // Solo puede bombear una mesa a la vez
            }
        }
    }

    private void aplicarPremio(Location loc, ItemStack[] originales) {
        if (loc.getBlock().getState() instanceof BrewingStand stand) {
            for (int i = 0; i < 3; i++) {
                var item = originales[i];
                // 🌟 PAPER 1.21 FIX: isEmpty() nativo
                if (item != null && !item.isEmpty() && item.getType() == Material.POTION && item.hasItemMeta()) {
                    var meta = (PotionMeta) item.getItemMeta();
                    if (meta.hasCustomEffects()) {
                        for (PotionEffect effect : meta.getCustomEffects()) {
                            meta.removeCustomEffect(effect.getType());
                            // Multiplica la duración por 2 como premio
                            meta.addCustomEffect(new PotionEffect(effect.getType(), effect.getDuration() * 2, effect.getAmplifier()), true);
                        }
                    }
                    item.setItemMeta(meta);
                    stand.getInventory().setItem(i, item);
                }
            }
        }
    }

    private void aplicarCastigo(Location loc) {
        if (loc.getBlock().getState() instanceof BrewingStand stand) {
            for (int i = 0; i < 3; i++) {
                var item = stand.getInventory().getItem(i);
                if (item != null && !item.isEmpty()) {
                    // Se arruina la poción
                    stand.getInventory().setItem(i, new ItemStack(Material.GLASS_BOTTLE));
                }
            }
            loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.5f);
            loc.getWorld().spawnParticle(Particle.SMOKE, loc.clone().add(0.5, 0.5, 0.5), 20, 0.2, 0.2, 0.2, 0.05);
        }
    }
}