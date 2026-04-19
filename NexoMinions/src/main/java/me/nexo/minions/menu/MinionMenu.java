package me.nexo.minions.menu;

import me.nexo.colecciones.api.ColeccionesExpansion;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.menus.NexoMenu;
import me.nexo.core.utils.NexoColor;
import me.nexo.minions.NexoMinions;
import me.nexo.minions.config.ConfigManager;
import me.nexo.minions.data.MinionKeys;
import me.nexo.minions.manager.ActiveMinion;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 🤖 NexoMinions - Menú Principal de Granjas (Arquitectura Enterprise)
 */
public class MinionMenu extends NexoMenu {

    private final ActiveMinion minion;
    private final NexoMinions plugin;
    private final ConfigManager configManager;

    public static final int[] UPGRADE_SLOTS = {10, 11, 15, 16};

    public MinionMenu(Player player, NexoMinions plugin, ActiveMinion minion) {
        super(player);
        this.plugin = plugin;
        this.minion = minion;
        this.configManager = plugin.getConfigManager(); // 💡 Memoria RAM Segura
    }

    @Override
    public String getMenuName() {
        return configManager.getMessages().menu().titulo();
    }

    @Override
    public int getSlots() {
        return 36;
    }

    @Override
    public void setMenuItems() {
        setFillerGlass();

        crearIconoGuia(1, Material.COAL, configManager.getMessages().menu().iconosGuia().combustible());
        crearIconoGuia(2, Material.DROPPER, configManager.getMessages().menu().iconosGuia().compresion());
        crearIconoGuia(6, Material.CHEST, configManager.getMessages().menu().iconosGuia().almacenamiento());
        crearIconoGuia(7, Material.HOPPER, configManager.getMessages().menu().iconosGuia().vinculo());

        for (int i = 0; i < 4; i++) {
            ItemStack upgrade = minion.getUpgrades()[i];
            if (upgrade != null && !upgrade.getType().isAir()) {
                inventory.setItem(UPGRADE_SLOTS[i], upgrade);
            } else {
                inventory.setItem(UPGRADE_SLOTS[i], new ItemStack(Material.AIR));
            }
        }

        // --- STATS ---
        List<String> loreStats = new ArrayList<>();
        loreStats.add(configManager.getMessages().menu().stats().lore().materiaDevorada().replace("%items%", String.valueOf(minion.getStoredItems())));
        double vel = (1.0 - minion.getSpeedMultiplier()) * 100;
        String eficiencia = vel > 0 ? configManager.getMessages().menu().stats().lore().eficienciaActiva().replace("%speed%", String.valueOf((int) vel)) : configManager.getMessages().menu().stats().lore().eficienciaBase();
        loreStats.add(configManager.getMessages().menu().stats().lore().eficiencia() + eficiencia);
        // 🌟 FIX: Usamos el nombre correcto del método (tieneMejoraActiva)
        if (minion.tieneMejoraActiva("COMPACTOR")) loreStats.add(configManager.getMessages().menu().stats().lore().selloAmalgama());
        if (minion.tieneMejoraActiva("STORAGE_LINK")) loreStats.add(configManager.getMessages().menu().stats().lore().nexoLogistico());

        String statsTitle = configManager.getMessages().menu().stats().titulo()
                .replace("%type%", minion.getType().getDisplayName())
                .replace("%tier%", String.valueOf(minion.getTier()));
        setItem(13, minion.getType().getTargetMaterial(), statsTitle, loreStats);

        // --- EVOLUCIÓN ---
        int sigNivel = minion.getTier() + 1;
        if (sigNivel > 12) {
            setItem(22, Material.NETHER_STAR, configManager.getMessages().menu().evolucion().maxNivel(), new ArrayList<>());
        } else {
            List<String> loreEvo = new ArrayList<>(configManager.getMessages().menu().evolucion().lore());
            ConfigurationSection costo = plugin.getTiersConfig().getCostoEvolucion(minion.getType(), sigNivel);
            if (costo != null) {
                String reqName = costo.getString("nexo_id", costo.getString("material", "Alma Perdida"));
                loreEvo.add(configManager.getMessages().menu().evolucion().costoRitual()
                        .replace("%amount%", String.valueOf(costo.getInt("cantidad")))
                        .replace("%item%", reqName));
            } else {
                loreEvo.add(configManager.getMessages().menu().evolucion().errorRitual());
            }
            String evoTitle = configManager.getMessages().menu().evolucion().titulo().replace("%level%", String.valueOf(sigNivel));
            setItem(22, Material.NETHER_STAR, evoTitle, loreEvo);
        }

        // --- COSECHAR ---
        String tipoMinion = minion.getType().name();
        int xpAcumulada = minion.getStoredItems() * 2;
        final String tipoSkillFinal;

        if (tipoMinion.contains("WHEAT") || tipoMinion.contains("CARROT") || tipoMinion.contains("POTATO") || tipoMinion.contains("MELON") || tipoMinion.contains("PUMPKIN") || tipoMinion.contains("SUGAR_CANE")) {
            tipoSkillFinal = "Agricultura";
        } else if (tipoMinion.contains("ORE") || tipoMinion.contains("COBBLESTONE") || tipoMinion.contains("STONE") || tipoMinion.contains("OBSIDIAN")) {
            tipoSkillFinal = "Minería";
        } else {
            tipoSkillFinal = "";
        }

        List<String> loreExtraer = configManager.getMessages().menu().cosechar().lore().stream()
                .map(line -> line.replace("%items%", String.valueOf(minion.getStoredItems()))
                        .replace("%skill%", tipoSkillFinal)
                        .replace("%xp%", String.valueOf(xpAcumulada)))
                .collect(Collectors.toList());

        if (tipoSkillFinal.isEmpty() || minion.getStoredItems() == 0) {
            loreExtraer.removeIf(line -> line.contains("Conocimiento Arcano") || line.contains("XP"));
        }
        setItem(getSlots() - 5, Material.HOPPER, configManager.getMessages().menu().cosechar().titulo(), loreExtraer);

        // --- DESTERRAR ---
        setItem(getSlots() - 1, Material.BARRIER, configManager.getMessages().menu().desterrar().titulo(), null);
    }

    private void crearIconoGuia(int slot, Material mat, me.nexo.minions.config.nodes.MinionsMessagesConfig.MenuItem configItem) {
        List<String> loreConfig = configItem.lore().stream()
                .map(line -> "&#E6CCFF" + line).collect(Collectors.toList());
        setItem(slot, mat, configItem.titulo(), loreConfig);
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        // ==========================================
        // 🌟 MECÁNICA 100% BEDROCK-FRIENDLY PARA MEJORAS
        // ==========================================
        if (event.getClickedInventory() != null && event.getClickedInventory().equals(player.getInventory())) {

            if (plugin.getUpgradesConfig().getUpgradeData(clickedItem) == null) {
                CrossplayUtils.sendMessage(player, configManager.getMessages().manager().mejoraInvalida());
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            for (int i = 0; i < 4; i++) {
                if (minion.getUpgrades()[i] == null || minion.getUpgrades()[i].getType() == Material.AIR) {
                    ItemStack upgradeToApply = clickedItem.clone();
                    upgradeToApply.setAmount(1);
                    minion.setUpgrade(i, upgradeToApply);

                    clickedItem.setAmount(clickedItem.getAmount() - 1);
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                    setMenuItems();
                    return;
                }
            }
            CrossplayUtils.sendMessage(player, configManager.getMessages().manager().ranurasLlenas());
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        int slot = event.getRawSlot();
        for (int i = 0; i < 4; i++) {
            if (slot == UPGRADE_SLOTS[i]) {
                HashMap<Integer, ItemStack> left = player.getInventory().addItem(clickedItem);
                if (!left.isEmpty()) player.getWorld().dropItemNaturally(player.getLocation(), left.get(0));

                minion.setUpgrade(i, null);
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 1f);
                setMenuItems();
                return;
            }
        }

        // ==========================================
        // 🌟 BOTONES INTERACTIVOS
        // ==========================================
        String plainName = "";
        if (clickedItem.hasItemMeta() && clickedItem.getItemMeta().hasDisplayName()) {
            plainName = PlainTextComponentSerializer.plainText().serialize(clickedItem.getItemMeta().displayName());
        }

        // 📦 Clic en Cosechar Tributo
        if (clickedItem.getType() == Material.HOPPER && plainName.contains("COSECHAR")) {
            int cantidad = minion.getStoredItems();
            if (cantidad <= 0) {
                CrossplayUtils.sendMessage(player, configManager.getMessages().manager().faucesVacias());
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            boolean tieneCompactador = minion.tieneMejoraActiva("ITEM_UPGRADES");
            HashMap<Integer, ItemStack> sobrante = new HashMap<>();

            // 🛡️ PARCHE DE SEGURIDAD 3: Descomposición de Stacks
            if (tieneCompactador) {
                int bloques = cantidad / 9;
                int sueltos = cantidad % 9;
                Material matBase = minion.getType().getTargetMaterial();
                Material matCompactado = obtenerBloqueCompactado(matBase);

                if (bloques > 0) darItemsSeguros(player, matCompactado, bloques, sobrante);
                if (sueltos > 0) darItemsSeguros(player, matBase, sueltos, sobrante);
            } else {
                darItemsSeguros(player, minion.getType().getTargetMaterial(), cantidad, sobrante);
            }

            for (ItemStack drop : sobrante.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }

            String tipoMinion = minion.getType().name();
            double xpGanada = cantidad * 2.0;
            String skillAura = "";
            String nombreSkill = "";

            if (tipoMinion.contains("ORE") || tipoMinion.contains("COBBLESTONE") || tipoMinion.contains("STONE") || tipoMinion.contains("OBSIDIAN")) { skillAura = "mining"; nombreSkill = "Minería"; }
            else if (tipoMinion.contains("WHEAT") || tipoMinion.contains("CARROT") || tipoMinion.contains("POTATO") || tipoMinion.contains("MELON") || tipoMinion.contains("PUMPKIN") || tipoMinion.contains("SUGAR_CANE")) { skillAura = "farming"; nombreSkill = "Agricultura"; }
            else if (tipoMinion.contains("LOG") || tipoMinion.contains("WOOD")) { skillAura = "foraging"; nombreSkill = "Tala"; }
            else if (tipoMinion.contains("FISH") || tipoMinion.contains("SALMON")) { skillAura = "fishing"; nombreSkill = "Pesca"; }
            else if (tipoMinion.contains("FLESH") || tipoMinion.contains("BONE") || tipoMinion.contains("SPIDER") || tipoMinion.contains("GUNPOWDER") || tipoMinion.contains("SLIME") || tipoMinion.contains("BLAZE") || tipoMinion.contains("ROTTEN")) { skillAura = "fighting"; nombreSkill = "Combate"; }

            if (!skillAura.isEmpty()) {
                String comando = "skill xp add " + player.getName() + " " + skillAura + " " + (int)xpGanada + " silent";
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), comando);

                String msg = configManager.getMessages().manager().conocimientoArcano()
                        .replace("%xp%", String.valueOf((int)xpGanada))
                        .replace("%skill%", nombreSkill);
                CrossplayUtils.sendMessage(player, msg);
            }

            if (Bukkit.getPluginManager().getPlugin("NexoColecciones") != null) {
                // 💡 Usando inyección a través de las APIs en lugar de singletons estáticos
                me.nexo.colecciones.NexoColecciones pluginCol = org.bukkit.plugin.java.JavaPlugin.getPlugin(me.nexo.colecciones.NexoColecciones.class);
                pluginCol.getCollectionManager().addProgress(player, minion.getType().getTargetMaterial().name(), cantidad);
            }

            minion.setStoredItems(0);
            minion.getEntity().getPersistentDataContainer().set(MinionKeys.STORED_ITEMS, PersistentDataType.INTEGER, 0);

            String compactText = tieneCompactador ? configManager.getMessages().manager().textoCompactadas() : "";
            CrossplayUtils.sendMessage(player, configManager.getMessages().manager().tributoCosechado().replace("%compactadas%", compactText));
            player.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1f, 2f);

            player.closeInventory();
            Bukkit.getScheduler().runTaskLater(plugin, this::open, 3L);
        }

        if (clickedItem.getType() == Material.BARRIER && plainName.contains("DESTERRAR")) {
            player.closeInventory();
            plugin.getMinionManager().recogerMinion(player, minion.getEntity().getUniqueId());
        }

        if (clickedItem.getType() == Material.NETHER_STAR) {
            int sigNivel = minion.getTier() + 1;
            if (sigNivel > 12) return;

            ConfigurationSection costo = plugin.getTiersConfig().getCostoEvolucion(minion.getType(), sigNivel);
            if (costo == null) {
                CrossplayUtils.sendMessage(player, configManager.getMessages().manager().errorArcano());
                return;
            }

            if (!cobrarItems(player, costo.getInt("cantidad"), costo.getString("material", ""), costo.getString("nexo_id", ""))) {
                CrossplayUtils.sendMessage(player, configManager.getMessages().manager().ritualFallido());
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            minion.setTier(sigNivel);
            player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5f, 1.5f);
            CrossplayUtils.sendMessage(player, configManager.getMessages().manager().ritualCompletado().replace("%tier%", String.valueOf(sigNivel)));
            minion.getEntity().getWorld().spawnParticle(org.bukkit.Particle.SCULK_SOUL, minion.getEntity().getLocation().add(0, 1, 0), 50, 0.5, 0.5, 0.5, 0.1);

            player.closeInventory();
            Bukkit.getScheduler().runTaskLater(plugin, this::open, 3L);
        }
    }

    private void darItemsSeguros(Player player, Material mat, int amount, HashMap<Integer, ItemStack> sobrante) {
        int maxStack = mat.getMaxStackSize();
        while (amount > 0) {
            int toGive = Math.min(amount, maxStack);
            sobrante.putAll(player.getInventory().addItem(new ItemStack(mat, toGive)));
            amount -= toGive;
        }
    }

    private Material obtenerBloqueCompactado(Material matBase) {
        return switch (matBase) {
            case COAL -> Material.COAL_BLOCK;
            case IRON_INGOT -> Material.IRON_BLOCK;
            case GOLD_INGOT -> Material.GOLD_BLOCK;
            case DIAMOND -> Material.DIAMOND_BLOCK;
            case EMERALD -> Material.EMERALD_BLOCK;
            case REDSTONE -> Material.REDSTONE_BLOCK;
            case LAPIS_LAZULI -> Material.LAPIS_BLOCK;
            case WHEAT -> Material.HAY_BLOCK;
            case SLIME_BALL -> Material.SLIME_BLOCK;
            case BONE -> Material.BONE_BLOCK;
            default -> matBase;
        };
    }

    private boolean cobrarItems(Player player, int cantidadNecesaria, String material, String nexoId) {
        int recolectado = 0;
        NamespacedKey nexoKey = new NamespacedKey("nexo", "id");

        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType().isAir()) continue;
            boolean coincide = false;
            if (!nexoId.isEmpty() && item.hasItemMeta()) {
                String id = item.getItemMeta().getPersistentDataContainer().get(nexoKey, PersistentDataType.STRING);
                if (nexoId.equals(id)) coincide = true;
            } else if (nexoId.isEmpty() && item.getType().name().equalsIgnoreCase(material)) {
                coincide = true;
            }
            if (coincide) recolectado += item.getAmount();
        }

        if (recolectado < cantidadNecesaria) return false;

        int porCobrar = cantidadNecesaria;
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (porCobrar <= 0) break;
            if (item == null || item.getType().isAir()) continue;

            boolean coincide = false;
            if (!nexoId.isEmpty() && item.hasItemMeta()) {
                String id = item.getItemMeta().getPersistentDataContainer().get(nexoKey, PersistentDataType.STRING);
                if (nexoId.equals(id)) coincide = true;
            } else if (nexoId.isEmpty() && item.getType().name().equalsIgnoreCase(material)) {
                coincide = true;
            }

            if (coincide) {
                if (item.getAmount() <= porCobrar) {
                    porCobrar -= item.getAmount();
                    player.getInventory().setItem(i, null);
                } else {
                    item.setAmount(item.getAmount() - porCobrar);
                    porCobrar = 0;
                }
            }
        }
        return true;
    }
}