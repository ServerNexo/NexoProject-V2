package me.nexo.items.mecanicas;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.items.NexoItems;
import me.nexo.items.managers.ItemManager;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

/**
 * 🎁 NexoItems - Gestor de Botín Nativo para Jefes
 * Intercepta Jefes Nativos de NexoCore y les inyecta el Custom Loot.
 */
@Singleton
public class BossLootListener implements Listener {

    private final NexoItems plugin;
    private final ItemManager itemManager;
    private final NamespacedKey bossKey;

    @Inject
    public BossLootListener(NexoItems plugin, ItemManager itemManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;

        // 🌟 Esta es la llave que asumo que usarás en NexoCore para identificar a tus jefes
        this.bossKey = new NamespacedKey("nexocore", "boss_id");
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

            // 🌟 FIX: Usamos métodos reales de tu ItemManager

            // Ítem Garantizado (Polvo Estelar como ejemplo de material)
            ItemStack material = itemManager.crearPolvoEstelar();
            if (material != null) {
                material.setAmount(3); // Dropea 3 unidades
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
}