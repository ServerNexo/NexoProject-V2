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
import me.nexo.factories.visuals.FactoryVisualEngine;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.inventory.ItemStack;

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
 * Rendimiento: Executor Unificado, Folia RegionScheduler, Spatial Grid y Deferred Routing.
 */
@Singleton
public class FactoryManager {

    private final NexoFactories plugin;
    private final DatabaseManager databaseManager;
    private final ScriptEvaluator logicEngine;
    private final FactoryVisualEngine visualEngine;

    private final Cache<UUID, ActiveFactory> factoryCache;
    private final Map<String, ActiveFactory> locationMap = new ConcurrentHashMap<>();
    private final ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();

    private static final double ENERGY_COST_PER_CYCLE = 15.0;

    // 🌟 MODO DEV: 5 Segundos
    private static final long CYCLE_DURATION_MS = 5_000L;

    private boolean integrationsLoaded = false;
    private boolean auraSkillsEnabled = false;
    private AuraSkillsApi auraSkillsApi;

    private Object claimManagerCache;
    private MethodHandle getStoneByIdHandle;
    private MethodHandle getCurrentEnergyHandle;

    @Inject
    public FactoryManager(NexoFactories plugin, DatabaseManager databaseManager, ScriptEvaluator logicEngine, FactoryVisualEngine visualEngine) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.logicEngine = logicEngine;
        this.visualEngine = visualEngine;

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

    private void setupIntegrations() {
        if (integrationsLoaded) return;
        try {
            if (Bukkit.getPluginManager().isPluginEnabled("AuraSkills")) {
                auraSkillsApi = AuraSkillsApi.get();
                auraSkillsEnabled = true;
            }

            if (Bukkit.getPluginManager().isPluginEnabled("NexoProtections")) {
                Class<?> apiClass = Class.forName("me.nexo.core.user.NexoAPI");
                Object services = apiClass.getMethod("getServices").invoke(null);
                claimManagerCache = services.getClass().getMethod("get", Class.class).invoke(services, Class.forName("me.nexo.protections.managers.ClaimManager"));

                if (claimManagerCache instanceof java.util.Optional<?> opt && opt.isPresent()) {
                    claimManagerCache = opt.get();
                }

                if (claimManagerCache != null) {
                    MethodHandles.Lookup lookup = MethodHandles.publicLookup();
                    Class<?> claimClass = Class.forName("me.nexo.protections.managers.ClaimManager");
                    Class<?> stoneClass = Class.forName("me.nexo.protections.core.ProtectionStone");

                    getStoneByIdHandle = lookup.findVirtual(claimClass, "getStoneById", MethodType.methodType(Object.class, UUID.class));
                    getCurrentEnergyHandle = lookup.findVirtual(stoneClass, "getCurrentEnergy", MethodType.methodType(double.class));
                }
            }
        } catch (Throwable ignored) { }
        integrationsLoaded = true;
    }

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

                    // 🌟 FASE 2: Lectura de Enlace Logístico
                    String targetStr = rs.getString("target_link_id");
                    UUID targetLinkId = targetStr != null ? UUID.fromString(targetStr) : null;

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
                            rs.getLong("last_evaluation"),
                            targetLinkId // 🌟 Añadido
                    );

                    factoryCache.put(factory.getId(), factory);
                    locationMap.put(serializeLocation(coreLocation), factory);
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "❌ Error cargando las fábricas", e);
            }
        }, virtualExecutor);
    }

    public void tickFactories() {
        virtualExecutor.execute(() -> {
            setupIntegrations();
            long now = System.currentTimeMillis();

            for (ActiveFactory factory : factoryCache.asMap().values()) {
                long diff = now - factory.getLastEvaluationTime();

                if (diff < CYCLE_DURATION_MS) continue;

                long cycles = diff / CYCLE_DURATION_MS;

                if (claimManagerCache == null || getStoneByIdHandle == null || getCurrentEnergyHandle == null) {
                    procesarProduccion(factory, cycles, now, diff, 1000000.0);
                    continue;
                }

                try {
                    Object stone = getStoneByIdHandle.invoke(claimManagerCache, factory.getStoneId());

                    if (stone == null) {
                        factory.setCurrentStatus("NO_STONE");
                        continue;
                    }

                    double currentEnergy = (double) getCurrentEnergyHandle.invoke(stone);

                    if (!logicEngine.shouldRun(factory, currentEnergy, factory.getJsonLogic())) {
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

    // ==========================================
    // ⚙️ GESTIÓN LOGÍSTICA (OPTIMIZADA PARA VIRTUAL THREADS)
    // ==========================================
    private void procesarProduccion(ActiveFactory factory, long cycles, long now, long diff, double availableEnergy) {
        double requiredEnergy = ENERGY_COST_PER_CYCLE * cycles;
        long actualCycles = (availableEnergy < requiredEnergy) ? (long) (availableEnergy / ENERGY_COST_PER_CYCLE) : cycles;

        if (actualCycles > 0) {

            // 🌟 CÁLCULOS PESADOS EN EL HILO VIRTUAL
            String type = factory.getFactoryType().toUpperCase();
            Material matOutput = Material.IRON_INGOT;
            Material inputRequerido = null;

            if (type.contains("FORJA")) {
                inputRequerido = Material.RAW_IRON;
                matOutput = Material.IRON_INGOT;
            } else if (type.contains("ASERRADERO")) {
                matOutput = Material.OAK_LOG;
            } else if (type.contains("GRANJA")) {
                matOutput = Material.WHEAT;
            } else if (type.contains("COBBLESTONE")) {
                matOutput = Material.COBBLESTONE;
            }

            double multiplier = getProfessionMultiplier(factory.getOwnerId(), factory.getFactoryType());
            if (factory.getCatalystItem() != null && factory.getCatalystItem().equals("OVERCLOCK_T1")) {
                multiplier += 0.5;
            }
            int finalOutput = (int) Math.round((factory.getLevel() * 2) * multiplier * actualCycles);

            final Material fInputRequerido = inputRequerido;
            final Material fMatOutput = matOutput;

            // Saltamos al hilo de la región SOLO para modificar el cofre físicamente
            Bukkit.getRegionScheduler().execute(plugin, factory.getCoreLocation(), () -> {
                ejecutarLogisticaFisica(factory, actualCycles, now, diff, cycles, fInputRequerido, fMatOutput, finalOutput);
            });

        } else {
            factory.setCurrentStatus("NO_ENERGY");
            factory.setLastEvaluationTime(now - (diff % CYCLE_DURATION_MS));
            saveFactoryStatusAsync(factory);
        }
    }

    private void ejecutarLogisticaFisica(ActiveFactory factory, long actualCycles, long now, long diff, long expectedCycles,
                                         Material inputRequerido, Material matOutput, int finalOutput) {

        Block coreBlock = factory.getCoreLocation().getBlock();
        boolean tieneMateriales = true;

        // 📥 FASE 1: EXTRACCIÓN DE INPUTS FÍSICOS
        if (inputRequerido != null) {
            tieneMateriales = false;
            for (BlockFace face : new BlockFace[]{BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
                Block adjacent = coreBlock.getRelative(face);
                if (adjacent.getState() instanceof Container container) {
                    if (container.getInventory().contains(inputRequerido)) {
                        container.getInventory().removeItem(new ItemStack(inputRequerido, 1));
                        tieneMateriales = true;
                        break;
                    }
                }
            }
        }

        if (!tieneMateriales) {
            factory.setCurrentStatus("NO_INPUT");
            factory.setLastEvaluationTime(now - (diff % CYCLE_DURATION_MS));
            saveFactoryStatusAsync(factory);
            return;
        }

        // 📤 FASE 3: ENRUTAMIENTO LOGÍSTICO INALÁMBRICO (NUEVO)
        ItemStack itemAInsertar = new ItemStack(matOutput, finalOutput);

        // ¿Tiene una conexión Wi-Fi configurada hacia otra fábrica?
        if (factory.getTargetLinkId() != null) {
            ActiveFactory target = getFactoryById(factory.getTargetLinkId());

            if (target != null) {
                // CHUNK DESTINO CARGADO: Inyección directa O(1) en RAM
                target.addOutput(finalOutput);
                // Animación cruzando el cielo
                if (actualCycles == 1) {
                    visualEngine.playProductionAnimation(factory.getCoreLocation(), itemAInsertar);
                }
            } else {
                // CHUNK DESTINO DESCARGADO: Deferred Queue (No genera lag de carga de mundos)
                addDeferredOutputAsync(factory.getTargetLinkId(), finalOutput);
            }

            factory.setCurrentStatus(actualCycles == expectedCycles ? "ACTIVE" : "NO_ENERGY");
            factory.setLastEvaluationTime(now - (diff % CYCLE_DURATION_MS));
            saveFactoryStatusAsync(factory);
            return; // Terminamos aquí, ignoramos cofres físicos.
        }

        // Si NO hay red Wi-Fi, intentamos inyectar de forma clásica en cofres físicos adyacentes
        boolean insertado = false;
        for (BlockFace face : new BlockFace[]{BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP}) {
            Block adjacent = coreBlock.getRelative(face);
            if (adjacent.getState() instanceof Container container) {
                Map<Integer, ItemStack> sobrante = container.getInventory().addItem(itemAInsertar);
                if (sobrante.isEmpty()) {
                    insertado = true;
                    break;
                } else {
                    itemAInsertar = sobrante.get(0);
                }
            }
        }

        // 💾 FASE 4: ALMACENAMIENTO CACHE (Si no hay cofres ni red)
        if (!insertado && itemAInsertar != null && itemAInsertar.getAmount() > 0) {
            factory.addOutput(itemAInsertar.getAmount());
        }

        // 🌟 MAGIA VISUAL AAA (Producción clásica)
        if (actualCycles == 1) {
            visualEngine.playProductionAnimation(factory.getCoreLocation(), new ItemStack(matOutput));
        }

        factory.setCurrentStatus(actualCycles == expectedCycles ? "ACTIVE" : "NO_ENERGY");
        factory.setLastEvaluationTime(now - (diff % CYCLE_DURATION_MS));
        saveFactoryStatusAsync(factory);
    }

    private double getProfessionMultiplier(UUID ownerId, String factoryType) {
        if (!auraSkillsEnabled || auraSkillsApi == null) return 1.0;

        try {
            SkillsUser user = auraSkillsApi.getUser(ownerId);
            if (user != null) {
                int level = switch (factoryType) {
                    case String s when s.contains("MINA") || s.contains("FORJA") -> user.getSkillLevel(dev.aurelium.auraskills.api.skill.Skills.MINING);
                    case String s when s.contains("ASERRADERO") -> user.getSkillLevel(dev.aurelium.auraskills.api.skill.Skills.FORAGING);
                    case String s when s.contains("GRANJA") -> user.getSkillLevel(dev.aurelium.auraskills.api.skill.Skills.FARMING);
                    default -> 1;
                };
                return 1.0 + (level * 0.02);
            }
        } catch (Throwable ignored) {}

        return 1.0;
    }

    // ==========================================
    // 🗄️ BASE DE DATOS Y PERSISTENCIA (ACTUALIZADA)
    // ==========================================
    public CompletableFuture<Void> createFactoryAsync(ActiveFactory factory) {
        factory.setLastEvaluationTime(System.currentTimeMillis());
        factoryCache.put(factory.getId(), factory);
        locationMap.put(serializeLocation(factory.getCoreLocation()), factory);

        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO nexo_factories (id, stone_id, owner_id, factory_type, level, current_status, stored_output, core_location, last_evaluation, catalyst_item, json_logic, target_link_id) VALUES (CAST(? AS UUID), CAST(? AS UUID), CAST(? AS UUID), ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS UUID))";
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
                ps.setString(12, factory.getTargetLinkId() != null ? factory.getTargetLinkId().toString() : null); // 🌟 Añadido

                ps.executeUpdate();
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "❌ Error creando fábrica", e);
            }
        }, virtualExecutor);
    }

    public void saveFactoryStatusAsync(ActiveFactory factory) {
        virtualExecutor.execute(() -> {
            String sql = "UPDATE nexo_factories SET current_status = ?, stored_output = ?, last_evaluation = ?, target_link_id = CAST(? AS UUID) WHERE id = CAST(? AS UUID)";
            try (var conn = databaseManager.getConnection();
                 var ps = conn.prepareStatement(sql)) {

                ps.setString(1, factory.getCurrentStatus());
                ps.setInt(2, factory.getStoredOutput());
                ps.setLong(3, factory.getLastEvaluationTime());
                ps.setString(4, factory.getTargetLinkId() != null ? factory.getTargetLinkId().toString() : null); // 🌟 Añadido
                ps.setString(5, factory.getId().toString());
                ps.executeUpdate();
            } catch (Exception ignored) {}
        });
    }

    public void saveAllFactoriesSync() {
        String sql = "UPDATE nexo_factories SET current_status = ?, stored_output = ?, last_evaluation = ?, target_link_id = CAST(? AS UUID) WHERE id = CAST(? AS UUID)";
        try (var conn = databaseManager.getConnection();
             var ps = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false);

            for (ActiveFactory factory : factoryCache.asMap().values()) {
                ps.setString(1, factory.getCurrentStatus());
                ps.setInt(2, factory.getStoredOutput());
                ps.setLong(3, factory.getLastEvaluationTime());
                ps.setString(4, factory.getTargetLinkId() != null ? factory.getTargetLinkId().toString() : null); // 🌟 Añadido
                ps.setString(5, factory.getId().toString());
                ps.addBatch();
            }
            ps.executeBatch();
            conn.commit();
            plugin.getLogger().info("💾 [AUTO-SAVE] Guardado en lote exitoso.");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "❌ Error síncrono", e);
        }
    }

    // ==========================================
    // 🔍 UTILIDADES ESPACIALES Y DE ENLACE
    // ==========================================
    public ActiveFactory getFactoryAt(Location loc) {
        return locationMap.get(serializeLocation(loc));
    }

    // 🌟 NUEVO: Obtener por ID (O(1) desde Caché)
    public ActiveFactory getFactoryById(UUID id) {
        return factoryCache.getIfPresent(id);
    }

    public ActiveFactory getFactoryFromStructuralBlock(Location loc) {
        ActiveFactory coreMatch = getFactoryAt(loc);
        if (coreMatch != null) return coreMatch;

        for (ActiveFactory factory : factoryCache.asMap().values()) {
            if (!factory.getCoreLocation().getWorld().equals(loc.getWorld())) continue;
            if (factory.getCoreLocation().distanceSquared(loc) <= 25.0) return factory;
        }
        return null;
    }

    private String serializeLocation(Location loc) {
        if (loc == null || loc.getWorld() == null) return "null";
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    // 🌟 NUEVO: COLA DIFERIDA ANTI-LAG (Añade ítems directos a SQL sin cargar chunks)
    public void addDeferredOutputAsync(UUID factoryId, int amount) {
        virtualExecutor.execute(() -> {
            String sql = "UPDATE nexo_factories SET stored_output = stored_output + ? WHERE id = CAST(? AS UUID)";
            try (var conn = databaseManager.getConnection();
                 var ps = conn.prepareStatement(sql)) {
                ps.setInt(1, amount);
                ps.setString(2, factoryId.toString());
                ps.executeUpdate();
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "❌ Error en Deferred Queue Logística", e);
            }
        });
    }

    public void deleteFactoryAsync(ActiveFactory factory) {
        factoryCache.invalidate(factory.getId());
        locationMap.remove(serializeLocation(factory.getCoreLocation()));

        virtualExecutor.execute(() -> {
            String sql = "DELETE FROM nexo_factories WHERE id = CAST(? AS UUID)";
            try (var conn = databaseManager.getConnection();
                 var ps = conn.prepareStatement(sql)) {

                ps.setString(1, factory.getId().toString());
                ps.executeUpdate();
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "❌ Error eliminando fábrica de la DB", e);
            }
        });
    }
}