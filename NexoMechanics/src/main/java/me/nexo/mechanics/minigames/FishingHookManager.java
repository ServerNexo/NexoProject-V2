package me.nexo.mechanics.minigames;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.NexoCore;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.user.NexoUser;
import me.nexo.mechanics.NexoMechanics;
import me.nexo.mechanics.config.ConfigManager;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;

@Singleton
public class FishingHookManager implements Listener {

    private final NexoMechanics plugin;
    private final ConfigManager configManager;
    private final NexoCore core;

    @Inject
    public FishingHookManager(NexoMechanics plugin, ConfigManager configManager, NexoCore core) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.core = core;
    }

    @EventHandler
    public void alPescar(PlayerFishEvent event) {
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {

            if (event.getCaught() instanceof Item itemCapturado) {
                ItemStack pescado = itemCapturado.getItemStack();
                if (pescado == null || !pescado.hasItemMeta()) return;

                var pdc = pescado.getItemMeta().getPersistentDataContainer();

                boolean esPezCustom = false;
                for (NamespacedKey key : pdc.getKeys()) {
                    if (key.getNamespace().equalsIgnoreCase("evenmorefish") || key.getKey().contains("emf")) {
                        esPezCustom = true;
                        break;
                    }
                }

                if (esPezCustom) {
                    Player p = event.getPlayer();
                    NexoUser user = core.getUserManager().getUserOrNull(p.getUniqueId());

                    if (user != null) {
                        int energiaAct = user.getEnergiaMineria();
                        int maxEnergia = 100 + ((user.getNexoNivel() - 1) * 20) + user.getEnergiaExtraAccesorios();

                        user.setEnergiaMineria(Math.min(energiaAct + 5, maxEnergia));
                        CrossplayUtils.sendMessage(p, configManager.getMessages().mensajes().minijuegos().pesca().extraccionAcuatica());
                    }
                }
            }
        }
    }
}