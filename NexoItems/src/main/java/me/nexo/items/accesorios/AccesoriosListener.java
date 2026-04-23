package me.nexo.items.accesorios;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.user.UserManager; // Asumido desde el Core
import me.nexo.items.NexoItems;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🎒 NexoItems - Controlador de Accesorios y Stats (Arquitectura Enterprise Java 21)
 * Rendimiento: Cero Estáticos, Atributos Nativos 1.21.5 y Pattern Matching.
 */
@Singleton
public class AccesoriosListener implements Listener {

    // 🌟 DEPENDENCIAS PROPAGADAS
    private final AccesoriosManager manager;
    private final UserManager userManager;
    private final CrossplayUtils crossplayUtils;

    private final Map<UUID, Long> cooldownCorazon = new ConcurrentHashMap<>();

    private final NamespacedKey keyVida;
    private final NamespacedKey keyFuerza;
    private final NamespacedKey keyVelocidad;
    private final NamespacedKey keyArmadura;

    // 🛡️ PATRÓN ENTERPRISE: InventoryHolder Personalizado
    public static class AccesoriosMenuHolder implements InventoryHolder {
        private final Inventory inventory;
        public AccesoriosMenuHolder(net.kyori.adventure.text.Component title, int size) {
            this.inventory = Bukkit.createInventory(this, size, title);
        }
        @Override
        public Inventory getInventory() { return inventory; }
    }

    // 💉 PILAR 1: Inyección Estricta
    @Inject
    public AccesoriosListener(NexoItems plugin, AccesoriosManager manager, UserManager userManager, CrossplayUtils crossplayUtils) {
        this.manager = manager;
        this.userManager = userManager;
        this.crossplayUtils = crossplayUtils;
        
        this.keyVida = new NamespacedKey(plugin, "accesorio_vida");
        this.keyFuerza = new NamespacedKey(plugin, "accesorio_fuerza");
        this.keyVelocidad = new NamespacedKey(plugin, "accesorio_velocidad");
        this.keyArmadura = new NamespacedKey(plugin, "accesorio_armadura");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void alCerrarBolsa(InventoryCloseEvent event) {
        // 🛡️ Verificación Inhackeable
        if (!(event.getInventory().getHolder() instanceof AccesoriosMenuHolder)) return;

        var p = (Player) event.getPlayer();
        manager.procesarYGuardarBolsa(p, event.getInventory());
        p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 1.2f);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void alHacerClic(InventoryClickEvent event) {
        // 🛡️ Verificación Inhackeable
        if (!(event.getInventory().getHolder() instanceof AccesoriosMenuHolder)) return;

        var currentItem = event.getCurrentItem();

        // 🌟 GHOST-ITEM PROOF
        if (currentItem != null && !currentItem.isEmpty() && currentItem.getType() == Material.RED_STAINED_GLASS_PANE) {
            event.setCancelled(true);
            return;
        }

        // Evita mover ítems hacia los cristales rojos usando los números del teclado (1-9)
        if (event.getClick().name().contains("NUMBER_KEY")) {
            int slotDestino = event.getRawSlot();
            if (slotDestino < event.getView().getTopInventory().getSize()) {
                var slotItem = event.getView().getTopInventory().getItem(slotDestino);
                if (slotItem != null && !slotItem.isEmpty() && slotItem.getType() == Material.RED_STAINED_GLASS_PANE) {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        // Evita Shift-Click a los cristales
        if (event.isShiftClick() && currentItem != null && !currentItem.isEmpty() && currentItem.getType() == Material.RED_STAINED_GLASS_PANE) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void alActualizarStats(AccessoryStatsUpdateEvent event) {
        var p = event.getPlayer();
        Map<AccessoryDTO.StatType, Double> stats = event.getStats();

        // 🌟 MODERNIZACIÓN PAPER 1.21.5: Nuevos nombres de Atributos
        aplicarAtributo(p, Attribute.MAX_HEALTH, keyVida, stats.getOrDefault(AccessoryDTO.StatType.VIDA, 0.0));
        aplicarAtributo(p, Attribute.ATTACK_DAMAGE, keyFuerza, stats.getOrDefault(AccessoryDTO.StatType.FUERZA, 0.0));
        aplicarAtributo(p, Attribute.MOVEMENT_SPEED, keyVelocidad, stats.getOrDefault(AccessoryDTO.StatType.VELOCIDAD, 0.0));
        aplicarAtributo(p, Attribute.ARMOR, keyArmadura, stats.getOrDefault(AccessoryDTO.StatType.ARMADURA, 0.0));

        int energiaExtra = stats.getOrDefault(AccessoryDTO.StatType.ENERGIA_CUSTOM, 0.0).intValue();
        
        // 🌟 INYECCIÓN DE DEPENDENCIAS: Uso de la instancia inyectada
        var user = userManager.getUserLocal(p.getUniqueId());
        if (user != null) {
            user.setEnergiaExtraAccesorios(energiaExtra);
        }

        crossplayUtils.sendMessage(p, "&#55FF55[✓] <bold>MATRIZ RECALIBRADA</bold> | Poder Nexo: &#00E5FF" + event.getNexoPower());
    }

    private void aplicarAtributo(Player p, Attribute atributo, NamespacedKey key, double valor) {
        AttributeInstance instancia = p.getAttribute(atributo);
        if (instancia == null) return;

        // Limpiamos los modificadores anteriores
        for (var mod : instancia.getModifiers()) {
            if (mod.getKey().equals(key)) {
                instancia.removeModifier(mod);
            }
        }

        // 🌟 MODERNIZACIÓN PAPER 1.21.5: EquipmentSlotGroup.ANY obligatorio
        if (valor > 0) {
            var modificador = new AttributeModifier(key, valor, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY);
            instancia.addModifier(modificador);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void alMorir(EntityResurrectEvent event) {
        // 🌟 JAVA 21: Pattern Matching
        if (event.getEntity() instanceof Player p) {
            if (event.isCancelled() && manager.usuariosCorazonNexo.contains(p.getUniqueId())) {

                long ahora = System.currentTimeMillis();
                long cooldownMilis = 3600 * 1000L; // 1 Hora

                if (!cooldownCorazon.containsKey(p.getUniqueId()) || (ahora - cooldownCorazon.get(p.getUniqueId())) > cooldownMilis) {

                    event.setCancelled(false); // Revivimos al jugador
                    cooldownCorazon.put(p.getUniqueId(), ahora);

                    var healthAttr = p.getAttribute(Attribute.MAX_HEALTH); // 🌟 Actualizado
                    if (healthAttr != null) {
                        p.setHealth(healthAttr.getValue() * 0.5); // Cura el 50%
                    }

                    p.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, p.getLocation(), 150);
                    p.playSound(p.getLocation(), Sound.ITEM_TOTEM_USE, 1f, 0.5f);

                    crossplayUtils.sendTitle(p, "&#FF5555¡MILAGRO DEL NEXO!", "&#E6CCFFEl Corazón del Nexo ha evitado tu muerte.");
                }
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        cooldownCorazon.remove(event.getPlayer().getUniqueId());
    }
}