package me.nexo.mechanics.managers;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.mechanics.NexoMechanics;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vindicator;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

/**
 * 🕵️ NexoMechanics - Gestor de Contrabando y Leviatanes (Arquitectura Enterprise)
 * Escanea inventarios asíncronamente y castiga de forma segura mediante EntityScheduler.
 */
@Singleton
public class ContrabandManager {

    private final NexoMechanics plugin;
    private final NamespacedKey contrabandKey;

    // Motor asíncrono para el escaneo masivo de inventarios
    private final ExecutorService scannerExecutor = Executors.newVirtualThreadPerTaskExecutor();

    @Inject
    public ContrabandManager(NexoMechanics plugin) {
        this.plugin = plugin;
        // Creamos la llave única para registrar el contrabando nativamente
        this.contrabandKey = new NamespacedKey(plugin, "contraband_expiry");
    }

    // ==========================================
    // 💉 INYECCIÓN DE CONTRABANDO (Para crear ítems)
    // ==========================================

    /**
     * Convierte cualquier ItemStack en un ítem de contrabando.
     * @param item El ítem a modificar.
     * @param durationMillis Cuánto tiempo tiene el jugador antes de que expire (Ej. 60000 = 60s)
     */
    public void applyContrabandComponent(ItemStack item, long durationMillis) {
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        long expiryTime = System.currentTimeMillis() + durationMillis;

        meta.getPersistentDataContainer().set(contrabandKey, PersistentDataType.LONG, expiryTime);
        // Opcional: Podrías añadir un lore aquí para avisar al jugador del peligro

        item.setItemMeta(meta);
    }

    // ==========================================
    // 👁️ EL OJO DEL NEXO (Escáner Asíncrono)
    // ==========================================

    /**
     * Este método debe llamarse desde un Scheduler recurrente (Ej. cada 3 segundos).
     * Todo el cálculo se hace en un Hilo Virtual.
     */
    public void tickScanner(long currentTimeMillis) {
        scannerExecutor.submit(() -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                scanPlayerInventory(player, currentTimeMillis);
            }
        });
    }

    private void scanPlayerInventory(Player player, long currentTimeMillis) {
        ItemStack[] contents = player.getInventory().getContents();

        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || item.isEmpty() || !item.hasItemMeta()) continue;

            ItemMeta meta = item.getItemMeta();
            Long expiryTime = meta.getPersistentDataContainer().get(contrabandKey, PersistentDataType.LONG);

            if (expiryTime != null) {
                // Verificamos si la fecha de caducidad ya pasó
                if (currentTimeMillis >= expiryTime) {
                    final int slotToRemove = i;
                    // 🌟 REGLA FOLIA: Modificar inventarios y spawnear entidades DEBE ser síncrono
                    executePunishment(player, slotToRemove);
                }
            }
        }
    }

    // ==========================================
    // ⚔️ CASTIGO FÍSICO (Entity Thread Seguro)
    // ==========================================

    private void executePunishment(Player player, int slot) {
        player.getScheduler().run(plugin, task -> {
            if (!player.isOnline()) return;

            // 1. Borramos el ítem de contrabando caducado
            ItemStack item = player.getInventory().getItem(slot);
            if (item != null && item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(contrabandKey)) {
                player.getInventory().setItem(slot, null);
            } else {
                return; // El ítem ya no está (el jugador lo tiró en el milisegundo entre hilos)
            }

            // 2. Efectos visuales de confiscación
            player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.05);
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 0.5f);

            // Reemplazar este texto por tu sistema de mensajes CrossplayUtils
            player.sendMessage("§c[!] ¡Tu contrabando ha expirado! Las fuerzas del Nexo te han localizado.");

            // 3. Spawneamos al Guardia (Vindicator)
            spawnGuard(player);
        }, null);
    }

    private void spawnGuard(Player target) {
        target.getWorld().spawn(target.getLocation(), Vindicator.class, guard -> {
            // 🌟 FIX NATIVE PAPER (Kyori Adventure API):
            // Usamos customName(Component) y el serializador nativo
            guard.customName(LegacyComponentSerializer.legacyAmpersand().deserialize("&4Guardia del Nexo"));
            guard.setCustomNameVisible(true);
            guard.setTarget(target);
            guard.setCanJoinRaid(false); // Para que no active invasiones en aldeas

            // Le damos buffs considerables
            guard.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1));
            guard.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 0));

            // Partícula de aparición
            guard.getWorld().spawnParticle(Particle.PORTAL, guard.getLocation(), 30, 0.5, 1.0, 0.5, 0.1);
            guard.getWorld().playSound(guard.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
        });
    }
}