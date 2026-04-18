package me.nexo.economy.core;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.database.DatabaseManager;
import me.nexo.economy.NexoEconomy;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 💰 NexoEconomy - Manager Central de Economía (Arquitectura Enterprise Java 25)
 * I/O Atómico No-Bloqueante, Hilos Virtuales y Batch Processing.
 */
@Singleton
public class EconomyManager {

    private final NexoEconomy plugin;
    private final DatabaseManager db; // 🌟 Desacoplado: Ya no dependemos de obtener el plugin NexoCore.

    // 🚀 EL MOTOR DE RENDIMIENTO I/O: Hilos Virtuales nativos para evitar Thread Starvation
    private static final Executor VIRTUAL_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    // ⚡ Caché Ultrarrápido: Las cuentas expiran a los 30 min de inactividad
    private final Cache<String, NexoAccount> accountCache = Caffeine.newBuilder()
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .build();

    // 💉 PILAR 3: Inyección de Dependencias
    @Inject
    public EconomyManager(NexoEconomy plugin, DatabaseManager db) {
        this.plugin = plugin;
        this.db = db;
        crearTablaEconomia();
    }

    private void crearTablaEconomia() {
        // 🌟 I/O ASÍNCRONO NATIVO: Adiós al Bukkit Scheduler para tareas de SQL
        Thread.startVirtualThread(() -> {
            String sql = """
                    CREATE TABLE IF NOT EXISTS nexo_economy (
                        id UUID PRIMARY KEY,
                        account_type VARCHAR(20) NOT NULL,
                        coins DECIMAL(20,2) DEFAULT 0.00,
                        gems DECIMAL(20,2) DEFAULT 0.00,
                        mana DECIMAL(20,2) DEFAULT 0.00
                    );
                    """;
            try (var conn = db.getConnection();
                 var stmt = conn.createStatement()) {
                stmt.execute(sql);
            } catch (Exception e) {
                plugin.getLogger().severe("❌ Error creando tabla de economía: " + e.getMessage());
            }
        });
    }

    /**
     * Identificador único para el Caché (Ej: "PLAYER:uuid" o "CLAN:uuid")
     */
    private String getCacheKey(UUID id, NexoAccount.AccountType type) {
        return type.name() + ":" + id.toString();
    }

    /**
     * 🌟 MÉTODO NUEVO DE SEGURIDAD (Requerido por el TradeListener)
     * Revisa de forma instantánea O(1) si el jugador tiene fondos suficientes en su caché local.
     */
    public boolean hasBalance(UUID ownerId, NexoAccount.AccountType type, NexoAccount.Currency currency, BigDecimal amount) {
        NexoAccount acc = accountCache.getIfPresent(getCacheKey(ownerId, type));
        if (acc == null) return false; // Por seguridad Anti-Dupe, si no cargó la BD, denegamos el gasto.
        return acc.hasEnough(currency, amount);
    }

    /**
     * Carga una cuenta desde la DB (o la crea si no existe) de forma no bloqueante.
     */
    public CompletableFuture<NexoAccount> getAccountAsync(UUID ownerId, NexoAccount.AccountType type) {
        String cacheKey = getCacheKey(ownerId, type);
        NexoAccount cached = accountCache.getIfPresent(cacheKey);

        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }

        return CompletableFuture.supplyAsync(() -> {
            try (var conn = db.getConnection()) {

                String selectSQL = "SELECT * FROM nexo_economy WHERE id = CAST(? AS UUID)";
                try (var ps = conn.prepareStatement(selectSQL)) {
                    ps.setString(1, ownerId.toString());
                    var rs = ps.executeQuery();

                    if (rs.next()) {
                        NexoAccount acc = new NexoAccount(
                                ownerId, type,
                                rs.getBigDecimal("coins"),
                                rs.getBigDecimal("gems"),
                                rs.getBigDecimal("mana")
                        );
                        accountCache.put(cacheKey, acc);
                        return acc;
                    }
                }

                String insertSQL = "INSERT INTO nexo_economy (id, account_type) VALUES (CAST(? AS UUID), ?)";
                try (var ps = conn.prepareStatement(insertSQL)) {
                    ps.setString(1, ownerId.toString());
                    ps.setString(2, type.name());
                    ps.executeUpdate();

                    NexoAccount newAcc = new NexoAccount(ownerId, type, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
                    accountCache.put(cacheKey, newAcc);
                    return newAcc;
                }
            } catch (Exception e) {
                plugin.getLogger().severe("❌ Error cargando cuenta: " + e.getMessage());
                return null;
            }
        }, VIRTUAL_EXECUTOR); // 🚀 Inyectamos el Virtual Executor para evitar cuellos de botella
    }

    /**
     * 🛡️ TRANSACCIÓN ATÓMICA: Actualiza la DB y el Caché de forma segura (Anti-Dupe).
     */
    public CompletableFuture<Boolean> updateBalanceAsync(UUID ownerId, NexoAccount.AccountType type, NexoAccount.Currency currency, BigDecimal amount, boolean isDeposit) {
        return getAccountAsync(ownerId, type).thenApplyAsync(account -> {
            if (account == null) return false;

            if (!isDeposit && !account.hasEnough(currency, amount)) {
                return false; // No tiene fondos suficientes
            }

            String column = currency.name().toLowerCase();
            String operator = isDeposit ? "+" : "-";
            String updateSQL = "UPDATE nexo_economy SET " + column + " = " + column + " " + operator + " ? WHERE id = CAST(? AS UUID)";

            try (var conn = db.getConnection();
                 var ps = conn.prepareStatement(updateSQL)) {

                ps.setBigDecimal(1, amount);
                ps.setString(2, ownerId.toString());
                int rows = ps.executeUpdate();

                if (rows > 0) {
                    // Solo si la BD se actualizó correctamente (Commit a disco), actualizamos la RAM.
                    // Esto evita duplicaciones de saldo o saldos fantasma en caso de crasheos.
                    if (isDeposit) account.addBalance(currency, amount);
                    else account.removeBalance(currency, amount);
                    return true;
                }
            } catch (Exception e) {
                plugin.getLogger().severe("❌ Fallo de Transacción Atómica: " + e.getMessage());
            }
            return false;
        }, VIRTUAL_EXECUTOR); // 🚀 Inyectamos el Virtual Executor
    }

    public Optional<NexoAccount> getCachedAccount(UUID ownerId, NexoAccount.AccountType type) {
        return Optional.ofNullable(accountCache.getIfPresent(getCacheKey(ownerId, type)));
    }

    /**
     * 🛡️ GUARDADO SÍNCRONO BATCH: Se ejecuta en onDisable() para prevenir rollbacks.
     */
    public void saveAllAccountsSync() {
        if (accountCache.asMap().isEmpty()) return;

        String sql = "UPDATE nexo_economy SET coins = ?, gems = ?, mana = ? WHERE id = CAST(? AS UUID)";
        try (var conn = db.getConnection();
             var ps = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false); // 🌟 OPTIMIZACIÓN: Transacciones en Lote (Batch Processing) para proteger el disco duro.

            for (NexoAccount acc : accountCache.asMap().values()) {
                ps.setBigDecimal(1, acc.getBalance(NexoAccount.Currency.COINS));
                ps.setBigDecimal(2, acc.getBalance(NexoAccount.Currency.GEMS));
                ps.setBigDecimal(3, acc.getBalance(NexoAccount.Currency.MANA));
                ps.setString(4, acc.getOwnerId().toString());
                ps.addBatch();
            }

            ps.executeBatch();
            conn.commit();
            plugin.getLogger().info("✅ Se guardaron " + accountCache.estimatedSize() + " cuentas exitosamente (Batch Mode).");

        } catch (Exception e) {
            plugin.getLogger().severe("❌ Error en guardado de emergencia de economía: " + e.getMessage());
        }
    }
}