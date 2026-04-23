package me.nexo.factories.managers;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.aurelium.auraskills.api.AuraSkillsApi;
import dev.aurelium.auraskills.api.user.SkillsUser;
import me.nexo.core.database.DatabaseManager;
import me.nexo.factories.NexoFactories;
import me.nexo.factories.core.ActiveFactory;
import me.nexo.factories.logic.ScriptEvaluator;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * 🏭 NexoFactories - Manager Central de Máquinas (Arquitectura Enterprise Java 21)
 * Rendimiento: Executor Unificado, MethodHandles O(1), Caché de APIs externas y Spatial Grid.
 */
@Singleton
public class FactoryManager {

    // 🌟 DEPENDENCIAS PROPAGADAS
    private final NexoFactories plugin;
    private final DatabaseManager databaseManager;
    private final ScriptEvaluator logicEngine;

    // Caché principal (Memoria Volátil Rápida)
    private final Cache<UUID, ActiveFactory> factoryCache;

    // 🌟 OPTIMIZACIÓN O(1): Mapa espacial para búsquedas instantáneas
    private final Map<String, ActiveFactory> locationMap = new ConcurrentHashMap<>();

    // 🚀 EL MOTOR DE RENDIMIENTO I/O: Unificado y gestionado por la instancia
    private final ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();

    private static final double ENERGY_COST_PER_CYCLE = 15.0;
    private static final long CYCLE_DURATION_MS = 60_000L; // 1 Minuto por ciclo

    // 🌟 METHOD HANDLES & API CACHE
    private boolean integrationsLoaded = false;
    private boolean auraSkillsEnabled = false;
    private AuraSkillsApi auraSkillsApi; // Caché de la API externa para evitar llamadas estáticas repetitivas
    
    private Object claimManagerCache;
    private MethodHandle getStoneByIdHandle;
    private MethodHandle getCurrentEnergyHandle;

    // 💉 PILAR 1: Inyección de Dependencias Estricta
    @Inject
    public FactoryManager(NexoFactories plugin, DatabaseManager databaseManager, ScriptEvaluator logicEngine) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.logicEngine = logicEngine;

        this.factoryCache = Caffeine.newBuilder()
                .expireAfterAccess(30, TimeUnit.MINUTES)
                .maximumSize(10_000)
                .removalListener((key, value, cause) -> {
                    if (value instanceof ActiveFactory factory) {
                        locationMap.remove(serializeLocation(factory.getCoreLocation()));
                    }
                })
                .build();
    }

    /**
     * 🧠 Cachea las dependencias cruzadas O(1) usando MethodHandles para no asfixiar el Tick Loop
     */
    private void setupIntegrations() {
        if (integrationsLoaded) return;
        try {
            if (Bukkit.getPluginManager().isPluginEnabled("AuraSkills")) {
                auraSkillsApi = AuraSkillsApi.get();
                auraSkillsEnabled = true;
            }

            if (Bukkit.getPluginManager().isPluginEnabled("NexoProtections")) {
                // Obtenemos el ClaimManager de forma segura vía Reflection sin depender del ServiceLocator Estático
                Class<?> apiClass = Class.forName("me.nexo.core.user.NexoAPI");
                Object services = apiClass.getMethod("getServices").invoke(null);
                claimManagerCache = services.getClass().getMethod("get", Class.class).invoke(services, Class.forName("me.nexo.protections.managers.ClaimManager"));

                // Desempaquetamos el Optional si es necesario
                if (claimManagerCache instanceof java.util.Optional<?> opt && opt.isPresent()) {
                    claimManagerCache = opt.get();
                }

                if (claimManagerCache != null) {
                    MethodHandles.Lookup lookup = MethodHandles.publicLookup();
                    Class<?> claimClass = Class.forName("me.nexo.protections.managers.ClaimManager");
                    Class<?> stoneClass = Class.forName("me.nexo.protections.core.ProtectionStone");

                    // 🌟 Vinculación a nivel de Bytecode
                    getStoneByIdHandle = lookup.findVirtual(claimClass, "getStoneById", MethodType.methodType(Object.class, UUID.class));
                    getCurrentEnergyHandle = lookup.findVirtual(stoneClass, "getCurrentEnergy", MethodType.methodType(double.class));
                }
            }
        } catch (Throwable ignored) {
            // MethodHandles tira Throwable en vez de Exception. Falla silenciosa permitida.
        }
        integrationsLoaded = true;
    }

    // ==========================================
    // 🗄️ CARGA Y GUARDADO ASÍNCRONO (Virtual Threads)
    // ==========================================
    public CompletableFuture<Void> loadFactoriesAsync() {
        return CompletableFuture.runAsync(() -> {
            String sql = "SELECT * FROM nexo_factories";
            try (var conn = databaseManager.getConnection();
                 var ps = conn.prepareStatement(sql);
                 var rs = ps.executeQuery()) {

                while (rs.next()) {
                    String[] locParts = rs.getString("core_location").split(",");
                    var world = Bukkit.getWorld(locParts[0]);
                    if (world == null) continue;

                    var coreLocation = new Location(world, Double.parseDouble(locParts[1]), Double.parseDouble(locParts[2]), Double.parseDouble(locParts[3]));

                    var factory = new ActiveFactory(
                            UUID.fromString(rs.getString("id")),
                            UUID.fromString(rs.getString("stone_id")),
                            UUID.fromString(rs.getString("owner_id")),
                            rs.getString("factory_type"),
                            rs.getInt("level"),
                            rs.getString("current_status"),
                            rs.getInt("stored_output"),
                            coreLocation,
                            rs.getString("catalyst_item"),
                            rs.getString("json_logic"),
                            rs.getLong("last_evaluation")
                    );

                    factoryCache.put(factory.getId(), factory);
                    locationMap.put(serializeLocation(coreLocation), factory);
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "❌ Error cargando las fábricas desde la DB", e);
            }
        }, virtualExecutor);
    }

    // ==========================================
    // ⚙️ MOTOR INDUSTRIAL (Tick Engine)
    // ==========================================
    public void tickFactories() {
        // 🌟 FIX: Unificado al Executor para evitar hilos huérfanos
        virtualExecutor.execute(() -> {
            setupIntegrations(); // O(1) Carga la reflexión ultra veloz la primera vez
            long now = System.currentTimeMillis();

            for (ActiveFactory factory : factoryCache.asMap().values()) {
                long diff = now - factory.getLastEvaluationTime();
                if (diff < CYCLE_DURATION_MS) continue; // Aún no le toca ciclo

                long cycles = diff / CYCLE_DURATION_MS;

                // Si no tenemos el ClaimManager, asumimos que no hay escudo de energía (Fábrica Gratis)
                if (claimManagerCache == null || getStoneByIdHandle == null || getCurrentEnergyHandle == null) {
                    procesarProduccion(factory, cycles, now, diff, 1000000.0); // Energía infinita simulada
                    continue;
                }

                try {
                    // 🚀 MethodHandle invoke: Misma velocidad que llamar a un método nativo
                    Object stone = getStoneByIdHandle.invoke(claimManagerCache, factory.getStoneId());

                    if (stone == null) {
                        factory.setCurrentStatus("NO_STONE");
                        continue;
                    }

                    double currentEnergy = (double) getCurrentEnergyHandle.invoke(stone);

                    // 🧠 Evaluador Lógico Cacheado O(1)
                    if (!logicEngine.shouldRun(factory, null, factory.getJsonLogic())) {
                        factory.setCurrentStatus("SCRIPT_PAUSED");
                        continue;
                    }

                    procesarProduccion(factory, cycles, now, diff, currentEnergy);

                } catch (Throwable e) {
                    factory.setCurrentStatus("ERROR");
                }
            }
        });
    }

    private void procesarProduccion(ActiveFactory factory, long cycles, long now, long diff, double availableEnergy) {
        double requiredEnergy = ENERGY_COST_PER_CYCLE * cycles;
        long actualCycles = (availableEnergy < requiredEnergy) ? (long) (availableEnergy / ENERGY_COST_PER_CYCLE) : cycles;

        if (actualCycles > 0) {
            double multiplier = getProfessionMultiplier(factory.getOwnerId(), factory.getFactoryType());
            if (factory.getCatalystItem() != null && factory.getCatalystItem().equals("OVERCLOCK_T1")) {
                multiplier += 0.5;
            }
            int finalOutput = (int) Math.round((factory.getLevel() * 2) * multiplier * actualCycles);
            factory.addOutput(finalOutput);
        }

        factory.setCurrentStatus(actualCycles == cycles ? "ACTIVE" : "NO_ENERGY");
        factory.setLastEvaluationTime(now - (diff % CYCLE_DURATION_MS));
        saveFactoryStatusAsync(factory); // Auto-guardado
    }

    private double getProfessionMultiplier(UUID ownerId, String factoryType) {
        if (!auraSkillsEnabled || auraSkillsApi == null) return 1.0; 

        try {
            SkillsUser user = auraSkillsApi.getUser(ownerId);
            if (user != null) {
                // 🌟 Pattern Matching de Java 21 (Velocidad pura)
                int level = switch (factoryType) {
                    case String s when s.contains("MINA") || s.contains("FORJA") -> user.getSkillLevel(dev.aurelium.auraskills.api.skill.Skills.MINING);
                    case String s when s.contains("ASERRADERO") -> user.getSkillLevel(dev.aurelium.auraskills.api.skill.Skills.FORAGING);
                    case String s when s.contains("GRANJA") -> user.getSkillLevel(dev.aurelium.auraskills.api.skill.Skills.FARMING);
                    default -> 1;
                };

                return 1.0 + (level * 0.02); // 2% de bonus por nivel de habilidad
            }
        } catch (Throwable ignored) {}

        return 1.0;
    }

    // ==========================================
    // 🗄️ SQL Y GUARDADOS EN BATCH
    // ==========================================
    public CompletableFuture<Void> createFactoryAsync(ActiveFactory factory) {
        factory.setLastEvaluationTime(System.currentTimeMillis());
        factoryCache.put(factory.getId(), factory);
        locationMap.put(serializeLocation(factory.getCoreLocation()), factory);

        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO nexo_factories (id, stone_id, owner_id, factory_type, level, current_status, stored_output, core_location, last_evaluation, catalyst_item, json_logic) VALUES (CAST(? AS UUID), CAST(? AS UUID), CAST(? AS UUID), ?, ?, ?, ?, ?, ?, ?, ?)";
            try (var conn = databaseManager.getConnection();
                 var ps = conn.prepareStatement(sql)) {

                ps.setString(1, factory.getId().toString());
                ps.setString(2, factory.getStoneId().toString());
                ps.setString(3, factory.getOwnerId().toString());
                ps.setString(4, factory.getFactoryType());
                ps.setInt(5, factory.getLevel());
                ps.setString(6, factory.getCurrentStatus());
                ps.setInt(7, factory.getStoredOutput());
                ps.setString(8, serializeLocation(factory.getCoreLocation()));
                ps.setLong(9, factory.getLastEvaluationTime());
                ps.setString(10, factory.getCatalystItem());
                ps.setString(11, factory.getJsonLogic());

                ps.executeUpdate();
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "❌ Error creando fábrica en la DB", e);
            }
        }, virtualExecutor);
    }

    public void saveFactoryStatusAsync(ActiveFactory factory) {
        // 🌟 FIX: Unificado al Executor
        virtualExecutor.execute(() -> {
            String sql = "UPDATE nexo_factories SET current_status = ?, stored_output = ?, last_evaluation = ? WHERE id = CAST(? AS UUID)";
            try (var conn = databaseManager.getConnection();
                 var ps = conn.prepareStatement(sql)) {

                ps.setString(1, factory.getCurrentStatus());
                ps.setInt(2, factory.getStoredOutput());
                ps.setLong(3, factory.getLastEvaluationTime());
                ps.setString(4, factory.getId().toString());
                ps.executeUpdate();
            } catch (Exception ignored) {}
        });
    }

    public void saveAllFactoriesSync() {
        String sql = "UPDATE nexo_factories SET current_status = ?, stored_output = ?, last_evaluation = ? WHERE id = CAST(? AS UUID)";
        try (var conn = databaseManager.getConnection();
             var ps = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false); // 🌟 OPTIMIZACIÓN: Batch Mode Activo

            for (ActiveFactory factory : factoryCache.asMap().values()) {
                ps.setString(1, factory.getCurrentStatus());
                ps.setInt(2, factory.getStoredOutput());
                ps.setLong(3, factory.getLastEvaluationTime());
                ps.setString(4, factory.getId().toString());
                ps.addBatch();
            }
            ps.executeBatch();
            conn.commit();
            plugin.getLogger().info("💾 [AUTO-SAVE] Progreso industrial de todas las fábricas guardado en lote.");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "❌ Error en guardado síncrono", e);
        }
    }

    // ==========================================
    // 📍 BÚSQUEDAS ESPACIALES O(1)
    // ==========================================
    public ActiveFactory getFactoryAt(Location loc) {
        return locationMap.get(serializeLocation(loc));
    }

    private String serializeLocation(Location loc) {
        if (loc == null || loc.getWorld() == null) return "null";
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }
}