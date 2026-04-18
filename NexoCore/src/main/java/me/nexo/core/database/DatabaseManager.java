package me.nexo.core.database;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.nexo.core.NexoCore;
import me.nexo.core.config.ConfigManager;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * 🏛️ Nexo Network - Database Manager (Motor HikariCP + Java 25)
 * I/O O(1): Usa Hilos Virtuales Nativos para evitar el Thread Starvation de Bukkit.
 */
@Singleton
public class DatabaseManager {

    private final NexoCore plugin;
    private final ConfigManager configManager;
    private HikariDataSource dataSource;

    @Inject
    public DatabaseManager(NexoCore plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public void conectar() {
        try {
            var config = new HikariConfig(); // 🌟 Java 21+ var
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
            crearTablas(); // 🚀 Esto ahora corre en un Hilo Virtual

            plugin.getLogger().info("✅ ¡Conexión a Supabase establecida (HikariCP + Virtual Threads)!");

        } catch (Exception e) {
            plugin.getLogger().severe("❌ ERROR: No se pudo conectar a la base de datos Supabase.");
        }
    }

    public void desconectar() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null) throw new SQLException("DataSource no inicializado.");
        return dataSource.getConnection();
    }

    public HikariDataSource getDataSource() {
        return dataSource;
    }

    private void crearTablas() {
        if (dataSource == null) return;

        // 🌟 Text Blocks nativos de Java para SQL más limpio
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

        // 🚀 JAVA 25 VIRTUAL THREADS: Extirpamos el Bukkit.getScheduler()
        // Este hilo no cuesta nada, se crea, hace el I/O en la DB, y muere sin molestar al procesador central.
        Thread.startVirtualThread(() -> {
            try (var conn = getConnection(); var stmt = conn.createStatement()) {
                stmt.execute(sqlJugadores);

                // Mantenemos tus inyecciones de ALTER TABLE seguras
                try { stmt.execute("ALTER TABLE jugadores ADD COLUMN IF NOT EXISTS blessings TEXT DEFAULT '';"); } catch (Exception ignored) {}
                try { stmt.execute("ALTER TABLE jugadores ADD COLUMN IF NOT EXISTS void_blessing_until BIGINT DEFAULT 0;"); } catch (Exception ignored) {}
                try { stmt.execute("ALTER TABLE jugadores ADD COLUMN IF NOT EXISTS web_password TEXT;"); } catch (Exception ignored) {}

                stmt.execute(sqlMochilas);
                stmt.execute(sqlGuardarropa);
                stmt.execute(sqlStorage);
                stmt.execute(sqlColecciones);
            } catch (SQLException e) {
                plugin.getLogger().severe("Error al crear tablas: " + e.getMessage());
            }
        });
    }
}