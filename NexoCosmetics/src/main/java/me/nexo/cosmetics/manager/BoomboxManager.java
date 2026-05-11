package me.nexo.cosmetics.manager;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.xxmicloxx.NoteBlockAPI.model.Song;
import com.xxmicloxx.NoteBlockAPI.songplayer.PositionSongPlayer;
import com.xxmicloxx.NoteBlockAPI.songplayer.SongPlayer;
import com.xxmicloxx.NoteBlockAPI.utils.NBSDecoder;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🎵 NexoCosmetics - Gestor del Boombox (NoteBlockAPI)
 */
@Singleton
public class BoomboxManager {

    private final JavaPlugin plugin;
    private final Map<UUID, SongPlayer> activeSongs = new ConcurrentHashMap<>();
    private final File songsFolder;

    @Inject
    public BoomboxManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.songsFolder = new File(plugin.getDataFolder(), "songs");
    }

    public boolean playSong(Player player, String fileName) {
        stopSong(player);

        // 🌟 Búsqueda Case-Insensitive (Ignora mayúsculas)
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

        songPlayer.setPlaying(true);
        activeSongs.put(player.getUniqueId(), songPlayer);
        return true;
    }

    public void stopSong(Player player) {
        SongPlayer existing = activeSongs.remove(player.getUniqueId());
        if (existing != null) {
            existing.setPlaying(false);
            existing.destroy();
        }
    }

    // 🌟 MOTOR DE ESCANEO A PRUEBA DE BALAS
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
                // Acepta .nbs o .NBS, e incluso el doble .nbs.nbs de Windows
                if (name.toLowerCase().endsWith(".nbs")) {
                    // Limpiamos la extensión para la lista
                    String cleanName = name.replaceAll("(?i)\\.nbs$", "");
                    songs.add(cleanName);
                }
            }
        }

        // Chivato en la consola para confirmar qué leyó
        if (songs.isEmpty()) {
            plugin.getLogger().info("🔍 Boombox escaneó la carpeta pero no encontró canciones válidas.");
        } else {
            plugin.getLogger().info("🎵 Boombox detectó " + songs.size() + " canciones: " + songs);
        }

        return songs;
    }
}