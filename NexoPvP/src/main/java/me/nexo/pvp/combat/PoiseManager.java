package me.nexo.pvp.combat;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.pvp.NexoPvP;
import me.nexo.pvp.classes.ArmorWeightManager;
import net.kyori.adventure.text.Component; // 🌟 IMPORT COMPONENT
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title; // 🌟 IMPORT TITLE API
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.time.Duration; // 🌟 IMPORT DURATION
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🛡️ NexoPvP - Motor de Postura y Aturdimiento (Poise System)
 * Gestiona el aguante de los jugadores y sus penalizaciones al romperse la guardia.
 */
@Singleton
public class PoiseManager {

    private final NexoPvP plugin;
    private final ArmorWeightManager weightManager;
    private final MiniMessage mm = MiniMessage.miniMessage(); // 🌟 Instancia ligera de MiniMessage

    private static final long REGEN_DELAY_MS = 3000L; // 3 Segundos sin recibir daño para curar postura

    private static class PoiseData {
        double currentPoise;
        long lastHitTime;
        BossBar bossBar;
    }

    // ⚡ Memoria Concurrente
    private final Map<UUID, PoiseData> activePoise = new ConcurrentHashMap<>();

    @Inject
    public PoiseManager(NexoPvP plugin, ArmorWeightManager weightManager) {
        this.plugin = plugin;
        this.weightManager = weightManager;

        // 🌟 FIX: Hemos movido startRegenTask() de aquí al método public start()
        // para evitar que el hilo asíncrono cause un 'this-escape' durante la construcción.
    }

    /**
     * 🌟 FIX THIS-ESCAPE: Inicia el motor de regeneración de forma segura.
     * DEBE llamarse desde la clase principal (NexoPvP) en el onEnable().
     */
    public void start() {
        startRegenTask();
    }

    /**
     * Obtiene la postura máxima de un jugador basándose en su clase actual.
     */
    public double getMaxPoise(Player player) {
        ArmorWeightManager.ArmorClass armorClass = weightManager.calculatePlayerClass(player);
        return switch (armorClass) {
            case HEAVY -> 100.0; // Inamovible
            case MEDIUM -> 70.0; // Estándar
            case LIGHT -> 40.0;  // Frágil
        };
    }

    /**
     * Aplica daño a la postura del jugador.
     * Llamar a esto cuando un jugador recibe un golpe normal o un combo.
     */
    public void damagePoise(Player player, double amount) {
        double maxPoise = getMaxPoise(player);
        PoiseData data = activePoise.computeIfAbsent(player.getUniqueId(), k -> createPoiseData(player, maxPoise));

        data.lastHitTime = System.currentTimeMillis();
        data.currentPoise -= amount;

        if (data.currentPoise <= 0) {
            triggerStagger(player, data, maxPoise);
        } else {
            updateBossBar(data, maxPoise);
        }
    }

    /**
     * 💥 ESTADO DE ATURDIMIENTO (STAGGER)
     * La guardia del jugador se ha roto.
     */
    private void triggerStagger(Player player, PoiseData data, double maxPoise) {
        // 1. Penalización Física (Aturdimiento por 1.5s)
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 30, 5, false, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20, 0, false, false, false));

        // 2. Feedback Audiovisual
        player.playSound(player.getLocation(), Sound.ITEM_SHIELD_BREAK, 1.0f, 0.8f);

        // 🌟 FIX: Título moderno de Adventure API (5, 20, 5 ticks -> 250, 1000, 250 ms)
        Component mainTitle = mm.deserialize("<bold><red>¡GUARDIA ROTA!</red></bold>");
        Component subTitle = mm.deserialize("<gray>Estás aturdido...</gray>");
        Title.Times times = Title.Times.times(Duration.ofMillis(250), Duration.ofMillis(1000), Duration.ofMillis(250));
        player.showTitle(Title.title(mainTitle, subTitle, times));

        // 3. Reinicio de Postura (Se llena al instante, pero pagó el precio)
        data.currentPoise = maxPoise;
        updateBossBar(data, maxPoise);

        // Cambiamos el color de la barra a rojo un segundo para indicar el Stagger
        data.bossBar.setColor(BarColor.RED);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (activePoise.containsKey(player.getUniqueId())) data.bossBar.setColor(BarColor.YELLOW);
        }, 30L);
    }

    private PoiseData createPoiseData(Player player, double maxPoise) {
        PoiseData data = new PoiseData();
        data.currentPoise = maxPoise;
        data.lastHitTime = System.currentTimeMillis();
        // Creamos una BossBar amarilla nativa de Spigot
        data.bossBar = Bukkit.createBossBar("§e🛡 Postura", BarColor.YELLOW, BarStyle.SEGMENTED_10);
        data.bossBar.addPlayer(player);
        return data;
    }

    private void updateBossBar(PoiseData data, double maxPoise) {
        double progress = Math.max(0.0, Math.min(1.0, data.currentPoise / maxPoise));
        data.bossBar.setProgress(progress);

        // Si está a punto de romperse, la ponemos roja
        if (progress < 0.3) {
            data.bossBar.setColor(BarColor.RED);
        } else {
            data.bossBar.setColor(BarColor.YELLOW);
        }
    }

    /**
     * 🔄 BUCLE LIGERO DE REGENERACIÓN
     * Se ejecuta 2 veces por segundo. Solo regenera a quienes no han recibido golpes recientes.
     */
    private void startRegenTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            long now = System.currentTimeMillis();

            for (Map.Entry<UUID, PoiseData> entry : activePoise.entrySet()) {
                Player player = Bukkit.getPlayer(entry.getKey());
                PoiseData data = entry.getValue();

                if (player == null || !player.isOnline()) {
                    data.bossBar.removeAll();
                    activePoise.remove(entry.getKey());
                    continue;
                }

                double maxPoise = getMaxPoise(player);

                // Si pasaron 3 segundos sin daño, regeneramos agresivamente (20 pts por tick)
                if (now - data.lastHitTime > REGEN_DELAY_MS && data.currentPoise < maxPoise) {
                    data.currentPoise = Math.min(maxPoise, data.currentPoise + 20.0);
                    updateBossBar(data, maxPoise);
                }

                // Ocultar BossBar si la postura está al 100% y pasó el peligro
                if (data.currentPoise >= maxPoise && now - data.lastHitTime > REGEN_DELAY_MS + 2000L) {
                    data.bossBar.removeAll();
                    activePoise.remove(entry.getKey());
                }
            }
        }, 20L, 10L); // Empieza en 1 seg, repite cada 0.5 seg (10 ticks)
    }
}