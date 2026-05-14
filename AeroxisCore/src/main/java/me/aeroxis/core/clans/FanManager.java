package me.aeroxis.core.clans;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.aeroxis.core.AeroxisCore;
import me.aeroxis.core.crossplay.CrossplayUtils;
import net.kyori.adventure.text.Component; // 🌟 IMPORT COMPONENT
import net.kyori.adventure.title.Title; // 🌟 IMPORT TITLE
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.Duration; // 🌟 IMPORT DURATION
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🚩 AeroxisCore - Motor del Sistema de Fans (Patrocinadores de Clanes)
 * Gestiona las suscripciones de los jugadores y distribuye recompensas globales.
 */
@Singleton
public class FanManager {

    private final AeroxisCore plugin;
    private final CrossplayUtils crossplayUtils;

    // Caché de Fans: UUID del Jugador -> ID del Clan al que apoya
    // En un entorno real, esto se cargaría de la base de datos al entrar el jugador
    private final Map<UUID, String> fanSubscriptions = new ConcurrentHashMap<>();

    @Inject
    public FanManager(AeroxisCore plugin, CrossplayUtils crossplayUtils) {
        this.plugin = plugin;
        this.crossplayUtils = crossplayUtils;
    }

    /**
     * Registra a un jugador como Fan Oficial de un clan.
     */
    public void addFan(Player player, String clanId, String clanName) {
        fanSubscriptions.put(player.getUniqueId(), clanId);

        // Sonido de victoria
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

        // 🌟 FIX: Título moderno de Adventure API (10, 50, 10 ticks -> 500, 2500, 500 ms)
        Component mainTitle = crossplayUtils.parseCrossplay(null, "&#FFD700<bold>¡NUEVO FAN!</bold>");
        Component subTitle = crossplayUtils.parseCrossplay(null, "&#FFFFFFAhora apoyas a &#FFAA00" + clanName);
        Title.Times times = Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(2500), Duration.ofMillis(500));
        player.showTitle(Title.title(mainTitle, subTitle, times));

        crossplayUtils.sendMessage(player, "&#55FF55[!] Has comprado el Pase de Fan de &#FFD700" + clanName + "&#55FF55. ¡Recibirás premios cuando ellos triunfen!");

        // TODO: Enviar guardado a la Base de Datos (DatabaseManager)
    }

    /**
     * Obtiene el clan que apoya el jugador (si tiene uno).
     */
    public String getSupportedClan(Player player) {
        return fanSubscriptions.get(player.getUniqueId());
    }

    /**
     * 🎁 LA MAGIA SOCIAL: Reparte recompensas a todos los fans conectados de un clan.
     * Este método lo llamarás desde tu sistema de Mazmorras o Guerras.
     *
     * @param clanId El ID del clan que logró la hazaña.
     * @param clanName El nombre del clan para mostrar en los mensajes.
     * @param hazaña Texto descriptivo (Ej: "ha derrotado al Rey Esqueleto").
     */
    public void rewardFans(String clanId, String clanName, String hazaña) {
        // Anuncio a todo el servidor para generar hype y envidia sana
        crossplayUtils.broadcastMessage("\n&#FFD700<bold>🚩 ¡HAZAÑA DE CLAN!</bold>");
        crossplayUtils.broadcastMessage("&#E6CCFFEl clan &#FFD700" + clanName + " &#E6CCFF" + hazaña + "!");
        crossplayUtils.broadcastMessage("&#E6CCFFSus Fans conectados están recibiendo recompensas...\n");

        int fansRecompensados = 0;

        // Repartimos el botín solo a los fans que estén online
        for (Player p : Bukkit.getOnlinePlayers()) {
            String supported = fanSubscriptions.get(p.getUniqueId());

            if (supported != null && supported.equals(clanId)) {
                fansRecompensados++;
                entregarPremio(p);
            }
        }

        plugin.getLogger().info("🚩 Sistema de Fans: " + fansRecompensados + " fans de " + clanName + " recompensados.");
    }

    /**
     * Lógica de inyección del premio al jugador.
     */
    private void entregarPremio(Player player) {
        // Creamos un "Cofre de Fan" (Puedes cambiar esto por una llave de crate o un ítem de NexoItems)
        ItemStack premio = new ItemStack(Material.EMERALD, 3);

        // Inyectamos directo a la RAM del inventario
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(premio);

        // Efectos visuales de que le llegó un paquete
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 2.0f);
        crossplayUtils.sendMessage(player, "&#55FF55[!] ¡Tu clan patrocinado triunfó! Has recibido 3 Esmeraldas.");

        if (!leftover.isEmpty()) {
            for (ItemStack item : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), item);
            }
            crossplayUtils.sendMessage(player, "&#FFAA00[!] Tu inventario estaba lleno. El premio cayó a tus pies.");
        }
    }
}