package me.nexo.core.user;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 🏛️ Nexo Network - Modelo de Usuario (Entity)
 * Arquitectura Enterprise: Modelo Thread-Safe nativo para Java 21+.
 * (No es un servicio, por ende no lleva @Singleton ni @Inject).
 */
public class NexoUser {

    private final UUID uuid;
    private final String nombre;

    // 🛡️ MODIFICACIÓN: Datos de Clan (Volatile para visibilidad entre Hilos Virtuales)
    private volatile UUID clanId;
    private volatile String clanRole; // LIDER, OFICIAL, MIEMBRO, NONE

    // Estadísticas seguras para múltiples hilos (Atomic)
    private final AtomicInteger nexoNivel;
    private final AtomicInteger nexoXp;
    private final AtomicInteger combateNivel;
    private final AtomicInteger combateXp;
    private final AtomicInteger mineriaNivel;
    private final AtomicInteger mineriaXp;
    private final AtomicInteger agriculturaNivel;
    private final AtomicInteger agriculturaXp;

    // Variables temporales de sesión
    private final AtomicInteger energiaMineria;
    private final AtomicInteger energiaExtraAccesorios;
    private volatile String claseJugador; // Volatile por ser reasignable

    // 🩸 MODIFICACIÓN: Caché de Bendiciones Thread-Safe
    private final Set<String> activeBlessings;

    // =====================================
    // 🧠 MÓDULOS EXTRA (Ahora Seguros para Concurrencia)
    // =====================================
    private final AtomicInteger knowledgePoints = new AtomicInteger(0);
    private final AtomicInteger gems = new AtomicInteger(0);
    
    private final AtomicLong voidBlessingUntil = new AtomicLong(0);
    private final AtomicBoolean isBlessingActiveCache = new AtomicBoolean(false);

    public NexoUser(UUID uuid, String nombre, int nNivel, int nXp, int cNivel, int cXp, int mNivel, int mXp, int aNivel, int aXp, UUID clanId, String clanRole) {
        this.uuid = uuid;
        this.nombre = nombre;

        // 🏛️ Inicialización de Clan
        this.clanId = clanId;
        this.clanRole = (clanRole == null || clanRole.isEmpty()) ? "NONE" : clanRole;

        this.nexoNivel = new AtomicInteger(nNivel);
        this.nexoXp = new AtomicInteger(nXp);
        this.combateNivel = new AtomicInteger(cNivel);
        this.combateXp = new AtomicInteger(cXp);
        this.mineriaNivel = new AtomicInteger(mNivel);
        this.mineriaXp = new AtomicInteger(mXp);
        this.agriculturaNivel = new AtomicInteger(aNivel);
        this.agriculturaXp = new AtomicInteger(aXp);

        // Valores por defecto al iniciar sesión
        this.energiaMineria = new AtomicInteger(100 + ((nNivel - 1) * 20));
        this.energiaExtraAccesorios = new AtomicInteger(0);
        this.claseJugador = "Ninguna";

        // Inicializar el caché de bendiciones concurrentemente
        this.activeBlessings = ConcurrentHashMap.newKeySet();
    }

    // 🏛️ GETTERS Y SETTERS DE CLAN
    public UUID getClanId() { return clanId; }
    public void setClanId(UUID clanId) { this.clanId = clanId; }

    public String getClanRole() { return clanRole; }
    public void setClanRole(String clanRole) { this.clanRole = clanRole; }

    public boolean hasClan() { return clanId != null; }

    // Getters básicos
    public UUID getUuid() { return uuid; }
    public String getNombre() { return nombre; }
    public String getClaseJugador() { return claseJugador; }
    public void setClaseJugador(String clase) { this.claseJugador = clase; }

    // Getters de Niveles y XP
    public int getNexoNivel() { return nexoNivel.get(); }
    public int getNexoXp() { return nexoXp.get(); }
    public int getCombateNivel() { return combateNivel.get(); }
    public int getCombateXp() { return combateXp.get(); }
    public int getMineriaNivel() { return mineriaNivel.get(); }
    public int getMineriaXp() { return mineriaXp.get(); }
    public int getAgriculturaNivel() { return agriculturaNivel.get(); }
    public int getAgriculturaXp() { return agriculturaXp.get(); }

    public int getEnergiaMineria() { return energiaMineria.get(); }
    public int getEnergiaExtraAccesorios() { return energiaExtraAccesorios.get(); }

    // Setters Seguros
    public void setNexoNivel(int valor) { this.nexoNivel.set(valor); }
    public void addNexoXp(int cantidad) { this.nexoXp.addAndGet(cantidad); }
    public void setNexoXp(int valor) { this.nexoXp.set(valor); }

    public void setCombateNivel(int valor) { this.combateNivel.set(valor); }
    public void addCombateXp(int cantidad) { this.combateXp.addAndGet(cantidad); }
    public void setCombateXp(int valor) { this.combateXp.set(valor); }

    public void setMineriaNivel(int valor) { this.mineriaNivel.set(valor); }
    public void addMineriaXp(int cantidad) { this.mineriaXp.addAndGet(cantidad); }
    public void setMineriaXp(int valor) { this.mineriaXp.set(valor); }

    public void setAgriculturaNivel(int valor) { this.agriculturaNivel.set(valor); }
    public void addAgriculturaXp(int cantidad) { this.agriculturaXp.addAndGet(cantidad); }
    public void setAgriculturaXp(int valor) { this.agriculturaXp.set(valor); }

    public void setEnergiaMineria(int valor) { this.energiaMineria.set(valor); }
    public void setEnergiaExtraAccesorios(int valor) { this.energiaExtraAccesorios.set(valor); }

    // =====================================
    // 🧠 MÓDULO 3: ÁRBOL DE TALENTOS (Ahora seguro)
    // =====================================
    public int getKnowledgePoints() {
        return knowledgePoints.get();
    }

    public void addKnowledgePoints(int amount) {
        this.knowledgePoints.addAndGet(amount);
    }

    public void removeKnowledgePoints(int amount) {
        this.knowledgePoints.addAndGet(-amount);
    }

    public void setKnowledgePoints(int amount) {
        this.knowledgePoints.set(amount);
    }

    // =====================================
    // 💎 MÓDULO 4: ECONOMÍA PREMIUM (Gemas)
    // =====================================
    public int getGems() {
        return gems.get();
    }

    public void addGems(int amount) {
        this.gems.addAndGet(amount);
    }

    public void removeGems(int amount) {
        this.gems.addAndGet(-amount);
    }

    public void setGems(int amount) {
        this.gems.set(amount);
    }

    // =====================================
    // 🩸 MÓDULO 5: SISTEMA DE BENDICIONES
    // =====================================
    public boolean hasActiveBlessing(String blessingId) {
        return this.activeBlessings.contains(blessingId.toUpperCase());
    }

    public void addBlessing(String blessingId) {
        this.activeBlessings.add(blessingId.toUpperCase());
    }

    public void removeBlessing(String blessingId) {
        this.activeBlessings.remove(blessingId.toUpperCase());
    }

    public Set<String> getActiveBlessings() {
        return this.activeBlessings;
    }

    public void setBlessings(Set<String> blessings) {
        this.activeBlessings.clear();
        if (blessings != null) {
            for (String b : blessings) {
                this.activeBlessings.add(b.toUpperCase());
            }
        }
    }

    // Método helper para BD
    public String serializeBlessings() {
        return String.join(",", this.activeBlessings);
    }

    // =====================================
    // 🌌 MÓDULO 6: VOID BLESSING (Booster Cookie Seguro)
    // =====================================
    public long getVoidBlessingUntil() {
        return voidBlessingUntil.get();
    }

    public void setVoidBlessingUntil(long timestamp) {
        this.voidBlessingUntil.set(timestamp);
        updateBlessingCache();
    }

    public void addVoidBlessingTime(long millisMillis) {
        long now = System.currentTimeMillis();
        long current = this.voidBlessingUntil.get();
        if (current < now) {
            this.voidBlessingUntil.set(now + millisMillis);
        } else {
            this.voidBlessingUntil.addAndGet(millisMillis);
        }
        updateBlessingCache();
    }

    public void updateBlessingCache() {
        this.isBlessingActiveCache.set(this.voidBlessingUntil.get() > System.currentTimeMillis());
    }

    // ⚡ ZERO-LAG CHECK
    public boolean isVoidBlessingActive() {
        // Validación perezosa atómica
        if (isBlessingActiveCache.get() && voidBlessingUntil.get() <= System.currentTimeMillis()) {
            isBlessingActiveCache.set(false);
        }
        return isBlessingActiveCache.get();
    }
}