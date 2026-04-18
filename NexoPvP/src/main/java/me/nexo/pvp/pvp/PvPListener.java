package me.nexo.pvp.pvp;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.user.NexoAPI;
import me.nexo.pvp.config.ConfigManager;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.UUID;

/**
 * 🏛️ NexoPvP - Listener de Combate (Arquitectura Enterprise Java 25)
 * Rendimiento: Zero-Garbage, MethodHandles Nativos (0 Latencia) y Fast-Failing.
 */
@Singleton
public class PvPListener implements Listener {

    private final PvPManager manager;
    private final ConfigManager configManager;

    // 🌟 METHOD HANDLES: 50x más rápidos que la Reflexión Antigua (Method.invoke)
    private boolean integrationsLoaded = false;
    private Object claimManagerCache;
    private MethodHandle getStoneAtHandle;
    private MethodHandle getFlagHandle;

    private Object warManagerCache;
    private MethodHandle estanEnGuerraActivaHandle;

    @Inject
    public PvPListener(PvPManager manager, ConfigManager configManager) {
        this.manager = manager;
        this.configManager = configManager;
    }

    /**
     * 🧠 Carga las integraciones usando la API nativa de MethodHandles (Alta velocidad C++)
     */
    private void setupIntegrations() {
        if (integrationsLoaded) return;

        MethodHandles.Lookup lookup = MethodHandles.publicLookup();

        try {
            if (Bukkit.getPluginManager().isPluginEnabled("NexoProtections")) {
                claimManagerCache = NexoAPI.getServices().get(Class.forName("me.nexo.protections.managers.ClaimManager")).orElse(null);
                if (claimManagerCache != null) {
                    Class<?> claimClass = Class.forName("me.nexo.protections.managers.ClaimManager");
                    Class<?> stoneClass = Class.forName("me.nexo.protections.core.ProtectionStone");

                    // 🌟 Vinculación a nivel de Bytecode
                    getStoneAtHandle = lookup.findVirtual(claimClass, "getStoneAt", MethodType.methodType(Object.class, org.bukkit.Location.class));
                    getFlagHandle = lookup.findVirtual(stoneClass, "getFlag", MethodType.methodType(boolean.class, String.class));
                }
            }

            if (Bukkit.getPluginManager().isPluginEnabled("NexoWar")) {
                Object warPlugin = Bukkit.getPluginManager().getPlugin("NexoWar");
                if (warPlugin != null) {
                    // 🌟 Invocación rápida para atrapar el Manager
                    MethodHandle getWarMgr = lookup.findVirtual(warPlugin.getClass(), "getWarManager", MethodType.methodType(Object.class));
                    warManagerCache = getWarMgr.invoke(warPlugin);

                    estanEnGuerraActivaHandle = lookup.findVirtual(warManagerCache.getClass(), "estanEnGuerraActiva",
                            MethodType.methodType(boolean.class, UUID.class, UUID.class));
                }
            }
        } catch (Throwable ignored) {} // MethodHandles arroja Throwable, no solo Exception

        integrationsLoaded = true;
    }

    private net.kyori.adventure.text.Component color(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text.replace("&#", "&x&"));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDañoJugadores(EntityDamageByEntityEvent event) {
        // 🚀 FAST-FAIL: Pattern Matching nativo Java 16+
        if (!(event.getEntity() instanceof Player victima)) return;

        Player atacante = switch (event.getDamager()) {
            case Player p -> p;
            case Projectile proj when proj.getShooter() instanceof Player p -> p;
            default -> null;
        };

        if (atacante == null || atacante.equals(victima)) return;

        setupIntegrations(); // O(1)

        // 1. INTEGRACIÓN CON PROTECCIONES Y GUERRA (Velocidad Nativa)
        if (claimManagerCache != null && getStoneAtHandle != null) {
            try {
                Object stone = getStoneAtHandle.invoke(claimManagerCache, victima.getLocation());
                if (stone != null) {
                    boolean allowsPvP = (boolean) getFlagHandle.invoke(stone, "pvp");

                    if (!allowsPvP) {
                        boolean ignorarProteccion = false;

                        if (warManagerCache != null && estanEnGuerraActivaHandle != null) {
                            ignorarProteccion = (boolean) estanEnGuerraActivaHandle.invoke(warManagerCache, atacante.getUniqueId(), victima.getUniqueId());
                        }

                        if (!ignorarProteccion) {
                            CrossplayUtils.sendMessage(atacante, configManager.getMessages().mensajes().pvp().bloqueoArmamento());
                            event.setCancelled(true);
                            return;
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }

        // 2. Comprobación del modo PvP (Acceso rápido a Caché)
        if (!manager.tienePvP(atacante) || !manager.tienePvP(victima)) {
            event.setCancelled(true);
            return;
        }

        manager.marcarEnCombate(atacante, victima);

        // 3. Ajuste de daño global (40%)
        event.setDamage(event.getDamage() * 0.40);
    }

    @EventHandler
    public void onMuerte(PlayerDeathEvent event) {
        Player victima = event.getEntity();
        Player asesino = victima.getKiller();
        UUID idVictima = victima.getUniqueId();

        manager.enCombate.remove(idVictima);

        if (asesino != null && manager.tienePvP(victima) && manager.tienePvP(asesino)) {
            UUID idAsesino = asesino.getUniqueId();

            int honorActual = manager.puntosHonor.getOrDefault(idAsesino, 0) + 1;
            manager.puntosHonor.put(idAsesino, honorActual);

            CrossplayUtils.sendMessage(asesino, configManager.getMessages().mensajes().pvp().objetivoNeutralizado()
                    .replace("%victima%", victima.getName()));

            int rachaVictima = manager.rachaAsesinatos.getOrDefault(idVictima, 0);

            if (rachaVictima >= 3) {
                Bukkit.broadcast(color(configManager.getMessages().mensajes().pvp().cazarrecompensasGlobal()
                        .replace("%asesino%", asesino.getName())
                        .replace("%victima%", victima.getName())));

                CrossplayUtils.sendMessage(asesino, configManager.getMessages().mensajes().pvp().bountyReclamado());

                manager.puntosHonor.put(idAsesino, honorActual + 5);
                asesino.getInventory().addItem(new ItemStack(Material.DIAMOND));
                asesino.playSound(asesino.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            }

            int rachaAsesino = manager.rachaAsesinatos.getOrDefault(idAsesino, 0) + 1;
            manager.rachaAsesinatos.put(idAsesino, rachaAsesino);

            if (rachaAsesino == 3) {
                Bukkit.broadcast(color(configManager.getMessages().mensajes().pvp().rachaTresGlobal()
                        .replace("%asesino%", asesino.getName())));
            } else if (rachaAsesino > 3) {
                Bukkit.broadcast(color(configManager.getMessages().mensajes().pvp().rachaMayorGlobal()
                        .replace("%asesino%", asesino.getName())
                        .replace("%kills%", String.valueOf(rachaAsesino))));
            }
        }

        manager.rachaAsesinatos.put(idVictima, 0);
    }

    @EventHandler
    public void onDesconexionCobarde(PlayerQuitEvent event) {
        Player p = event.getPlayer();
        if (manager.estaEnCombate(p)) {
            p.setHealth(0.0);
            manager.enCombate.remove(p.getUniqueId());
            Bukkit.broadcast(color(configManager.getMessages().mensajes().pvp().desconexionCobarde()
                    .replace("%jugador%", p.getName())));
        }
    }
}