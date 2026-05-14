package me.aeroxis.core.database;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.aeroxis.core.AeroxisCore;
import me.aeroxis.core.config.ConfigManager;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 🏛️ Nexo Network - Database Manager (Motor HikariCP + Java 21)
 * Arquitectura Enterprise: @Singleton puro (sin statics), I/O con Virtual Threads
 * y CountDownLatch para evitar Race Conditions en el arranque temprano.
 */
@Singleton
public class DatabaseManager {

    private final AeroxisCore plugin;
    private final ConfigManager configManager;
    private HikariDataSource dataSource;

    // 🌟 Candado de concurrencia: Evita que submódulos pidan datos antes de que las tablas existan
    private final CountDownLatch latch = new CountDownLatch(1);

    // 🚀 Ejecutor de Hilos Virtuales (Java 21+)
    private final ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();

    @Inject
    public DatabaseManager(AeroxisCore plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public void conectar() {
        if (dataSource != null && dataSource.isRunning()) return;

        try {
            var config = new HikariConfig();

            @SuppressWarnings("deprecation")
            var yaml = configManager.getConfig("config.yml");

            config.setJdbcUrl(yaml.getString("database.url"));
            config.setUsername(yaml.getString("database.username"));
            config.setPassword(yaml.getString("database.password"));
            config.setDriverClassName("org.postgresql.Driver");

            config.setMaximumPoolSize(30); // 🌟 Súbelo a 30
            config.setMinimumIdle(5);      // 🌟 Súbelo a 5
            config.setIdleTimeout(30000);
            config.setMaxLifetime(1800000);
            config.setConnectionTimeout(10000);

            this.dataSource = new HikariDataSource(config);

            // 🚀 Delegamos la creación de tablas al Ejecutor Virtual de forma segura
            virtualExecutor.submit(this::crearTablas);

        } catch (Exception e) {
            plugin.getLogger().severe("❌ ERROR: No se pudo conectar a la base de datos Supabase.");
            latch.countDown(); // Liberamos en caso de error para no congelar el ecosistema
        }
    }

    public void desconectar() {
        if (dataSource != null && dataSource.isRunning()) {
            dataSource.close(); // close() es el estándar moderno en Java (AutoCloseable)
            virtualExecutor.shutdown(); // Limpiamos el pool virtual al apagar
        }
    }

    public Connection getConnection() throws SQLException {
        try {
            // 🛡️ Protegemos el ecosistema: Si un módulo pide DB muy rápido, lo hacemos esperar (Max 15s)
            if (!latch.await(15, TimeUnit.SECONDS)) {
                throw new SQLException("Timeout: La base de datos tardó demasiado en inicializar las tablas.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SQLException("Hilo interrumpido esperando a la base de datos.");
        }

        if (dataSource == null) throw new SQLException("DataSource no inicializado.");
        return dataSource.getConnection();
    }

    public HikariDataSource getDataSource() {
        return dataSource;
    }

    private void crearTablas() {
        if (dataSource == null) {
            latch.countDown();
            return;
        }

        String sqlJugadores = """
                CREATE TABLE IF NOT EXISTS jugadores (
                    uuid VARCHAR(36) PRIMARY KEY, nombre VARCHAR(16) NOT NULL,
                    nexo_nivel INT DEFAULT 1, nexo_xp INT DEFAULT 0,
                    combate_nivel INT DEFAULT 1, combate_xp INT DEFAULT 0,
                    mineria_nivel INT DEFAULT 1, mineria_xp INT DEFAULT 0,
                    agricultura_nivel INT DEFAULT 1, agricultura_xp INT DEFAULT 0,
                    clan_id UUID DEFAULT NULL, clan_role VARCHAR(15) DEFAULT 'NONE',
                    blessings TEXT DEFAULT ''
                );""";

        String sqlMochilas = "CREATE TABLE IF NOT EXISTS mochilas (uuid VARCHAR(36), mochila_id INT, contenido TEXT, PRIMARY KEY (uuid, mochila_id));";
        String sqlGuardarropa = "CREATE TABLE IF NOT EXISTS guardarropa (uuid VARCHAR(36), preset_id INT, contenido TEXT, PRIMARY KEY (uuid, preset_id));";
        String sqlStorage = "CREATE TABLE IF NOT EXISTS nexo_storage (uuid VARCHAR(36), tipo VARCHAR(32), contenido TEXT, PRIMARY KEY (uuid, tipo));";
        String sqlColecciones = "CREATE TABLE IF NOT EXISTS nexo_collections (uuid VARCHAR(36) PRIMARY KEY, collections_data JSONB NOT NULL DEFAULT '{}'::jsonb);";

        // 🌟 Tablas para el sistema de moderación (NexoStaff)
        String sqlCastigos = """
                CREATE TABLE IF NOT EXISTS nexo_punishments (
                    id SERIAL PRIMARY KEY, uuid VARCHAR(36) NOT NULL,
                    target_name VARCHAR(16) NOT NULL, staff_name VARCHAR(16) NOT NULL,
                    type VARCHAR(10) NOT NULL, reason TEXT NOT NULL,
                    date_issued BIGINT NOT NULL, duration BIGINT NOT NULL,
                    active BOOLEAN DEFAULT TRUE
                );""";

        String sqlIps = """
                CREATE TABLE IF NOT EXISTS player_ips (
                    uuid VARCHAR(36) PRIMARY KEY, name VARCHAR(16) NOT NULL, ip VARCHAR(45) NOT NULL
                );""";

        // 🌟 Tablas para el sistema de utilidades (NexoTools)
        String sqlHomes = """
                CREATE TABLE IF NOT EXISTS nexo_homes (
                    uuid VARCHAR(36) NOT NULL,
                    name VARCHAR(32) NOT NULL,
                    world VARCHAR(64) NOT NULL,
                    x DOUBLE PRECISION NOT NULL,
                    y DOUBLE PRECISION NOT NULL,
                    z DOUBLE PRECISION NOT NULL,
                    yaw REAL NOT NULL,
                    pitch REAL NOT NULL,
                    PRIMARY KEY (uuid, name)
                );""";

        String sqlWarps = """
                CREATE TABLE IF NOT EXISTS nexo_warps (
                    name VARCHAR(32) PRIMARY KEY,
                    world VARCHAR(64) NOT NULL,
                    x DOUBLE PRECISION NOT NULL,
                    y DOUBLE PRECISION NOT NULL,
                    z DOUBLE PRECISION NOT NULL,
                    yaw REAL NOT NULL,
                    pitch REAL NOT NULL,
                    permission VARCHAR(64)
                );""";

        String sqlTpaBlocks = """
                CREATE TABLE IF NOT EXISTS tpa_blocks (
                    uuid VARCHAR(36) NOT NULL,
                    blocked_uuid VARCHAR(36) NOT NULL,
                    PRIMARY KEY (uuid, blocked_uuid)
                );""";

        // 🌟 NUEVO: Tablas para el sistema AAA de Cajas (NexoCrates)
        String sqlCratesKeys = """
                CREATE TABLE IF NOT EXISTS nexo_crates_keys (
                    uuid VARCHAR(36) NOT NULL,
                    crate_id VARCHAR(32) NOT NULL,
                    amount INT DEFAULT 0,
                    PRIMARY KEY (uuid, crate_id)
                );""";

        String sqlCratesPity = """
                CREATE TABLE IF NOT EXISTS nexo_crates_pity (
                    uuid VARCHAR(36) NOT NULL,
                    crate_id VARCHAR(32) NOT NULL,
                    pity_count INT DEFAULT 0,
                    PRIMARY KEY (uuid, crate_id)
                );""";

        String sqlCratesHistory = """
                CREATE TABLE IF NOT EXISTS nexo_crates_history (
                    id SERIAL PRIMARY KEY,
                    uuid VARCHAR(36) NOT NULL,
                    player_name VARCHAR(16) NOT NULL,
                    crate_id VARCHAR(32) NOT NULL,
                    reward_id VARCHAR(64) NOT NULL,
                    timestamp BIGINT NOT NULL
                );""";

        // 🚨 ANTI-DEADLOCK: Usamos dataSource.getConnection() directamente.
        try (var conn = dataSource.getConnection(); var stmt = conn.createStatement()) {
            stmt.execute(sqlJugadores);

            // 🌟 Migraciones dinámicas seguras (Evitan romper BD en producción)
            try { stmt.execute("ALTER TABLE jugadores ADD COLUMN IF NOT EXISTS blessings TEXT DEFAULT '';"); } catch (Exception ignored) {}
            try { stmt.execute("ALTER TABLE jugadores ADD COLUMN IF NOT EXISTS void_blessing_until BIGINT DEFAULT 0;"); } catch (Exception ignored) {}
            try { stmt.execute("ALTER TABLE jugadores ADD COLUMN IF NOT EXISTS web_password TEXT;"); } catch (Exception ignored) {}
            try { stmt.execute("ALTER TABLE jugadores ADD COLUMN IF NOT EXISTS chat_color VARCHAR(64) DEFAULT '<gray>';"); } catch (Exception ignored) {}
            try { stmt.execute("ALTER TABLE jugadores ADD COLUMN IF NOT EXISTS unlocked_cosmetics TEXT DEFAULT '';"); } catch (Exception ignored) {}

            stmt.execute(sqlMochilas);
            stmt.execute(sqlGuardarropa);
            stmt.execute(sqlStorage);
            stmt.execute(sqlColecciones);
            stmt.execute(sqlCastigos);
            stmt.execute(sqlIps);
            stmt.execute(sqlHomes);
            stmt.execute(sqlWarps);
            stmt.execute(sqlTpaBlocks);

            // 🌟 NUEVO: Ejecutamos las tablas de NexoCrates
            stmt.execute(sqlCratesKeys);
            stmt.execute(sqlCratesPity);
            stmt.execute(sqlCratesHistory);

            plugin.getLogger().info("✅ ¡Conexión a Supabase establecida y tablas verificadas (Virtual Threads)!");
        } catch (SQLException e) {
            plugin.getLogger().severe("Error al crear tablas: " + e.getMessage());
        } finally {
            // 🔓 Liberamos el candado para todo el ecosistema (Pase lo que pase)
            latch.countDown();
        }
    }
}