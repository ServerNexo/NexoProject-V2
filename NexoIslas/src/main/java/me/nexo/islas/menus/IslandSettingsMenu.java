package me.nexo.islas.menus;

import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.menus.NexoMenu;
import me.nexo.islas.NexoIslas;
import me.nexo.islas.data.IslandProfile;
import me.nexo.islas.managers.IslandManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * ⚙️ NexoIslas - Panel de Configuración (Arquitectura Enterprise)
 * Rendimiento: Modificaciones en Tiempo Real, Guardado Asíncrono y Manipulación Instanciada (ASP).
 */
public class IslandSettingsMenu extends NexoMenu {

    private final NexoIslas plugin;
    private final IslandManager islandManager;
    private final IslandProfile profile;

    public IslandSettingsMenu(Player player, CrossplayUtils crossplayUtils, NexoIslas plugin, IslandManager islandManager, IslandProfile profile) {
        super(player, crossplayUtils);
        this.plugin = plugin;
        this.islandManager = islandManager;
        this.profile = profile;
    }

    @Override
    public String getMenuName() {
        return "&#AAAAAA⚙ &#00f5ffConfiguración de Isla";
    }

    @Override
    public int getSlots() {
        return 54; // Diseño AAA Consistente
    }

    @Override
    public void setMenuItems() {
        // 🔲 FONDO INMERSIVO
        ItemStack bg = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        bg.editMeta(meta -> meta.displayName(Component.empty()));
        for (int i = 0; i < getSlots(); i++) inventory.setItem(i, bg);

        // 🔒 1. PRIVACIDAD DE LA ISLA (Slot 20)
        boolean locked = profile.isLocked();
        ItemStack privacy = new ItemStack(locked ? Material.IRON_DOOR : Material.OAK_DOOR);
        privacy.editMeta(meta -> {
            meta.displayName(crossplayUtils.parseCrossplay(player, "&#00f5ff<bold>🔒 Privacidad del Territorio</bold>"));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(crossplayUtils.parseCrossplay(player, "&#AAAAAAControla si otros jugadores pueden"));
            lore.add(crossplayUtils.parseCrossplay(player, "&#AAAAAAvisitar tu isla usando comandos."));
            lore.add(Component.empty());
            
            if (locked) {
                lore.add(crossplayUtils.parseCrossplay(player, "&#E6CCFFEstado Actual: &#FF5555CERRADA"));
                lore.add(crossplayUtils.parseCrossplay(player, "&#FF5555Solo los miembros pueden entrar."));
            } else {
                lore.add(crossplayUtils.parseCrossplay(player, "&#E6CCFFEstado Actual: &#55FF55ABIERTA"));
                lore.add(crossplayUtils.parseCrossplay(player, "&#55FF55Cualquier jugador puede visitarte."));
            }
            
            lore.add(Component.empty());
            lore.add(crossplayUtils.parseCrossplay(player, "&#FFAA00▶ Haz clic para cambiar"));
            meta.lore(lore);
        });
        inventory.setItem(20, privacy);

        // 🌤️ 2. CONTROL DEL CLIMA (Slot 22)
        ItemStack weather = new ItemStack(Material.SUNFLOWER);
        weather.editMeta(meta -> {
            meta.displayName(crossplayUtils.parseCrossplay(player, "&#55FF55<bold>🌤️ Control Climático</bold>"));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(crossplayUtils.parseCrossplay(player, "&#AAAAAAAlterna entre cielos despejados"));
            lore.add(crossplayUtils.parseCrossplay(player, "&#AAAAAAy tormentas en tu micromundo."));
            lore.add(Component.empty());
            lore.add(crossplayUtils.parseCrossplay(player, "&#FFAA00▶ Haz clic para cambiar el clima"));
            meta.lore(lore);
        });
        inventory.setItem(22, weather);

        // ⏱️ 3. CONTROL DEL TIEMPO (Slot 24)
        ItemStack time = new ItemStack(Material.CLOCK);
        time.editMeta(meta -> {
            meta.displayName(crossplayUtils.parseCrossplay(player, "&#FFAA00<bold>⏱️ Ciclo de Luz</bold>"));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(crossplayUtils.parseCrossplay(player, "&#AAAAAAAlterna entre el amanecer"));
            lore.add(crossplayUtils.parseCrossplay(player, "&#AAAAAAy la medianoche en tu territorio."));
            lore.add(Component.empty());
            lore.add(crossplayUtils.parseCrossplay(player, "&#FFAA00▶ Haz clic para cambiar la hora"));
            meta.lore(lore);
        });
        inventory.setItem(24, time);

        // 🔙 BOTÓN DE REGRESAR (Slot 49)
        ItemStack back = new ItemStack(Material.RED_BED);
        back.editMeta(meta -> meta.displayName(crossplayUtils.parseCrossplay(player, "&#FF5555<bold>⬅ Regresar al Menú Principal</bold>")));
        inventory.setItem(49, back);
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        e.setCancelled(true);
        if (e.getClickedInventory() == null || !e.getClickedInventory().equals(inventory)) return;

        // Validar que solo el dueño pueda cambiar estas configuraciones
        if (e.getSlot() == 20 || e.getSlot() == 22 || e.getSlot() == 24) {
            if (!profile.getOwnerId().equals(player.getUniqueId())) {
                crossplayUtils.sendMessage(player, "&#FF5555[x] Solo el líder absoluto puede alterar las leyes de la isla.");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }
        }

        switch (e.getSlot()) {
            case 20: // 🔒 PRIVACIDAD
                profile.setLocked(!profile.isLocked());
                
                // Guardado Asíncrono de la BD
                CompletableFuture.runAsync(() -> islandManager.saveIslandProfileAsync(profile));
                
                player.playSound(player.getLocation(), profile.isLocked() ? Sound.BLOCK_IRON_DOOR_CLOSE : Sound.BLOCK_IRON_DOOR_OPEN, 1.0f, 1.0f);
                crossplayUtils.sendMessage(player, profile.isLocked() ? "&#FF5555[🔒] Tu isla ha sido cerrada al público." : "&#55FF55[🔓] Tu isla ahora está abierta a visitantes.");
                
                setMenuItems(); // Refrescamos el ítem visualmente
                break;

            case 22: // 🌤️ CLIMA
                manipularMundo("WEATHER");
                break;

            case 24: // ⏱️ TIEMPO
                manipularMundo("TIME");
                break;

            case 49: // 🔙 REGRESAR
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                new IslandMainMenu(player, crossplayUtils, plugin, islandManager, profile).open();
                break;
        }
    }

    // ==========================================
    // 🌍 MOTOR FÍSICO DE MUNDOS (ASP COMPATIBLE)
    // ==========================================
    private void manipularMundo(String accion) {
        // En la arquitectura ASP, el mundo se llama "island_UUID"
        World islandWorld = Bukkit.getWorld("island_" + profile.getOwnerId().toString());

        if (islandWorld == null) {
            crossplayUtils.sendMessage(player, "&#FF5555[x] La isla debe estar cargada y activa para alterar sus leyes.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // 🌟 FOLIA-READY: Operaciones sobre un mundo específico deben ir en su propio Scheduler
        Bukkit.getRegionScheduler().run(plugin, new org.bukkit.Location(islandWorld, 0, 0, 0), task -> {
            if (accion.equals("WEATHER")) {
                if (islandWorld.hasStorm()) {
                    islandWorld.setStorm(false);
                    crossplayUtils.sendMessage(player, "&#55FF55[🌤️] Has despejado los cielos de tu territorio.");
                } else {
                    islandWorld.setStorm(true);
                    crossplayUtils.sendMessage(player, "&#00f5ff[🌧️] Has invocado una tormenta sobre tu territorio.");
                }
            } else if (accion.equals("TIME")) {
                if (islandWorld.getTime() < 12000) {
                    islandWorld.setTime(18000); // Noche
                    crossplayUtils.sendMessage(player, "&#E6CCFF[🌙] Has adelantado el tiempo hacia la medianoche.");
                } else {
                    islandWorld.setTime(1000); // Día
                    crossplayUtils.sendMessage(player, "&#FFAA00[☀️] Has invocado un nuevo amanecer.");
                }
            }
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.5f);
        });
    }
}