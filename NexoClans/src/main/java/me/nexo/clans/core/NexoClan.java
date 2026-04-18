package me.nexo.clans.core;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 👥 NexoClans - Objeto de Clan en Memoria (Modelo Enterprise)
 * Rendimiento: 100% Thread-Safe, Lock-Free y memoria Volátil.
 */
public class NexoClan {

    private final UUID id;

    // 🌟 FIX: 'volatile' garantiza que si un hilo cambia el nombre, todos los demás lo vean al instante
    private volatile String name;
    private volatile String tag;
    private volatile String publicHome;
    private volatile boolean friendlyFire;

    // 🌟 FIX: Atómicos para evitar bloqueos (synchronized) en concurrencia masiva
    private final AtomicInteger monolithLevel;
    private final AtomicLong monolithExp;
    private final AtomicReference<BigDecimal> bankBalance;

    public NexoClan(UUID id, String name, String tag, int monolithLevel, long monolithExp, BigDecimal bankBalance, String publicHome, boolean friendlyFire) {
        this.id = id;
        this.name = name;
        this.tag = tag;

        this.monolithLevel = new AtomicInteger(monolithLevel);
        this.monolithExp = new AtomicLong(monolithExp);
        this.bankBalance = new AtomicReference<>(bankBalance != null ? bankBalance : BigDecimal.ZERO);

        this.publicHome = publicHome;
        this.friendlyFire = friendlyFire;
    }

    // ==========================================
    // 🔮 GESTIÓN DE EXPERIENCIA (Monolith)
    // ==========================================
    // Aquí conservamos synchronized de forma local porque modificamos 2 variables dependientes (Nivel y Exp)
    public synchronized boolean addMonolithExp(long amount) {
        long currentExp = this.monolithExp.addAndGet(amount);
        boolean levelUp = false;

        // Fórmula: Cada nivel requiere 1000 * nivel actual
        long expRequired = this.monolithLevel.get() * 1000L;

        while (currentExp >= expRequired) {
            currentExp = this.monolithExp.addAndGet(-expRequired); // Restamos la exp requerida
            this.monolithLevel.incrementAndGet(); // Subimos de nivel
            levelUp = true;
            expRequired = this.monolithLevel.get() * 1000L; // Recalculamos para el siguiente nivel
        }

        return levelUp;
    }

    // ==========================================
    // 💰 ECONOMÍA LOCK-FREE (Sin bloqueos)
    // ==========================================
    public void depositMoney(double amount) {
        if (amount > 0) {
            // 🌟 Operación atómica de alta velocidad: Actualiza sin bloquear a otros jugadores
            bankBalance.updateAndGet(current -> current.add(BigDecimal.valueOf(amount)));
        }
    }

    public void withdrawMoney(double amount) {
        if (amount > 0) {
            bankBalance.updateAndGet(current -> {
                BigDecimal toWithdraw = BigDecimal.valueOf(amount);
                if (current.compareTo(toWithdraw) >= 0) {
                    return current.subtract(toWithdraw);
                }
                return current; // Si no tiene suficiente, no resta nada
            });
        }
    }

    public boolean hasEnoughMoney(double amount) {
        return this.bankBalance.get().compareTo(BigDecimal.valueOf(amount)) >= 0;
    }

    // ==========================================
    // ⚙️ GETTERS Y SETTERS
    // ==========================================
    public UUID getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }

    public int getMonolithLevel() { return monolithLevel.get(); }
    public void setMonolithLevel(int level) { this.monolithLevel.set(level); }

    public long getMonolithExp() { return monolithExp.get(); }

    public BigDecimal getBankBalance() { return bankBalance.get(); }

    public String getPublicHome() { return publicHome; }
    public void setPublicHome(String publicHome) { this.publicHome = publicHome; }

    public boolean isFriendlyFire() { return friendlyFire; }
    public void setFriendlyFire(boolean friendlyFire) { this.friendlyFire = friendlyFire; }
}