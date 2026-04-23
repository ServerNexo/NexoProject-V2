package me.nexo.factories.menu;

import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.menus.NexoMenu;
import me.nexo.factories.NexoFactories;
import me.nexo.factories.core.ActiveFactory;
import me.nexo.factories.managers.FactoryManager;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;

/**
 * 🏭 NexoFactories - Interfaz de la Fábrica (Arquitectura Enterprise Java 21)
 * Rendimiento: Cero Estáticos, Prevención de Dupes (Mutex), Fragmentación de Stacks y Soft-Dependency.
 */
public class FactoryMenu extends NexoMenu {

    // 🌟 DEPENDENCIAS PROPAGADAS
    private final NexoFactories plugin;
    private final FactoryManager factoryManager;
    private final CrossplayUtils crossplayUtils;
    
    // 🌟 OPCIONAL: Instancia del Manager de Protecciones (Inyectado si existe)
    private final Object claimManager; 

    private final ActiveFactory factory;

    // 💉 PILAR 1: Inyección Transitiva
    public FactoryMenu(Player player, NexoFactories plugin, FactoryManager factoryManager, CrossplayUtils crossplayUtils, Object claimManager, ActiveFactory factory) {
        super(player);
        this.plugin = plugin;
        this.factoryManager = factoryManager;
        this.crossplayUtils = crossplayUtils;
        this.claimManager = claimManager;
        this.factory = factory;
    }

    @Override
    public String getMenuName() {
        return LegacyComponentSerializer.legacySection().serialize(
                crossplayUtils.parseCrossplay(player, "&#ff00ff🏭 <bold>FÁBRICA: " + factory.getFactoryType() + "</bold>")
        );
    }

    @Override
    public int getSlots() {
        return 27;
    }

    @Override
    public void setMenuItems() {
        setFillerGlass();

        // 🌟 FIX DESACOPLADO: Soft-Dependency Segura
        String energyStatus = "&#8b0000Desconectada";

        if (claimManager != null && Bukkit.getPluginManager().isPluginEnabled("NexoProtections")) {
            try {
                // Usamos la API del Core para obtener el NexoStone y su energía, asumiendo que ClaimManager
                // tiene un método 'getStoneById(UUID)' y 'NexoStone' tiene 'getCurrentEnergy()'.
                var stone = claimManager.getClass().getMethod("getStoneById", java.util.UUID.class).invoke(claimManager, factory.getStoneId());
                if (stone != null) {
                    double energy = (double) stone.getClass().getMethod("getCurrentEnergy").invoke(stone);
                    energyStatus = "&#00f5ff" + energy + " ⚡";
                }
            } catch (Exception ignored) {
                // Falla silenciosa si NexoProtections cambió su API
            }
        }

        // Ítem 1: Información de la Fábrica (PAPER NATIVE BUILDER)
        var infoItem = new ItemStack(Material.REPEATER);
        final String finalEnergyStatus = energyStatus;
        infoItem.editMeta(meta -> {
            meta.displayName(crossplayUtils.parseCrossplay(player, "&#00f5ff📊 <bold>ESTADO DEL SISTEMA</bold>"));
            meta.lore(List.of(
                    crossplayUtils.parseCrossplay(player, "&#E6CCFFNivel del Núcleo: &#00f5ff" + factory.getLevel()),
                    crossplayUtils.parseCrossplay(player, "&#E6CCFFEstado: " + getStatusColor(factory.getCurrentStatus())),
                    crossplayUtils.parseCrossplay(player, "&#E6CCFFRed Eléctrica: " + finalEnergyStatus),
                    net.kyori.adventure.text.Component.empty(),
                    crossplayUtils.parseCrossplay(player, "&#E6CCFFAutoridad: &#ff00ff" + Bukkit.getOfflinePlayer(factory.getOwnerId()).getName())
            ));
        });
        inventory.setItem(11, infoItem);

        // Ítem 2: Catalizador / Mejora
        String catName = factory.getCatalystItem().equals("NONE") ? "&#8b0000Vacante" : "&#00f5ff" + factory.getCatalystItem();
        var catItem = new ItemStack(Material.LODESTONE);
        catItem.editMeta(meta -> {
            meta.displayName(crossplayUtils.parseCrossplay(player, "&#FFAA00✨ <bold>MÓDULO CATALIZADOR</bold>"));
            meta.lore(List.of(
                    crossplayUtils.parseCrossplay(player, "&#E6CCFFMódulo Actual: " + catName),
                    net.kyori.adventure.text.Component.empty(),
                    crossplayUtils.parseCrossplay(player, "&#FF5555(Sistema de inserción en desarrollo)")
            ));
        });
        inventory.setItem(13, catItem);

        // Ítem 3: Salida de Producción
        String action = factory.getStoredOutput() > 0 ? "&#55FF55► Clic para Recolectar" : "&#FF5555[!] La bandeja está vacía";
        var outItem = new ItemStack(Material.CHEST);
        outItem.editMeta(meta -> {
            meta.displayName(crossplayUtils.parseCrossplay(player, "&#55FF55📦 <bold>BANDEJA DE SALIDA</bold>"));
            meta.lore(List.of(
                    crossplayUtils.parseCrossplay(player, "&#E6CCFFRecursos Procesados: &#55FF55" + factory.getStoredOutput() + " und(s)."),
                    net.kyori.adventure.text.Component.empty(),
                    crossplayUtils.parseCrossplay(player, action)
            ));
        });
        inventory.setItem(15, outItem);

        // Ítem 4: Botón Lógico (Terminal)
        var terminalItem = new ItemStack(Material.COMMAND_BLOCK);
        terminalItem.editMeta(meta -> {
            meta.displayName(crossplayUtils.parseCrossplay(player, "&#FF5555⚙ <bold>TERMINAL LÓGICA</bold>"));
            meta.lore(List.of(
                    crossplayUtils.parseCrossplay(player, "&#E6CCFFPrograma el comportamiento"),
                    crossplayUtils.parseCrossplay(player, "&#E6CCFFautónomo de esta maquinaria."),
                    net.kyori.adventure.text.Component.empty(),
                    crossplayUtils.parseCrossplay(player, "&#00f5ff► Clic para abrir el compilador")
            ));
        });
        inventory.setItem(22, terminalItem);
    }

    private String getStatusColor(String status) {
        return switch (status) {
            case "ACTIVE" -> "&#00f5ff<bold>PRODUCIENDO</bold>";
            case "NO_ENERGY" -> "&#8b0000<bold>SIN ENERGÍA</bold>";
            case "SCRIPT_PAUSED" -> "&#ff00ff<bold>EN ESPERA (SCRIPT)</bold>";
            default -> "&#E6CCFF<bold>SISTEMA APAGADO</bold>";
        };
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        event.setCancelled(true); // 🛑 BLOQUEO ABSOLUTO: Evita robo de paneles de cristal

        int slot = event.getRawSlot();

        if (slot == 15) {
            // 🌟 BLOQUEO ATÓMICO (MUTEX): Evita Dupes si 2 jugadores (con permisos) abren la máquina a la vez
            synchronized (factory) {
                int totalAmount = factory.getStoredOutput();

                if (totalAmount <= 0) {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    return;
                }

                // Limpiamos la base de datos inmediatamente ANTES de dar ítems (Previene dupes por crash)
                factory.clearOutput();
                
                // 🌟 USO DE DEPENDENCIA INYECTADA
                factoryManager.saveFactoryStatusAsync(factory);

                // Preparamos el ítem base
                ItemStack baseReward = new ItemStack(Material.IRON_INGOT);
                String nexoItemId = factory.getFactoryType() + "_OUTPUT";

                try {
                    if (com.nexomc.nexo.api.NexoItems.itemFromId(nexoItemId) != null) {
                        baseReward = com.nexomc.nexo.api.NexoItems.itemFromId(nexoItemId).build();
                    }
                } catch (NoClassDefFoundError | Exception ignored) {
                    // Ignoramos si no encuentra Nexo o el Item. Entrega lingotes de hierro.
                }

                // 🌟 ALGORITMO DE FRAGMENTACIÓN: Evita que Bukkit borre ítems por exceder 64
                int remaining = totalAmount;

                while (remaining > 0) {
                    int chunk = Math.min(remaining, 64);
                    ItemStack chunkItem = baseReward.clone();
                    chunkItem.setAmount(chunk);

                    HashMap<Integer, ItemStack> left = player.getInventory().addItem(chunkItem);
                    if (!left.isEmpty()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), left.get(0));
                    }
                    remaining -= chunk;
                }

                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);
                crossplayUtils.sendMessage(player, "&#55FF55[✓] Has extraído " + totalAmount + " unidades de la fábrica.");

                // Refrescamos el menú
                setMenuItems();
            }

        } else if (slot == 22) {
            // Abrimos el menú lógico (Terminal), inyectando las dependencias que necesita
            new LogicMenu(player, plugin, factoryManager, crossplayUtils, factory).open();
        }
    }
}