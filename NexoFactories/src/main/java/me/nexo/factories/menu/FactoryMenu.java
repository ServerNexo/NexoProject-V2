package me.nexo.factories.menu;

import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.menus.NexoMenu;
import me.nexo.core.utils.NexoColor;
import me.nexo.factories.NexoFactories;
import me.nexo.factories.core.ActiveFactory;
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
 * 🏭 NexoFactories - Interfaz de la Fábrica (Arquitectura Enterprise)
 * Rendimiento: Cero Acoplamiento a Protections, Prevención de Dupes (Mutex) y Fragmentación de Stacks.
 */
public class FactoryMenu extends NexoMenu {

    private final NexoFactories plugin;
    private final ActiveFactory factory;

    public FactoryMenu(Player player, NexoFactories plugin, ActiveFactory factory) {
        super(player);
        this.plugin = plugin;
        this.factory = factory;
    }

    @Override
    public String getMenuName() {
        return LegacyComponentSerializer.legacySection().serialize(NexoColor.parse("&#ff00ff🏭 <bold>FÁBRICA: " + factory.getFactoryType() + "</bold>"));
    }

    @Override
    public int getSlots() {
        return 27;
    }

    @Override
    public void setMenuItems() {
        setFillerGlass();

        // 🌟 FIX DESACOPLADO: Obtenemos la energía mediante reflexión ligera O(1)
        String energyStatus = "&#8b0000Desconectada";

        if (Bukkit.getPluginManager().isPluginEnabled("NexoProtections")) {
            try {
                Object claimManager = me.nexo.core.user.NexoAPI.getServices()
                        .get(Class.forName("me.nexo.protections.managers.ClaimManager"))
                        .orElse(null);

                if (claimManager != null) {
                    Object stone = claimManager.getClass().getMethod("getStoneById", java.util.UUID.class).invoke(claimManager, factory.getStoneId());
                    if (stone != null) {
                        double energy = (double) stone.getClass().getMethod("getCurrentEnergy").invoke(stone);
                        energyStatus = "&#00f5ff" + energy + " ⚡";
                    }
                }
            } catch (Exception ignored) {}
        }

        // Ítem 1: Información de la Fábrica
        List<String> infoLoreRaw = List.of(
                "&#E6CCFFNivel del Núcleo: &#00f5ff" + factory.getLevel(),
                "&#E6CCFFEstado: " + getStatusColor(factory.getCurrentStatus()),
                "&#E6CCFFRed Eléctrica: " + energyStatus,
                " ",
                "&#E6CCFFAutoridad: &#ff00ff" + Bukkit.getOfflinePlayer(factory.getOwnerId()).getName()
        );
        setItem(11, Material.REPEATER, "&#00f5ff📊 <bold>ESTADO DEL SISTEMA</bold>",
                infoLoreRaw.stream().map(line -> LegacyComponentSerializer.legacySection().serialize(NexoColor.parse(line))).toList());

        // Ítem 2: Catalizador / Mejora
        String catName = factory.getCatalystItem().equals("NONE") ? "&#8b0000Vacante" : "&#00f5ff" + factory.getCatalystItem();
        List<String> catLoreRaw = List.of(
                "&#E6CCFFMódulo Actual: " + catName,
                "",
                "&#FF5555(Sistema de inserción en desarrollo)"
        );
        setItem(13, Material.LODESTONE, "&#FFAA00✨ <bold>MÓDULO CATALIZADOR</bold>",
                catLoreRaw.stream().map(line -> LegacyComponentSerializer.legacySection().serialize(NexoColor.parse(line))).toList());

        // Ítem 3: Salida de Producción
        String action = factory.getStoredOutput() > 0 ? "&#55FF55► Clic para Recolectar" : "&#FF5555[!] La bandeja está vacía";
        List<String> outputLoreRaw = List.of(
                "&#E6CCFFRecursos Procesados: &#55FF55" + factory.getStoredOutput() + " und(s).",
                "",
                action
        );
        setItem(15, Material.CHEST, "&#55FF55📦 <bold>BANDEJA DE SALIDA</bold>",
                outputLoreRaw.stream().map(line -> LegacyComponentSerializer.legacySection().serialize(NexoColor.parse(line))).toList());

        // Ítem 4: Botón Lógico (Terminal)
        List<String> terminalLoreRaw = List.of(
                "&#E6CCFFPrograma el comportamiento",
                "&#E6CCFFautónomo de esta maquinaria.",
                "",
                "&#00f5ff► Clic para abrir el compilador"
        );
        setItem(22, Material.COMMAND_BLOCK, "&#FF5555⚙ <bold>TERMINAL LÓGICA</bold>",
                terminalLoreRaw.stream().map(line -> LegacyComponentSerializer.legacySection().serialize(NexoColor.parse(line))).toList());
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
            // 🌟 BLOQUEO ATÓMICO (MUTEX): Evita Dupes si 2 jugadores abren la máquina a la vez
            synchronized (factory) {
                int totalAmount = factory.getStoredOutput();

                if (totalAmount <= 0) {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    return;
                }

                // Limpiamos la base de datos inmediatamente ANTES de dar ítems (Previene dupes por crash)
                factory.clearOutput();
                plugin.getFactoryManager().saveFactoryStatusAsync(factory);

                // Preparamos el ítem base
                ItemStack baseReward = new ItemStack(Material.IRON_INGOT);
                String nexoItemId = factory.getFactoryType() + "_OUTPUT";

                try {
                    if (com.nexomc.nexo.api.NexoItems.itemFromId(nexoItemId) != null) {
                        baseReward = com.nexomc.nexo.api.NexoItems.itemFromId(nexoItemId).build();
                    }
                } catch (NoClassDefFoundError | Exception ignored) {}

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
                CrossplayUtils.sendMessage(player, "&#55FF55[✓] Has extraído " + totalAmount + " unidades de la fábrica.");

                // Refrescamos el menú para que vea que la bandeja está vacía
                setMenuItems();
            }

        } else if (slot == 22) {
            // Abrimos el menú lógico (Terminal)
            new LogicMenu(player, plugin, factory).open();
        }
    }
}