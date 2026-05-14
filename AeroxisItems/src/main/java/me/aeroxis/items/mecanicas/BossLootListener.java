package me.aeroxis.items.mecanicas;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.aeroxis.items.AeroxisItems;
import me.aeroxis.items.managers.ItemManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * 🎁 AeroxisItems - Gestor de Botín Nativo para Jefes
 * Intercepta Jefes Nativos de AeroxisCore y les inyecta Custom Loot y Contrabando.
 * Rendimiento: Lore con Kyori Adventure API (Paper Native).
 */
@Singleton
public class BossLootListener implements Listener {

    private final AeroxisItems plugin;
    private final ItemManager itemManager;
    private final NamespacedKey bossKey;
    private final NamespacedKey contrabandKey; // 🌟 Llave maestra de contrabando

    @Inject
    public BossLootListener(AeroxisItems plugin, ItemManager itemManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;

        // Llave para identificar al jefe
        this.bossKey = new NamespacedKey("nexocore", "boss_id");
        // 🌟 Llave para marcar el botín como contrabando rastreado
        this.contrabandKey = new NamespacedKey("nexomechanics", "contraband_expiry");
    }

    @EventHandler
    public void onBossDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();

        // 1. BÚSQUEDA O(1): Verificamos al instante si la entidad es un Jefe de Nexo
        if (!entity.getPersistentDataContainer().has(bossKey, PersistentDataType.STRING)) {
            return; // Era un mob normal, lo ignoramos
        }

        String bossId = entity.getPersistentDataContainer().get(bossKey, PersistentDataType.STRING);

        // 2. PURGA VANILLA: Quitamos carne podrida, huesos, etc.
        event.getDrops().clear();
        event.setDroppedExp(500); // 🌟 Puedes escalar la XP aquí

        // 3. INYECCIÓN DE BOTÍN ENTERPRISE
        if ("REVENANT_TIER_1".equals(bossId)) {

            // 🌟 BOTÍN DE CONTRABANDO: Polvo Estelar (Ejemplo)
            ItemStack material = itemManager.crearPolvoEstelar();
            if (material != null) {
                material.setAmount(3); // Dropea 3 unidades

                // Inyectamos la mutación de Contrabando
                marcarComoContrabando(material, 300000L); // 5 minutos de tiempo de vida

                event.getDrops().add(material);
            }

            // Ítem Épico (Probabilidad del 5%)
            if (Math.random() <= 0.05) {
                // Generamos un arma real configurada en tus YMLs
                ItemStack espada = itemManager.generarArmaRPG("ESPADA_DEL_VACIO");
                if (espada != null) event.getDrops().add(espada);
            }

        } else if ("OTRO_JEFE".equals(bossId)) {
            // Lógica para otro jefe
        }
    }

    /**
     * 🕵️ Método auxiliar para inyectar la expiración a cualquier botín.
     * @param item El ItemStack que será marcado.
     * @param durationMillis Tiempo en milisegundos antes de que los guardias ataquen.
     */
    private void marcarComoContrabando(ItemStack item, long durationMillis) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        // 1. Inyectamos la llave en el PDC (El motor asíncrono de NexoMechanics lo detectará al instante)
        long expiryTime = System.currentTimeMillis() + durationMillis;
        meta.getPersistentDataContainer().set(contrabandKey, PersistentDataType.LONG, expiryTime);

        // 2. Modificamos el Lore para darle inmersión al jugador (NATIVO PAPER)
        // Usamos meta.lore() en lugar del obsoleto meta.getLore()
        List<Component> lore = meta.hasLore() && meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();

        var serializer = LegacyComponentSerializer.legacySection(); // Interpreta el símbolo §

        lore.add(serializer.deserialize("§8----------------------"));
        lore.add(serializer.deserialize("§4☠ §lCONTRABANDO RASTREADO §4☠"));
        lore.add(serializer.deserialize("§cLas fuerzas del Nexo te buscan."));
        lore.add(serializer.deserialize("§eExpira en: §f" + (durationMillis / 60000) + " minutos."));
        lore.add(serializer.deserialize("§8Véndelo rápido en el Mercado Negro."));
        lore.add(serializer.deserialize("§8----------------------"));

        // Usamos meta.lore(List) en lugar del obsoleto meta.setLore()
        meta.lore(lore);
        item.setItemMeta(meta);
    }
}