package me.nexo.islas.data;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 🏝️ Perfil de Isla - Datos en RAM (Grid + Ranking + Roles Ready)
 * Arquitectura Enterprise: Cálculos O(1) y Economía de Stat Points.
 */
@Getter
@Setter
public class IslandProfile {

    private final UUID ownerId;

    // 🗺️ Sistema Grid Nativo (NexoPaster)
    private int gridIndex; // ID único para calcular las coordenadas X, Z

    // 👥 Sistema Co-op Original
    private List<UUID> members;

    // Configuraciones
    private boolean isLocked; // Si es true, nadie excepto miembros puede entrar

    // 🏆 Punto 4 del Prompt Maestro (Doble Ranking)
    private double wealthScore;   // Top Riqueza (Banco/Cristales depositados)
    private double activityScore; // Top Actividad (XP por farmear/minar)

    // ==========================================
    // 🌟 ECONOMÍA DE MEJORAS (STAT POINTS)
    // ==========================================
    private int upgradePoints; // Se ganan al subir de nivel la isla (depositando riqueza/XP)

    // 🌟 NIVELES DE MEJORAS (Rango 1 a 5)
    private int borderLevel;
    private int memberLimitLevel;
    private int minionLimitLevel;
    private int spawnerLimitLevel;
    private int factoryLimitLevel; // Fábricas de NexoFactories
    private int cropGrowthLevel;
    private int spawnerRateLevel;
    private int mobDropLevel;
    private int farmingDropLevel;
    private int generatorLevel;
    private int xpBonusLevel; // Bendición de Sabiduría (AuraSkills)

    public IslandProfile(UUID ownerId, int gridIndex) {
        this.ownerId = ownerId;
        this.gridIndex = gridIndex;
        this.members = new ArrayList<>();
        this.isLocked = false;
        this.wealthScore = 0.0;
        this.activityScore = 0.0;

        // Inicialización del Árbol de Mejoras
        this.upgradePoints = 0;
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
    // 🧮 CÁLCULOS MATEMÁTICOS O(1) PARA LÍMITES
    // ==========================================

    public int getRealBorderSize() { return 50 * borderLevel; } // 50, 100, 150, 200, 250

    public int getRealMemberLimit() { return 2 + ((memberLimitLevel - 1) * 2); } // 2, 4, 6, 8, 10

    public int getRealMinionLimit() { return 2 + ((minionLimitLevel - 1) * 2); } // 2, 4, 6, 8, 10

    public int getRealFactoryLimit() { return 2 + ((factoryLimitLevel - 1) * 3); } // 2, 5, 8, 11, 14

    public int getRealSpawnerLimit() {
        return switch(spawnerLimitLevel) {
            case 1 -> 10;
            case 2 -> 25;
            case 3 -> 50;
            case 4 -> 100;
            default -> 200;
        };
    }

    /**
     * 🌟 Bono de Experiencia (AuraSkills) Balanceado [NO OP].
     * Incrementa solo un 2% por nivel. Máximo: +8% de XP adicional.
     * Niveles: 1.00x, 1.02x, 1.04x, 1.06x, 1.08x
     */
    public double getRealXpBonus() {
        return 1.0 + ((xpBonusLevel - 1) * 0.02);
    }

    // ==========================================
    // 💰 MÉTODOS AUXILIARES DE ECONOMÍA/COMPATIBILIDAD
    // ==========================================

    public void removeUpgradePoints(int points) {
        this.upgradePoints = Math.max(0, this.upgradePoints - points);
    }

    public void addUpgradePoints(int points) {
        this.upgradePoints += points;
    }

    // 🌟 Wrapper para compatibilidad con IslandMainMenu
    public double getValorActividad() {
        return this.activityScore;
    }

    // 🌟 Usado por los Minions para inyectar XP
    public void addValorActividad(double amount) {
        this.activityScore += amount;
    }

    // 🌟 Wrapper para compatibilidad con IslandMainMenu
    public int getMemberLimit() {
        return getRealMemberLimit();
    }

    // ==========================================
    // 👥 GESTIÓN DE MIEMBROS Y ROLES
    // ==========================================

    public boolean isMember(UUID playerId) {
        return ownerId.equals(playerId) || members.contains(playerId);
    }

    public void addMember(UUID playerId) {
        if (!members.contains(playerId)) {
            members.add(playerId);
        }
    }

    public void removeMember(UUID playerId) {
        members.remove(playerId);
    }

    /**
     * 🔍 Obtiene el rol de cualquier jugador en esta isla al instante.
     * Usado por el IslandSecurityListener para los permisos.
     */
    public IslandRole getRole(UUID playerId) {
        if (ownerId.equals(playerId)) return IslandRole.OWNER;
        if (members.contains(playerId)) return IslandRole.MEMBER;
        return IslandRole.VISITOR;
    }
}