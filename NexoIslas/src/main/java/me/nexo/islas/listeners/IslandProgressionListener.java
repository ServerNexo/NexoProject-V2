package me.nexo.islas.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.api.ServiceManager;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.islas.NexoIslas;
import me.nexo.islas.data.IslandProfile;
import me.nexo.islas.managers.IslandLevelEngine;
import me.nexo.islas.managers.IslandManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 📈 Motor de Progresión (RPG, Anti-Abusos y NexoCore Hooks)
 * Rendimiento: Folia-Ready, RNG seguro, YML Parsing dinámico y XP O(1).
 */
@Singleton
public class IslandProgressionListener implements Listener {

    private final NexoIslas plugin;
    private final IslandManager islandManager;
    private final IslandLevelEngine levelEngine;
    private final CrossplayUtils crossplayUtils;
    private final ServiceManager serviceManager;
    private final NamespacedKey wealthKey;

    @Inject
    public IslandProgressionListener(NexoIslas plugin, IslandManager islandManager, IslandLevelEngine levelEngine, CrossplayUtils crossplayUtils, ServiceManager serviceManager) {
        this.plugin = plugin;
        this.islandManager = islandManager;
        this.levelEngine = levelEngine;
        this.crossplayUtils = crossplayUtils;
        this.serviceManager = serviceManager;
        this.wealthKey = new NamespacedKey(plugin, "island_wealth");

        plugin.getServer().getPluginManager().registerEvents(this, plugin); // Auto-registro
    }

    // ==========================================
    // 🌟 GESTIÓN DE CACHÉ INVISIBLE (LOGIN/LOGOUT PARA EL TAB)
    // ==========================================
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Carga los datos a la RAM silenciosamente para que PlaceholderAPI tenga la info al instante
        islandManager.loadProfileToCache(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Borra la RAM de este jugador y guarda los datos al desconectarse
        islandManager.clearCacheOnQuit(event.getPlayer().getUniqueId());
    }

    // ==========================================
    // 🛡️ LÓGICA DE PROGRESIÓN MULTIMUNDO (BLUETOOTH XP)
    // ==========================================
    private IslandProfile getValidatedProfile(Player player) {
        Location loc = player.getLocation();

        // 🌍 MODO LOCAL: El jugador está físicamente dentro de un mundo de Isla
        if (loc.getWorld() != null && loc.getWorld().getName().startsWith("island_")) {

            IslandProfile worldProfile = islandManager.getIslandAt(loc);
            if (worldProfile == null) return null;

            boolean isOwner = worldProfile.getOwnerId().equals(player.getUniqueId());
            boolean isMember = worldProfile.getMembers().containsKey(player.getUniqueId());

            if (!isOwner && !isMember) return null;

            return worldProfile;
        }

        // 🌌 MODO REMOTO (Bluetooth): Buscamos su isla para enviarle XP desde las Minas o el Spawn
        return islandManager.getIslandByOwner(player.getUniqueId());
    }

    // ==========================================
    // 🛡️ ANTI-ABUSOS: MARCAR BLOQUES COLOCADOS MANUALMENTE
    // ==========================================
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Material type = event.getBlock().getType();

        // 🌟 FIX: Las semillas no se marcan como "colocadas por el jugador" para que den XP al crecer
        if (type.name().contains("SEEDS") || type.name().contains("SAPLING") ||
                type == Material.WHEAT || type == Material.CARROTS ||
                type == Material.POTATOES || type == Material.BEETROOTS ||
                type == Material.SUGAR_CANE || type == Material.NETHER_WART ||
                type == Material.COCOA_BEANS || type == Material.SWEET_BERRY_BUSH) {
            return;
        }

        event.getBlock().setMetadata("nexo_placed", new FixedMetadataValue(plugin, true));
    }

    // ==========================================
    // ⛏️ 1. FARMEO (MINERÍA Y AGRICULTURA)
    // ==========================================
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFarmAndMine(BlockBreakEvent event) {

        if (event.getBlock().hasMetadata("nexo_placed")) {
            event.getBlock().removeMetadata("nexo_placed", plugin);
            return;
        }

        // 🌟 FIX: Verifica que el cultivo esté al 100% de crecimiento antes de dar XP
        if (event.getBlock().getBlockData() instanceof org.bukkit.block.data.Ageable ageable) {
            if (ageable.getAge() != ageable.getMaximumAge()) {
                return;
            }
        }

        Player player = event.getPlayer();

        IslandProfile profile = getValidatedProfile(player);
        if (profile == null) return;

        double xpBase = levelEngine.getBlockXp(event.getBlock().getType().name());
        if (xpBase <= 0) return;

        double finalXp = xpBase * profile.getRealXpBonus();

        levelEngine.addXp(profile, player.getUniqueId(), finalXp);
        player.sendActionBar(crossplayUtils.parseCrossplay(null, "&#55FF55+" + String.format("%.1f", finalXp) + " XP de Isla"));

        if (ThreadLocalRandom.current().nextDouble() <= 0.005) {
            Bukkit.getRegionScheduler().execute(plugin, event.getBlock().getLocation(), () -> {
                dropWealthCrystal(event.getBlock().getLocation(), player);
            });
        }
    }

    // ==========================================
    // ⚔️ 2. COMBATE (ENTIDADES VANILLA Y JEFES CUSTOM)
    // ==========================================
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMobKill(EntityDeathEvent event) {
        Player player = event.getEntity().getKiller();
        if (player == null) return;

        IslandProfile profile = getValidatedProfile(player);
        if (profile == null) return;

        double xpBase = levelEngine.getMobXp(event.getEntity());
        if (xpBase <= 0) return;

        double finalXp = xpBase * profile.getRealXpBonus();
        levelEngine.addXp(profile, player.getUniqueId(), finalXp);

        player.sendActionBar(crossplayUtils.parseCrossplay(null, "&#55FF55+" + String.format("%.1f", finalXp) + " XP de Isla"));

        if (ThreadLocalRandom.current().nextDouble() <= 0.005) {
            Bukkit.getRegionScheduler().execute(plugin, event.getEntity().getLocation(), () -> {
                dropWealthCrystal(event.getEntity().getLocation(), player);
            });
        }
    }

    // ==========================================
    // 🎣 3. PESCA (CON SOPORTE EVEN MORE FISH)
    // ==========================================
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;

        org.bukkit.entity.Item caughtEntity = (org.bukkit.entity.Item) event.getCaught();
        if (caughtEntity == null) return;

        Player player = event.getPlayer();
        IslandProfile profile = getValidatedProfile(player);
        if (profile == null) return;

        double xpBase = levelEngine.getFishXp(caughtEntity.getItemStack());
        if (xpBase <= 0) xpBase = 15.0; // Fallback Vanilla

        double finalXp = xpBase * profile.getRealXpBonus();
        levelEngine.addXp(profile, player.getUniqueId(), finalXp);

        player.sendActionBar(crossplayUtils.parseCrossplay(null, "&#55FF55+" + String.format("%.1f", finalXp) + " XP de Isla"));

        serviceManager.get(me.nexo.core.api.NexoColeccionesAPI.class)
                .ifPresent(api -> api.addCollectionProgress(player.getUniqueId(), "EMF_FISH", 1));

        if (ThreadLocalRandom.current().nextDouble() <= 0.01) {
            Bukkit.getRegionScheduler().execute(plugin, player.getLocation(), () -> {
                dropWealthCrystal(player.getLocation(), player);
            });
        }
    }

    // ==========================================
    // 💎 GENERADOR DE CRISTAL DEL NEXO
    // ==========================================
    private void dropWealthCrystal(Location loc, Player player) {
        ItemStack crystal = new ItemStack(Material.EMERALD);

        crystal.editMeta(meta -> {
            meta.displayName(crossplayUtils.parseCrossplay(null, "&#55FF55💎 Cristal del Nexo (Valor)"));
            List<Component> lore = new ArrayList<>();
            lore.add(crossplayUtils.parseCrossplay(null, "&#AAAAAADropeado por: &#FFFFFF" + player.getName()));
            lore.add(Component.empty());
            lore.add(crossplayUtils.parseCrossplay(null, "&#FFAA00<bold>¡CLIC DERECHO EN EL FARO DE TU ISLA!</bold>"));
            lore.add(crossplayUtils.parseCrossplay(null, "&#E6CCFFAñade &#55FF551 Punto &#E6CCFFal valor total de la isla."));
            meta.lore(lore);

            meta.getPersistentDataContainer().set(wealthKey, PersistentDataType.INTEGER, 1);
        });

        loc.getWorld().dropItemNaturally(loc, crystal);
        loc.getWorld().playSound(loc, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.5f);
        crossplayUtils.sendMessage(player, "&#55FF55✨ ¡El esfuerzo ha dado sus frutos! Ha aparecido un Cristal del Nexo.");
    }

    // ==========================================
    // 🏦 4. DEPOSITAR VALOR EN EL NÚCLEO (BEACON)
    // ==========================================
    @EventHandler
    public void onDepositWealth(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        var block = event.getClickedBlock();
        if (block == null || block.getType() != Material.BEACON) return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || item.isEmpty() || !item.hasItemMeta()) return;

        var pdc = item.getItemMeta().getPersistentDataContainer();
        if (!pdc.has(wealthKey, PersistentDataType.INTEGER)) return;

        int value = pdc.get(wealthKey, PersistentDataType.INTEGER);

        event.setCancelled(true); // Bloqueamos la UI del faro Vanilla

        IslandProfile profile = getValidatedProfile(player);
        if (profile == null) {
            crossplayUtils.sendMessage(player, "&#FF5555[x] Solo puedes depositar valor siendo el dueño o miembro activo de esta isla.");
            return;
        }

        item.setAmount(item.getAmount() - 1);
        profile.addValue(value);

        islandManager.saveIslandProfileAsync(profile);

        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1f);
        crossplayUtils.sendMessage(player, "&#FFAA00📈 ¡Cristal fusionado con el núcleo! Valor de la isla: &#55FF55" + profile.getValue() + " Puntos");
    }
}