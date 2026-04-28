package me.nexo.islas.data;

/**
 * 🎭 Roles de permisos dentro de una Isla.
 */
public enum IslandRole {
    OWNER,      // El dueño absoluto
    MEMBER,     // Miembro del Co-op (Puede construir/abrir cofres)
    VISITOR     // Visitante (Solo puede mirar y caminar)
}