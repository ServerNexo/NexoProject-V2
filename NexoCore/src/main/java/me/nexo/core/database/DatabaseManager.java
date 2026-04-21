package me.nexo.core.database;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.nexo.core.NexoCore;
import me.nexo.core.config.ConfigManager;

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

    private final NexoCore plugin;
    private final ConfigManager configManager;
    private HikariDataSource dataSource;

    // 🌟 Candado de concurrencia: Evita que submódulos pidan datos antes de que las tablas existan
    private final CountDownLatch latch = new CountDownLatch(1);

    // 🚀 Ejecutor de Hilos Virtuales (Java 21+)
    private final ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();

    @Inject
    public DatabaseManager(NexoCore plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public void conectar() {
        if (dataSource != null && !dataSource.isClosed()) return;

        try {
            var config = new HikariConfig();
            var yaml = configManager.getConfig("config.yml");

            config.setJdbcUrl(yaml.getString("database.url"));
            config.setUsername(yaml.getString("database.username"));
            config.setPassword(yaml.getString("database.password"));
            config.setDriverClassName("org.postgresql.Driver");

            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
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
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
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

        // 🚨 ANTI-DEADLOCK: Usamos dataSource.getConnection() directamente.
        // Si usáramos this.getConnection(), este hilo esperaría infinitamente a que el latch se abra.
        try (var conn = dataSource.getConnection(); var stmt = conn.createStatement()) {
            stmt.execute(sqlJugadores);

            try { stmt.execute("ALTER TABLE jugadores ADD COLUMN IF NOT EXISTS blessings TEXT DEFAULT '';"); } catch (Exception ignored) {}
            try { stmt.execute("ALTER TABLE jugadores ADD COLUMN IF NOT EXISTS void_blessing_until BIGINT DEFAULT 0;"); } catch (Exception ignored) {}
            try { stmt.execute("ALTER TABLE jugadores ADD COLUMN IF NOT EXISTS web_password TEXT;"); } catch (Exception ignored) {}

            stmt.execute(sqlMochilas);
            stmt.execute(sqlGuardarropa);
            stmt.execute(sqlStorage);
            stmt.execute(sqlColecciones);

            plugin.getLogger().info("✅ ¡Conexión a Supabase establecida y tablas verificadas (Virtual Threads)!");
        } catch (SQLException e) {
            plugin.getLogger().severe("Error al crear tablas: " + e.getMessage());
        } finally {
            // 🔓 Liberamos el candado para todo el ecosistema (Pase lo que pase)
            latch.countDown();
        }
    }
}