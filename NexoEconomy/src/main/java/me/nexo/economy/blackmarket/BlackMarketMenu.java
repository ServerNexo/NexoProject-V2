package me.nexo.economy.blackmarket;

import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.menus.NexoMenu;
import me.nexo.economy.NexoEconomy;
import me.nexo.economy.core.EconomyManager;
import me.nexo.economy.core.NexoAccount;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;

/**
 * 💰 NexoEconomy - Menú del Mercado Negro (Arquitectura Enterprise)
 * Rendimiento: Hilo-Seguro (Async-to-Sync), Dependencias Transitivas y Cero Estáticos.
 * Nota: Instanciado dinámicamente por jugador. NO usa @Singleton.
 */
public class BlackMarketMenu extends NexoMenu {

    // 🌟 DEPENDENCIAS PROPAGADAS DESDE EL COMANDO
    private final NexoEconomy plugin; // Requerido estrictamente para el Scheduler
    private final BlackMarketManager bmManager;
    private final EconomyManager ecoManager;
    private final CrossplayUtils crossplayUtils;

    public BlackMarketMenu(Player player, NexoEconomy plugin, BlackMarketManager bmManager, EconomyManager ecoManager, CrossplayUtils crossplayUtils) {
        super(player);
        this.plugin = plugin;
        this.bmManager = bmManager;
        this.ecoManager = ecoManager;
        this.crossplayUtils = crossplayUtils;
    }

    @Override
    public String getMenuName() {
        // Retornamos Legacy Serialized para respetar la firma de Bukkit nativo, pero parseado asíncronamente
        return LegacyComponentSerializer.legacySection().serialize(crossplayUtils.parseCrossplay(player, "&#8b0000🌑 <bold>MERCADO NEGRO</bold>"));
    }

    @Override
    public int getSlots() {
        return 27;
    }

    @Override
    public void setMenuItems() {
        setFillerGlass(); // Rellena el fondo morado automáticamente

        var stock = bmManager.getCurrentStock();
        int[] slots = {11, 13, 15};

        for (int i = 0; i < stock.size() && i < slots.length; i++) {
            var bmItem = stock.get(i);
            var display = bmItem.displayItem().clone();
            
            // 🌟 PAPER 1.21 FIX: Modificación de Meta fluida y segura
            final int finalIndex = i;
            display.editMeta(meta -> {
                String color = bmItem.currency() == NexoAccount.Currency.GEMS ? "&#55FF55" : "&#ff00ff";
                String divisaNombre = bmItem.currency() == NexoAccount.Currency.GEMS ? "💎 Gemas" : "💧 Maná";

                var loreRaw = List.of(
                        "",
                        "&#E6CCFFPrecio: " + color + bmItem.price().toPlainString() + " " + divisaNombre,
                        "",
                        "&#8b0000► Clic para negociar"
                );

                // Java 21 Streams puros (Cero anidamientos extraños)
                var lore = loreRaw.stream()
                        .map(line -> crossplayUtils.parseCrossplay(player, line))
                        .toList();

                if (meta.hasLore() && meta.lore() != null) {
                    var originalLore = meta.lore();
                    originalLore.addAll(lore);
                    meta.lore(originalLore);
                } else {
                    meta.lore(lore);
                }

                // 🌟 MAGIA PDC: Sin depender de 'plugin', usamos un string explícito
                meta.getPersistentDataContainer().set(new NamespacedKey("nexoeconomy", "bm_index"), org.bukkit.persistence.PersistentDataType.INTEGER, finalIndex);
            });
            
            inventory.setItem(slots[i], display);
        }
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        event.setCancelled(true); // Bloqueo absoluto contra robos

        if (!bmManager.isMarketOpen()) {
            player.closeInventory();
            crossplayUtils.sendMessage(player, "&#FF5555[!] El mercader ha desaparecido entre las sombras. El mercado está cerrado.");
            return;
        }

        var item = event.getCurrentItem();
        // 🌟 PAPER FIX: Bloquear Ghost Items (isEmpty en lugar de comparar con AIR)
        if (item == null || item.isEmpty() || !item.hasItemMeta()) return;

        var meta = item.getItemMeta();
        var indexKey = new NamespacedKey("nexoeconomy", "bm_index");

        if (meta.getPersistentDataContainer().has(indexKey, org.bukkit.persistence.PersistentDataType.INTEGER)) {
            int index = meta.getPersistentDataContainer().get(indexKey, org.bukkit.persistence.PersistentDataType.INTEGER);
            var stock = bmManager.getCurrentStock();

            if (index >= 0 && index < stock.size()) {
                var bmItem = stock.get(index);

                crossplayUtils.sendMessage(player, "&#FFAA00[⏳] Sellando pacto con el Vacío...");

                // 🚀 Compra asíncrona segura contra la base de datos (Ejecuta en Hilo Virtual)
                ecoManager.updateBalanceAsync(player.getUniqueId(), NexoAccount.AccountType.PLAYER, bmItem.currency(), bmItem.price(), false)
                        .thenAccept(success -> {
                            
                            // 🌟 FOLIA NATIVE: ¡CRÍTICO! Retornamos la respuesta al Hilo Síncrono del Jugador
                            player.getScheduler().run(plugin, task -> {
                                if (success) {
                                    var buyItem = bmItem.displayItem().clone();
                                    
                                    if (player.getInventory().firstEmpty() == -1) {
                                        player.getWorld().dropItemNaturally(player.getLocation(), buyItem);
                                    } else {
                                        player.getInventory().addItem(buyItem);
                                    }
                                    
                                    crossplayUtils.sendMessage(player, "&#8b0000🌑 <bold>MERCADO NEGRO:</bold> &#E6CCFFUn placer hacer negocios contigo.");
                                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_TRADE, 1.0f, 1.0f);
                                    player.closeInventory();
                                } else {
                                    String divisa = bmItem.currency() == NexoAccount.Currency.GEMS ? "Gemas" : "Maná";
                                    crossplayUtils.sendMessage(player, "&#FF5555[!] El mercader se ríe de ti. No tienes suficientes " + divisa + ".");
                                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                                }
                            }, null);
                            
                        });
            }
        }
    }
}