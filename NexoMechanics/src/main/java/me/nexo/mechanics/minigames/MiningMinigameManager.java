package me.nexo.mechanics.minigames;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.user.NexoAPI;
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
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
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
 * ⛏️ NexoMechanics - Minijuego de Minería RPG (Arquitectura Enterprise Java 25)
 * Folia-Ready: Schedulers Nativos y Optimización O(1) de memoria.
 */
@Singleton
public class MiningMinigameManager implements Listener {

    private final NexoMechanics plugin;
    private final ConfigManager configManager;
    private final Map<UUID, VetaActiva> vetasActivas = new ConcurrentHashMap<>();

    private record VetaActiva(Location loc, long expiracion, Material tipoOriginal) {}

    @Inject
    public MiningMinigameManager(NexoMechanics plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        iniciarLimpiador();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void alPicar(BlockBreakEvent event) {
        Player p = event.getPlayer();
        Block b = event.getBlock();
        UUID id = p.getUniqueId();

        NexoAPI.getServices().get(ClaimManager.class).ifPresent(claimManager -> {
            me.nexo.protections.core.ProtectionStone stone = claimManager.getStoneAt(b.getLocation());
            if (stone != null && !stone.hasPermission(id, me.nexo.protections.core.ClaimAction.BREAK)) {
                event.setCancelled(true);
            }
        });

        if (event.isCancelled()) return;

        // 🌟 FIX RENDIMIENTO O(1): Obtenemos directamente la veta, evitamos hacer containsKey + get
        VetaActiva veta = vetasActivas.get(id);
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
                BigDecimal recompensaMonedas = new BigDecimal(monedasRandom);

                // 🌟 Economía 100% Asíncrona Inyectada
                NexoAPI.getServices().get(EconomyManager.class).ifPresent(eco ->
                        eco.updateBalanceAsync(p.getUniqueId(), NexoAccount.AccountType.PLAYER, NexoAccount.Currency.COINS, recompensaMonedas, true));

                String msg = configManager.getMessages().mensajes().minijuegos().mineria().extraccionRentable().replace("%amount%", String.valueOf(monedasRandom));
                CrossplayUtils.sendActionBar(p, msg);

                generarVetaContigua(p, b);
                return;
            }
        }

        if (b.getType().toString().contains("STONE") || b.getType().toString().contains("ORE")) {
            if (veta == null && Math.random() <= 0.02) {
                generarVetaContigua(p, b);
            }
        }
    }

    private void generarVetaContigua(Player p, Block origen) {
        BlockFace[] caras = {BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};
        for (BlockFace cara : caras) {
            Block contiguo = origen.getRelative(cara);
            if (contiguo.getType().toString().contains("STONE") || contiguo.getType().toString().contains("ORE")) {

                p.sendBlockChange(contiguo.getLocation(), Bukkit.createBlockData(Material.RAW_GOLD_BLOCK));
                p.playSound(contiguo.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);
                p.getWorld().spawnParticle(Particle.WAX_ON, contiguo.getLocation().add(0.5, 0.5, 0.5), 10);

                CrossplayUtils.sendActionBar(p, configManager.getMessages().mensajes().minijuegos().mineria().anomaliaGeologica());
                vetasActivas.put(p.getUniqueId(), new VetaActiva(contiguo.getLocation(), System.currentTimeMillis() + 4000L, contiguo.getType()));
                break;
            }
        }
    }

    private void iniciarLimpiador() {
        // 🌟 FOLIA FIX: Usamos el GlobalRegionScheduler para el limpiador.
        // Ejecuta la revisión periódica sin congelar el hilo de operaciones del servidor.
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, scheduledTask -> {
            long ahora = System.currentTimeMillis();

            for (Map.Entry<UUID, VetaActiva> entry : vetasActivas.entrySet()) {
                if (ahora > entry.getValue().expiracion()) {
                    Player p = Bukkit.getPlayer(entry.getKey());
                    if (p != null && p.isOnline()) { // Verificación de seguridad extra
                        p.sendBlockChange(entry.getValue().loc(), Bukkit.createBlockData(entry.getValue().tipoOriginal()));
                        p.playSound(p.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.5f, 1f);
                        CrossplayUtils.sendActionBar(p, configManager.getMessages().mensajes().minijuegos().mineria().anomaliaEstabilizada());
                    }
                    vetasActivas.remove(entry.getKey());
                }
            }
        }, 10L, 10L); // Retraso de 10 ticks, se repite cada 10 ticks
    }
}