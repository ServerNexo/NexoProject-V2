package me.aeroxis.core.events.types;

import com.google.inject.Inject;
import me.aeroxis.core.AeroxisCore;
import me.aeroxis.core.bosses.AeroxisBoss; // 🌟 IMPORTAMOS LA CLASE BASE
import me.aeroxis.core.bosses.AeroxisBossRegistry;
import me.aeroxis.core.crossplay.CrossplayUtils;
import me.aeroxis.core.events.AeroxisEvent;
import me.aeroxis.core.visuals.MobVisualManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey; // 🌟 NUEVO IMPORT
import org.bukkit.Registry;      // 🌟 NUEVO IMPORT
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * ☄️ Evento Nativo - Invasión de Jefe Global
 * Lee la configuración y hace aparecer al jefe usando la API del Core.
 */
public class BossInvasionEvent implements AeroxisEvent {

    private final AeroxisCore plugin;
    private final CrossplayUtils crossplayUtils;
    private final MobVisualManager visualManager;
    private final AeroxisBossRegistry bossRegistry; // 🌟 NUEVA DEPENDENCIA

    private final String id = "invasion_renacido";
    private boolean activo = false;

    // 🌟 FIX: Añadimos @Inject y solicitamos el Registry
    @Inject
    public BossInvasionEvent(AeroxisCore plugin, CrossplayUtils crossplayUtils, MobVisualManager visualManager, AeroxisBossRegistry bossRegistry) {
        this.plugin = plugin;
        this.crossplayUtils = crossplayUtils;
        this.visualManager = visualManager;
        this.bossRegistry = bossRegistry;
    }

    @Override
    public String getId() { return id; }

    @Override
    public String getNombre() { return "&#FF3366🩸 El Despertar del Renacido"; }

    @Override
    public String getTipo() { return "BOSS_SPAWN"; }

    @Override
    public boolean cumpleRequisitos(int minJugadores) {
        // ¿Hay suficientes jugadores online para lanzar el evento?
        return Bukkit.getOnlinePlayers().size() >= minJugadores;
    }

    @Override
    public void iniciarEvento() {
        this.activo = true;

        // 1. Leer ubicación desde el events.yml (Ej: "world,0.5,65.0,0.5")
        File file = new File(plugin.getDataFolder(), "events.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        String locStr = config.getString("eventos." + id + ".spawn_location", "world,0,100,0");
        String[] parts = locStr.split(",");

        World world = Bukkit.getWorld(parts[0]);
        if (world == null) {
            plugin.getLogger().severe("❌ Evento Cancelado: El mundo '" + parts[0] + "' no existe.");
            this.activo = false;
            return;
        }

        Location loc = new Location(world, Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), Double.parseDouble(parts[3]));

        // 2. Cinemática (Sonidos y Anuncios)
        for (String accion : config.getStringList("eventos." + id + ".acciones_inicio")) {
            if (accion.startsWith("[broadcast] ")) {
                crossplayUtils.broadcastMessage(accion.replace("[broadcast] ", ""));
            } else if (accion.startsWith("[sound] ")) {
                // Formato: [sound] ENTITY_WITHER_SPAWN 1.0 0.5
                String[] soundArgs = accion.replace("[sound] ", "").split(" ");
                try {
                    // 🌟 FIX: Uso de la API Moderna de Registros de Paper 1.20+
                    String soundName = soundArgs[0].toLowerCase();
                    Sound sound = Registry.SOUNDS.get(NamespacedKey.minecraft(soundName));

                    if (sound != null) {
                        float vol = Float.parseFloat(soundArgs[1]);
                        float pitch = Float.parseFloat(soundArgs[2]);
                        // Reproducimos el sonido globalmente para todos los jugadores
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            p.playSound(p.getLocation(), sound, vol, pitch);
                        }
                    } else {
                        plugin.getLogger().warning("⚠️ Sonido desconocido en events.yml: " + soundName);
                    }
                } catch (Exception ignored) {}
            }
        }

        // 3. ¡Spawneamos la Bestia usando el Registry!
        // 🌟 FIX: Ahora el Core simplemente pide el Jefe sin importarlo
        AeroxisBoss jefe = bossRegistry.spawnBoss("EL_RENACIDO", plugin, loc, 100);

        if (jefe != null && jefe.getEntity() != null) {
            visualManager.attachCustomHologram(jefe.getEntity(), 50);

            // 4. Lógica para auto-apagar el evento cuando el jefe muera
            Bukkit.getScheduler().runTaskTimer(plugin, task -> {
                if (jefe.getEntity() == null || jefe.getEntity().isDead()) {
                    finalizarEvento();
                    task.cancel();
                }
            }, 100L, 20L); // Chequea cada segundo
        } else {
            plugin.getLogger().severe("❌ Evento Cancelado: No se pudo crear al jefe 'EL_RENACIDO'.");
            this.activo = false;
        }
    }

    @Override
    public void finalizarEvento() {
        this.activo = false;
        crossplayUtils.broadcastMessage("&#55FF55[!] La invasión de El Renacido ha sido contenida.");
    }

    @Override
    public boolean estaActivo() { return activo; }

    @Override
    public List<Player> getParticipantes() { return new ArrayList<>(Bukkit.getOnlinePlayers()); }
}