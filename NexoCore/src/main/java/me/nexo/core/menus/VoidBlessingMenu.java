package me.nexo.core.menus;

import me.nexo.core.config.ConfigManager;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.user.NexoUser;
import me.nexo.core.user.UserManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * 🏛️ Nexo Network - Menú de Bendiciones (Arquitectura Enterprise)
 * Patrón: Instancia de Estado Temporal (Stateful Instance).
 * Se crea mediante "new" pasándole los Singletons inyectados desde el Comando.
 */
public class VoidBlessingMenu implements InventoryHolder {

    private final UserManager userManager;
    private final ConfigManager configManager;
    private final Player player;
    private Inventory inventory;

    // 🛠️ Constructor Desacoplado: Recibe los Singletons desde la clase constructora
    public VoidBlessingMenu(UserManager userManager, ConfigManager configManager, Player player) {
        this.userManager = userManager;
        this.configManager = configManager;
        this.player = player;
    }

    public void openMenu() {
        NexoUser user = userManager.getUserOrNull(player.getUniqueId());
        if (user == null) return;

        String title = "&#ff00ff✧ &#00f5ffEstado del Vacío";
        
        // 🌟 NATIVO PAPER: Usamos el Component devuelto por CrossplayUtils para el título
        this.inventory = Bukkit.createInventory(this, 27, CrossplayUtils.parseCrossplay(player, title));

        // 🟪 FONDO VIVID VOID (Púrpura Profundo)
        ItemStack bg = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta bgMeta = bg.getItemMeta();
        if (bgMeta != null) {
            bgMeta.displayName(Component.empty());
            bg.setItemMeta(bgMeta);
        }
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, bg);
        }

        // 🔮 ÍTEM CENTRAL: ESTADO DE LA BENDICIÓN
        ItemStack statusItem = new ItemStack(Material.AMETHYST_CLUSTER);
        ItemMeta statusMeta = statusItem.getItemMeta();
        List<Component> lore = new ArrayList<>();

        if (user.isVoidBlessingActive()) {
            statusMeta.displayName(CrossplayUtils.parseCrossplay(player, "&#ff00ff<bold>Bendición del Vacío</bold>"));

            long remainingMillis = user.getVoidBlessingUntil() - System.currentTimeMillis();
            String timeFormatted = formatTime(remainingMillis);

            lore.add(CrossplayUtils.parseCrossplay(player, "&#E6CCFFEstado: &#00f5ffACTIVO"));
            lore.add(CrossplayUtils.parseCrossplay(player, "&#E6CCFFTiempo restante: &#ff00ff" + timeFormatted));
            lore.add(Component.empty());
            lore.add(CrossplayUtils.parseCrossplay(player, "&#00f5ff[✧] Beneficios Canalizados:"));
            lore.add(CrossplayUtils.parseCrossplay(player, "&#8b0000 ▶ Protección Hardcore (0% pérdida de XP/Skills)"));
            lore.add(CrossplayUtils.parseCrossplay(player, "&#8b0000 ▶ Void Greed (+15% Nexo Coins en toda la red)"));
            lore.add(CrossplayUtils.parseCrossplay(player, "&#8b0000 ▶ Void Reach (Acceso remoto al mercado negro)"));
        } else {
            statusItem.setType(Material.COAL);
            statusMeta.displayName(CrossplayUtils.parseCrossplay(player, "&#8b0000<bold>Bendición Inactiva</bold>"));

            lore.add(CrossplayUtils.parseCrossplay(player, "&#E6CCFFEstado: &#8b0000DESACTIVADO"));
            lore.add(CrossplayUtils.parseCrossplay(player, "&#E6CCFFTu alma se encuentra vulnerable."));
            lore.add(Component.empty());
            lore.add(CrossplayUtils.parseCrossplay(player, "&#8b0000[!] Consigue una Esencia del Vacío"));
            lore.add(CrossplayUtils.parseCrossplay(player, "&#8b0000    para proteger tu progreso."));
        }

        // 🌟 NATIVO PAPER: Establecemos el lore directamente como lista de Componentes
        statusMeta.lore(lore);
        statusItem.setItemMeta(statusMeta);

        inventory.setItem(13, statusItem);
        player.openInventory(inventory);
    }

    private String formatTime(long millis) {
        long seconds = millis / 1000;
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        return String.format("%02dh %02dm %02ds", h, m, s);
    }

    // 🌟 FIX CRÍTICO PAPER 1.21.5: Contrato de nulabilidad estricto
    @Override
    public @NotNull Inventory getInventory() { 
        return this.inventory; 
    }
}