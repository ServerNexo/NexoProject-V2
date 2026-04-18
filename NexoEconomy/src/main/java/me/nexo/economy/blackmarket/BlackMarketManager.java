package me.nexo.economy.blackmarket;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.user.NexoAPI;
import me.nexo.economy.NexoEconomy;
import me.nexo.economy.core.NexoAccount;
import me.nexo.items.managers.ItemManager;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 💰 NexoEconomy - Manager del Mercado Negro (Arquitectura Enterprise)
 */
@Singleton
public class BlackMarketManager {

    private final NexoEconomy plugin;
    private boolean isMarketOpen = false;
    private final List<BlackMarketItem> currentStock = new ArrayList<>();
    private final List<BlackMarketItem> possibleLootPool = new ArrayList<>();

    // 💉 PILAR 3: Inyección de Dependencias
    @Inject
    public BlackMarketManager(NexoEconomy plugin) {
        this.plugin = plugin;
        cargarLootPool();
    }

    private void cargarLootPool() {
        // Pedimos amablemente los ítems al módulo de NexoItems si está cargado
        NexoAPI.getServices().get(ItemManager.class).ifPresent(itemManager -> {
            possibleLootPool.add(new BlackMarketItem("hoja_vacio", itemManager.crearHojaVacio(), new BigDecimal("1500"), NexoAccount.Currency.MANA));

            ItemStack polvos = itemManager.crearPolvoEstelar();
            polvos.setAmount(16);
            possibleLootPool.add(new BlackMarketItem("polvo_estelar_x16", polvos, new BigDecimal("400"), NexoAccount.Currency.GEMS));

            try {
                ItemStack libroMagico = itemManager.generarLibroEncantamiento("vampirismo", 3);
                if (libroMagico != null && libroMagico.getType() != Material.BOOK) {
                    possibleLootPool.add(new BlackMarketItem("libro_vampirismo", libroMagico, new BigDecimal("800"), NexoAccount.Currency.GEMS));
                }
            } catch (Exception ignored) {}

            try {
                ItemStack armaProhibida = itemManager.generarArmaRPG("guadana_oscura");
                if (armaProhibida != null && armaProhibida.getType() != Material.WOODEN_SWORD) {
                    possibleLootPool.add(new BlackMarketItem("arma_rpg_oculta", armaProhibida, new BigDecimal("2500"), NexoAccount.Currency.MANA));
                }
            } catch (Exception ignored) {}
        });

        // Fallback por defecto siempre disponible
        possibleLootPool.add(new BlackMarketItem(
                "forbidden_apple",
                crearItemMagico(Material.ENCHANTED_GOLDEN_APPLE, "&#8b0000<bold>🍎 Manzana Prohibida</bold>", "&#E6CCFFFruta del inframundo."),
                new BigDecimal("150"), NexoAccount.Currency.GEMS
        ));
    }

    public void openMarket() {
        if (isMarketOpen) return;
        this.isMarketOpen = true;
        this.currentStock.clear();

        List<BlackMarketItem> shuffled = new ArrayList<>(possibleLootPool);
        Collections.shuffle(shuffled);

        for (int i = 0; i < Math.min(3, shuffled.size()); i++) {
            currentStock.add(shuffled.get(i));
        }

        // 🌟 FIX: Mensajes directos con diseño sin leer la config en cada evento
        CrossplayUtils.broadcastMessage("&#555555--------------------------------");
        CrossplayUtils.broadcastMessage("&#8b0000🌑 <bold>EL MERCADO NEGRO HA ABIERTO</bold>");
        CrossplayUtils.broadcastMessage("&#E6CCFFEl mercader sombrío ha traído nuevos artefactos prohibidos.");
        CrossplayUtils.broadcastMessage("&#555555--------------------------------");
    }

    public void closeMarket() {
        if (!isMarketOpen) return;
        this.isMarketOpen = false;
        this.currentStock.clear();

        // 🌟 FIX: Mensajes directos
        CrossplayUtils.broadcastMessage("&#555555--------------------------------");
        CrossplayUtils.broadcastMessage("&#8b0000🌑 <bold>EL MERCADO NEGRO SE HA DESVANECIDO</bold>");
        CrossplayUtils.broadcastMessage("&#E6CCFFEl mercader regresó a las sombras.");
        CrossplayUtils.broadcastMessage("&#555555--------------------------------");
    }

    public boolean isMarketOpen() { return isMarketOpen; }
    public List<BlackMarketItem> getCurrentStock() { return currentStock; }

    private ItemStack crearItemMagico(Material mat, String hexName, String hexLore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(CrossplayUtils.parseCrossplay(null, hexName));
            meta.lore(List.of(CrossplayUtils.parseCrossplay(null, hexLore)));
            item.setItemMeta(meta);
        }
        return item;
    }
}