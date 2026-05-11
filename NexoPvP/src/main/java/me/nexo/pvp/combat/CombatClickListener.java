package me.nexo.pvp.combat;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ⚔️ NexoPvP - Interceptor de Clicks para Combos (Arquitectura Anti-Spam)
 * Filtra los eventos nativos de Bukkit, evalúa los combos y aplica daño de Postura.
 */
@Singleton
public class CombatClickListener implements Listener {

    private final ComboCacheManager comboManager;
    private final CrossplayUtils crossplayUtils;
    private final PoiseManager poiseManager;

    // ⚡ Filtro Anti-Doble-Evento de Bukkit (50ms de tolerancia)
    private final Map<UUID, Long> clickDebounce = new ConcurrentHashMap<>();

    @Inject
    public CombatClickListener(ComboCacheManager comboManager, CrossplayUtils crossplayUtils, PoiseManager poiseManager) {
        this.comboManager = comboManager;
        this.crossplayUtils = crossplayUtils;
        this.poiseManager = poiseManager;
    }

    // ==========================================
    // 🛡️ FILTRO ESTRICTO DE ARMAS
    // ==========================================
    private boolean isHoldingWeapon(Player player) {
        Material mat = player.getInventory().getItemInMainHand().getType();
        String name = mat.name();
        // Solo permitimos combos si tiene una Espada, Hacha o Tridente
        return name.endsWith("_SWORD") || name.endsWith("_AXE") || name.equals("TRIDENT") || name.equals("MACE");
    }

    // ==========================================
    // 🖱️ INTERCEPTOR DE CLICKS EN EL AIRE / BLOQUES
    // ==========================================
    @EventHandler(priority = EventPriority.HIGH)
    @SuppressWarnings("deprecation") // 🌟 FIX: Silenciamos la advertencia de PaperMC. Sigue siendo el mejor método.
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Action action = event.getAction();

        // 🛑 FILTRO 1: Ignoramos el evento si NO tiene un arma en la mano
        if (!isHoldingWeapon(player)) return;

        // 🛑 FILTRO 2: Si hace clic derecho a un bloque interactivo (Cofres, puertas, mesas), lo ignoramos
        if (action == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            if (event.getClickedBlock().getType().isInteractable()) {
                return;
            }
        }

        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            processClick(player, ComboCacheManager.ClickType.LEFT);
        } else if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            processClick(player, ComboCacheManager.ClickType.RIGHT);
        }
    }

    // ==========================================
    // ⚔️ INTERCEPTOR DE GOLPES A ENTIDADES (Daño Base a Postura)
    // ==========================================
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {

            // Solo fluye el combo si pegó con un arma (evita combos al pegar con tierra o a puños)
            if (isHoldingWeapon(player)) {
                processClick(player, ComboCacheManager.ClickType.LEFT);
            }

            // 🛡️ DESGASTE DE POSTURA NORMAL
            if (event.getEntity() instanceof Player victim) {
                // Solo reducimos postura si le pegaron con un arma
                if (isHoldingWeapon(player)) {
                    poiseManager.damagePoise(victim, 5.0);
                }
            }
        }
    }

    // ==========================================
    // 🧹 LIMPIEZA DE MEMORIA
    // ==========================================
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        clickDebounce.remove(event.getPlayer().getUniqueId());
        comboManager.removePlayer(event.getPlayer());
    }

    // ==========================================
    // ⚙️ MOTOR DE PROCESAMIENTO
    // ==========================================
    private void processClick(Player player, ComboCacheManager.ClickType clickType) {
        long now = System.currentTimeMillis();
        long lastClick = clickDebounce.getOrDefault(player.getUniqueId(), 0L);

        // Si han pasado menos de 50ms desde el último evento, es duplicado de Bukkit. Lo ignoramos.
        if (now - lastClick < 50) return;
        clickDebounce.put(player.getUniqueId(), now);

        List<ComboCacheManager.ClickType> currentCombo = comboManager.registerClick(player, clickType);
        drawComboHUD(player, currentCombo);

        if (currentCombo.size() == 3) {
            evaluateSkill(player, currentCombo);
        }
    }

    private void drawComboHUD(Player player, List<ComboCacheManager.ClickType> combo) {
        StringBuilder hud = new StringBuilder("&#E6CCFF[ ");
        for (int i = 0; i < 3; i++) {
            if (i < combo.size()) {
                hud.append(combo.get(i) == ComboCacheManager.ClickType.LEFT ? "&#FF5555Izq" : "&#55FF55Der");
            } else {
                hud.append("&#777777-");
            }
            if (i < 2) hud.append(" &#E6CCFF| ");
        }
        hud.append(" &#E6CCFF]");
        crossplayUtils.sendActionBar(player, hud.toString());

        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.2f, 2.0f);
    }

    private void evaluateSkill(Player player, List<ComboCacheManager.ClickType> combo) {
        ComboCacheManager.ClickType c1 = combo.get(0);
        ComboCacheManager.ClickType c2 = combo.get(1);
        ComboCacheManager.ClickType c3 = combo.get(2);

        // 🌟 COMBO 1: L-L-R (Rompe-Guardias / Heavy Smash)
        if (c1 == ComboCacheManager.ClickType.LEFT && c2 == ComboCacheManager.ClickType.LEFT && c3 == ComboCacheManager.ClickType.RIGHT) {
            comboManager.clearCombo(player);

            crossplayUtils.sendActionBar(player, "&#FFD700💥 ¡GOLPE ROMPE-GUARDIAS!");
            player.playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 0.5f, 0.8f);

            Entity target = player.getTargetEntity(5);
            if (target instanceof Player victim) {
                poiseManager.damagePoise(victim, 40.0);
                victim.getWorld().spawnParticle(Particle.EXPLOSION, victim.getLocation().add(0, 1, 0), 2);
            }
        }

        // 🌟 COMBO 2: R-L-R (Torbellino / Whirlwind AoE)
        else if (c1 == ComboCacheManager.ClickType.RIGHT && c2 == ComboCacheManager.ClickType.LEFT && c3 == ComboCacheManager.ClickType.RIGHT) {
            comboManager.clearCombo(player);

            crossplayUtils.sendActionBar(player, "&#00f5ff🌪 ¡CORTE CICLÓN!");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.5f);
            player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, player.getLocation().add(0, 1, 0), 5, 1.5, 0.5, 1.5, 0);

            for (Entity entity : player.getNearbyEntities(4, 2, 4)) {
                if (entity instanceof LivingEntity targetHit && targetHit != player) {
                    targetHit.damage(4.0, player);

                    if (targetHit instanceof Player victim) {
                        poiseManager.damagePoise(victim, 15.0);
                    }
                }
            }
        }
    }
}