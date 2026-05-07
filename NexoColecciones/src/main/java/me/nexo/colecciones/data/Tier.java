package me.nexo.colecciones.data;

/**
 * 📚 NexoColecciones - Modelo de Tier (Arquitectura Enterprise)
 * Rendimiento: Convertido a Record inmutable (Java 16+). 100% Thread-Safe y Zero-Garbage overhead.
 * Nota: Al ser un portador de datos en RAM, no requiere @Singleton ni inyección.
 */
public record Tier(int nivel, long requerido, String recompensaId) {

    // ==========================================
    // 💡 GETTERS DE COMPATIBILIDAD (LEGACY BRIDGE)
    // Para no romper las llamadas existentes en menús y managers.
    // ==========================================

    public int getNivel() {
        return nivel();
    }

    public long getRequerido() {
        return requerido();
    }

    public String getRecompensaId() {
        return recompensaId();
    }
}