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
import org.bukkit.block.Block;
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
 * ⚙️ NexoMechanics - Minijuego de Alquimia (Arquitectura Enterprise)
 */
@Singleton
public class AlchemyMinigameManager implements Listener {

    private final NexoMechanics plugin;
    private final ConfigManager configManager;
    private final Map<Location, MezclaVolatil> mezclas = new ConcurrentHashMap<>();

    private static class MezclaVolatil {
        int bombeos = 0;
        int tiempoRestante = 5;
        ItemStack[] pocionesOriginales;

        public MezclaVolatil(ItemStack[] originales) {
            this.pocionesOriginales = originales;
        }
    }

    @Inject
    public AlchemyMinigameManager(NexoMechanics plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        iniciarReloj();
    }

    @EventHandler
    public void alDestilar(BrewEvent event) {
        Block b = event.getBlock();

        // 10% de probabilidad de que ocurra el minijuego
        if (Math.random() <= 0.10) {
            if (!(b.getState() instanceof BrewingStand stand)) return;

            ItemStack[] resultados = new ItemStack[3];
            for (int i = 0; i < 3; i++) {
                ItemStack item = stand.getInventory().getItem(i);
                if (item != null) resultados[i] = item.clone();
            }

            mezclas.put(b.getLocation(), new MezclaVolatil(resultados));

            b.getWorld().playSound(b.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1f, 0.5f);

            for (Player p : b.getWorld().getPlayers()) {
                if (p.getLocation().distance(b.getLocation()) <= 5) {
                    // 💡 Ruta corregida: mensajes() -> minijuegos() -> alquimia()
                    CrossplayUtils.sendTitle(p,
                            configManager.getMessages().mensajes().minijuegos().alquimia().peligroTitulo(),
                            configManager.getMessages().mensajes().minijuegos().alquimia().peligroSubtitulo()
                    );
                }
            }
        }
    }

    @EventHandler
    public void alAgacharse(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) return;

        Player p = event.getPlayer();
        Location pLoc = p.getLocation();

        for (Map.Entry<Location, MezclaVolatil> entry : mezclas.entrySet()) {
            Location locSoporte = entry.getKey();

            // Verifica que estén en el mismo mundo y cerca
            if (locSoporte.getWorld().equals(pLoc.getWorld()) && locSoporte.distance(pLoc) <= 3) {

                MezclaVolatil mezcla = entry.getValue();
                mezcla.bombeos++;

                p.playSound(locSoporte, Sound.ITEM_BUCKET_FILL, 0.5f, 1f + (mezcla.bombeos * 0.2f));
                locSoporte.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, locSoporte.clone().add(0.5, 1, 0.5), 5);

                // Si se completa el minijuego
                if (mezcla.bombeos >= 3) {
                    p.playSound(locSoporte, Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);

                    // 💡 Ruta corregida
                    CrossplayUtils.sendMessage(p, configManager.getMessages().mensajes().minijuegos().alquimia().mezclaEstabilizada());

                    aplicarPremio(locSoporte, mezcla.pocionesOriginales);
                    mezclas.remove(locSoporte);
                }
                break; // Solo puede bombear una mesa a la vez
            }
        }
    }

    private void aplicarPremio(Location loc, ItemStack[] originales) {
        if (loc.getBlock().getState() instanceof BrewingStand stand) {
            for (int i = 0; i < 3; i++) {
                ItemStack item = originales[i];
                if (item != null && item.getType() == Material.POTION && item.hasItemMeta()) {
                    PotionMeta meta = (PotionMeta) item.getItemMeta();
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
                ItemStack item = stand.getInventory().getItem(i);
                if (item != null && item.getType() != Material.AIR) {
                    // Se arruina la poción
                    stand.getInventory().setItem(i, new ItemStack(Material.GLASS_BOTTLE));
                }
            }
            loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.5f);
            loc.getWorld().spawnParticle(Particle.SMOKE, loc.clone().add(0.5, 0.5, 0.5), 20, 0.2, 0.2, 0.2, 0.05);
        }
    }

    private void iniciarReloj() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Map.Entry<Location, MezclaVolatil> entry : mezclas.entrySet()) {
                MezclaVolatil mezcla = entry.getValue();
                Location loc = entry.getKey();

                loc.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, loc.clone().add(0.5, 1, 0.5), 2, 0.1, 0.1, 0.1, 0.01);

                mezcla.tiempoRestante--;
                if (mezcla.tiempoRestante <= 0) {
                    aplicarCastigo(loc);
                    mezclas.remove(loc);
                }
            }
        }, 20L, 20L); // Se ejecuta cada segundo
    }
}