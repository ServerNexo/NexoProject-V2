package me.aeroxis.economy.blackmarket;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.aeroxis.core.crossplay.CrossplayUtils;
import me.aeroxis.economy.AeroxisEconomy;
import me.aeroxis.economy.core.AeroxisAccount;
import me.aeroxis.items.managers.ItemManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 💰 AeroxisEconomy - Manager del Mercado Negro (Arquitectura Enterprise Java 21)
 * Rendimiento: Sinergia de Módulos (ItemManager), Colecciones Inmutables y Validación PDC Asíncrona.
 */
@Singleton
public class BlackMarketManager {

    private final AeroxisEconomy plugin;

    // 🌟 DEPENDENCIAS PROPAGADAS (Desde AeroxisCore y AeroxisItems)
    private final ItemManager itemManager;
    private final CrossplayUtils crossplayUtils;
    private final NamespacedKey contrabandKey;

    private boolean isMarketOpen = false;
    private final List<BlackMarketItem> currentStock = new ArrayList<>();
    private final List<BlackMarketItem> possibleLootPool = new ArrayList<>();

    // 💉 PILAR 1: Inyección de Dependencias Estricta (Cero Service Locators)
    @Inject
    public BlackMarketManager(AeroxisEconomy plugin, ItemManager itemManager, CrossplayUtils crossplayUtils) {
        this.plugin = plugin;
        this.itemManager = itemManager;
        this.crossplayUtils = crossplayUtils;

        // 🌟 LLAVE DE CONTRABANDO NATIVA (Debe coincidir con AeroxisMechanics)
        this.contrabandKey = new NamespacedKey("aeroxismechanics", "contraband_expiry");

        cargarLootPool();
    }

    // ==========================================
    // 🛒 GESTIÓN DEL STOCK Y APERTURA (COMPRA)
    // ==========================================

    private void cargarLootPool() {
        // 🌟 USO DIRECTO DEL MÓDULO INYECTADO: Adiós AeroxisAPI
        possibleLootPool.add(new BlackMarketItem("hoja_vacio", itemManager.crearHojaVacio(), new BigDecimal("1500"), AeroxisAccount.Currency.MANA));

        var polvos = itemManager.crearPolvoEstelar();
        polvos.setAmount(16);
        possibleLootPool.add(new BlackMarketItem("polvo_estelar_x16", polvos, new BigDecimal("400"), AeroxisAccount.Currency.GEMS));

        try {
            var libroMagico = itemManager.generarLibroEncantamiento("vampirismo", 3);
            // Validamos que el generador no haya devuelto el ítem base por defecto (ej: fallo de carga)
            if (libroMagico != null && libroMagico.getType() != Material.BOOK) {
                possibleLootPool.add(new BlackMarketItem("libro_vampirismo", libroMagico, new BigDecimal("800"), AeroxisAccount.Currency.GEMS));
            }
        } catch (Exception ignored) {}

        try {
            var armaProhibida = itemManager.generarArmaRPG("guadana_oscura");
            if (armaProhibida != null && armaProhibida.getType() != Material.WOODEN_SWORD) {
                possibleLootPool.add(new BlackMarketItem("arma_rpg_oculta", armaProhibida, new BigDecimal("2500"), AeroxisAccount.Currency.MANA));
            }
        } catch (Exception ignored) {}

        // Fallback por defecto siempre disponible
        possibleLootPool.add(new BlackMarketItem(
                "forbidden_apple",
                crearItemMagico(Material.ENCHANTED_GOLDEN_APPLE, "&#8b0000<bold>🍎 Manzana Prohibida</bold>", "&#E6CCFFFruta del inframundo."),
                new BigDecimal("150"), AeroxisAccount.Currency.GEMS
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

    // ==========================================
    // 💰 LÓGICA DE LAVADO DE DINERO (VENTA)
    // ==========================================

    /**
     * Intenta vender un ítem de contrabando en el Mercado Negro.
     * @param player El jugador que intenta vender.
     * @param slot El slot del inventario donde está el ítem.
     * @param baseValue El valor base en la economía del ítem.
     */
    public void sellContrabandItem(Player player, int slot, double baseValue) {
        ItemStack item = player.getInventory().getItem(slot);

        if (item == null || item.isEmpty() || !item.hasItemMeta()) {
            crossplayUtils.sendMessage(player, "&#FF5555[!] Ese no es un objeto válido para el mercado negro.");
            return;
        }

        ItemMeta meta = item.getItemMeta();
        Long expiryTime = meta.getPersistentDataContainer().get(contrabandKey, PersistentDataType.LONG);

        if (expiryTime == null) {
            crossplayUtils.sendMessage(player, "&#FF5555[!] El sindicato solo acepta mercancía marcada como contrabando.");
            return;
        }

        long currentTime = System.currentTimeMillis();

        // 1. Validamos si ya está caducado (El jugador llegó tarde)
        if (currentTime >= expiryTime) {
            crossplayUtils.sendMessage(player, "&#AA0000[!] ¡Demasiado tarde! Esta mercancía está rastreada. ¡Huye!");
            // Nota: Aquí el ContrabandManager de AeroxisMechanics ya lo castigará
            return;
        }

        // 2. Cálculo de Riesgo: Matemática Asíncrona
        CompletableFuture.supplyAsync(() -> {
            long timeLeft = expiryTime - currentTime;
            double riskMultiplier = 1.0;

            // Si le quedan menos de 10 segundos, el multiplicador sube por el riesgo
            if (timeLeft <= 10000) {
                riskMultiplier = 2.5;
            } else if (timeLeft <= 30000) {
                riskMultiplier = 1.5;
            }

            return (baseValue * item.getAmount()) * riskMultiplier;

        }).thenAccept(finalValue -> {
            // 3. Ejecución Física (Síncrona en la Región del Jugador para compatibilidad con Folia)
            Bukkit.getRegionScheduler().execute(plugin, player.getLocation(), () -> {

                if (!player.isOnline()) return;

                // Doble chequeo de seguridad (evita dupes)
                ItemStack checkItem = player.getInventory().getItem(slot);
                if (checkItem != null && checkItem.equals(item)) {

                    // Borramos el ítem
                    player.getInventory().setItem(slot, null);

                    // Pagamos al jugador (Ejecutamos comando o API directa de economía)
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "eco give " + player.getName() + " " + finalValue);

                    // Efectos visuales de transacción mafiosa
                    player.playSound(player.getLocation(), Sound.ENTITY_WANDERING_TRADER_YES, 1.0f, 0.8f);
                    player.getWorld().spawnParticle(Particle.SCULK_SOUL, player.getLocation().add(0, 1, 0), 15, 0.5, 0.5, 0.5, 0.02);

                    String formattedValue = String.format("%.2f", finalValue);
                    crossplayUtils.sendMessage(player, "&#55FF55[✓] Mercancía lavada. Has recibido &#FFAA00$" + formattedValue + " &#55FF55por tu riesgo.");
                }
            });
        });
    }
}