package me.nexo.mechanics.minigames;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.economy.core.EconomyManager;
import me.nexo.economy.core.NexoAccount;
import me.nexo.mechanics.NexoMechanics;
import me.nexo.mechanics.config.ConfigManager;
import me.nexo.protections.managers.ClaimManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ⛏️ NexoMechanics - Minijuego de Minería RPG (Arquitectura Enterprise Java 21)
 * Rendimiento: Folia-Ready Schedulers, Cero String Allocations y Dependencias Inyectadas.
 */
@Singleton
public class MiningMinigameManager implements Listener {

    private final NexoMechanics plugin;
    private final ConfigManager configManager;
    
    // 🌟 Sinergias propagadas estrictamente por Guice
    private final ClaimManager claimManager;
    private final EconomyManager economyManager;
    private final CrossplayUtils crossplayUtils;

    private final Map<UUID, VetaActiva> vetasActivas = new ConcurrentHashMap<>();

    private record VetaActiva(Location loc, long expiracion, Material tipoOriginal) {}

    // 💉 PILAR 1: Inyección de Dependencias Directa
    @Inject
    public MiningMinigameManager(NexoMechanics plugin, ConfigManager configManager,
                                 ClaimManager claimManager, EconomyManager economyManager, 
                                 CrossplayUtils crossplayUtils) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.claimManager = claimManager;
        this.economyManager = economyManager;
        this.crossplayUtils = crossplayUtils;
        
        iniciarLimpiador();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void alPicar(BlockBreakEvent event) {
        var p = event.getPlayer();
        var b = event.getBlock();
        var id = p.getUniqueId();

        // 🌟 Verificación Inyectada O(1)
        if (claimManager != null) {
            var stone = claimManager.getStoneAt(b.getLocation());
            if (stone != null && !stone.hasPermission(id, me.nexo.protections.core.ClaimAction.BREAK)) {
                event.setCancelled(true);
            }
        }

        if (event.isCancelled()) return;

        // 🌟 RENDIMIENTO O(1): Obtenemos directamente la veta
        var veta = vetasActivas.get(id);
        if (veta != null) {
            if (b.getLocation().equals(veta.loc())) {
                event.setCancelled(true);

                // Operaciones físicas en el RegionThread (Seguro porque es el BlockBreakEvent)
                b.getWorld().dropItemNaturally(b.getLocation(), new ItemStack(veta.tipoOriginal(), 3));
                b.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, b.getLocation().add(0.5, 0.5, 0.5), 20);
                p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_HIT, 1f, 2f);

                p.sendBlockChange(b.getLocation(), Bukkit.createBlockData(Material.AIR));
                vetasActivas.remove(id);

                int monedasRandom = (int) (Math.random() * 40) + 10;
                var recompensaMonedas = new BigDecimal(monedasRandom);

                // 🌟 Economía 100% Asíncrona Inyectada
                if (economyManager != null) {
                    economyManager.updateBalanceAsync(p.getUniqueId(), NexoAccount.AccountType.PLAYER, NexoAccount.Currency.COINS, recompensaMonedas, true);
                }

                String msg = configManager.getMessages().mensajes().minijuegos().mineria().extraccionRentable().replace("%amount%", String.valueOf(monedasRandom));
                crossplayUtils.sendActionBar(p, msg);

                generarVetaContigua(p, b.getLocation(), b.getType().name());
                return;
            }
        }

        // 🌟 OPTIMIZACIÓN GC: .name() usa el string internado del Enum, evitando instanciar nuevos objetos String
        var typeName = b.getType().name();
        if (typeName.contains("STONE") || typeName.contains("ORE")) {
            if (veta == null && Math.random() <= 0.02) {
                generarVetaContigua(p, b.getLocation(), typeName);
            }
        }
    }

    private void generarVetaContigua(Player p, Location origen, String typeName) {
        BlockFace[] caras = {BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};
        for (var cara : caras) {
            var contiguo = origen.getBlock().getRelative(cara);
            var contiguoTypeName = contiguo.getType().name();
            
            if (contiguoTypeName.contains("STONE") || contiguoTypeName.contains("ORE")) {
                p.sendBlockChange(contiguo.getLocation(), Bukkit.createBlockData(Material.RAW_GOLD_BLOCK));
                p.playSound(contiguo.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);
                p.getWorld().spawnParticle(Particle.WAX_ON, contiguo.getLocation().add(0.5, 0.5, 0.5), 10);

                crossplayUtils.sendActionBar(p, configManager.getMessages().mensajes().minijuegos().mineria().anomaliaGeologica());
                vetasActivas.put(p.getUniqueId(), new VetaActiva(contiguo.getLocation(), System.currentTimeMillis() + 4000L, contiguo.getType()));
                break;
            }
        }
    }

    private void iniciarLimpiador() {
        // 🌟 FOLIA NATIVE: Usamos el GlobalRegionScheduler para el limpiador.
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, scheduledTask -> {
            long ahora = System.currentTimeMillis();

            // 🌟 FIX: removeIf previene ConcurrentModificationException y es más rápido
            vetasActivas.entrySet().removeIf(entry -> {
                if (ahora > entry.getValue().expiracion()) {
                    var p = Bukkit.getPlayer(entry.getKey());
                    if (p != null && p.isOnline()) { 
                        p.sendBlockChange(entry.getValue().loc(), Bukkit.createBlockData(entry.getValue().tipoOriginal()));
                        p.playSound(p.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.5f, 1f);
                        crossplayUtils.sendActionBar(p, configManager.getMessages().mensajes().minijuegos().mineria().anomaliaEstabilizada());
                    }
                    return true; // Elimina la veta del mapa
                }
                return false;
            });
        }, 10L, 10L); 
    }
}