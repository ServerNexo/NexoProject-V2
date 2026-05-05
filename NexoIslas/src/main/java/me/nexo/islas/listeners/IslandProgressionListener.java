package me.nexo.islas.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
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
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 📈 Motor de Progresión (RPG & Anti-Abusos)
 * Rendimiento: Folia-Ready, RNG seguro, y Validaciones O(1) de Entorno.
 */
@Singleton
public class IslandProgressionListener implements Listener {

    private final NexoIslas plugin;
    private final IslandManager islandManager;
    private final IslandLevelEngine levelEngine;
    private final CrossplayUtils crossplayUtils;
    private final NamespacedKey wealthKey;

    @Inject
    public IslandProgressionListener(NexoIslas plugin, IslandManager islandManager, IslandLevelEngine levelEngine, CrossplayUtils crossplayUtils) {
        this.plugin = plugin;
        this.islandManager = islandManager;
        this.levelEngine = levelEngine;
        this.crossplayUtils = crossplayUtils;
        this.wealthKey = new NamespacedKey(plugin, "island_wealth");

        plugin.getServer().getPluginManager().registerEvents(this, plugin); // Auto-registro
    }

    /**
     * 🛡️ FILTRO MAESTRO ANTI-ABUSOS Y CO-OP
     * Garantiza que la XP vaya a la isla donde el jugador está parado,
     * SIEMPRE Y CUANDO pertenezca a esa isla.
     */
    private IslandProfile getValidatedProfile(Player player) {
        Location loc = player.getLocation();
        if (loc.getWorld() == null || !loc.getWorld().getName().startsWith("island_")) return null;

        // Buscamos la isla física en la que está parado
        IslandProfile worldProfile = islandManager.getIslandAt(loc);
        if (worldProfile == null) return null;

        // Si es un visitante (no es miembro), no puede farmear para esta isla
        if (!worldProfile.isMember(player.getUniqueId())) return null;

        return worldProfile; // Retornamos la isla actual para sumarle los puntos a ella
    }

    // ==========================================
    // ⛏️ 1. FARMEO (MINERÍA Y AGRICULTURA)
    // ==========================================
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFarmAndMine(BlockBreakEvent event) {
        Player player = event.getPlayer();

        IslandProfile profile = getValidatedProfile(player);
        if (profile == null) return;

        Material blockType = event.getBlock().getType();

        // 🌟 XP de Minería/Agricultura
        double xpBase = switch (blockType) {
            case WHEAT, POTATOES, CARROTS -> 0.5;
            case COAL_ORE, DEEPSLATE_COAL_ORE, COPPER_ORE -> 1.0;
            case IRON_ORE, DEEPSLATE_IRON_ORE -> 2.0;
            case GOLD_ORE, DEEPSLATE_GOLD_ORE -> 3.0;
            case DIAMOND_ORE, DEEPSLATE_DIAMOND_ORE, EMERALD_ORE -> 10.0;
            case STONE, COBBLESTONE, DEEPSLATE -> 0.1;
            case OAK_LOG, BIRCH_LOG, SPRUCE_LOG, JUNGLE_LOG, ACACIA_LOG, DARK_OAK_LOG -> 0.5;
            default -> 0.0;
        };

        if (xpBase <= 0) return;

        // Multiplicador de AuraSkills/Mejoras del perfil
        double finalXp = xpBase * profile.getRealXpBonus();
        levelEngine.addXp(profile, finalXp);

        // 💎 DROP RNG (0.5% de chance)
        if (ThreadLocalRandom.current().nextDouble() <= 0.005) {
            Bukkit.getRegionScheduler().execute(plugin, event.getBlock().getLocation(), () -> {
                dropWealthCrystal(event.getBlock().getLocation(), player);
            });
        }
    }

    // ==========================================
    // ⚔️ 2. COMBATE (ENTIDADES)
    // ==========================================
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMobKill(EntityDeathEvent event) {
        Player player = event.getEntity().getKiller();
        if (player == null) return;

        IslandProfile profile = getValidatedProfile(player);
        if (profile == null) return;

        // 🌟 XP de Combate
        double xpBase = switch (event.getEntityType()) {
            case COW, PIG, SHEEP, CHICKEN, RABBIT -> 1.0;
            case ZOMBIE, SKELETON, SPIDER, CREEPER, SLIME -> 2.5;
            case ENDERMAN, BLAZE, MAGMA_CUBE -> 5.0;
            case IRON_GOLEM, RAVAGER -> 15.0;
            case WITHER -> 1000.0;
            default -> 0.0;
        };

        if (xpBase <= 0) return;

        double finalXp = xpBase * profile.getRealXpBonus();
        levelEngine.addXp(profile, finalXp);

        // 💎 DROP RNG COMBATE (0.5% de chance)
        if (ThreadLocalRandom.current().nextDouble() <= 0.005) {
            Bukkit.getRegionScheduler().execute(plugin, event.getEntity().getLocation(), () -> {
                dropWealthCrystal(event.getEntity().getLocation(), player);
            });
        }
    }

    // ==========================================
    // 🎣 3. PESCA (RECOLECCIÓN)
    // ==========================================
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;

        Player player = event.getPlayer();
        IslandProfile profile = getValidatedProfile(player);
        if (profile == null) return;

        // 🌟 XP de Pesca (Fija porque pescar toma tiempo)
        double xpBase = 15.0;
        double finalXp = xpBase * profile.getRealXpBonus();
        levelEngine.addXp(profile, finalXp);

        // 💎 DROP RNG PESCA (1% de chance de sacar un cristal del agua)
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
            crossplayUtils.sendMessage(player, "&#FF5555[x] Solo puedes depositar valor siendo miembro de esta isla.");
            return;
        }

        item.setAmount(item.getAmount() - 1);
        profile.addValue(value);

        islandManager.saveIslandProfileAsync(profile);

        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1f);
        crossplayUtils.sendMessage(player, "&#FFAA00📈 ¡Cristal fusionado con el núcleo! Valor de la isla: &#55FF55" + profile.getValue() + " Puntos");
    }
}