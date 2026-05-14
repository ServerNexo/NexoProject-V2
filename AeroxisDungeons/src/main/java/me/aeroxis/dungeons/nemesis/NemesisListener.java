package me.aeroxis.dungeons.nemesis;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.aeroxis.core.utils.Base64Util; // Importamos la clase
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

@Singleton
public class NemesisListener implements Listener {

    private final NemesisManager nemesisManager;
    private final Base64Util base64Util; // 🌟 NUEVA DEPENDENCIA

    // 💉 Inyectamos ambas dependencias por el constructor
    @Inject
    public NemesisListener(NemesisManager nemesisManager, Base64Util base64Util) {
        this.nemesisManager = nemesisManager;
        this.base64Util = base64Util;
    }

    @EventHandler
    public void onPlayerDie(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        if (victim.getKiller() instanceof LivingEntity killer && !(killer instanceof Player)) {

            // 🌟 FIX: Usamos la instancia inyectada (base64Util en minúscula) en lugar del método estático
            String lootSerialized = base64Util.itemStackArrayToBase64(
                    event.getDrops().stream().toArray(org.bukkit.inventory.ItemStack[]::new)
            );

            // Mutamos al mob
            nemesisManager.mutateMob(killer, victim.getName(), lootSerialized);

            // Limpiamos los drops del suelo porque el mob los "robó"
            event.getDrops().clear();
        }
    }
}