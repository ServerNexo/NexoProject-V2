package me.nexo.items.estaciones;

import dev.aurelium.auraskills.api.AuraSkillsApi;
import dev.aurelium.auraskills.api.skill.Skills;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.menus.NexoMenu;
import me.nexo.items.NexoItems;
import me.nexo.items.managers.ItemManager;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

/**
 * 🎒 NexoItems - Menú de Forja Cénit (Arquitectura Enterprise Java 21)
 * Rendimiento: Folia Region Sync, CompletableFuture Pipeline y Cero Estáticos.
 * Nota: Los menús son instanciados por jugador, NO usan @Singleton.
 */
public class UpgradeMenu extends NexoMenu {

    // 🌟 DEPENDENCIAS PROPAGADAS
    private final NexoItems plugin;
    private final ItemManager itemManager;
    private final CrossplayUtils crossplayUtils;
    private final AuraSkillsApi auraSkillsApi;

    // 🌟 LLAVES DESACOPLADAS O(1)
    private final NamespacedKey weaponIdKey;
    private final NamespacedKey toolIdKey;
    private final NamespacedKey evolucionLevelKey;
    private final NamespacedKey actionKey;

    public UpgradeMenu(Player player, NexoItems plugin, ItemManager itemManager, CrossplayUtils crossplayUtils) {
        super(player, crossplayUtils); // 🌟 FIX ERROR SUPER: Pasamos CrossplayUtils a la superclase
        this.plugin = plugin;
        this.itemManager = itemManager;
        this.crossplayUtils = crossplayUtils;

        this.auraSkillsApi = AuraSkillsApi.get(); // Cacheo inicial seguro
        this.weaponIdKey = new NamespacedKey("nexoitems", "weapon_id");
        this.toolIdKey = new NamespacedKey("nexoitems", "herramienta_id");
        this.evolucionLevelKey = new NamespacedKey("nexoitems", "nivel_evolucion");
        this.actionKey = new NamespacedKey(plugin, "action");
    }

    @Override
    public String getMenuName() {
        // 🌟 FIX: Retornamos el String en crudo, NexoMenu (Paper API) lo procesará nativamente.
        return "&#ff8c00⬆ <bold>FORJA CÉNIT</bold>";
    }

    @Override
    public int getSlots() {
        return 27;
    }

    @Override
    public void setMenuItems() {
        setFillerGlass(); // El fondo morado del Vacío

        // El slot 13 es la bahía de procesamiento
        inventory.setItem(13, null);

        // Botón de Evolución (Paper Native Builder)
        var btn = new ItemStack(Material.NETHER_STAR);
        btn.editMeta(meta -> {
            meta.displayName(crossplayUtils.parseCrossplay(player, "&#00f5ff✨ <bold>EVOLUCIONAR ACTIVO</bold>"));

            // 🌟 JAVA 21: Lore inmutable, sin instanciar Streams basura
            meta.lore(List.of(
                    crossplayUtils.parseCrossplay(player, "&#E6CCFFAsciende tu activo al siguiente"),
                    crossplayUtils.parseCrossplay(player, "&#E6CCFFnivel de maestría (Cénit)."),
                    net.kyori.adventure.text.Component.empty(),
                    crossplayUtils.parseCrossplay(player, "&#00f5ff► Clic para procesar")
            ));

            meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "evolve_item");
        });

        inventory.setItem(22, btn);
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        int slot = event.getRawSlot();

        // 🛡️ REGLA OMEGA: Solo permitimos interacción libre con el inventario del jugador y el slot 13 (La bahía)
        if (event.getClickedInventory() == inventory && slot != 13) {
            event.setCancelled(true);
        }

        // Si hizo clic en el botón de evolucionar
        if (slot == 22 && event.getClickedInventory() == inventory) {
            procesarEvolucion();
        }
    }

    private void procesarEvolucion() {
        var item = inventory.getItem(13);

        // 🌟 GHOST ITEM PROOF
        if (item == null || item.isEmpty() || !item.hasItemMeta()) {
            crossplayUtils.sendMessage(player, "&#FF5555[!] Inserta un activo válido en la bahía de procesamiento.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        var pdc = item.getItemMeta().getPersistentDataContainer();
        boolean isWeapon = pdc.has(weaponIdKey, PersistentDataType.STRING);
        boolean isTool = pdc.has(toolIdKey, PersistentDataType.STRING);

        if (!isWeapon && !isTool) {
            crossplayUtils.sendMessage(player, "&#FF5555[!] Este activo no soporta la Evolución Cénit.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        int nivelActual = pdc.getOrDefault(evolucionLevelKey, PersistentDataType.INTEGER, 1);

        // 🌟 LÍMITE ABSOLUTO DEL JUEGO
        if (nivelActual >= 60) {
            crossplayUtils.sendMessage(player, "&#FFAA00[!] El activo ya ha alcanzado su Cénit (Nivel Máximo 60).");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f);
            return;
        }

        // 🌟 VALIDACIÓN CONTRA LA RAMA DE HABILIDADES (AURASKILLS)
        int targetNivel = nivelActual + 1;
        int playerSkillLevel = 1;
        String skillName = "Habilidad";

        try {
            var aUser = auraSkillsApi.getUser(player.getUniqueId());
            if (aUser != null) {
                if (isWeapon) {
                    playerSkillLevel = aUser.getSkillLevel(Skills.FIGHTING);
                    skillName = "Combate";
                } else {
                    String mat = item.getType().name();
                    if (mat.contains("PICKAXE")) { playerSkillLevel = aUser.getSkillLevel(Skills.MINING); skillName = "Minería"; }
                    else if (mat.contains("AXE")) { playerSkillLevel = aUser.getSkillLevel(Skills.FORAGING); skillName = "Tala"; }
                    else if (mat.contains("HOE")) { playerSkillLevel = aUser.getSkillLevel(Skills.FARMING); skillName = "Agricultura"; }
                    else if (mat.contains("SPADE") || mat.contains("SHOVEL")) { playerSkillLevel = aUser.getSkillLevel(Skills.EXCAVATION); skillName = "Excavación"; }
                }
            }
        } catch (Exception ignored) {
            // Ignorado, asume nivel 1 en caso de error
        }

        // Bloquear la evolución si el ítem superaría la maestría del jugador
        if (targetNivel > playerSkillLevel) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            crossplayUtils.sendMessage(player, "&#FF5555[!] Nivel de Operario Insuficiente. Requiere Nivel " + targetNivel + " en " + skillName + ".");
            return;
        }

        // 🌟 PREPARACIÓN DE LA PIEZA
        // (Nota: Quitamos el ítem visualmente para evitar dupes si hacen click rápido)
        inventory.setItem(13, new ItemStack(Material.AIR));

        item.editPersistentDataContainer(data -> data.set(evolucionLevelKey, PersistentDataType.INTEGER, targetNivel));

        // 🛡️ PIPELINE ASÍNCRONO SEGURO:
        // ItemManager lo calcula en Virtual Threads, y nosotros lo reinsertamos en el menú
        itemManager.sincronizarItemAsync(item).thenAccept(itemMejorado -> {

            // FOLIA SYNC: Volver a la Región del Jugador
            player.getScheduler().run(plugin, task -> {
                inventory.setItem(13, itemMejorado);

                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1.5f);
                crossplayUtils.sendMessage(player, "&#55FF55[✓] Evolución completada. El activo ha ascendido al Nivel " + targetNivel + ".");
            }, null);
        });
    }

    // 🛡️ SALVAVIDAS: Método llamado externamente cuando se cierra el inventario
    // Asegúrate de llamarlo desde tu EventHandler InventoryCloseEvent si no tienes integrado este sistema nativamente
    public void handleClose(InventoryCloseEvent event) {
        var item = inventory.getItem(13);
        if (item != null && !item.isEmpty()) {
            player.getInventory().addItem(item).values().forEach(
                    leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover)
            );
        }
    }
}