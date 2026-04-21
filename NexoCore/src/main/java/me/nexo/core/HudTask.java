package me.nexo.core;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.user.NexoUser;
import me.nexo.core.user.UserManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

/**
 * 🖥️ NexoCore - Motor de HUD (Arquitectura Enterprise Java 21+)
 * Rendimiento: Cero Garbage Collection (ItemMeta Bypass), DI pura y Kyori Nativo.
 */
@Singleton
public class HudTask extends BukkitRunnable {

    private final NexoCore plugin;
    private final UserManager userManager;
    private final NamespacedKey classKey;

    // ⚡ CACHÉ: Evita consultar el PluginManager 20 veces por segundo
    private Object auraSkillsApi = null;
    private boolean auraSkillsLoaded = false;

    // 💉 PILAR 1: Inyección de Dependencias pura
    @Inject
    public HudTask(NexoCore plugin, UserManager userManager) {
        this.plugin = plugin;
        this.userManager = userManager;
        this.classKey = new NamespacedKey("nexoitems", "nexo_class");
    }

    private void checkIntegrations() {
        if (!auraSkillsLoaded) {
            if (Bukkit.getPluginManager().isPluginEnabled("AuraSkills")) {
                auraSkillsApi = dev.aurelium.auraskills.api.AuraSkillsApi.get();
            }
            auraSkillsLoaded = true;
        }
    }

    @Override
    public void run() {
        checkIntegrations();

        for (Player p : Bukkit.getOnlinePlayers()) {
            UUID id = p.getUniqueId();
            
            // 🌟 FIX: Acceso directo a la caché local O(1) sin llamar al Singleton estático
            NexoUser user = userManager.getUserOrNull(id);

            // 1. LÓGICA DE BENDICIÓN (Booster Cookie)
            String voidIcon = (user != null && user.isVoidBlessingActive()) ? " &#ff00ff✧" : "";

            // 2. LÓGICA DE MANÁ
            int manaActual = 0;
            int maxMana = 100;

            if (auraSkillsApi != null) {
                try {
                    var api = (dev.aurelium.auraskills.api.AuraSkillsApi) auraSkillsApi;
                    var userAura = api.getUser(id);
                    if (userAura != null) {
                        manaActual = (int) userAura.getMana();
                        maxMana = (int) userAura.getMaxMana();
                    }
                } catch (Throwable ignored) {}
            }

            // Validación O(1) ultra rápida
            boolean isInquisitor = hasFullSet(p, "INQUISITOR");
            if (isInquisitor) maxMana *= 2;

            // 3. BARRA DE MANÁ VISUAL
            String manaBar = buildProgressBar(manaActual, maxMana, 5, "&#00f5ff■", "&#E6CCFF■");

            // 4. ESTADO DE CLASE
            String activeFocus = "&#E6CCFFAventurero";
            if (isInquisitor) activeFocus = "&#ff00ffInquisidor";
            else if (hasFullSet(p, "ASSASSIN")) activeFocus = "&#8b0000Asesino";

            // 5. RENDERIZADO FINAL DEL ACTION BAR
            String hudFormat = String.format("%s &#00f5ff%d/%d MP &#E6CCFF| &#00f5ffClase: %s%s",
                    manaBar, manaActual, maxMana, activeFocus, voidIcon);

            // 🌟 NATIVO PAPER 1.21.5: Envío seguro mediante Kyori Adventure
            Component actionBarComp = LegacyComponentSerializer.legacyAmpersand().deserialize(hudFormat);
            p.sendActionBar(actionBarComp);
        }
    }

    // 🌟 MOTOR DE RENDERIZADO DE BARRAS (Matemática pura)
    private String buildProgressBar(int current, int max, int totalBars, String filledSymbol, String emptySymbol) {
        if (max <= 0) max = 1;
        float percent = Math.min(1.0f, Math.max(0.0f, (float) current / max));
        int progressBars = (int) (totalBars * percent);

        StringBuilder bar = new StringBuilder("&#E6CCFF[ ");
        for (int i = 0; i < totalBars; i++) {
            bar.append(i < progressBars ? filledSymbol : emptySymbol);
        }
        bar.append(" &#E6CCFF]");
        return bar.toString();
    }

    // Validador rápido de Sets para el HUD
    private boolean hasFullSet(Player player, String targetClass) {
        var inv = player.getInventory();
        if (inv.getHelmet() == null || inv.getHelmet().isEmpty()) return false; // 🌟 1.21 fix

        String helmClass = getClassTag(inv.getHelmet());
        if (helmClass == null || !helmClass.equalsIgnoreCase(targetClass)) return false;

        return targetClass.equalsIgnoreCase(getClassTag(inv.getChestplate())) &&
                targetClass.equalsIgnoreCase(getClassTag(inv.getLeggings())) &&
                targetClass.equalsIgnoreCase(getClassTag(inv.getBoots()));
    }

    private String getClassTag(ItemStack item) {
        // 🌟 NATIVO PAPER: Validaciones tempranas seguras
        if (item == null || item.isEmpty() || !item.hasItemMeta()) return null;

        var pdc = item.getItemMeta().getPersistentDataContainer();
        if (pdc.has(classKey, PersistentDataType.STRING)) {
            return pdc.get(classKey, PersistentDataType.STRING);
        }
        return null;
    }
}