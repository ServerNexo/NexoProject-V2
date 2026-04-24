package me.nexo.core.menus;

import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.user.NexoUser;
import me.nexo.core.user.UserManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 🏛️ Nexo Network - Menú de Bendiciones (Arquitectura Enterprise Java 21)
 * Rendimiento: Inyección Transitiva, editMeta O(1) y Estandarización NexoMenu.
 */
public class VoidBlessingMenu extends NexoMenu {

    private final UserManager userManager;
    private final CrossplayUtils crossplayUtils;
    private final NexoUser user; // Guardamos el usuario para renderizado rápido

    // 🛠️ Constructor Desacoplado: Recibe los Singletons inyectados
    public VoidBlessingMenu(Player player, UserManager userManager, CrossplayUtils crossplayUtils) {
        super(player, crossplayUtils); // 🌟 FIX ERROR SUPER: Pasamos el utilitario a la clase base
        this.userManager = userManager;
        this.crossplayUtils = crossplayUtils;

        // Buscamos al usuario de forma segura. Si no existe, usamos null
        this.user = userManager.getUserOrNull(player.getUniqueId());
    }

    @Override
    public String getMenuName() {
        return "&#ff00ff✧ &#00f5ffEstado del Vacío";
    }

    @Override
    public int getSlots() {
        return 27;
    }

    @Override
    public void setMenuItems() {
        // Si el usuario no cargó, no renderizamos el contenido
        if (user == null) {
            var errorItem = new ItemStack(Material.BARRIER);
            errorItem.editMeta(meta -> meta.displayName(crossplayUtils.parseCrossplay(player, "&#FF5555[!] Error cargando perfil del Nexo.")));
            inventory.setItem(13, errorItem);
            return;
        }

        // 🟪 FONDO VIVID VOID (Heredado de NexoMenu, lo aplicamos manualmente)
        ItemStack bg = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        bg.editMeta(meta -> meta.displayName(Component.empty()));
        for (int i = 0; i < getSlots(); i++) {
            inventory.setItem(i, bg);
        }

        // 🔮 ÍTEM CENTRAL: ESTADO DE LA BENDICIÓN
        ItemStack statusItem;
        List<Component> lore = new ArrayList<>();

        if (user.isVoidBlessingActive()) {
            statusItem = new ItemStack(Material.AMETHYST_CLUSTER);
            long remainingMillis = user.getVoidBlessingUntil() - System.currentTimeMillis();
            String timeFormatted = formatTime(remainingMillis);

            statusItem.editMeta(meta -> {
                // 🌟 FIX: Uso de la instancia crossplayUtils inyectada
                meta.displayName(crossplayUtils.parseCrossplay(player, "&#ff00ff<bold>Bendición del Vacío</bold>"));

                lore.add(crossplayUtils.parseCrossplay(player, "&#E6CCFFEstado: &#00f5ffACTIVO"));
                lore.add(crossplayUtils.parseCrossplay(player, "&#E6CCFFTiempo restante: &#ff00ff" + timeFormatted));
                lore.add(Component.empty());
                lore.add(crossplayUtils.parseCrossplay(player, "&#00f5ff[✧] Beneficios Canalizados:"));
                lore.add(crossplayUtils.parseCrossplay(player, "&#8b0000 ▶ Protección Hardcore (0% pérdida de XP/Skills)"));
                lore.add(crossplayUtils.parseCrossplay(player, "&#8b0000 ▶ Void Greed (+15% Nexo Coins en toda la red)"));
                lore.add(crossplayUtils.parseCrossplay(player, "&#8b0000 ▶ Void Reach (Acceso remoto al mercado negro)"));

                meta.lore(lore);
            });
        } else {
            statusItem = new ItemStack(Material.COAL);
            statusItem.editMeta(meta -> {
                // 🌟 FIX: Uso de la instancia crossplayUtils inyectada
                meta.displayName(crossplayUtils.parseCrossplay(player, "&#8b0000<bold>Bendición Inactiva</bold>"));

                lore.add(crossplayUtils.parseCrossplay(player, "&#E6CCFFEstado: &#8b0000DESACTIVADO"));
                lore.add(crossplayUtils.parseCrossplay(player, "&#E6CCFFTu alma se encuentra vulnerable."));
                lore.add(Component.empty());
                lore.add(crossplayUtils.parseCrossplay(player, "&#8b0000[!] Consigue una Esencia del Vacío"));
                lore.add(crossplayUtils.parseCrossplay(player, "&#8b0000    para proteger tu progreso."));

                meta.lore(lore);
            });
        }

        inventory.setItem(13, statusItem);
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        // En este menú solo mostramos información, no hay botones clickeables
        event.setCancelled(true);
    }

    private String formatTime(long millis) {
        long seconds = millis / 1000;
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        return String.format("%02dh %02dm %02ds", h, m, s);
    }
}