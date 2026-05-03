package me.nexo.dungeons.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.dungeons.NexoDungeons;
import me.nexo.dungeons.instances.DungeonSlimeManager;
import me.nexo.dungeons.mechanics.AbyssBackpackManager; // 🌟 NUEVA INYECCIÓN
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 🏰 NexoDungeons - Gestor de Muerte en El Abismo (Arquitectura AAA)
 * Conectado al Backpack Virtual para pérdida selectiva real.
 */
@Singleton
public class AbyssDeathListener implements Listener {

    private final NexoDungeons plugin;
    private final CrossplayUtils crossplayUtils;
    private final DungeonSlimeManager slimeManager;
    private final AbyssBackpackManager backpackManager; // 🌟 GESTOR DE BOTÍN

    private final ExecutorService deathExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private final org.bukkit.NamespacedKey SHATTERED_KEY;

    @Inject
    public AbyssDeathListener(NexoDungeons plugin, CrossplayUtils crossplayUtils, DungeonSlimeManager slimeManager, AbyssBackpackManager backpackManager) {
        this.plugin = plugin;
        this.crossplayUtils = crossplayUtils;
        this.slimeManager = slimeManager;
        this.backpackManager = backpackManager;
        this.SHATTERED_KEY = new org.bukkit.NamespacedKey("nexo", "shattered_armor");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAbyssDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();

        // 🌟 FIX: Como isDungeonWorld no existe en SlimeManager, leemos el prefijo nativo del mundo.
        // Asegúrate de que al crear el mundo en SlimeManager lo llames algo como "dungeon_" + UUID
        String worldName = player.getWorld().getName().toLowerCase();
        if (!worldName.startsWith("dungeon_") && !worldName.startsWith("inst_")) return;

        event.setKeepInventory(true);
        event.setKeepLevel(true);
        event.getDrops().clear();

        deathExecutor.submit(() -> processAbyssDeath(player, event));
    }

    private void processAbyssDeath(Player player, PlayerDeathEvent event) {
        // Simulación de conexión futura con NexoPvP
        boolean hasBlessing = false;

        if (hasBlessing) {
            crossplayUtils.sendMessage(player, "\n&#9146FF<bold>🔮 LA BENDICIÓN DEL VACÍO TE HA SALVADO</bold>");
            crossplayUtils.sendMessage(player, "&#E6CCFFTu armadura y el 100% de tu botín han sido protegidos.");
            Bukkit.getScheduler().runTask(plugin, () -> teleportToHub(player));
            return;
        }

        // 🌟 FRACTURA FÍSICA
        Bukkit.getScheduler().runTask(plugin, () -> {
            fractureEquipment(player);

            // 🌟 CONEXIÓN REAL CON LA MOCHILA EN RAM
            List<ItemStack> abyssLoot = backpackManager.flushAndGetLoot(player.getUniqueId());
            if (abyssLoot == null) abyssLoot = new ArrayList<>();

            if (!abyssLoot.isEmpty()) {
                Collections.shuffle(abyssLoot); // Randomizamos

                int itemsToLose = Math.max(1, (int) (abyssLoot.size() * 0.20)); // 20%

                List<ItemStack> droppedLoot = new ArrayList<>();
                for (int i = 0; i < itemsToLose; i++) {
                    droppedLoot.add(abyssLoot.remove(0)); // Robamos de la mochila
                }

                // Tiramos el 20% robado físicamente al suelo de la mazmorra
                for (ItemStack drop : droppedLoot) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                }

                crossplayUtils.sendMessage(player, "\n&#FF5555<bold>💀 HAS MUERTO EN EL ABISMO</bold>");
                crossplayUtils.sendMessage(player, "&#FFaaaaTu armadura se ha fracturado y perdiste el &#FF555520% &#FFaaaade tu botín.");
            } else {
                crossplayUtils.sendMessage(player, "\n&#FF5555<bold>💀 HAS MUERTO EN EL ABISMO</bold>");
                crossplayUtils.sendMessage(player, "&#FFaaaaTu armadura se ha fracturado.");
            }

            player.playSound(player.getLocation(), Sound.ENTITY_WITHER_DEATH, 0.5f, 0.8f);
            teleportToHub(player);
        });
    }

    private void fractureEquipment(Player player) {
        ItemStack[] equipment = player.getInventory().getArmorContents();
        for (ItemStack item : equipment) {
            if (item == null || !item.hasItemMeta()) continue;

            ItemMeta meta = item.getItemMeta();
            if (meta instanceof Damageable damageable) {
                int maxDurability = item.getType().getMaxDurability();
                if (maxDurability > 0) {
                    int damageToApply = (int) (maxDurability * 0.15);
                    int currentDamage = damageable.getDamage();
                    int newDamage = currentDamage + damageToApply;

                    if (newDamage >= maxDurability) {
                        damageable.setDamage(maxDurability - 1);
                        meta.getPersistentDataContainer().set(SHATTERED_KEY, PersistentDataType.BYTE, (byte) 1);

                        List<net.kyori.adventure.text.Component> lore = meta.hasLore() ? meta.lore() : new ArrayList<>();
                        lore.add(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<dark_red><bold>☠ FRACTURADO (Requiere Herrero)</bold></dark_red>"));
                        meta.lore(lore);
                    } else {
                        damageable.setDamage(newDamage);
                    }
                    item.setItemMeta(meta);
                }
            }
        }
        player.getInventory().setArmorContents(equipment);
    }

    private void teleportToHub(Player player) {
        player.spigot().respawn();
        player.performCommand("spawn");
    }
}