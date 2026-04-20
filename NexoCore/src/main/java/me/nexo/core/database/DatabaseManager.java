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
import java.util.concurrent.TimeUnit;

/**
 * 🏛️ Nexo Network - Database Manager (Motor HikariCP)
 * 🌟 ARQUITECTURA SEGURA: DataSource estático para ser compartido entre los múltiples Inyectores de Guice.
 */
@Singleton
public class DatabaseManager {

    private final NexoCore plugin;
    private final ConfigManager configManager;

    // 🌟 FIX MAGISTRAL: Al hacerlo estático, todos los submódulos que pidan
    // un DatabaseManager a Guice compartirán este mismo Pool de conexiones
    // en lugar de crear uno nuevo y vacío.
    private static HikariDataSource dataSource;

    // 🌟 FIX ENTERPRISE: Candado de concurrencia para bloquear el acceso hasta que Hikari esté 100% listo.
    private static final CountDownLatch latch = new CountDownLatch(1);

    @Inject
    public DatabaseManager(NexoCore plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public void conectar() {
        // 🛡️ Prevenimos que otro módulo intente reconectar por error o borrar la conexión actual
        if (dataSource != null && !dataSource.isClosed()) {
            return;
        }

        try {
            plugin.getLogger().info("Conectando a la base de datos de Supabase...");
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

            // 🌟 1. Inicialización de DataSource SÍNCRONA compartida
            dataSource = new HikariDataSource(config);

            // 🌟 2. Creación de tablas SÍNCRONA
            crearTablas();

            plugin.getLogger().info("✅ ¡Conexión a Supabase establecida y tablas verificadas!");

        } catch (Exception e) {
            plugin.getLogger().severe("❌ ERROR FATAL: No se pudo conectar a la base de datos Supabase.");
            e.printStackTrace();
        } finally {
            // 🌟 FIX ANTI-DEADLOCK: Siempre abrimos el candado al terminar, pase lo que pase.
            latch.countDown();
        }
    }

    public void desconectar() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("Base de datos desconectada correctamente.");
        }
    }

    public Connection getConnection() throws SQLException {
        try {
            // 🛡️ Si la BD no está lista, obligamos al submódulo a esperar máximo 15 segundos en vez de crashear.
            if (!latch.await(15, TimeUnit.SECONDS)) {
                throw new SQLException("Timeout extremo: La base de datos tardó demasiado en inicializarse.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SQLException("Interrumpido mientras se esperaba a la base de datos.");
        }

        if (dataSource == null) throw new SQLException("DataSource no inicializado.");
        return dataSource.getConnection();
    }

    public HikariDataSource getDataSource() {
        return dataSource;
    }

    private void crearTablas() {
        if (dataSource == null) return;

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

        // 🌟 FIX ANTI-DEADLOCK: Usamos dataSource.getConnection() directamente para no pedirnos el candado a nosotros mismos.
        try (var conn = dataSource.getConnection(); var stmt = conn.createStatement()) {
            stmt.execute(sqlJugadores);

            // Mantenemos las inyecciones de ALTER TABLE seguras
            try { stmt.execute("ALTER TABLE jugadores ADD COLUMN IF NOT EXISTS blessings TEXT DEFAULT '';"); } catch (Exception ignored) {}
            try { stmt.execute("ALTER TABLE jugadores ADD COLUMN IF NOT EXISTS void_blessing_until BIGINT DEFAULT 0;"); } catch (Exception ignored) {}
            try { stmt.execute("ALTER TABLE jugadores ADD COLUMN IF NOT EXISTS web_password TEXT;"); } catch (Exception ignored) {}

            stmt.execute(sqlMochilas);
            stmt.execute(sqlGuardarropa);
            stmt.execute(sqlStorage);
            stmt.execute(sqlColecciones);
        } catch (SQLException e) {
            plugin.getLogger().severe("Error crítico al crear las tablas base: " + e.getMessage());
        }
    }
}