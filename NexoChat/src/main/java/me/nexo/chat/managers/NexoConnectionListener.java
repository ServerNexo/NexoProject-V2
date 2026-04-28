package me.nexo.chat.managers;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.chat.NexoChatPlugin;
import me.nexo.chat.database.NexoChatDatabase;
import me.nexo.chat.utils.PlayerHeadDrawer; // 🌟 IMPORTAMOS EL CREADOR DE CARAS
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.geysermc.floodgate.api.FloodgateApi; // 🌟 IMPORTAMOS FLOODGATE

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Singleton
public class NexoConnectionListener implements Listener {

    private final NexoChatPlugin plugin;
    private final NexoChatDatabase database;
    private final NexoChatManager chatManager;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final boolean hasFloodgate; // 🌟 BANDERA CROSSPLAY

    @Inject
    public NexoConnectionListener(NexoChatPlugin plugin, NexoChatDatabase database, NexoChatManager chatManager) {
        this.plugin = plugin;
        this.database = database;
        this.chatManager = chatManager;
        this.hasFloodgate = Bukkit.getPluginManager().getPlugin("floodgate") != null; // Verificamos si existe Geyser
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // 🌟 VERIFICACIÓN BEDROCK
        boolean isBedrock = hasFloodgate && FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId());

        // 📥 Cargar datos desde PostgreSQL y Dibujar Skin (Asíncrono = Cero lag)
        Bukkit.getAsyncScheduler().runNow(plugin, task -> {
            // 1. Carga Nicks, Tags y Muteos de NexoChat
            database.loadPlayerData(player.getUniqueId());

            // 2. 🎨 ENVIAMOS EL MOTD PERSONALIZADO
            List<String> motdLines = plugin.getConfig().getStringList("motd_personal");
            if (!motdLines.isEmpty()) {
                player.sendMessage(Component.text(" ")); // Espacio superior

                if (isBedrock) {
                    // 📱 BEDROCK: Enviamos el MOTD limpio sin la cara rota de píxeles
                    for (String line : motdLines) {
                        String parsedLine = line.replace("%player%", player.getName());
                        player.sendMessage(chatManager.parseColors(parsedLine));
                    }
                } else {
                    // ☕ JAVA: Generamos la lista de componentes con la cara dibujada + texto
                    List<Component> faceMotd = PlayerHeadDrawer.getFaceMotd(player, motdLines, chatManager);
                    for (Component line : faceMotd) {
                        player.sendMessage(line);
                    }
                }

                player.sendMessage(Component.text(" ")); // Espacio inferior
            }
        });

        // 🌟 MENSAJES DE BROADCAST (Lo que ven los demás al entrar)
        if (!player.hasPlayedBefore()) {
            String welcomeRaw = plugin.getConfig().getString("eventos.entradas_salidas.primera_vez", "<green>Bienvenido %player%!</green>");
            event.joinMessage(mm.deserialize(welcomeRaw.replace("%player%", player.getName())));

            Bukkit.getOnlinePlayers().forEach(p ->
                    p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.0f)
            );
        } else {
            // 🌟 INGRESO NORMAL
            String joinRaw = plugin.getConfig().getString("eventos.entradas_salidas.entrar", "<gray>+ %player%</gray>");
            event.joinMessage(mm.deserialize(joinRaw.replace("%player%", player.getName())));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // 📤 Recolectamos datos de la RAM antes de que se desconecte
        Set<UUID> ignores = chatManager.getIgnoredPlayersMap().get(uuid);

        // Guardar asíncronamente en PostgreSQL
        Bukkit.getAsyncScheduler().runNow(plugin, task -> {
            // 🌟 Guardamos Nicks, Tags, Muteos e Ignorados
            database.savePlayerData(uuid, ignores);

            // 🧹 Limpiamos la RAM del ChatManager (Cero fugas de memoria)
            chatManager.getIgnoredPlayersMap().remove(uuid);
            chatManager.getUnlockedTagsMap().remove(uuid);
            chatManager.setPlayerNickname(uuid, "");
            chatManager.setPlayerActiveTag(uuid, "");
            chatManager.removeMute(uuid);
        });

        // 🌟 SALIDA NORMAL
        String quitRaw = plugin.getConfig().getString("eventos.entradas_salidas.salir", "<gray>- %player%</gray>");
        event.quitMessage(mm.deserialize(quitRaw.replace("%player%", player.getName())));
    }
}