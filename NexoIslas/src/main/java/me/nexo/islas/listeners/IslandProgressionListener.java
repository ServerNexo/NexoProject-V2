package me.nexo.islas.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.islas.NexoIslas;
import me.nexo.islas.data.IslandProfile;
import me.nexo.islas.managers.IslandManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 📈 Motor de Progresión (Estilo AkumaMC)
 * Rendimiento: Matemáticas de XP en RAM O(1), Auto-LevelUp y Spawns Folia-Ready.
 */
@Singleton
public class IslandProgressionListener implements Listener {

    private final NexoIslas plugin;
    private final IslandManager islandManager;
    private final CrossplayUtils crossplayUtils;
    private final NamespacedKey wealthKey;

    // 🌟 MATEMÁTICA DE PROGRESIÓN
    // Cada cuántos puntos de actividad la isla sube de nivel y otorga 1 Stat Point
    private static final double XP_PER_LEVEL = 1000.0;

    @Inject
    public IslandProgressionListener(NexoIslas plugin, IslandManager islandManager, CrossplayUtils crossplayUtils) {
        this.plugin = plugin;
        this.islandManager = islandManager;
        this.crossplayUtils = crossplayUtils;
        this.wealthKey = new NamespacedKey(plugin, "island_wealth");
    }

    // ==========================================
    // ⛏️ 1. FARMEO Y DROPEOS (NIVEL DE ISLA)
    // ==========================================
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFarmAndMine(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!player.getWorld().getName().equals("nexo_islas_world")) return;

        Material blockType = event.getBlock().getType();

        // 🌟 Calculamos XP según el bloque
        double xpGained = switch (blockType) {
            case WHEAT, POTATOES, CARROTS -> 0.5;
            case DIAMOND_ORE, DEEPSLATE_DIAMOND_ORE, EMERALD_ORE -> 5.0;
            case STONE, COBBLESTONE -> 0.1;
            default -> 0.0;
        };

        if (xpGained <= 0) return;

        // 🌟 RENDIMIENTO AAA: Obtenemos la isla desde la memoria RAM (Caché), NO desde MySQL.
        // (Asumo que tu IslandManager tiene un método para obtener la isla cargada del jugador)
        IslandProfile profile = getCachedProfile(player);
        if (profile == null) return;

        double oldScore = profile.getActivityScore();

        // Sumamos a la RAM
        profile.addValorActividad(xpGained);

        // Verificamos si subió de nivel
        checkLevelUp(player, profile, oldScore, profile.getActivityScore());

        // 💎 PROBABILIDAD DE DROP DEL OBJETO DE VALOR (Ej: 0.5% de chance)
        if (ThreadLocalRandom.current().nextDouble() <= 0.005) {
            // 🌟 PAPER NATIVE: Spawneamos el ítem en el hilo de la región del bloque (Folia-Ready)
            Bukkit.getRegionScheduler().execute(plugin, event.getBlock().getLocation(), () -> {
                dropWealthCrystal(event.getBlock().getLocation(), player);
            });
        }
    }

    /**
     * 🌟 LÓGICA DE LEVEL UP (STAT POINTS)
     */
    private void checkLevelUp(Player player, IslandProfile profile, double oldScore, double newScore) {
        int oldLevel = (int) (oldScore / XP_PER_LEVEL);
        int newLevel = (int) (newScore / XP_PER_LEVEL);

        if (newLevel > oldLevel) {
            int pointsEarned = newLevel - oldLevel;
            profile.addUpgradePoints(pointsEarned);

            // Efectos Inmersivos de Subida de Nivel
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            crossplayUtils.sendMessage(player, "");
            crossplayUtils.sendMessage(player, "&#00f5ff✧ &#55FF55<bold>¡NÚCLEO DE ISLA MEJORADO!</bold>");
            crossplayUtils.sendMessage(player, "&#E6CCFFTu isla ha alcanzado el nivel &#FFAA00" + newLevel + "&#E6CCFF.");
            crossplayUtils.sendMessage(player, "&#55FF55+" + pointsEarned + " Puntos de Mejora &#E6CCFF(Úsalos en el menú principal).");
            crossplayUtils.sendMessage(player, "");

            // Guardado Asíncrono de Seguridad en hitos importantes
            CompletableFuture.runAsync(() -> islandManager.saveIslandProfileAsync(profile));
        }
    }

    private void dropWealthCrystal(org.bukkit.Location loc, Player player) {
        ItemStack crystal = new ItemStack(Material.EMERALD);

        crystal.editMeta(meta -> {
            meta.displayName(crossplayUtils.parseCrossplay(null, "&#55FF55💎 Cristal de Valor de la Isla"));
            List<Component> lore = new ArrayList<>();
            lore.add(crossplayUtils.parseCrossplay(null, "&#AAAAAADropeado por: &#FFFFFF" + player.getName()));
            lore.add(Component.empty());
            lore.add(crossplayUtils.parseCrossplay(null, "&#FFAA00<bold>¡CLIC DERECHO EN EL FARO DE TU ISLA!</bold>"));
            lore.add(crossplayUtils.parseCrossplay(null, "&#E6CCFFAñade &#55FF55$1,000 &#E6CCFFal valor total de la isla."));
            meta.lore(lore);

            // Inyectamos el valor en el PDC
            meta.getPersistentDataContainer().set(wealthKey, PersistentDataType.DOUBLE, 1000.0);
        });

        loc.getWorld().dropItemNaturally(loc, crystal);
        loc.getWorld().playSound(loc, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.5f);
        crossplayUtils.sendMessage(player, "&#55FF55✨ ¡Has encontrado un Cristal de Valor farmeando!");
    }

    // ==========================================
    // 🏦 2. DEPOSITAR VALOR EN EL NÚCLEO (BEACON)
    // ==========================================
    @EventHandler
    public void onDepositWealth(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        var block = event.getClickedBlock();
        if (block == null || block.getType() != Material.BEACON) return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || item.isEmpty() || !item.hasItemMeta()) return;

        // Verificamos si es el Cristal de Valor
        var pdc = item.getItemMeta().getPersistentDataContainer();
        if (!pdc.has(wealthKey, PersistentDataType.DOUBLE)) return;

        double value = pdc.get(wealthKey, PersistentDataType.DOUBLE);

        event.setCancelled(true); // Bloqueamos la UI del faro

        IslandProfile profile = getCachedProfile(player);
        if (profile == null) return;

        // Quitamos 1 ítem de la mano
        item.setAmount(item.getAmount() - 1);

        // Sumamos la riqueza en la RAM
        profile.setWealthScore(profile.getWealthScore() + value);

        // Guardado Asíncrono por ser una transacción económica (Ítem -> Datos)
        CompletableFuture.runAsync(() -> islandManager.saveIslandProfileAsync(profile));

        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1f);
        crossplayUtils.sendMessage(player, "&#FFAA00📈 Has depositado el cristal. Valor de isla: &#55FF55$" + String.format("%,.0f", profile.getWealthScore()));
    }

    /**
     * Método auxiliar.
     * En una arquitectura O(1), tu IslandManager debe tener un Map<UUID, IslandProfile>
     * y un método rápido para retornar el perfil sin tocar MySQL.
     */
    private IslandProfile getCachedProfile(Player player) {
        // 🌟 FIX: Usamos el método correcto que ya existe en tu IslandManager
        return islandManager.getIslandByOwner(player.getUniqueId());
    }
}