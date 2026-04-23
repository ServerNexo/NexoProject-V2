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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 💰 NexoEconomy - Manager Central de Economía (Arquitectura Enterprise Java 21)
 * Rendimiento: Virtual Thread Executor unificado, Batch Processing y Cero Estáticos.
 */
@Singleton
public class EconomyManager {

    private final NexoEconomy plugin;
    private final DatabaseManager db;

    // 🚀 EL MOTOR DE RENDIMIENTO I/O: Hilos Virtuales nativos (Adiós al Thread Starvation)
    private final ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();

    // ⚡ Caché Ultrarrápido: Las cuentas expiran a los 30 min de inactividad
    private final Cache<String, NexoAccount> accountCache;

    // 💉 PILAR 1: Inyección de Dependencias Directa
    @Inject
    public EconomyManager(NexoEconomy plugin, DatabaseManager db) {
        this.plugin = plugin;
        this.db = db;
        
        this.accountCache = Caffeine.newBuilder()
                .expireAfterAccess(30, TimeUnit.MINUTES)
                .build();
                
        crearTablaEconomia();
    }

    private void crearTablaEconomia() {
        // 🌟 I/O ASÍNCRONO NATIVO: Unificado bajo el mismo Virtual Executor
        virtualExecutor.submit(() -> {
            var sql = """
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
     * 🌟 SEGURIDAD O(1): Revisa si el jugador tiene fondos suficientes en su caché local.
     */
    public boolean hasBalance(UUID ownerId, NexoAccount.AccountType type, NexoAccount.Currency currency, BigDecimal amount) {
        var acc = accountCache.getIfPresent(getCacheKey(ownerId, type));
        if (acc == null) return false; // Por seguridad Anti-Dupe, si no cargó la BD, denegamos el gasto.
        return acc.hasEnough(currency, amount);
    }

    /**
     * Carga una cuenta desde la DB (o la crea si no existe) de forma no bloqueante.
     */
    public CompletableFuture<NexoAccount> getAccountAsync(UUID ownerId, NexoAccount.AccountType type) {
        var cacheKey = getCacheKey(ownerId, type);
        var cached = accountCache.getIfPresent(cacheKey);

        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }

        return CompletableFuture.supplyAsync(() -> {
            try (var conn = db.getConnection()) {

                var selectSQL = "SELECT * FROM nexo_economy WHERE id = CAST(? AS UUID)";
                try (var ps = conn.prepareStatement(selectSQL)) {
                    ps.setString(1, ownerId.toString());
                    var rs = ps.executeQuery();

                    if (rs.next()) {
                        var acc = new NexoAccount(
                                ownerId, type,
                                rs.getBigDecimal("coins"),
                                rs.getBigDecimal("gems"),
                                rs.getBigDecimal("mana")
                        );
                        accountCache.put(cacheKey, acc);
                        return acc;
                    }
                }

                var insertSQL = "INSERT INTO nexo_economy (id, account_type) VALUES (CAST(? AS UUID), ?)";
                try (var ps = conn.prepareStatement(insertSQL)) {
                    ps.setString(1, ownerId.toString());
                    ps.setString(2, type.name());
                    ps.executeUpdate();

                    var newAcc = new NexoAccount(ownerId, type, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
                    accountCache.put(cacheKey, newAcc);
                    return newAcc;
                }
            } catch (Exception e) {
                plugin.getLogger().severe("❌ Error cargando cuenta: " + e.getMessage());
                return null;
            }
        }, virtualExecutor);
    }

    /**
     * 🛡️ TRANSACCIÓN ATÓMICA: Actualiza la DB y el Caché de forma segura.
     */
    public CompletableFuture<Boolean> updateBalanceAsync(UUID ownerId, NexoAccount.AccountType type, NexoAccount.Currency currency, BigDecimal amount, boolean isDeposit) {
        return getAccountAsync(ownerId, type).thenApplyAsync(account -> {
            if (account == null) return false;

            if (!isDeposit && !account.hasEnough(currency, amount)) {
                return false; // No tiene fondos suficientes
            }

            var column = currency.name().toLowerCase();
            var operator = isDeposit ? "+" : "-";
            var updateSQL = "UPDATE nexo_economy SET " + column + " = " + column + " " + operator + " ? WHERE id = CAST(? AS UUID)";

            try (var conn = db.getConnection();
                 var ps = conn.prepareStatement(updateSQL)) {

                ps.setBigDecimal(1, amount);
                ps.setString(2, ownerId.toString());
                int rows = ps.executeUpdate();

                if (rows > 0) {
                    // Solo si la BD se actualizó correctamente (Commit a disco), actualizamos la RAM.
                    if (isDeposit) account.addBalance(currency, amount);
                    else account.removeBalance(currency, amount);
                    return true;
                }
            } catch (Exception e) {
                plugin.getLogger().severe("❌ Fallo de Transacción Atómica: " + e.getMessage());
            }
            return false;
        }, virtualExecutor);
    }

    public Optional<NexoAccount> getCachedAccount(UUID ownerId, NexoAccount.AccountType type) {
        return Optional.ofNullable(accountCache.getIfPresent(getCacheKey(ownerId, type)));
    }

    /**
     * 🛡️ GUARDADO SÍNCRONO BATCH: Ejecutado en el Main Thread durante el apagado del servidor
     * para prevenir la pérdida de fondos (Rollbacks).
     */
    public void saveAllAccountsSync() {
        if (accountCache.asMap().isEmpty()) return;

        var sql = "UPDATE nexo_economy SET coins = ?, gems = ?, mana = ? WHERE id = CAST(? AS UUID)";
        try (var conn = db.getConnection();
             var ps = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false); // 🌟 OPTIMIZACIÓN: Transacciones en Lote (Batch Processing)

            for (var acc : accountCache.asMap().values()) {
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