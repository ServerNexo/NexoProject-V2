package me.nexo.chat.managers;

import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import me.nexo.chat.NexoChatPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Singleton
public class NexoAnnouncementManager implements Listener {

    private final NexoChatPlugin plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    private int currentAnnouncement = 0;
    private int currentMotd = 0;

    // 🌟 Usamos ScheduledTask nativo de Paper para control asíncrono
    private ScheduledTask announcementTask;

    @Inject
    public NexoAnnouncementManager(NexoChatPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 🚀 INICIO DE TAREAS (Lee directamente del config.yml)
     */
    public void startTasks() {
        // Cancelamos la tarea anterior si existe (para evitar duplicados al usar /nexochat recargar)
        if (announcementTask != null && !announcementTask.isCancelled()) {
            announcementTask.cancel();
        }

        // Leemos el tiempo del config (por defecto 300 segundos = 5 minutos si no existe)
        int interval = plugin.getConfig().getInt("anuncios.intervalo_segundos", 300);

        // Iniciamos el reloj asíncrono
        announcementTask = Bukkit.getAsyncScheduler().runAtFixedRate(plugin, task -> {

            // Leemos la lista de anuncios frescos del archivo config.yml
            List<String> announcements = plugin.getConfig().getStringList("anuncios.mensajes");

            // Si la lista está vacía o no hay jugadores online, no hacemos spam
            if (announcements.isEmpty() || Bukkit.getOnlinePlayers().isEmpty()) return;

            // Prevención de errores si borraste anuncios del config mientras estaba corriendo
            if (currentAnnouncement >= announcements.size()) {
                currentAnnouncement = 0;
            }

            // Construimos y enviamos el anuncio
            Component broadcast = mm.deserialize(announcements.get(currentAnnouncement));
            Bukkit.broadcast(broadcast);

            // Sonido para todos los jugadores
            Bukkit.getOnlinePlayers().forEach(p ->
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.4f, 1.5f)
            );

            // Pasamos al siguiente anuncio
            currentAnnouncement++;

        }, interval, interval, TimeUnit.SECONDS);
    }

    /**
     * 🌐 EVENTO DE MOTD (Lee directamente del config.yml)
     */
    @EventHandler
    public void onServerPing(PaperServerListPingEvent event) {
        List<String> motds = plugin.getConfig().getStringList("motd");

        if (motds.isEmpty()) return;

        if (currentMotd >= motds.size()) {
            currentMotd = 0;
        }

        // Aplicamos el MOTD al jugador que está refrescando su lista de servidores
        event.motd(mm.deserialize(motds.get(currentMotd)));
        currentMotd++;
    }
}