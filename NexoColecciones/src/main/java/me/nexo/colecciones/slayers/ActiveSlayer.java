package me.nexo.colecciones.slayers;

import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 📚 NexoColecciones - Contrato Slayer Activo (POJO / Data Carrier)
 * Rendimiento: 100% Thread-Safe. Incrementos Atómicos Puros.
 * Nota: Objeto en RAM en tiempo real. No requiere Inyección de Guice.
 */
public class ActiveSlayer {

    private final UUID playerId;
    private final SlayerManager.SlayerTemplate template;

    // 🌟 Atómicos y Volátiles para evitar Crashes en muertes simultáneas (Multihilo)
    private final AtomicInteger currentKills;
    private volatile boolean bossSpawned;
    private BossBar bossBar; // Interfaz mantenida en Bukkit API para compatibilidad con .removeAll()

    public ActiveSlayer(Player player, SlayerManager.SlayerTemplate template) {
        this.playerId = player.getUniqueId();
        this.template = template;
        this.currentKills = new AtomicInteger(0);
        this.bossSpawned = false;
        this.bossBar = null;
    }

    public void addKill() {
        if (!bossSpawned) {
            // 🌟 FIX CONCURRENCIA: Actualización condicional 100% Atómica.
            // Evita que dos hilos evalúen la condición al mismo tiempo y pasen el límite.
            currentKills.updateAndGet(k -> k < template.requiredKills() ? k + 1 : k);
        }
    }

    // ==========================================
    // 🌟 GETTERS Y SETTERS ENTERPRISE
    // ==========================================
    
    public UUID getPlayerId() { 
        return playerId; 
    }

    public SlayerManager.SlayerTemplate getTemplate() { 
        return template; 
    }

    public int getKills() { 
        return currentKills.get(); 
    }

    public String getBossName() { 
        return template.bossName(); 
    }

    public boolean isBossSpawned() { 
        return bossSpawned; 
    }
    
    public void setBossSpawned(boolean bossSpawned) { 
        this.bossSpawned = bossSpawned; 
    }

    public BossBar getBossBar() { 
        return bossBar; 
    }
    
    public void setBossBar(BossBar bossBar) { 
        this.bossBar = bossBar; 
    }
}