package me.nexo.economy.blackmarket;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.economy.core.NexoAccount;
import me.nexo.items.managers.ItemManager;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 💰 NexoEconomy - Manager del Mercado Negro (Arquitectura Enterprise Java 21)
 * Rendimiento: Sinergia de Módulos (Inyección Directa de ItemManager) y Colecciones Inmutables.
 */
@Singleton
public class BlackMarketManager {

    // 🌟 DEPENDENCIAS PROPAGADAS (Desde NexoCore y NexoItems)
    private final ItemManager itemManager;
    private final CrossplayUtils crossplayUtils;

    private boolean isMarketOpen = false;
    private final List<BlackMarketItem> currentStock = new ArrayList<>();
    private final List<BlackMarketItem> possibleLootPool = new ArrayList<>();

    // 💉 PILAR 1: Inyección de Dependencias Estricta (Cero Service Locators)
    @Inject
    public BlackMarketManager(ItemManager itemManager, CrossplayUtils crossplayUtils) {
        this.itemManager = itemManager;
        this.crossplayUtils = crossplayUtils;
        cargarLootPool();
    }

    private void cargarLootPool() {
        // 🌟 USO DIRECTO DEL MÓDULO INYECTADO: Adiós NexoAPI
        possibleLootPool.add(new BlackMarketItem("hoja_vacio", itemManager.crearHojaVacio(), new BigDecimal("1500"), NexoAccount.Currency.MANA));

        var polvos = itemManager.crearPolvoEstelar();
        polvos.setAmount(16);
        possibleLootPool.add(new BlackMarketItem("polvo_estelar_x16", polvos, new BigDecimal("400"), NexoAccount.Currency.GEMS));

        try {
            var libroMagico = itemManager.generarLibroEncantamiento("vampirismo", 3);
            // Validamos que el generador no haya devuelto el ítem base por defecto (ej: fallo de carga)
            if (libroMagico != null && libroMagico.getType() != Material.BOOK) {
                possibleLootPool.add(new BlackMarketItem("libro_vampirismo", libroMagico, new BigDecimal("800"), NexoAccount.Currency.GEMS));
            }
        } catch (Exception ignored) {}

        try {
            var armaProhibida = itemManager.generarArmaRPG("guadana_oscura");
            if (armaProhibida != null && armaProhibida.getType() != Material.WOODEN_SWORD) {
                possibleLootPool.add(new BlackMarketItem("arma_rpg_oculta", armaProhibida, new BigDecimal("2500"), NexoAccount.Currency.MANA));
            }
        } catch (Exception ignored) {}

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

        var shuffled = new ArrayList<>(possibleLootPool);
        Collections.shuffle(shuffled);

        for (int i = 0; i < Math.min(3, shuffled.size()); i++) {
            currentStock.add(shuffled.get(i));
        }

        // 🌟 FIX: Dependencia inyectada para mensajes
        crossplayUtils.broadcastMessage("&#555555--------------------------------");
        crossplayUtils.broadcastMessage("&#8b0000🌑 <bold>EL MERCADO NEGRO HA ABIERTO</bold>");
        crossplayUtils.broadcastMessage("&#E6CCFFEl mercader sombrío ha traído nuevos artefactos prohibidos.");
        crossplayUtils.broadcastMessage("&#555555--------------------------------");
    }

    public void closeMarket() {
        if (!isMarketOpen) return;
        this.isMarketOpen = false;
        this.currentStock.clear();

        crossplayUtils.broadcastMessage("&#555555--------------------------------");
        crossplayUtils.broadcastMessage("&#8b0000🌑 <bold>EL MERCADO NEGRO SE HA DESVANECIDO</bold>");
        crossplayUtils.broadcastMessage("&#E6CCFFEl mercader regresó a las sombras.");
        crossplayUtils.broadcastMessage("&#555555--------------------------------");
    }

    public boolean isMarketOpen() { return isMarketOpen; }

    // 🌟 PROTECCIÓN DE MEMORIA: Retorna una copia inmutable para evitar modificaciones accidentales (Exploits)
    public List<BlackMarketItem> getCurrentStock() { 
        return List.copyOf(currentStock); 
    }

    private ItemStack crearItemMagico(Material mat, String hexName, String hexLore) {
        var item = new ItemStack(mat);
        // 🌟 PAPER NATIVE: Modificación de Meta fluida
        item.editMeta(meta -> {
            meta.displayName(crossplayUtils.parseCrossplay(null, hexName));
            meta.lore(List.of(crossplayUtils.parseCrossplay(null, hexLore)));
        });
        return item;
    }
}