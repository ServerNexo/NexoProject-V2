package me.nexo.factories.core;

import org.bukkit.Location;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 🏭 NexoFactories - Modelo de Máquina Activa (Arquitectura Enterprise Java 21)
 * Rendimiento: 100% Thread-Safe, Lock-Free y Blindado con Copias Defensivas.
 */
public class ActiveFactory {

    // Identificadores inmutables
    private final UUID id;
    private final UUID stoneId;
    private final UUID ownerId;
    private final String factoryType;
    private final Location coreLocation;

    // 🌟 OPTIMIZACIÓN: Atómicos para operaciones matemáticas masivas sin bloqueos (synchronized)
    private final AtomicInteger level;
    private final AtomicInteger storedOutput;
    private final AtomicLong lastEvaluationTime;

    // 🌟 OPTIMIZACIÓN: Volátiles para garantizar que los Hilos Virtuales siempre lean el valor más reciente en RAM
    private volatile String currentStatus;
    private volatile String catalystItem;
    private volatile String jsonLogic;

    public ActiveFactory(UUID id, UUID stoneId, UUID ownerId, String factoryType, int level, String currentStatus, int storedOutput, Location coreLocation, String catalystItem, String jsonLogic, long lastEvaluationTime) {
        this.id = id;
        this.stoneId = stoneId;
        this.ownerId = ownerId;
        this.factoryType = factoryType;
        
        // Almacenamos una copia limpia de la ubicación inicial
        this.coreLocation = coreLocation.clone();

        // Inicializamos los valores atómicos
        this.level = new AtomicInteger(level);
        this.storedOutput = new AtomicInteger(storedOutput);
        this.lastEvaluationTime = new AtomicLong(lastEvaluationTime);

        this.currentStatus = currentStatus;
        this.catalystItem = catalystItem;
        this.jsonLogic = jsonLogic;
    }

    // ==========================================
    // ⚙️ OPERACIONES MATEMÁTICAS LOCK-FREE
    // ==========================================
    public void addOutput(int amount) {
        this.storedOutput.addAndGet(amount); // Suma atómica, 0 micro-retrasos
    }

    public void clearOutput() {
        this.storedOutput.set(0); // Seteo atómico
    }

    // ==========================================
    // 💾 GETTERS Y SETTERS THREAD-SAFE
    // ==========================================
    public UUID getId() { return id; }
    public UUID getStoneId() { return stoneId; }
    public UUID getOwnerId() { return ownerId; }
    public String getFactoryType() { return factoryType; }
    
    /**
     * 🌟 PARCHE DE SEGURIDAD: Copia Defensiva.
     * Retorna un clon para evitar que modificaciones externas corrompan la coordenada original.
     */
    public Location getCoreLocation() { return coreLocation.clone(); }

    public int getLevel() { return level.get(); }
    public void setLevel(int level) { this.level.set(level); }

    public int getStoredOutput() { return storedOutput.get(); }

    public String getCurrentStatus() { return currentStatus; }
    public void setCurrentStatus(String currentStatus) { this.currentStatus = currentStatus; }

    public String getCatalystItem() { return catalystItem; }
    public void setCatalystItem(String catalystItem) { this.catalystItem = catalystItem; }

    public String getJsonLogic() { return jsonLogic; }
    public void setJsonLogic(String jsonLogic) { this.jsonLogic = jsonLogic; }

    public long getLastEvaluationTime() { return lastEvaluationTime.get(); }
    public void setLastEvaluationTime(long time) { this.lastEvaluationTime.set(time); }
}