package me.aeroxis.core.menus;

import me.aeroxis.core.crossplay.CrossplayUtils;
import me.aeroxis.core.user.AeroxisUser;
import me.aeroxis.core.user.UserManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * 🏛️ Nexo Network - Menú de Bendiciones (Arquitectura Enterprise Java 21)
 * Rendimiento: Inyección Transitiva, editMeta O(1), Consumo de Ítems Seguro y Persistencia Asíncrona.
 */
public class VoidBlessingMenu extends AeroxisMenu {

    private final UserManager userManager;
    private final CrossplayUtils crossplayUtils;
    private final AeroxisUser user;

    // ========================================================================
    // 📝 RECORDATORIO PARA LA CREACIÓN DEL ÍTEM "ESENCIA DEL VACÍO":
    // Cuando entregues este ítem al jugador (ya sea por comando, drop de Boss
    // o tienda), DEBES inyectarle esta llave PDC exacta para que este menú
    // lo reconozca como un sacrificio válido, en lugar de revisar su nombre/lore.
    //
    // Ejemplo de inyección al crear el ítem:
    // ItemMeta meta = item.getItemMeta();
    // meta.getPersistentDataContainer().set(new NamespacedKey("nexocore", "void_essence"), PersistentDataType.BYTE, (byte) 1);
    // item.setItemMeta(meta);
    // ========================================================================
    private final NamespacedKey essenceKey;

    public VoidBlessingMenu(Player player, UserManager userManager, CrossplayUtils crossplayUtils) {
        super(player, crossplayUtils);
        this.userManager = userManager;
        this.crossplayUtils = crossplayUtils;
        this.user = userManager.getUserOrNull(player.getUniqueId());

        // Inicializamos la llave que identifica el ítem de sacrificio
        this.essenceKey = new NamespacedKey("nexocore", "void_essence");
    }

    @Override
    public String getMenuName() {
        return "&#ff00ff✧ &#00f5ffEstado del Vacío";
    }

    @Override
    public int getSlots() {
        return 27;
    }

    @Override
    public void setMenuItems() {
        if (user == null) {
            var errorItem = new ItemStack(Material.BARRIER);
            errorItem.editMeta(meta -> meta.displayName(crossplayUtils.parseCrossplay(player, "&#FF5555[!] Error cargando perfil del Nexo.")));
            inventory.setItem(13, errorItem);
            return;
        }

        // 🟪 FONDO VIVID VOID
        ItemStack bg = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        bg.editMeta(meta -> meta.displayName(Component.empty()));
        for (int i = 0; i < getSlots(); i++) {
            inventory.setItem(i, bg);
        }

        // 🔮 ÍTEM CENTRAL: ESTADO DE LA BENDICIÓN (Slot 13)
        renderizarEstadoBendicion();

        // 🩸 ÍTEM DE SACRIFICIO: BOTÓN DE CANALIZACIÓN (Slot 22)
        renderizarBotonsacrificio();
    }

    private void renderizarEstadoBendicion() {
        ItemStack statusItem;
        List<Component> lore = new ArrayList<>();

        if (user.isVoidBlessingActive()) {
            statusItem = new ItemStack(Material.AMETHYST_CLUSTER);
            long remainingMillis = user.getVoidBlessingUntil() - System.currentTimeMillis();

            statusItem.editMeta(meta -> {
                meta.displayName(crossplayUtils.parseCrossplay(player, "&#ff00ff<bold>Bendición del Vacío</bold>"));
                lore.add(crossplayUtils.parseCrossplay(player, "&#E6CCFFEstado: &#00f5ffACTIVO"));
                lore.add(crossplayUtils.parseCrossplay(player, "&#E6CCFFTiempo restante: &#ff00ff" + formatTime(remainingMillis)));
                lore.add(Component.empty());
                lore.add(crossplayUtils.parseCrossplay(player, "&#00f5ff[✧] Beneficios Canalizados:"));
                lore.add(crossplayUtils.parseCrossplay(player, "&#8b0000 ▶ Protección Hardcore (0% pérdida de XP/Skills)"));
                lore.add(crossplayUtils.parseCrossplay(player, "&#8b0000 ▶ Void Greed (+15% Nexo Coins)"));
                lore.add(crossplayUtils.parseCrossplay(player, "&#8b0000 ▶ Void Reach (Mercado Negro Remoto)"));
                meta.lore(lore);
            });
        } else {
            statusItem = new ItemStack(Material.COAL);
            statusItem.editMeta(meta -> {
                meta.displayName(crossplayUtils.parseCrossplay(player, "&#8b0000<bold>Bendición Inactiva</bold>"));
                lore.add(crossplayUtils.parseCrossplay(player, "&#E6CCFFEstado: &#8b0000DESACTIVADO"));
                lore.add(crossplayUtils.parseCrossplay(player, "&#E6CCFFTu alma se encuentra vulnerable."));
                meta.lore(lore);
            });
        }
        inventory.setItem(13, statusItem);
    }

    private void renderizarBotonsacrificio() {
        boolean tieneEsencia = playerHasVoidEssence();
        ItemStack sacrificeItem = new ItemStack(tieneEsencia ? Material.ENDER_EYE : Material.ENDER_PEARL);

        sacrificeItem.editMeta(meta -> {
            meta.displayName(crossplayUtils.parseCrossplay(player, "&#00f5ff<bold>Canalizar Esencia</bold>"));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(crossplayUtils.parseCrossplay(player, "&#E6CCFFSacrifica una Esencia del Vacío"));
            lore.add(crossplayUtils.parseCrossplay(player, "&#E6CCFFpara obtener o extender tu bendición"));
            lore.add(crossplayUtils.parseCrossplay(player, "&#E6CCFFpor &#ff00ff24 Horas&#E6CCFF."));
            lore.add(Component.empty());

            if (tieneEsencia) {
                lore.add(crossplayUtils.parseCrossplay(player, "&#55FF55[!] Tienes esencias. ¡Haz clic para sacrificar!"));
            } else {
                lore.add(crossplayUtils.parseCrossplay(player, "&#FF5555[x] No tienes Esencias del Vacío en tu inventario."));
            }
            meta.lore(lore);
        });

        inventory.setItem(22, sacrificeItem);
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        event.setCancelled(true); // Bloqueamos robos de ítems

        if (user == null) return;

        // 🌟 LÓGICA DE CLIC: Si hacen clic en el botón de sacrificio (Slot 22)
        if (event.getSlot() == 22) {
            if (playerHasVoidEssence()) {
                consumirVoidEssence();
                otorgarBendicion();
            } else {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            }
        }
    }

    // ==========================================
    // ⚙️ MÉTODOS DE MECÁNICA FÍSICA
    // ==========================================

    private boolean playerHasVoidEssence() {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(essenceKey, PersistentDataType.BYTE)) {
                return true;
            }
        }
        return false;
    }

    private void consumirVoidEssence() {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(essenceKey, PersistentDataType.BYTE)) {
                item.setAmount(item.getAmount() - 1);
                player.getInventory().setItem(i, item);
                break; // Solo consumimos una
            }
        }
    }

    private void otorgarBendicion() {
        // Añadimos 24 horas (86,400,000 milisegundos)
        long durationToAdd = 86400000L;
        long currentTime = System.currentTimeMillis();

        // 🌟 Mutamos el objeto en la RAM (El sistema lo guardará en la DB al desconectarse)
        long newUntil = user.isVoidBlessingActive() ? user.getVoidBlessingUntil() + durationToAdd : currentTime + durationToAdd;
        user.setVoidBlessingUntil(newUntil);

        // Efectos Visuales y Sonoros de Ritual
        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5f, 1.5f);
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 2.0f);
        player.getWorld().spawnParticle(Particle.REVERSE_PORTAL, player.getLocation().add(0, 1, 0), 50, 0.5, 1.0, 0.5, 0.1);

        crossplayUtils.sendMessage(player, "&#ff00ff✧ &#00f5ff¡Los Dioses del Vacío han aceptado tu sacrificio!");

        // Refrescamos el menú instantáneamente
        setMenuItems();
    }

    private String formatTime(long millis) {
        long seconds = millis / 1000;
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        return String.format("%02dh %02dm %02ds", h, m, s);
    }
}