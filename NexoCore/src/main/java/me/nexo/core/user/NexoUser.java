package me.nexo.core.user;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class NexoUser {

    private final UUID uuid;
    private final String nombre;

    // 🛡️ MODIFICACIÓN: Datos de Clan
    private UUID clanId;
    private String clanRole; // LIDER, OFICIAL, MIEMBRO, NONE

    // Estadísticas seguras para múltiples hilos
    private final AtomicInteger nexoNivel;
    private final AtomicInteger nexoXp;
    private final AtomicInteger combateNivel;
    private final AtomicInteger combateXp;
    private final AtomicInteger mineriaNivel;
    private final AtomicInteger mineriaXp;
    private final AtomicInteger agriculturaNivel;
    private final AtomicInteger agriculturaXp;

    // Variables temporales de sesión (no se guardan en BD de la misma forma)
    private final AtomicInteger energiaMineria;
    private final AtomicInteger energiaExtraAccesorios;
    private String claseJugador; // Guardará el nombre de su clase RPG si tiene

    // 🩸 MODIFICACIÓN (Nexo Architect V3.0): Caché de Bendiciones Thread-Safe
    private final Set<String> activeBlessings;

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

        // Inicializar el caché de bendiciones (Concurrente para evitar lag spikes)
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
    // 🧠 MÓDULO 3: ÁRBOL DE TALENTOS
    // =====================================
    private int knowledgePoints = 0;

    public int getKnowledgePoints() {
        return knowledgePoints;
    }

    public void addKnowledgePoints(int amount) {
        this.knowledgePoints += amount;
    }

    public void removeKnowledgePoints(int amount) {
        this.knowledgePoints -= amount;
    }

    // =====================================
    // 💎 MÓDULO 4: ECONOMÍA PREMIUM (Gemas)
    // =====================================
    private final AtomicInteger gems = new AtomicInteger(0);

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
    // 🩸 MÓDULO 5: SISTEMA DE BENDICIONES (Anti-Lag)
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
    // =====================================
    // 🌌 MÓDULO 6: VOID BLESSING (Booster Cookie)
    // =====================================
    private long voidBlessingUntil = 0;
    private boolean isBlessingActiveCache = false;

    public long getVoidBlessingUntil() {
        return voidBlessingUntil;
    }

    public void setVoidBlessingUntil(long timestamp) {
        this.voidBlessingUntil = timestamp;
        updateBlessingCache();
    }

    public void addVoidBlessingTime(long millisMillis) {
        long now = System.currentTimeMillis();
        if (this.voidBlessingUntil < now) {
            this.voidBlessingUntil = now + millisMillis;
        } else {
            this.voidBlessingUntil += millisMillis;
        }
        updateBlessingCache();
    }

    public void updateBlessingCache() {
        this.isBlessingActiveCache = this.voidBlessingUntil > System.currentTimeMillis();
    }

    // ⚡ ZERO-LAG CHECK: Ideal para eventos ultra rápidos (Muertes, Daño, Economía)
    public boolean isVoidBlessingActive() {
        // Validación perezosa: Si estaba activo pero acaba de expirar, lo apagamos en silencio
        if (isBlessingActiveCache && voidBlessingUntil <= System.currentTimeMillis()) {
            isBlessingActiveCache = false;
        }
        return isBlessingActiveCache;
    }
}