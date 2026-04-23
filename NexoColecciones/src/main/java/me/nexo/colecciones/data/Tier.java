package me.nexo.colecciones.data;

import java.util.List;

/**
 * 📚 NexoColecciones - Modelo de Tier (Arquitectura Enterprise)
 * Rendimiento: Convertido a Record inmutable (Java 16+). 100% Thread-Safe y Zero-Garbage overhead.
 * Nota: Al ser un portador de datos en RAM, no requiere @Singleton ni inyección.
 */
public record Tier(int nivel, long requerido, List<String> recompensas, List<String> loreRecompensa) {

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
    
    public List<String> getRecompensas() { 
        return recompensas(); 
    }
    
    public List<String> getLoreRecompensa() { 
        return loreRecompensa(); 
    }
}