package me.nexo.factories.menu;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.menus.NexoMenu;
import me.nexo.factories.NexoFactories;
import me.nexo.factories.core.ActiveFactory;
import me.nexo.factories.managers.FactoryManager;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * 🏭 NexoFactories - Compilador Lógico de Máquinas (Arquitectura Enterprise Java 21)
 * Rendimiento: Inyección Transitiva, editMeta nativo y Prevención de Robo de Ítems.
 */
public class LogicMenu extends NexoMenu {

    // 🌟 DEPENDENCIAS PROPAGADAS
    private final NexoFactories plugin;
    private final FactoryManager factoryManager;
    private final CrossplayUtils crossplayUtils;

    private final ActiveFactory factory;

    // 🌟 OPTIMIZACIÓN: Listas Inmutables Nativas (Java 21)
    private final List<String> conditions = List.of("NONE", "ENERGY_>_50", "ENERGY_>_20", "STORAGE_<_100", "STORAGE_<_500");
    private final List<String> actions = List.of("NONE", "START_MACHINE", "PAUSE_MACHINE");

    private int currentConditionIndex = 0;
    private int currentActionIndex = 0;

    // 💉 PILAR 1: Inyección Transitiva (Pasamos las dependencias explícitamente)
    public LogicMenu(Player player, NexoFactories plugin, FactoryManager factoryManager, CrossplayUtils crossplayUtils, ActiveFactory factory) {
        super(player, crossplayUtils); // 🌟 FIX ERROR SUPER: Pasamos CrossplayUtils a la superclase
        this.plugin = plugin;
        this.factoryManager = factoryManager;
        this.crossplayUtils = crossplayUtils;
        this.factory = factory;

        // Cargamos la configuración anterior de esta máquina de forma segura
        try {
            String currentLogic = factory.getJsonLogic();
            if (currentLogic != null && !currentLogic.equals("NONE")) {
                JsonObject json = JsonParser.parseString(currentLogic).getAsJsonObject();

                if (json.has("condition")) {
                    int idx = conditions.indexOf(json.get("condition").getAsString());
                    if (idx != -1) currentConditionIndex = idx;
                }

                if (json.has("action")) {
                    int idx = actions.indexOf(json.get("action").getAsString());
                    if (idx != -1) currentActionIndex = idx;
                }
            }
        } catch (Exception ignored) {
            // Falla silenciosa permitida: Si el JSON es inválido, asumimos "NONE"
        }
    }

    @Override
    public String getMenuName() {
        // 🌟 FIX: Retornamos el String puro para el motor del Core
        return "&#FF5555⚙ <bold>COMPILADOR LÓGICO</bold>";
    }

    @Override
    public int getSlots() {
        return 27;
    }

    @Override
    public void setMenuItems() {
        setFillerGlass();

        String cond = conditions.get(currentConditionIndex);
        String act = actions.get(currentActionIndex);

        // Ítem 1: Sensor de Entrada (PAPER NATIVE BUILDER)
        var sensor = new ItemStack(Material.COMPARATOR);
        sensor.editMeta(meta -> {
            meta.displayName(crossplayUtils.parseCrossplay(player, "&#00f5ff📡 <bold>SENSOR DE ENTRADA</bold>"));
            meta.lore(List.of(
                    crossplayUtils.parseCrossplay(player, "&#E6CCFFSelecciona la condición que"),
                    crossplayUtils.parseCrossplay(player, "&#E6CCFFdisparará el evento."),
                    net.kyori.adventure.text.Component.empty(),
                    crossplayUtils.parseCrossplay(player, "&#E6CCFFActual: &#00f5ff" + cond),
                    net.kyori.adventure.text.Component.empty(),
                    crossplayUtils.parseCrossplay(player, "&#00f5ff► Clic para alternar")
            ));
        });
        inventory.setItem(11, sensor);

        // Ítem 2: Operación de Respuesta
        var actionItem = new ItemStack(Material.REDSTONE_TORCH);
        actionItem.editMeta(meta -> {
            meta.displayName(crossplayUtils.parseCrossplay(player, "&#FFAA00⚡ <bold>OPERACIÓN DE RESPUESTA</bold>"));
            meta.lore(List.of(
                    crossplayUtils.parseCrossplay(player, "&#E6CCFFSelecciona lo que hará la máquina"),
                    crossplayUtils.parseCrossplay(player, "&#E6CCFFal cumplirse la condición."),
                    net.kyori.adventure.text.Component.empty(),
                    crossplayUtils.parseCrossplay(player, "&#E6CCFFActual: &#FFAA00" + act),
                    net.kyori.adventure.text.Component.empty(),
                    crossplayUtils.parseCrossplay(player, "&#FFAA00► Clic para alternar")
            ));
        });
        inventory.setItem(15, actionItem);

        // Ítem 3: Guardar Script
        var saveItem = new ItemStack(Material.LIME_DYE);
        saveItem.editMeta(meta -> {
            meta.displayName(crossplayUtils.parseCrossplay(player, "&#55FF55[✓] <bold>COMPILAR SCRIPT</bold>"));
            meta.lore(List.of(
                    crossplayUtils.parseCrossplay(player, "&#E6CCFFGuarda los cambios en el chip"),
                    crossplayUtils.parseCrossplay(player, "&#E6CCFFlógico de esta maquinaria.")
            ));
        });
        inventory.setItem(22, saveItem);
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        // 🛑 FIX CRÍTICO: ¡Prevención absoluta de robo de ítems del menú!
        event.setCancelled(true);

        int slot = event.getRawSlot();

        if (slot == 11) {
            currentConditionIndex = (currentConditionIndex + 1) % conditions.size();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
            setMenuItems(); // Actualizamos la vista O(1)

        } else if (slot == 15) {
            currentActionIndex = (currentActionIndex + 1) % actions.size();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
            setMenuItems(); // Actualizamos la vista O(1)

        } else if (slot == 22) {
            String cond = conditions.get(currentConditionIndex);
            String act = actions.get(currentActionIndex);

            if (cond.equals("NONE") || act.equals("NONE")) {
                factory.setJsonLogic("NONE");
            } else {
                var json = new JsonObject();
                json.addProperty("condition", cond);
                json.addProperty("action", act);
                factory.setJsonLogic(json.toString());
            }

            // 🌟 USO DE DEPENDENCIA INYECTADA
            factoryManager.saveFactoryStatusAsync(factory);

            player.closeInventory();
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f);

            crossplayUtils.sendMessage(player, "&#55FF55[✓] Script lógico compilado e inyectado en el procesador de la máquina.");
        }
    }
}