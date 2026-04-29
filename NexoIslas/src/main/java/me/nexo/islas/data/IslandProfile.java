package me.nexo.islas.data;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 🏝️ Perfil de Isla - Datos en RAM (Grid + Ranking + Roles Ready)
 */
@Getter
@Setter
public class IslandProfile {

    private final UUID ownerId;

    // 🗺️ Sistema Grid Nativo (NexoPaster)
    private int gridIndex; // ID único para calcular las coordenadas X, Z

    // 👥 Sistema Co-op Original
    private List<UUID> members;

    // Mejoras de la Isla
    private int borderLevel;
    private int memberLimit;

    // Configuraciones
    private boolean isLocked; // Si es true, nadie excepto miembros puede entrar

    // 🏆 Punto 4 del Prompt Maestro (Doble Ranking)
    private double wealthScore;   // Top Riqueza (Banco/Cristales depositados)
    private double activityScore; // Top Actividad (XP por farmear/minar)

    public IslandProfile(UUID ownerId, int gridIndex) {
        this.ownerId = ownerId;
        this.gridIndex = gridIndex;
        this.members = new ArrayList<>();
        this.borderLevel = 1;
        this.memberLimit = 4; // Límite por defecto
        this.isLocked = false;
        this.wealthScore = 0.0;
        this.activityScore = 0.0;
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
     * 🌟 Añade valor al Top Actividad (Usado por los Minions)
     */
    public void addValorActividad(double amount) {
        this.activityScore += amount;
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