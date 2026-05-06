package me.nexo.minions.menu;

import me.nexo.core.api.NexoColeccionesAPI;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.menus.NexoMenu;
import me.nexo.core.user.NexoAPI;
import me.nexo.minions.manager.ActiveMinion;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 📊 Menú de Sinergia Omni-Minion
 * Conecta los logros del jugador (Colecciones) con la automatización de los Minions.
 */
public class MinionProductionMenu extends NexoMenu {

    private final ActiveMinion minion;

    public MinionProductionMenu(Player player, CrossplayUtils crossplayUtils, ActiveMinion minion) {
        super(player, crossplayUtils);
        this.minion = minion;
    }

    @Override
    public String getMenuName() {
        return "&#00f5ff⚙ Configuración Industrial";
    }

    @Override
    public int getSlots() {
        return 27;
    }

    @Override
    public void setMenuItems() {
        // Fondo Inmersivo
        ItemStack bg = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        bg.editMeta(m -> m.displayName(Component.empty()));
        for (int i = 0; i < getSlots(); i++) inventory.setItem(i, bg);

        // API de Colecciones
        NexoColeccionesAPI colecciones = NexoAPI.getInstance().getServiceManager()
                .get(NexoColeccionesAPI.class)
                .orElse(null);

        // 🪨 SLOT 11: Cobblestone (Base, siempre desbloqueado)
        renderOption(11, "COBBLESTONE", Material.COBBLESTONE, 0, colecciones, "&#AAAAAA");

        // ⛏️ SLOT 13: Hierro (Requiere 5,000 bloques picados)
        renderOption(13, "RAW_IRON", Material.RAW_IRON, 5000, colecciones, "&#FFFFFF");

        // 💎 SLOT 15: Diamante (Requiere 15,000 bloques picados)
        renderOption(15, "DIAMOND_ORE", Material.DIAMOND_ORE, 15000, colecciones, "&#55FFFF");

        // 🎣 SLOT 17: Pesca EMF (Requiere 1,000 peces pescados manualmente)
        renderOption(17, "EMF_FISH", Material.FISHING_ROD, 1000, colecciones, "&#55AAFF");
    }

    private void renderOption(int slot, String productionId, Material mat, long requiredAmount, NexoColeccionesAPI api, String color) {

        // 🌟 FIX: Declaración "Efectivamente Final" en una sola línea para que la Lambda no se queje
        final long currentMined = (api != null) ? api.getCollectionAmount(player.getUniqueId(), productionId) : 0L;

        boolean isUnlocked = currentMined >= requiredAmount || requiredAmount == 0;
        boolean isActive = minion.getDna().currentProductionId().equals(productionId);

        ItemStack item = new ItemStack(isUnlocked ? mat : Material.RED_STAINED_GLASS_PANE);

        item.editMeta(meta -> {
            String displayName = productionId.equals("EMF_FISH") ? "RED DE PESCA (EMF)" : productionId;
            meta.displayName(crossplayUtils.parseCrossplay(player, color + "<bold>" + displayName + "</bold>"));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());

            if (isActive) {
                lore.add(crossplayUtils.parseCrossplay(player, "&#55FF55[✓] Producción Activa"));
            } else if (isUnlocked) {
                lore.add(crossplayUtils.parseCrossplay(player, "&#E6CCFFProgreso de Colección: &#55FF55" + currentMined + " / " + requiredAmount));
                lore.add(Component.empty());
                lore.add(crossplayUtils.parseCrossplay(player, "&#FFAA00▶ Haz clic para reasignar al Minion"));
                lore.add(crossplayUtils.parseCrossplay(player, "&#FF5555⚠️ Esto vaciará su inventario actual."));
            } else {
                lore.add(crossplayUtils.parseCrossplay(player, "&#FF5555[!] Producción Bloqueada"));
                lore.add(crossplayUtils.parseCrossplay(player, "&#E6CCFFProgreso de Colección: &#FF5555" + currentMined + " / " + requiredAmount));
                lore.add(Component.empty());

                if (productionId.equals("EMF_FISH")) {
                    lore.add(crossplayUtils.parseCrossplay(player, "&#AAAAAAVe al mundo y pesca peces reales"));
                } else {
                    lore.add(crossplayUtils.parseCrossplay(player, "&#AAAAAAVe a las minas y farmea este recurso"));
                }
                lore.add(crossplayUtils.parseCrossplay(player, "&#AAAAAApara enseñarle al minion cómo extraerlo."));
            }

            meta.lore(lore);
        });

        inventory.setItem(slot, item);
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        e.setCancelled(true);
        if (e.getClickedInventory() == null || !e.getClickedInventory().equals(inventory)) return;

        String targetProduction = null;
        long required = 0;

        switch (e.getSlot()) {
            case 11 -> { targetProduction = "COBBLESTONE"; required = 0; }
            case 13 -> { targetProduction = "RAW_IRON"; required = 5000; }
            case 15 -> { targetProduction = "DIAMOND_ORE"; required = 15000; }
            case 17 -> { targetProduction = "EMF_FISH"; required = 1000; } // 🌟 NUEVO
        }

        if (targetProduction != null) {
            if (minion.getDna().currentProductionId().equals(targetProduction)) return;

            NexoColeccionesAPI api = NexoAPI.getInstance().getServiceManager().get(NexoColeccionesAPI.class).orElse(null);
            long current = api != null ? api.getCollectionAmount(player.getUniqueId(), targetProduction) : 0;

            if (current >= required || required == 0) {
                player.closeInventory();
                // 🚀 AQUÍ SE ACTIVA LA MUTACIÓN
                minion.changeProduction(targetProduction);
                crossplayUtils.sendMessage(player, "&#55FF55[✓] Protocolo industrial reasignado a: " + targetProduction);
            } else {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                crossplayUtils.sendMessage(player, "&#FF5555[x] El Minion no sabe cómo farmear esto aún. ¡Demuéstraselo primero!");
            }
        }
    }
}