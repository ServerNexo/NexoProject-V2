package me.nexo.core.hub;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import net.kyori.adventure.text.Component; // 🌟 IMPORT COMPONENT
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer; // 🌟 IMPORT SERIALIZADOR
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * 🏛️ NexoCore - Interfaz Gráfica de Donaciones
 * Renderiza el menú del proyecto y procesa los pagos en Lingotes de Oro.
 */
@Singleton
public class HubDonationGUI implements Listener {

    private final HubDonationManager donationManager;
    private final CrossplayUtils crossplayUtils;

    private static final String MENU_TITLE = "🏗️ Reconstrucción del Hub";
    private static final Material DONATION_CURRENCY = Material.GOLD_INGOT; // Moneda física

    @Inject
    public HubDonationGUI(HubDonationManager donationManager, CrossplayUtils crossplayUtils) {
        this.donationManager = donationManager;
        this.crossplayUtils = crossplayUtils;
    }

    /**
     * Construye y abre el menú en la pantalla del jugador.
     */
    public void openMenu(Player player, String projectId) {
        HubDonationManager.HubProject project = donationManager.getProject(projectId);

        if (project == null) {
            crossplayUtils.sendMessage(player, "&#FF5555❌ El proyecto no existe.");
            return;
        }

        // 🌟 FIX: Usamos un Component para el título en lugar del String plano
        Component titleComponent = Component.text(MENU_TITLE);
        Inventory inv = Bukkit.createInventory(null, 27, titleComponent);

        // Renderizamos el Ítem Central (El Proyecto)
        ItemStack projectItem = new ItemStack(Material.ANVIL); // Icono por defecto (Herrería)
        ItemMeta meta = projectItem.getItemMeta();

        if (meta != null) {
            // Nombre del Proyecto
            meta.displayName(crossplayUtils.parseCrossplay(null, "&#FFD700<bold>" + project.displayName + "</bold>"));

            // Calculamos la barra de progreso
            double percent = (double) project.currentAmount / project.requiredAmount;
            String progressBar = generateProgressBar(percent);

            List<Component> lore = new ArrayList<>();
            lore.add(crossplayUtils.parseCrossplay(null, "&#E6CCFF¡Ayuda a la comunidad a construir este edificio!"));
            lore.add(crossplayUtils.parseCrossplay(null, ""));
            lore.add(crossplayUtils.parseCrossplay(null, "&#AAAAAAProgreso: &#FFFFFF" + project.currentAmount + " / " + project.requiredAmount));
            lore.add(crossplayUtils.parseCrossplay(null, progressBar + " &#FFD700(" + (int)(percent * 100) + "%)"));
            lore.add(crossplayUtils.parseCrossplay(null, ""));

            if (project.isCompleted) {
                lore.add(crossplayUtils.parseCrossplay(null, "&#55FF55✨ ¡PROYECTO COMPLETADO! ✨"));
            } else {
                lore.add(crossplayUtils.parseCrossplay(null, "&#55FF55► Click para donar hasta 64x " + DONATION_CURRENCY.name()));
            }

            meta.lore(lore);
            projectItem.setItemMeta(meta);
        }

        inv.setItem(13, projectItem); // Lo ponemos en el centro exacto (Slot 13)

        // Rellenamos el resto con cristal gris para diseño AAA
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.displayName(crossplayUtils.parseCrossplay(null, " "));
            filler.setItemMeta(fillerMeta);
        }
        for (int i = 0; i < inv.getSize(); i++) {
            if (i != 13) inv.setItem(i, filler);
        }

        player.openInventory(inv);
    }

    /**
     * Intercepta los clicks dentro del menú.
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // 🌟 FIX: Extraemos el Componente title() y lo convertimos a texto limpio para validar
        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (!title.equals(MENU_TITLE)) return;

        // Cancelamos para evitar que roben el yunque o los cristales
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getRawSlot() != 13) return; // Solo nos importa si clickean el Yunque

        // Extraemos el ID del proyecto de prueba que configuramos antes
        String projectId = "herreria_t2";
        HubDonationManager.HubProject project = donationManager.getProject(projectId);

        if (project == null || project.isCompleted) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // Buscamos cuánto oro tiene el jugador
        int playerGold = countItems(player, DONATION_CURRENCY);

        if (playerGold <= 0) {
            crossplayUtils.sendMessage(player, "&#FF5555❌ No tienes " + DONATION_CURRENCY.name() + " en tu inventario.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // Donamos un máximo de 64 por click (o lo que le falte al proyecto si es menos)
        int remainingToComplete = project.requiredAmount - project.currentAmount;
        int maxDonation = Math.min(64, remainingToComplete);
        int donationAmount = Math.min(playerGold, maxDonation);

        // Retiramos los ítems físicos
        removeItems(player, DONATION_CURRENCY, donationAmount);

        // Registramos la donación en el Manager
        donationManager.addDonation(player, projectId, donationAmount);

        // Refrescamos el menú visualmente para que la barra suba en vivo
        openMenu(player, projectId);
    }

    // ==========================================
    // 🛠️ UTILS INTERNOS
    // ==========================================

    private String generateProgressBar(double percentage) {
        int totalBars = 20;
        int filledBars = (int) (totalBars * percentage);

        StringBuilder bar = new StringBuilder();
        bar.append("&#55FF55"); // Verde para lo completado
        for (int i = 0; i < filledBars; i++) bar.append("■");

        bar.append("&#777777"); // Gris para lo vacío
        for (int i = filledBars; i < totalBars; i++) bar.append("■");

        return bar.toString();
    }

    private int countItems(Player player, Material material) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                count += item.getAmount();
            }
        }
        return count;
    }

    private void removeItems(Player player, Material material, int amountToRemove) {
        int remaining = amountToRemove;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                if (item.getAmount() <= remaining) {
                    remaining -= item.getAmount();
                    item.setAmount(0);
                } else {
                    item.setAmount(item.getAmount() - remaining);
                    break; // Ya quitamos todo
                }
            }
        }
    }
}