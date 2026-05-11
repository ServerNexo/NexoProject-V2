package me.nexo.mechanics.archeology;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.mechanics.NexoMechanics;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

@Singleton
public class ArcheologyListener implements Listener {

    private final NexoMechanics plugin;
    private final ArcheologyManager manager;

    @Inject
    public ArcheologyListener(NexoMechanics plugin, ArcheologyManager manager) {
        this.plugin = plugin;
        this.manager = manager;
        // Crea el archivo config.yml automáticamente si no existe
        plugin.saveDefaultConfig();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTreasureDig(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        Location loc = block.getLocation();

        if (manager.getHiddenTreasures().contains(loc)) {
            manager.removeTreasure(loc);
            Player p = event.getPlayer();

            loc.getWorld().spawnParticle(Particle.BLOCK, loc.clone().add(0.5, 1, 0.5), 30, 0.3, 0.3, 0.3, block.getBlockData());
            entregarRecompensaDinamica(p, loc);
        }
    }

    private void entregarRecompensaDinamica(Player p, Location loc) {
        loc.getWorld().playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);

        ConfigurationSection rewardsSection = plugin.getConfig().getConfigurationSection("recompensas_arqueologia");

        // Seguro Anti-Errores por si el config está mal escrito
        if (rewardsSection == null) {
            p.sendMessage("§c[!] Las recompensas no están configuradas en el sistema.");
            return;
        }

        // 1. Sumamos todas las probabilidades (para que siempre cuadre el 100%)
        double totalWeight = 0.0;
        for (String key : rewardsSection.getKeys(false)) {
            totalWeight += rewardsSection.getDouble(key + ".probabilidad", 0.0);
        }

        // 2. Tiramos la ruleta
        double random = Math.random() * totalWeight;
        String premioElegido = null;

        for (String key : rewardsSection.getKeys(false)) {
            double chance = rewardsSection.getDouble(key + ".probabilidad", 0.0);
            random -= chance;
            if (random <= 0) {
                premioElegido = key;
                break;
            }
        }

        // 3. Entregamos el premio ganador
        if (premioElegido != null) {
            MiniMessage mm = MiniMessage.miniMessage();

            // Enviar mensaje
            String mensaje = rewardsSection.getString(premioElegido + ".mensaje");
            if (mensaje != null && !mensaje.isEmpty()) {
                p.sendMessage(mm.deserialize(mensaje));
            }

            // Ejecutar todos los comandos asociados
            for (String cmd : rewardsSection.getStringList(premioElegido + ".comandos")) {
                String comandoFinal = cmd.replace("%player%", p.getName());
                // Ejecutamos desde la consola de forma síncrona
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), comandoFinal);
                });
            }
        }
    }
}