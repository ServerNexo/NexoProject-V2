package me.nexo.mechanics.minigames;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.NexoCore;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.user.NexoAPI;
import me.nexo.core.user.NexoUser;
import me.nexo.mechanics.NexoMechanics;
import me.nexo.mechanics.config.ConfigManager;
import me.nexo.protections.managers.ClaimManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
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

@Singleton
public class WoodcuttingMinigameManager implements Listener {

    private final NexoMechanics plugin;
    private final ConfigManager configManager;
    private final NexoCore core;
    private final Map<UUID, NucleoActivo> nucleos = new ConcurrentHashMap<>();

    private record NucleoActivo(Block bloque, long expiracion, Material tipoOriginal) {}

    @Inject
    public WoodcuttingMinigameManager(NexoMechanics plugin, ConfigManager configManager, NexoCore core) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.core = core;
        iniciarLimpiador();
    }

    @EventHandler
    public void alGolpearMadera(BlockDamageEvent event) {
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

        if (nucleos.containsKey(id)) {
            NucleoActivo nucleo = nucleos.get(id);
            if (b.getLocation().equals(nucleo.bloque().getLocation())) {
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);
                p.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, b.getLocation().add(0.5, 0.5, 0.5), 20);
                talarArbol(b);
                b.getWorld().dropItemNaturally(b.getLocation(), new ItemStack(Material.HONEYCOMB, 2));

                NexoUser user = core.getUserManager().getUserOrNull(id);

                if (user != null) {
                    int maxEnergia = 100 + ((user.getNexoNivel() - 1) * 20) + user.getEnergiaExtraAccesorios();
                    int nuevaEnergia = Math.min(user.getEnergiaMineria() + 10, maxEnergia);
                    user.setEnergiaMineria(nuevaEnergia);

                    CrossplayUtils.sendActionBar(p, configManager.getMessages().mensajes().minijuegos().tala().nucleoDestruidoEnergia());
                } else {
                    CrossplayUtils.sendActionBar(p, configManager.getMessages().mensajes().minijuegos().tala().nucleoDestruido());
                }
                p.sendBlockChange(b.getLocation(), Bukkit.createBlockData(Material.AIR));
                nucleos.remove(id);
                return;
            }
        }

        if (b.getType().toString().contains("LOG") && !nucleos.containsKey(id)) {
            if (Math.random() <= 0.05) {
                activarNucleo(p, b);
            }
        }
    }

    private void activarNucleo(Player p, Block origen) {
        Block objetivo = origen.getRelative(BlockFace.UP);
        if (!objetivo.getType().toString().contains("LOG")) {
            objetivo = origen.getRelative(BlockFace.DOWN);
        }
        if (objetivo.getType().toString().contains("LOG")) {
            p.sendBlockChange(objetivo.getLocation(), Bukkit.createBlockData(Material.CRIMSON_STEM));
            p.playSound(objetivo.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);
            p.getWorld().spawnParticle(Particle.WAX_ON, objetivo.getLocation().add(0.5, 0.5, 0.5), 15);

            CrossplayUtils.sendActionBar(p, configManager.getMessages().mensajes().minijuegos().tala().anomaliaBotanica());
            nucleos.put(p.getUniqueId(), new NucleoActivo(objetivo, System.currentTimeMillis() + 3000L, objetivo.getType()));
        }
    }

    private void talarArbol(Block inicio) {
        Block actual = inicio;
        while (actual.getType().toString().contains("LOG") || actual.getType().toString().contains("LEAVES")) {
            actual.breakNaturally();
            actual = actual.getRelative(BlockFace.UP);
        }
    }

    private void iniciarLimpiador() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long ahora = System.currentTimeMillis();
            for (Map.Entry<UUID, NucleoActivo> entry : nucleos.entrySet()) {
                if (ahora > entry.getValue().expiracion()) {
                    Player p = Bukkit.getPlayer(entry.getKey());
                    if (p != null) {
                        p.sendBlockChange(entry.getValue().bloque().getLocation(), Bukkit.createBlockData(entry.getValue().tipoOriginal()));
                        p.playSound(p.getLocation(), Sound.BLOCK_CANDLE_EXTINGUISH, 0.5f, 1f);
                        CrossplayUtils.sendActionBar(p, configManager.getMessages().mensajes().minijuegos().tala().biomasaEndurecida());
                    }
                    nucleos.remove(entry.getKey());
                }
            }
        }, 10L, 10L);
    }
}