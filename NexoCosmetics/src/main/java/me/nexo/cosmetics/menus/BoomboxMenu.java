package me.nexo.cosmetics.menus;

import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.menus.NexoMenu;
import me.nexo.core.utils.SoundManager;
import me.nexo.cosmetics.manager.BoomboxManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 📻 Menú Boombox Premium (Generación Dinámica)
 */
public class BoomboxMenu extends NexoMenu {

    private final SoundManager soundManager;
    private final BoomboxManager boomboxManager;
    private final MiniMessage mm = MiniMessage.miniMessage();

    // 🌟 Diccionario que recuerda qué canción está en cada Slot del inventario
    private final Map<Integer, String> songSlots = new HashMap<>();

    public BoomboxMenu(Player player, CrossplayUtils crossplayUtils, SoundManager soundManager, BoomboxManager boomboxManager) {
        super(player, crossplayUtils);
        this.soundManager = soundManager;
        this.boomboxManager = boomboxManager;
    }

    @Override public String getMenuName() { return "<dark_gray>📻 Boombox Premium</dark_gray>"; }
    @Override public int getSlots() { return 54; }

    @Override
    public void setMenuItems() {
        setFillerGlass();
        songSlots.clear();

        List<String> songs = boomboxManager.getAvailableSongs();

        // Empezamos a colocar canciones en el Slot 10 (dejando el borde de cristal)
        int currentSlot = 10;

        // Array de discos para que el menú se vea colorido y no todos los discos sean iguales
        Material[] discs = {Material.MUSIC_DISC_CAT, Material.MUSIC_DISC_BLOCKS, Material.MUSIC_DISC_CHIRP,
                Material.MUSIC_DISC_FAR, Material.MUSIC_DISC_MALL, Material.MUSIC_DISC_MELLOHI,
                Material.MUSIC_DISC_STAL, Material.MUSIC_DISC_STRAD, Material.MUSIC_DISC_WARD};

        for (String songFile : songs) {
            // Formateador visual: "crab_rave" -> "Crab Rave"
            String displayName = formatSongName(songFile);
            Material randomDisc = discs[currentSlot % discs.length];

            setItem(currentSlot, randomDisc, "<aqua>" + displayName + "</aqua>", List.of("<gray>▶ Clic para reproducir</gray>"));
            songSlots.put(currentSlot, songFile); // Guardamos la ruta en la memoria del menú

            currentSlot++;

            // Si llega al borde derecho, saltamos a la siguiente fila (Slot 17 -> 19)
            if (currentSlot == 17 || currentSlot == 26 || currentSlot == 35) {
                currentSlot += 2;
            }

            // Máximo de canciones por página simple (hasta el slot 43)
            if (currentSlot > 43) break;
        }

        // ⬅️ Controles fijos
        setItem(48, Material.DARK_OAK_DOOR, "<red><bold>Volver al Armario</bold></red>", List.of("<gray>Regresa al menú principal.</gray>"));
        setItem(50, Material.BARRIER, "<red><bold>Detener Música</bold></red>", List.of("<gray>Apaga el Boombox.</gray>"));

        if (songs.isEmpty()) {
            setItem(22, Material.BARRIER, "<red>No hay canciones</red>", List.of("<gray>Añade archivos .nbs a la carpeta.</gray>"));
        }
    }

    // Pequeña herramienta para capitalizar y limpiar los guiones bajos
    private String formatSongName(String rawName) {
        String[] words = rawName.split("_");
        StringBuilder formatted = new StringBuilder();
        for (String word : words) {
            if (word.length() > 0) {
                formatted.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1).toLowerCase()).append(" ");
            }
        }
        return formatted.toString().trim();
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        e.setCancelled(true);
        if (e.getClickedInventory() == null || !e.getClickedInventory().equals(inventory)) return;

        int slot = e.getSlot();

        if (slot == 48) { player.closeInventory(); player.performCommand("cosmeticos"); return; }

        if (slot == 50) {
            boomboxManager.stopSong(player);
            soundManager.playReset(player);
            player.sendMessage(mm.deserialize("<red>📻 Boombox apagado.</red>"));
            player.closeInventory();
            return;
        }

        // 🌟 SISTEMA DINÁMICO: Verificamos si el Slot clickeado tiene una canción guardada en memoria
        if (songSlots.containsKey(slot)) {
            String songFile = songSlots.get(slot);

            if (!player.hasPermission("nexocosmetics.boombox")) {
                soundManager.playError(player);
                player.sendMessage(mm.deserialize("<red>❌ Necesitas rango VIP para usar el Boombox.</red>"));
                return;
            }

            boolean success = boomboxManager.playSong(player, songFile);
            if (success) {
                soundManager.playClick(player);
                player.sendMessage(mm.deserialize("<green>🎵 Reproduciendo pista de audio...</green>"));
            } else {
                soundManager.playError(player);
            }
            player.closeInventory();
        }
    }
}