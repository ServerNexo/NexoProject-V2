package me.nexo.cosmetics.listeners;

import com.google.inject.Inject;
import me.nexo.cosmetics.manager.CosmeticManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class SkyblockCosmeticListener implements Listener {

    private final JavaPlugin plugin;
    private final CosmeticManager cosmeticManager;

    @Inject
    public SkyblockCosmeticListener(JavaPlugin plugin, CosmeticManager cosmeticManager) {
        this.plugin = plugin;
        this.cosmeticManager = cosmeticManager;
    }

    // ==========================================
    // ☄️ SALVACIÓN DEL VACÍO (Meteor Trail)
    // ==========================================
    @EventHandler
    public void onVoidFall(EntityDamageEvent event) {
        // 🌟 JAVA 21 PATTERN MATCHING: Comprobación y casteo en 1 sola línea
        if (event.getCause() == EntityDamageEvent.DamageCause.VOID && event.getEntity() instanceof Player p) {
            
            // Si el daño los mataría
            if ((p.getHealth() - event.getFinalDamage()) <= 0) {
                
                var cosmetics = cosmeticManager.getActiveCosmetics(p.getUniqueId());
                // Validamos si tienen el cosmético premium de salvación (o si lo hacemos general)
                if (cosmetics == null || !"METEOR_RESCUE".equals(cosmetics.joinTag())) return; 

                event.setCancelled(true);
                
                Location fallLoc = p.getLocation();
                
                // 🌟 Teletransportamos asíncronamente a su isla o al hub (Reemplazar con lógica de NexoIslas)
                Location spawnIsland = Bukkit.getWorlds().get(0).getSpawnLocation(); 
                p.teleportAsync(spawnIsland).thenAccept(success -> {
                    if (success) {
                        p.setHealth(p.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue());
                        p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                    }
                });

                // 🌟 FIX FOLIA: Dibuja la estela estática en la región de la caída usando un AsyncScheduler anidado
                Bukkit.getRegionScheduler().run(plugin, fallLoc, task -> {
                    fallLoc.getWorld().playSound(fallLoc, Sound.ENTITY_CHICKEN_HURT, 1.0f, 0.5f);
                    
                    Bukkit.getAsyncScheduler().runNow(plugin, asyncTask -> {
                        for (int y = 0; y > -50; y -= 2) {
                            Location pLoc = fallLoc.clone().add(0, y, 0);
                            pLoc.getWorld().spawnParticle(Particle.FLAME, pLoc, 2, 0, 0, 0, 0.0);
                            pLoc.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, pLoc, 1, 0, 0, 0, 0.0);
                        }
                    });
                });
            }
        }
    }
}