package me.aeroxis.cosmetics.manager;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.xxmicloxx.NoteBlockAPI.model.Song;
import com.xxmicloxx.NoteBlockAPI.songplayer.PositionSongPlayer;
import com.xxmicloxx.NoteBlockAPI.songplayer.SongPlayer;
import com.xxmicloxx.NoteBlockAPI.utils.NBSDecoder;
import me.aeroxis.cosmetics.AeroxisCosmetics;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🎵 AeroxisCosmetics - Gestor del Boombox (NoteBlockAPI)
 * 🌟 Sistema de Radio en Movimiento Integrado
 */
@Singleton
public class BoomboxManager {

    private final AeroxisCosmetics plugin;
    private final Map<UUID, SongPlayer> activeSongs = new ConcurrentHashMap<>();
    private final File songsFolder;

    @Inject
    public BoomboxManager(AeroxisCosmetics plugin) {
        this.plugin = plugin;
        this.songsFolder = new File(plugin.getDataFolder(), "songs");
    }

    public boolean playSong(Player player, String fileName) {
        stopSong(player);

        File songFile = null;
        File[] files = songsFolder.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.getName().equalsIgnoreCase(fileName + ".nbs") || f.getName().equalsIgnoreCase(fileName + ".nbs.nbs")) {
                    songFile = f;
                    break;
                }
            }
        }

        if (songFile == null || !songFile.exists()) {
            plugin.getLogger().warning("❌ Boombox: No se encontró el archivo -> " + fileName);
            return false;
        }

        Song song = NBSDecoder.parse(songFile);
        if (song == null) {
            plugin.getLogger().severe("❌ Boombox: Archivo corrupto o no válido -> " + songFile.getName());
            return false;
        }

        PositionSongPlayer songPlayer = new PositionSongPlayer(song);
        songPlayer.setTargetLocation(player.getLocation());

        int distance = plugin.getConfig().getInt("boombox.distancia_audio", 20);
        songPlayer.setDistance(distance);

        for (Player online : player.getWorld().getPlayers()) {
            songPlayer.addPlayer(online);
        }

        songPlayer.setPlaying(true);
        activeSongs.put(player.getUniqueId(), songPlayer);

        // ==========================================
        // 🌟 MOTOR DE RADIO EN MOVIMIENTO (0 Lag)
        // ==========================================
        player.getScheduler().runAtFixedRate(plugin, task -> {
            // Si el jugador se desconecta o la canción termina/se detiene, apagamos el motor
            if (!player.isOnline() || !songPlayer.isPlaying()) {
                task.cancel();
                return;
            }
            // Actualizamos la posición de la música a donde esté caminando el jugador
            songPlayer.setTargetLocation(player.getLocation());
        }, null, 1L, 2L); // Se ejecuta cada 2 ticks para un audio 3D ultra fluido

        return true;
    }

    public void stopSong(Player player) {
        SongPlayer existing = activeSongs.remove(player.getUniqueId());
        if (existing != null) {
            existing.setPlaying(false);
            existing.destroy();
        }
    }

    public List<String> getAvailableSongs() {
        List<String> songs = new ArrayList<>();

        if (!songsFolder.exists() || !songsFolder.isDirectory()) {
            plugin.getLogger().warning("⚠️ La carpeta 'songs' no existe en la ruta: " + songsFolder.getAbsolutePath());
            return songs;
        }

        File[] files = songsFolder.listFiles();
        if (files != null) {
            for (File file : files) {
                String name = file.getName();
                if (name.toLowerCase().endsWith(".nbs")) {
                    String cleanName = name.replaceAll("(?i)\\.nbs$", "");
                    songs.add(cleanName);
                }
            }
        }

        if (songs.isEmpty()) {
            plugin.getLogger().info("🔍 Boombox escaneó la carpeta pero no encontró canciones válidas.");
        } else {
            plugin.getLogger().info("🎵 Boombox detectó " + songs.size() + " canciones: " + songs);
        }

        return songs;
    }
}