package me.aeroxis.islas.data;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 🏝️ Perfil de Isla - Datos en RAM (Grid + Ranking + Roles + Mejoras)
 * Arquitectura Enterprise: Cálculos O(1), concurrencia nativa, y XP individual por miembro.
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
    // 🏆 NUEVO SISTEMA DE TOP Y PROGRESIÓN (Thread-Safe)
    // ==========================================
    private final AtomicInteger level;
    private final AtomicInteger value;        // Top Riqueza (Cristales Depositados)

    // 🌟 AÑADIDO: XP Individual por Miembro (UUID -> XP Aportada)
    private final ConcurrentHashMap<UUID, Double> memberXpContributions;

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

        // 🌟 Inyección de Top de Islas y Niveles
        this.level = new AtomicInteger(1);
        this.value = new AtomicInteger(0);

        // Inicializamos el mapa de XP y le damos 0.0 al dueño para evitar nulls
        this.memberXpContributions = new ConcurrentHashMap<>();
        this.memberXpContributions.put(ownerId, 0.0);

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
    // 🏆 GETTERS/SETTERS SISTEMA DE TOP Y XP
    // ==========================================

    public int getLevel() { return level.get(); }
    public void setLevel(int lvl) { this.level.set(lvl); }

    // 🌟 MÉTODOS DE XP INDIVIDUAL (NUEVOS)
    public ConcurrentHashMap<UUID, Double> getMemberXpContributions() { return memberXpContributions; }

    public double getTotalXp() {
        return memberXpContributions.values().stream().mapToDouble(Double::doubleValue).sum();
    }

    public void addPlayerXp(UUID playerId, double amount) {
        // Uso de merge para suma atómica y concurrente ultrarrápida
        memberXpContributions.merge(playerId, amount, Double::sum);
    }

    public void removePlayerXp(UUID playerId) {
        memberXpContributions.remove(playerId);
    }

    // Lógica de Riqueza (Valor depositado)
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