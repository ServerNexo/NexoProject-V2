package me.nexo.items.estaciones;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.utils.NexoColor;
import me.nexo.items.NexoItems;
import me.nexo.items.dtos.ReforgeDTO;
import me.nexo.items.dtos.ToolDTO;
import me.nexo.items.dtos.WeaponDTO;
import me.nexo.items.managers.FileManager;
import me.nexo.items.managers.ItemManager;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * 🎒 NexoItems - Mesa de Reforjas Aleatorias (Arquitectura Enterprise)
 */
@Singleton
public class ReforjaListener implements Listener {

    private final NexoItems plugin;
    private final FileManager fileManager;
    private final Random random = new Random();

    // 🌟 FIX: Declaramos la variable para nuestro ItemManager inyectado
    private final ItemManager itemManager;

    // 🛡️ PATRÓN ENTERPRISE: InventoryHolder Personalizado (Inhackeable)
    public static class ReforjaMenuHolder implements InventoryHolder {
        private final Inventory inventory;
        public ReforjaMenuHolder(net.kyori.adventure.text.Component title) {
            this.inventory = Bukkit.createInventory(this, 27, title);
        }
        @Override
        public Inventory getInventory() { return inventory; }
    }

    // 💉 PILAR 3: Inyección de Dependencias
    @Inject
    public ReforjaListener(NexoItems plugin, ItemManager itemManager) { // 🌟 FIX: Inyectamos ItemManager
        this.plugin = plugin;
        this.fileManager = plugin.getFileManager();
        this.itemManager = itemManager; // 🌟 FIX: Guardamos la instancia
    }

    public void abrirMenu(Player jugador) {
        // 🌟 Título Seguro a Component
        net.kyori.adventure.text.Component tituloFormat = CrossplayUtils.parseCrossplay(jugador, "&#ff00ff🔮 <bold>MESA DE REFORJA</bold>");

        ReforjaMenuHolder holder = new ReforjaMenuHolder(tituloFormat);
        Inventory inv = holder.getInventory();

        ItemStack cristal = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta metaCristal = cristal.getItemMeta();
        if (metaCristal != null) {
            metaCristal.displayName(CrossplayUtils.parseCrossplay(jugador, " "));
            cristal.setItemMeta(metaCristal);
        }

        for (int i = 0; i < 27; i++) {
            inv.setItem(i, cristal);
        }

        inv.setItem(11, new ItemStack(Material.AIR)); // Slot Arma
        inv.setItem(15, new ItemStack(Material.AIR)); // Slot Material

        ItemStack yunque = new ItemStack(Material.SMITHING_TABLE);
        ItemMeta metaYunque = yunque.getItemMeta();
        if (metaYunque != null) {
            metaYunque.displayName(CrossplayUtils.parseCrossplay(jugador, "&#00f5ff✨ <bold>ALTERAR MATRIZ (REFORJAR)</bold>"));

            // 🌟 Lore seguro para Bedrock y Java
            List<String> loreRaw = List.of(
                    "&#E6CCFFAltera aleatoriamente los",
                    "&#E6CCFFatributos base de tu activo.",
                    "",
                    "&#FF3366[!] La reforja anterior se perderá.",
                    "&#00f5ff► Clic para procesar"
            );
            List<String> loreFormateado = loreRaw.stream()
                    .map(line -> LegacyComponentSerializer.legacySection().serialize(NexoColor.parse(line)))
                    .collect(Collectors.toList());
            metaYunque.setLore(loreFormateado);

            yunque.setItemMeta(metaYunque);
        }
        inv.setItem(13, yunque);

        jugador.openInventory(inv);
    }

    @EventHandler
    public void alHacerClic(InventoryClickEvent event) {
        // 🛡️ Verificación Inhackeable
        if (!(event.getInventory().getHolder() instanceof ReforjaMenuHolder)) return;
        if (event.getClickedInventory() == null) return;

        Player jugador = (Player) event.getWhoClicked();

        // 🛡️ PROTECCIÓN ANTI-DUPE (Shift-Click bloqueado)
        if (event.isShiftClick() && event.getClickedInventory().equals(jugador.getInventory())) {
            event.setCancelled(true);
            CrossplayUtils.sendMessage(jugador, "&#FF5555⚠️ Utiliza el clic normal para colocar los objetos.");
            return;
        }

        int slot = event.getRawSlot();

        // Si hace clic en el menú superior
        if (event.getClickedInventory().equals(event.getInventory())) {
            if (slot != 11 && slot != 15 && slot != 13) {
                event.setCancelled(true);
                return;
            }

            // 🌟 LÓGICA DE REFORJA
            if (slot == 13) {
                event.setCancelled(true);
                Inventory inv = event.getInventory();
                ItemStack arma = inv.getItem(11);
                ItemStack material = inv.getItem(15);

                if (arma == null || arma.getType() == Material.AIR) {
                    CrossplayUtils.sendMessage(jugador, "&#FF5555[!] Inserta un activo válido en la ranura izquierda.");
                    jugador.playSound(jugador.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    return;
                }

                // 🌟 FIX: Cambiamos ItemManager.llaveMaterialMejora -> itemManager.llaveMaterialMejora
                if (material == null || !material.hasItemMeta() ||
                        !material.getItemMeta().getPersistentDataContainer().has(itemManager.llaveMaterialMejora, PersistentDataType.BYTE)) {
                    CrossplayUtils.sendMessage(jugador, "&#FF5555[!] Necesitas Polvo de Mejora en la ranura derecha.");
                    jugador.playSound(jugador.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    return;
                }

                var pdc = arma.getItemMeta().getPersistentDataContainer();

                // 🌟 FIX: Instanciado en vez de estático
                boolean esArma = pdc.has(itemManager.llaveWeaponId, PersistentDataType.STRING);
                boolean esHerramienta = pdc.has(itemManager.llaveHerramientaId, PersistentDataType.STRING);

                if (!esArma && !esHerramienta) {
                    CrossplayUtils.sendMessage(jugador, "&#FF5555[!] Este activo no soporta modificaciones de matriz (Reforja).");
                    jugador.playSound(jugador.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    return;
                }

                String claseItem = "Cualquiera";
                if (esArma) {
                    WeaponDTO armaDto = fileManager.getWeaponDTO(pdc.get(itemManager.llaveWeaponId, PersistentDataType.STRING)); // 🌟 FIX
                    if (armaDto != null) claseItem = armaDto.claseRequerida();
                } else {
                    ToolDTO toolDto = fileManager.getToolDTO(pdc.get(itemManager.llaveHerramientaId, PersistentDataType.STRING)); // 🌟 FIX
                    if (toolDto != null) claseItem = toolDto.profesion();
                }

                // Filtrar reforjas compatibles
                List<ReforgeDTO> reforjasCompatibles = new ArrayList<>();
                if (fileManager.getReforjas().getConfigurationSection("reforjas") != null) {
                    for (String key : fileManager.getReforjas().getConfigurationSection("reforjas").getKeys(false)) {
                        ReforgeDTO dto = fileManager.getReforgeDTO(key);
                        if (dto != null && (dto.aplicaAClase(claseItem) || dto.aplicaAClase("Cualquiera"))) {
                            reforjasCompatibles.add(dto);
                        }
                    }
                }

                if (reforjasCompatibles.isEmpty()) {
                    CrossplayUtils.sendMessage(jugador, "&#FF5555[!] El sistema no detecta reforjas compatibles para la clase: " + claseItem);
                    jugador.playSound(jugador.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    return;
                }

                // Consumir material
                material.setAmount(material.getAmount() - 1);

                // Tirada RNG
                ReforgeDTO reforjaElegida = reforjasCompatibles.get(random.nextInt(reforjasCompatibles.size()));

                // Aplicar reforja
                // 🌟 FIX: Llamada instanciada en vez de estática
                ItemStack armaReforjada = itemManager.aplicarReforja(arma, reforjaElegida.id());
                inv.setItem(11, armaReforjada);

                CrossplayUtils.sendMessage(jugador, "&#55FF55[✓] Matriz Alterada. Nuevo prefijo: " + reforjaElegida.prefijoColor() + reforjaElegida.nombre());
                jugador.playSound(jugador.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1f);
                jugador.playSound(jugador.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 2f);
            }
        }
    }

    @EventHandler
    public void alCerrar(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof ReforjaMenuHolder)) return;

        Player jugador = (Player) event.getPlayer();
        Inventory inv = event.getInventory();

        ItemStack arma = inv.getItem(11);
        ItemStack material = inv.getItem(15);

        // 🛡️ PROTECCIÓN ANTI-VOID
        HashMap<Integer, ItemStack> sobrantes = new HashMap<>();

        if (arma != null && arma.getType() != Material.AIR) {
            sobrantes.putAll(jugador.getInventory().addItem(arma));
        }
        if (material != null && material.getType() != Material.AIR) {
            sobrantes.putAll(jugador.getInventory().addItem(material));
        }

        for (ItemStack drop : sobrantes.values()) {
            jugador.getWorld().dropItemNaturally(jugador.getLocation(), drop);
        }
    }
}