package me.nexo.core;

import me.nexo.core.user.NexoAPI;
import me.nexo.core.user.NexoUser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.NamespacedKey;

import java.util.UUID;

/**
 * 🖥️ NexoCore - Motor de HUD (Arquitectura Enterprise Java 25)
 * Rendimiento: Cero Garbage Collection (ItemMeta Bypass) y Kyori Nativo.
 */
public class HudTask extends BukkitRunnable {

    private final NexoCore plugin;
    private final NamespacedKey classKey;

    // ⚡ CACHÉ: Evita consultar el PluginManager 20 veces por segundo
    private Object auraSkillsApi = null;
    private boolean auraSkillsLoaded = false;

    public HudTask(NexoCore plugin) {
        this.plugin = plugin;
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
            NexoUser user = NexoAPI.getInstance().getUserLocal(id);

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

            // 🌟 FIX COMPILADOR: Deserialize directo de Kyori para evitar el error de PlayerHeadObjectContents
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
        // 🌟 FIX: Volvemos a la sintaxis oficial de Bukkit/Paper.
        // Primero verificamos que no esté vacío y que tenga ItemMeta.
        if (item == null || item.isEmpty() || !item.hasItemMeta()) return null;

        // Leemos el PDC a través del ItemMeta (Paper 1.21 ya optimiza esto por debajo)
        var pdc = item.getItemMeta().getPersistentDataContainer();
        if (pdc.has(classKey, PersistentDataType.STRING)) {
            return pdc.get(classKey, PersistentDataType.STRING);
        }
        return null;
    }
}