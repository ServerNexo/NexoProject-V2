package me.nexo.core.cataclysms;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * ☄️ NexoCore - Interceptor de Extracción de Meteoritos
 * Inyecta botín directo a la RAM del inventario y previene el acaparamiento.
 */
@Singleton
public class MeteorListener implements Listener {

    private final CataclysmManager cataclysmManager;
    private final CrossplayUtils crossplayUtils;

    // ⚡ ADN del meteorito actual
    private UUID activeMeteorId;
    
    // Caché: UUID -> Extracciones
    private final Map<UUID, Integer> extractions = new ConcurrentHashMap<>();
    
    // Cooldown anti-spam (1.5 segundos entre clicks)
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    @Inject
    public MeteorListener(CataclysmManager cataclysmManager, CrossplayUtils crossplayUtils) {
        this.cataclysmManager = cataclysmManager;
        this.crossplayUtils = crossplayUtils;
        this.activeMeteorId = cataclysmManager.getCurrentMeteorId(); // 🌟 Sincronizamos al iniciar
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onMeteorClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        if (event.getClickedBlock() == null || !cataclysmManager.isMeteorBlock(event.getClickedBlock())) return;

        // 1. Cancelamos el evento Vanilla
        event.setCancelled(true);

        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        // 2. Control de Ciclo: Limpiamos la memoria si el meteorito es uno nuevo
        if (!cataclysmManager.getCurrentMeteorId().equals(activeMeteorId)) {
            activeMeteorId = cataclysmManager.getCurrentMeteorId();
            extractions.clear();
            cooldowns.clear();
        }

        // 3. Cooldown de 1.5 segundos
        long now = System.currentTimeMillis();
        if (now - cooldowns.getOrDefault(playerUUID, 0L) < 1500) {
            crossplayUtils.sendActionBar(player, "&#FF5555⛏ Extrayendo... ¡Espera!");
            return;
        }

        // 4. Límite de extracciones (🌟 Ajustado a Max 2 por jugador)
        int currentExtractions = extractions.getOrDefault(playerUUID, 0);
        if (currentExtractions >= 2) {
            crossplayUtils.sendActionBar(player, "&#FF5555❌ Ya has extraído todo lo que podías de este meteorito.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // 5. Procesamos la extracción
        cooldowns.put(playerUUID, now);
        extractions.put(playerUUID, currentExtractions + 1);

        injectLoot(player);
        cataclysmManager.damageMeteor(); 

        // 6. Efectos Visuales (Mostrando el X/2)
        crossplayUtils.sendActionBar(player, "&#55FF55✨ ¡Fragmento extraído! (" + (currentExtractions + 1) + "/2)");
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1.0f, 0.8f);
        player.getWorld().spawnParticle(org.bukkit.Particle.CRIT, event.getClickedBlock().getLocation().add(0.5, 1, 0.5), 15, 0.5, 0.5, 0.5, 0.1);
    }

    private void injectLoot(Player player) {
        double chance = ThreadLocalRandom.current().nextDouble();
        ItemStack reward;

        if (chance <= 0.05) {
            reward = new ItemStack(Material.NETHERITE_SCRAP, 1);
        } else if (chance <= 0.30) {
            reward = new ItemStack(Material.EMERALD, ThreadLocalRandom.current().nextInt(1, 4));
        } else {
            reward = new ItemStack(Material.GOLD_INGOT, ThreadLocalRandom.current().nextInt(2, 6));
        }

        Map<Integer, ItemStack> leftover = player.getInventory().addItem(reward);

        if (!leftover.isEmpty()) {
            for (ItemStack item : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), item);
            }
            crossplayUtils.sendMessage(player, "&#FFAA00[!] Tu inventario estaba lleno. El botín cayó a tus pies.");
        }
    }
}