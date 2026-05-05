package me.nexo.islas.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.papermc.paper.event.player.AsyncChatEvent;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.islas.NexoIslas;
import me.nexo.islas.data.IslandProfile;
import me.nexo.islas.managers.IslandManager;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

@Singleton
public class IslandRenameListener implements Listener {

    private final NexoIslas plugin;
    private final IslandManager islandManager;
    private final CrossplayUtils crossplayUtils;

    @Inject
    public IslandRenameListener(NexoIslas plugin, IslandManager islandManager, CrossplayUtils crossplayUtils) {
        this.plugin = plugin;
        this.islandManager = islandManager;
        this.crossplayUtils = crossplayUtils;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        
        // Verificamos si el jugador está en una sesión de renombre
        IslandProfile profile = islandManager.getRenameSession(player.getUniqueId());
        if (profile == null) return;

        // Cancelamos el evento para que nadie en el servidor vea el mensaje
        event.setCancelled(true);

        String newName = PlainTextComponentSerializer.plainText().serialize(event.message());

        if (newName.equalsIgnoreCase("cancelar")) {
            islandManager.removeRenameSession(player.getUniqueId());
            crossplayUtils.sendMessage(player, "&#FF5555[!] Renombre de isla cancelado.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        // Filtros de Seguridad Básicos
        if (newName.length() < 3 || newName.length() > 20) {
            crossplayUtils.sendMessage(player, "&#FF5555[x] El nombre debe tener entre 3 y 20 caracteres. Intenta de nuevo o escribe 'cancelar'.");
            return;
        }
        
        if (!newName.matches("^[a-zA-Z0-9_ ]*$")) {
            crossplayUtils.sendMessage(player, "&#FF5555[x] El nombre solo puede contener letras, números y espacios. Intenta de nuevo.");
            return;
        }

        // Aplicamos el nombre y guardamos asíncronamente
        profile.setIslandName(newName);
        islandManager.saveIslandProfileAsync(profile);
        islandManager.removeRenameSession(player.getUniqueId());

        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        crossplayUtils.sendMessage(player, "&#55FF55[✓] ¡El nombre de tu imperio ha sido cambiado a: &#FFFFFF" + newName + "&#55FF55!");
    }

    // Seguridad: Si se desconecta, se le borra la sesión
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        islandManager.removeRenameSession(event.getPlayer().getUniqueId());
    }
}