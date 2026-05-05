package me.nexo.islas.data;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 🏝️ Perfil de Isla - Datos en RAM (Grid + Ranking + Roles + Mejoras)
 * Arquitectura Enterprise: Cálculos O(1), concurrencia nativa para XP y economía de Stat Points.
 */
public class IslandProfile {

    // 🗺️ Sistema Core (Inmutables)
    private final UUID islandId; // 🌟 AÑADIDO: Identificador global de la isla
    private final UUID ownerId;
    @Getter @Setter private int gridIndex;

    // 👥 Sistema Co-op Original convertido a Concurrente
    private final AtomicReference<String> islandName;
    private final ConcurrentHashMap<UUID, IslandRole> members;

    // Configuraciones
    @Getter @Setter private boolean isLocked;

    // ==========================================
    // 🏆 NUEVO SISTEMA DE TOP (Thread-Safe / Atómico)
    // ==========================================
    private final AtomicInteger level;
    private final AtomicReference<Double> xp; // Top Actividad (Farmear/Minar)
    private final AtomicInteger value;        // Top Riqueza (Cristales Depositados)

    // ==========================================
    // 🌟 ECONOMÍA DE MEJORAS (STAT POINTS)
    // ==========================================
    private final AtomicInteger upgradePoints; // Ahora es atómico para soportar recompensas asíncronas

    // 🌟 NIVELES DE MEJORAS (Rango 1 a 5)
    @Getter @Setter private int borderLevel;
    @Getter @Setter private int memberLimitLevel;
    @Getter @Setter private int minionLimitLevel;
    @Getter @Setter private int spawnerLimitLevel;
    @Getter @Setter private int factoryLimitLevel;
    @Getter @Setter private int cropGrowthLevel;
    @Getter @Setter private int spawnerRateLevel;
    @Getter @Setter private int mobDropLevel;
    @Getter @Setter private int farmingDropLevel;
    @Getter @Setter private int generatorLevel;
    @Getter @Setter private int xpBonusLevel;

    public IslandProfile(UUID islandId, UUID ownerId, int gridIndex, String islandName) {
        this.islandId = islandId;
        this.ownerId = ownerId;
        this.gridIndex = gridIndex;

        // 🌟 Nombres y roles inicializados concurrentemente
        this.islandName = new AtomicReference<>(islandName != null ? islandName : "Isla de " + ownerId.toString().substring(0, 5));
        this.members = new ConcurrentHashMap<>();

        this.isLocked = false;

        // 🌟 Inyección de Top de Islas
        this.level = new AtomicInteger(1);
        this.xp = new AtomicReference<>(0.0);
        this.value = new AtomicInteger(0);

        // Inicialización del Árbol de Mejoras
        this.upgradePoints = new AtomicInteger(0);
        this.borderLevel = 1;
        this.memberLimitLevel = 1;
        this.minionLimitLevel = 1;
        this.spawnerLimitLevel = 1;
        this.factoryLimitLevel = 1;
        this.cropGrowthLevel = 1;
        this.spawnerRateLevel = 1;
        this.mobDropLevel = 1;
        this.farmingDropLevel = 1;
        this.generatorLevel = 1;
        this.xpBonusLevel = 1;
    }

    // ==========================================
    // 🧮 GETTERS/SETTERS CORE Y CO-OP
    // ==========================================

    public UUID getIslandId() { return islandId; }
    public UUID getOwnerId() { return ownerId; }

    public String getIslandName() { return islandName.get(); }
    public void setIslandName(String name) { this.islandName.set(name); }

    public ConcurrentHashMap<UUID, IslandRole> getMembers() { return members; }

    public void addMember(UUID uuid, IslandRole role) { members.put(uuid, role); }
    public void removeMember(UUID uuid) { members.remove(uuid); }

    public boolean isMember(UUID playerId) {
        return ownerId.equals(playerId) || members.containsKey(playerId);
    }

    public IslandRole getRole(UUID playerId) {
        if (ownerId.equals(playerId)) return IslandRole.OWNER;
        return members.getOrDefault(playerId, IslandRole.VISITOR);
    }

    // ==========================================
    // 🏆 GETTERS/SETTERS SISTEMA DE TOP
    // ==========================================

    public int getLevel() { return level.get(); }
    public void setLevel(int lvl) { this.level.set(lvl); }

    // Compatibilidad y nueva lógica de XP (Actividad)
    public double getXp() { return xp.get(); }
    public void setXp(double newXp) { this.xp.set(newXp); }

    public double getValorActividad() { return getXp(); }
    public void addValorActividad(double amount) {
        // Actualización atómica del valor double
        while (true) {
            Double current = xp.get();
            if (xp.compareAndSet(current, current + amount)) break;
        }
    }

    // Nueva lógica de Riqueza (Valor)
    public int getValue() { return value.get(); }
    public void setValue(int val) { this.value.set(val); }
    public void addValue(int amount) { this.value.addAndGet(amount); }

    // ==========================================
    // 🌟 MÉTODOS DE ECONOMÍA Y MEJORAS
    // ==========================================

    public int getUpgradePoints() { return upgradePoints.get(); }
    public void addUpgradePoints(int points) { this.upgradePoints.addAndGet(points); }
    public void removeUpgradePoints(int points) {
        int current;
        do {
            current = upgradePoints.get();
        } while (!upgradePoints.compareAndSet(current, Math.max(0, current - points)));
    }

    // ==========================================
    // 🧮 CÁLCULOS MATEMÁTICOS O(1) PARA LÍMITES
    // ==========================================

    public int getRealBorderSize() { return 50 * borderLevel; }

    public int getRealMemberLimit() { return 2 + ((memberLimitLevel - 1) * 2); }
    public int getMemberLimit() { return getRealMemberLimit(); }

    public int getRealMinionLimit() { return 2 + ((minionLimitLevel - 1) * 2); }

    public int getRealFactoryLimit() { return 2 + ((factoryLimitLevel - 1) * 3); }

    public int getRealSpawnerLimit() {
        return switch(spawnerLimitLevel) {
            case 1 -> 10;
            case 2 -> 25;
            case 3 -> 50;
            case 4 -> 100;
            default -> 200;
        };
    }

    public double getRealXpBonus() {
        return 1.0 + ((xpBonusLevel - 1) * 0.02);
    }
}