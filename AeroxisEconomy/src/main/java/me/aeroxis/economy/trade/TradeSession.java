package me.aeroxis.economy.trade;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import me.aeroxis.core.crossplay.CrossplayUtils;
import me.aeroxis.economy.AeroxisEconomy;
import me.aeroxis.economy.core.EconomyManager;
import me.aeroxis.economy.core.AeroxisAccount;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 💰 AeroxisEconomy - Sesión de Intercambio (Arquitectura Enterprise Java 21)
 * Rendimiento: Candados Atómicos, GlobalRegionScheduler, Streams O(1) y Propagación Transitiva.
 */
public class TradeSession implements InventoryHolder {

    // 🌟 DEPENDENCIAS PROPAGADAS (Entregadas por TradeManager)
    private final AeroxisEconomy plugin;
    private final EconomyManager economyManager;
    private final TradeManager tradeManager;
    private final CrossplayUtils crossplayUtils;

    private final Player player1;
    private final Player player2;
    private final Inventory inventory;

    private boolean p1Ready = false;
    private boolean p2Ready = false;

    // 🌟 CANDADOS ATÓMICOS: Evitan la clonación de ítems por concurrencia o lag
    private final AtomicBoolean isExecuting = new AtomicBoolean(false);
    private final AtomicBoolean isClosed = new AtomicBoolean(false);

    private BigDecimal p1Coins = BigDecimal.ZERO;
    private BigDecimal p2Coins = BigDecimal.ZERO;
    private BigDecimal p1Gems = BigDecimal.ZERO;
    private BigDecimal p2Gems = BigDecimal.ZERO;
    private BigDecimal p1Mana = BigDecimal.ZERO;
    private BigDecimal p2Mana = BigDecimal.ZERO;

    // 🌟 PAPER 1.21: Referencia nativa a tareas programadas (Folia-Ready)
    private ScheduledTask scheduledTask = null;

    // 🌟 FIX: Añadimos 'AeroxisEconomy plugin' al constructor para inyección transitiva
    public TradeSession(AeroxisEconomy plugin, EconomyManager economyManager, TradeManager tradeManager, CrossplayUtils crossplayUtils, Player player1, Player player2) {
        this.plugin = plugin;
        this.economyManager = economyManager;
        this.tradeManager = tradeManager;
        this.crossplayUtils = crossplayUtils;
        this.player1 = player1;
        this.player2 = player2;

        Component titleComp = crossplayUtils.parseCrossplay(player1, "&#00f5ff🤝 <bold>INTERCAMBIO SEGURO</bold>");
        this.inventory = Bukkit.createInventory(this, 54, titleComp);

        setupGUI();
    }

    @Override
    public Inventory getInventory() { return inventory; }

    private void setupGUI() {
        setItem(13, Material.GOLD_INGOT, "&#FFAA00[+] <bold>AÑADIR MONEDAS</bold>",
                "&#E6CCFFClic para transferir &#FFAA00+1,000 Monedas&#E6CCFF.");

        setItem(22, Material.EMERALD, "&#55FF55[+] <bold>AÑADIR GEMAS</bold>",
                "&#E6CCFFClic para transferir &#55FF55+100 Gemas&#E6CCFF.");

        setItem(31, Material.AMETHYST_SHARD, "&#ff00ff[+] <bold>AÑADIR MANÁ</bold>",
                "&#E6CCFFClic para transferir &#ff00ff+10 de Maná&#E6CCFF.");

        var separator = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        separator.editMeta(meta -> meta.displayName(crossplayUtils.parseCrossplay(player1, " ")));

        inventory.setItem(4, separator);
        inventory.setItem(40, separator);
        inventory.setItem(49, separator);

        updateReadyButtons();
    }

    private void setItem(int slot, Material mat, String name, String... lore) {
        var item = new ItemStack(mat);
        item.editMeta(meta -> {
            meta.displayName(crossplayUtils.parseCrossplay(player1, name));
            // 🌟 JAVA 21 NATIVO: Mapeo de Streams mucho más rápido y sin basura
            meta.lore(Arrays.stream(lore)
                    .map(l -> crossplayUtils.parseCrossplay(player1, l))
                    .toList());
        });
        inventory.setItem(slot, item);
    }

    public void updateReadyButtons() {
        // --- Jugador 1 ---
        String p1Color = p1Ready ? "&#55FF55[✓] <bold>AUTORIZADO</bold>" : "&#FF5555[X] <bold>ESPERANDO</bold>";
        var p1LoreRaw = List.of(
                "&#E6CCFFSocio: &#00f5ff" + player1.getName(), "",
                "&#E6CCFFOfreciendo:",
                "&#FFAA00" + p1Coins.toString() + " Monedas",
                "&#55FF55" + p1Gems.toString() + " Gemas",
                "&#ff00ff" + p1Mana.toString() + " Maná", "",
                "&#E6CCFFClic para " + (p1Ready ? "cancelar." : "aceptar trato.")
        );
        setItem(45, p1Ready ? Material.LIME_DYE : Material.RED_DYE, p1Color, p1LoreRaw.toArray(new String[0]));

        // --- Jugador 2 ---
        String p2Color = p2Ready ? "&#55FF55[✓] <bold>AUTORIZADO</bold>" : "&#FF5555[X] <bold>ESPERANDO</bold>";
        var p2LoreRaw = List.of(
                "&#E6CCFFSocio: &#00f5ff" + player2.getName(), "",
                "&#E6CCFFOfreciendo:",
                "&#FFAA00" + p2Coins.toString() + " Monedas",
                "&#55FF55" + p2Gems.toString() + " Gemas",
                "&#ff00ff" + p2Mana.toString() + " Maná", "",
                "&#E6CCFFClic para " + (p2Ready ? "cancelar." : "aceptar trato.")
        );
        setItem(53, p2Ready ? Material.LIME_DYE : Material.RED_DYE, p2Color, p2LoreRaw.toArray(new String[0]));
    }

    public void addCurrency(Player p, AeroxisAccount.Currency currency, BigDecimal amount) {
        if (isClosed.get() || isExecuting.get()) return; // Bloqueo de seguridad

        if (p.equals(player1)) {
            if (currency == AeroxisAccount.Currency.COINS) p1Coins = p1Coins.add(amount);
            else if (currency == AeroxisAccount.Currency.GEMS) p1Gems = p1Gems.add(amount);
            else if (currency == AeroxisAccount.Currency.MANA) p1Mana = p1Mana.add(amount);
        } else {
            if (currency == AeroxisAccount.Currency.COINS) p2Coins = p2Coins.add(amount);
            else if (currency == AeroxisAccount.Currency.GEMS) p2Gems = p2Gems.add(amount);
            else if (currency == AeroxisAccount.Currency.MANA) p2Mana = p2Mana.add(amount);
        }
        unready();
    }

    public void toggleReady(Player player) {
        if (isClosed.get() || isExecuting.get()) return;

        if (player.equals(player1)) p1Ready = !p1Ready;
        else p2Ready = !p2Ready;

        updateReadyButtons();

        if (p1Ready && p2Ready) iniciarCuentaRegresiva();
        else cancelarCuenta();
    }

    private void iniciarCuentaRegresiva() {
        // 🌟 FIX: Usamos la variable 'plugin' que acabamos de inyectar en el constructor
        scheduledTask = Bukkit.getGlobalRegionScheduler().runDelayed(plugin, task -> ejecutarIntercambio(), 60L);
        crossplayUtils.sendMessage(player1, "&#00f5ff[!] <bold>Ambas partes han aceptado. Intercambio en 3 segundos...</bold>");
        crossplayUtils.sendMessage(player2, "&#00f5ff[!] <bold>Ambas partes han aceptado. Intercambio en 3 segundos...</bold>");
    }

    public void cancelarCuenta() {
        if (scheduledTask != null && !scheduledTask.isCancelled()) {
            scheduledTask.cancel();
            scheduledTask = null;
        }
    }

    // 🌟 MOTOR ATÓMICO PRINCIPAL
    private void ejecutarIntercambio() {
        // CompareAndSet garantiza que aunque el servidor intente llamar esto 2 veces en 1 milisegundo, solo pasará 1 vez.
        if (!isExecuting.compareAndSet(false, true)) return;
        isClosed.set(true); // Bloquea la devolución accidental de ítems

        transferirLado(true);
        transferirLado(false);

        transferCurrency(AeroxisAccount.Currency.COINS, p1Coins, p2Coins);
        transferCurrency(AeroxisAccount.Currency.GEMS, p1Gems, p2Gems);
        transferCurrency(AeroxisAccount.Currency.MANA, p1Mana, p2Mana);

        player1.closeInventory();
        player2.closeInventory();

        crossplayUtils.sendMessage(player1, "&#55FF55[✓] <bold>Intercambio finalizado con éxito.</bold>");
        crossplayUtils.sendMessage(player2, "&#55FF55[✓] <bold>Intercambio finalizado con éxito.</bold>");

        tradeManager.removeSession(this);
    }

    // 🌟 RUTINA DE CANCELACIÓN (ANTI-SCAM / ANTI-DUPE)
    public void abortTrade(String reason) {
        if (isExecuting.get() || !isClosed.compareAndSet(false, true)) return;

        cancelarCuenta();

        devolverLado(true); // Devuelve P1 a P1
        devolverLado(false); // Devuelve P2 a P2

        refundCurrency(AeroxisAccount.Currency.COINS, p1Coins, p2Coins);
        refundCurrency(AeroxisAccount.Currency.GEMS, p1Gems, p2Gems);
        refundCurrency(AeroxisAccount.Currency.MANA, p1Mana, p2Mana);

        if (reason != null && !reason.isEmpty()) {
            crossplayUtils.sendMessage(player1, "&#FF5555[!] Intercambio abortado: " + reason);
            crossplayUtils.sendMessage(player2, "&#FF5555[!] Intercambio abortado: " + reason);
        }

        player1.closeInventory();
        player2.closeInventory();
        tradeManager.removeSession(this);
    }

    private void transferCurrency(AeroxisAccount.Currency currency, BigDecimal amountP1, BigDecimal amountP2) {
        if (amountP1.compareTo(BigDecimal.ZERO) > 0) {
            economyManager.updateBalanceAsync(player2.getUniqueId(), AeroxisAccount.AccountType.PLAYER, currency, amountP1, true);
        }
        if (amountP2.compareTo(BigDecimal.ZERO) > 0) {
            economyManager.updateBalanceAsync(player1.getUniqueId(), AeroxisAccount.AccountType.PLAYER, currency, amountP2, true);
        }
    }

    private void refundCurrency(AeroxisAccount.Currency currency, BigDecimal amountP1, BigDecimal amountP2) {
        if (amountP1.compareTo(BigDecimal.ZERO) > 0) economyManager.updateBalanceAsync(player1.getUniqueId(), AeroxisAccount.AccountType.PLAYER, currency, amountP1, true);
        if (amountP2.compareTo(BigDecimal.ZERO) > 0) economyManager.updateBalanceAsync(player2.getUniqueId(), AeroxisAccount.AccountType.PLAYER, currency, amountP2, true);
    }

    private void transferirLado(boolean deP1aP2) {
        Player receptor = deP1aP2 ? player2 : player1;
        procesarSlots(deP1aP2, receptor);
    }

    private void devolverLado(boolean isP1) {
        Player dueño = isP1 ? player1 : player2;
        procesarSlots(isP1, dueño);
    }

    private void procesarSlots(boolean isLadoIzquierdo, Player receptor) {
        for (int i = 0; i < 54; i++) {
            boolean esSlotDeOrigen = isLadoIzquierdo ? (i % 9 < 4) : (i % 9 > 4);
            if (i >= 45 || i % 9 == 4) esSlotDeOrigen = false; // Ignorar botones y cristal central

            if (esSlotDeOrigen) {
                var item = inventory.getItem(i);
                if (item != null && !item.isEmpty()) {
                    inventory.setItem(i, null); // 🌟 CRÍTICO: Eliminamos del GUI *antes* de devolver el ítem para evitar clonación

                    if (receptor.getInventory().firstEmpty() == -1) {
                        receptor.getWorld().dropItemNaturally(receptor.getLocation(), item);
                    } else {
                        receptor.getInventory().addItem(item);
                    }
                }
            }
        }
    }

    public void unready() {
        if (isExecuting.get() || isClosed.get()) return;
        p1Ready = false;
        p2Ready = false;
        updateReadyButtons();
        cancelarCuenta();
    }

    public Player getPlayer1() { return player1; }
    public Player getPlayer2() { return player2; }

    public void open() {
        player1.openInventory(inventory);
        player2.openInventory(inventory);
    }
}