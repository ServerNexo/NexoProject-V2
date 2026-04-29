package me.nexo.islas.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.islas.NexoIslas;
import me.nexo.islas.data.IslandDatabase;
import net.kyori.adventure.text.Component; // 🌟 NUEVO IMPORT
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer; // 🌟 NUEVO IMPORT
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 📈 Motor de Progresión (Estilo AkumaMC)
 * Gana XP minando/farmeando y deposita "Cristales de Valor" en el núcleo.
 */
@Singleton
public class IslandProgressionListener implements Listener {

    private final NexoIslas plugin;
    private final IslandDatabase db;
    private final NamespacedKey wealthKey; // PDC para el ítem de valor

    private final ExecutorService asyncExecutor = Executors.newVirtualThreadPerTaskExecutor();

    @Inject
    public IslandProgressionListener(NexoIslas plugin, IslandDatabase db) {
        this.plugin = plugin;
        this.db = db;
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
        double xpGained = 0;
        if (blockType == Material.WHEAT || blockType == Material.POTATOES) xpGained = 0.5;
        if (blockType == Material.DIAMOND_ORE || blockType == Material.DEEPSLATE_DIAMOND_ORE) xpGained = 5.0;
        if (blockType == Material.STONE || blockType == Material.COBBLESTONE) xpGained = 0.1;

        if (xpGained == 0) return; // No es un bloque de farmeo válido

        final double finalXp = xpGained;

        asyncExecutor.submit(() -> {
            db.loadIsland(player.getUniqueId()).thenAccept(profile -> {
                if (profile == null) return;

                // 📈 Sumamos XP al nivel de actividad
                profile.setActivityScore(profile.getActivityScore() + finalXp);
                db.saveIslandSync(profile);

                // 💎 PROBABILIDAD DE DROP DEL OBJETO DE VALOR (Ej: 0.5% de chance)
                if (ThreadLocalRandom.current().nextDouble() <= 0.005) {
                    Bukkit.getScheduler().runTask(plugin, () -> dropWealthCrystal(event.getBlock().getLocation(), player));
                }
            });
        });
    }

    private void dropWealthCrystal(org.bukkit.Location loc, Player player) {
        ItemStack crystal = new ItemStack(Material.EMERALD);
        ItemMeta meta = crystal.getItemMeta();

        // 🌟 FIX: Kyori Adventure API para el nombre
        meta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize("&a💎 Cristal de Valor de la Isla"));

        // 🌟 FIX: Kyori Adventure API para el Lore
        List<Component> lore = new ArrayList<>();
        lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize("&7Dropeado por: &f" + player.getName()));
        lore.add(Component.empty());
        lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize("&e&l¡CLIC DERECHO EN EL FARO DE TU ISLA!"));
        lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize("&7Añade &6$1,000 &7al valor total de la isla."));
        meta.lore(lore);

        // 🌟 Le inyectamos el valor en el PDC (1000 puntos de valor)
        meta.getPersistentDataContainer().set(wealthKey, PersistentDataType.DOUBLE, 1000.0);
        crystal.setItemMeta(meta);

        loc.getWorld().dropItemNaturally(loc, crystal);
        loc.getWorld().playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);
        player.sendMessage("§a✨ ¡Has encontrado un Cristal de Valor farmeando!");
    }

    // ==========================================
    // 🏦 2. DEPOSITAR VALOR EN EL NÚCLEO (BEACON)
    // ==========================================
    @EventHandler
    public void onDepositWealth(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        var block = event.getClickedBlock();
        if (block == null || block.getType() != Material.BEACON) return; // Asumimos que el centro es un BEACON

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || !item.hasItemMeta()) return;

        // Verificamos si es el Cristal de Valor leyendo el PDC
        var pdc = item.getItemMeta().getPersistentDataContainer();
        if (!pdc.has(wealthKey, PersistentDataType.DOUBLE)) return;

        double value = pdc.get(wealthKey, PersistentDataType.DOUBLE);

        event.setCancelled(true); // Cancelamos abrir la interfaz del faro

        // Sumamos a la base de datos asíncronamente
        asyncExecutor.submit(() -> {
            db.loadIsland(player.getUniqueId()).thenAccept(profile -> {
                if (profile == null) return;

                profile.setWealthScore(profile.getWealthScore() + value);
                db.saveIslandSync(profile);

                Bukkit.getScheduler().runTask(plugin, () -> {
                    // Consumir 1 ítem de la mano
                    item.setAmount(item.getAmount() - 1);
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1f);
                    player.sendMessage("§e📈 Has depositado el cristal. El valor de tu isla ahora es: §6$" + profile.getWealthScore());
                });
            });
        });
    }
}