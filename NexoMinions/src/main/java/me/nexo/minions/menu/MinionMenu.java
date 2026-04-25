package me.nexo.minions.menu;

import me.nexo.colecciones.colecciones.CollectionManager;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.menus.NexoMenu;
import me.nexo.minions.NexoMinions;
import me.nexo.minions.config.ConfigManager;
import me.nexo.minions.data.MinionDNA;
import me.nexo.minions.data.TiersConfig;
import me.nexo.minions.data.UpgradesConfig;
import me.nexo.minions.manager.ActiveMinion;
import me.nexo.minions.manager.MinionManager;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
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
 * Rendimiento: Snapshot genético Inmutable y Encapsulamiento Estricto (Cero MinionKeys).
 */
public class MinionMenu extends NexoMenu {

    private final ActiveMinion minion;
    private final MinionDNA snapshotDna; // 🌟 FOTOGRAFÍA O(1) PARA EVITAR RACE CONDITIONS
    private final NexoMinions plugin;

    private final ConfigManager configManager;
    private final TiersConfig tiersConfig;
    private final UpgradesConfig upgradesConfig;
    private final MinionManager minionManager;
    private final CrossplayUtils crossplayUtils;
    private final CollectionManager collectionManager;

    public static final int[] UPGRADE_SLOTS = {10, 11, 15, 16};

    public MinionMenu(Player player, ActiveMinion minion, NexoMinions plugin,
                      ConfigManager configManager, TiersConfig tiersConfig,
                      UpgradesConfig upgradesConfig, MinionManager minionManager,
                      CrossplayUtils crossplayUtils, CollectionManager collectionManager) {

        super(player, crossplayUtils);

        this.minion = minion;
        this.snapshotDna = minion.getDna();
        this.plugin = plugin;
        this.configManager = configManager;
        this.tiersConfig = tiersConfig;
        this.upgradesConfig = upgradesConfig;
        this.minionManager = minionManager;
        this.crossplayUtils = crossplayUtils;
        this.collectionManager = collectionManager;
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
            var upgrade = minion.getUpgrades()[i];
            if (upgrade != null && !upgrade.getType().isAir()) {
                inventory.setItem(UPGRADE_SLOTS[i], upgrade);
            } else {
                inventory.setItem(UPGRADE_SLOTS[i], new ItemStack(Material.AIR));
            }
        }

        List<String> loreStats = new ArrayList<>();
        loreStats.add(configManager.getMessages().menu().stats().lore().materiaDevorada().replace("%items%", String.valueOf(snapshotDna.storedItems())));
        double vel = (1.0 - minion.getSpeedMultiplier()) * 100;
        String eficiencia = vel > 0 ? configManager.getMessages().menu().stats().lore().eficienciaActiva().replace("%speed%", String.valueOf((int) vel)) : configManager.getMessages().menu().stats().lore().eficienciaBase();
        loreStats.add(configManager.getMessages().menu().stats().lore().eficiencia() + eficiencia);

        if (minion.tieneMejoraActiva("COMPACTOR")) loreStats.add(configManager.getMessages().menu().stats().lore().selloAmalgama());
        if (minion.tieneMejoraActiva("STORAGE_LINK")) loreStats.add(configManager.getMessages().menu().stats().lore().nexoLogistico());

        String statsTitle = configManager.getMessages().menu().stats().titulo()
                .replace("%type%", snapshotDna.type().getDisplayName())
                .replace("%tier%", String.valueOf(snapshotDna.tier()));
        setItem(13, snapshotDna.type().getTargetMaterial(), statsTitle, loreStats);

        int sigNivel = snapshotDna.tier() + 1;
        if (sigNivel > 12) {
            setItem(22, Material.NETHER_STAR, configManager.getMessages().menu().evolucion().maxNivel(), new ArrayList<>());
        } else {
            var loreEvo = new ArrayList<>(configManager.getMessages().menu().evolucion().lore());
            var costo = tiersConfig.getCostoEvolucion(snapshotDna.type(), sigNivel);

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

        String tipoMinion = snapshotDna.type().name();
        int xpAcumulada = snapshotDna.storedItems() * 2;
        final String tipoSkillFinal;

        if (tipoMinion.contains("WHEAT") || tipoMinion.contains("CARROT") || tipoMinion.contains("POTATO") || tipoMinion.contains("MELON") || tipoMinion.contains("PUMPKIN") || tipoMinion.contains("SUGAR_CANE")) {
            tipoSkillFinal = "Agricultura";
        } else if (tipoMinion.contains("ORE") || tipoMinion.contains("COBBLESTONE") || tipoMinion.contains("STONE") || tipoMinion.contains("OBSIDIAN")) {
            tipoSkillFinal = "Minería";
        } else {
            tipoSkillFinal = "";
        }

        var loreExtraer = configManager.getMessages().menu().cosechar().lore().stream()
                .map(line -> line.replace("%items%", String.valueOf(snapshotDna.storedItems()))
                        .replace("%skill%", tipoSkillFinal)
                        .replace("%xp%", String.valueOf(xpAcumulada)))
                .collect(Collectors.toList());

        if (tipoSkillFinal.isEmpty() || snapshotDna.storedItems() == 0) {
            loreExtraer.removeIf(line -> line.contains("Conocimiento Arcano") || line.contains("XP"));
        }
        setItem(getSlots() - 5, Material.HOPPER, configManager.getMessages().menu().cosechar().titulo(), loreExtraer);

        setItem(getSlots() - 1, Material.BARRIER, configManager.getMessages().menu().desterrar().titulo(), null);
    }

    private void crearIconoGuia(int slot, Material mat, me.nexo.minions.config.nodes.MinionsMessagesConfig.MenuItem configItem) {
        var loreConfig = configItem.lore().stream()
                .map(line -> "&#E6CCFF" + line).collect(Collectors.toList());
        setItem(slot, mat, configItem.titulo(), loreConfig);
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        event.setCancelled(true);

        var clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        if (event.getClickedInventory() != null && event.getClickedInventory().equals(player.getInventory())) {

            if (upgradesConfig.getUpgradeData(clickedItem) == null) {
                crossplayUtils.sendMessage(player, configManager.getMessages().manager().mejoraInvalida());
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            for (int i = 0; i < 4; i++) {
                if (minion.getUpgrades()[i] == null || minion.getUpgrades()[i].getType() == Material.AIR) {
                    var upgradeToApply = clickedItem.clone();
                    upgradeToApply.setAmount(1);
                    minion.setUpgrade(i, upgradeToApply);

                    clickedItem.setAmount(clickedItem.getAmount() - 1);
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);

                    player.closeInventory();
                    Bukkit.getScheduler().runTaskLater(plugin, this::open, 2L);
                    return;
                }
            }
            crossplayUtils.sendMessage(player, configManager.getMessages().manager().ranurasLlenas());
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        int slot = event.getRawSlot();
        for (int i = 0; i < 4; i++) {
            if (slot == UPGRADE_SLOTS[i]) {
                var left = player.getInventory().addItem(clickedItem);
                if (!left.isEmpty()) player.getWorld().dropItemNaturally(player.getLocation(), left.get(0));

                minion.setUpgrade(i, null);
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 1f);

                player.closeInventory();
                Bukkit.getScheduler().runTaskLater(plugin, this::open, 2L);
                return;
            }
        }

        String plainName = "";
        if (clickedItem.hasItemMeta() && clickedItem.getItemMeta().hasDisplayName()) {
            plainName = PlainTextComponentSerializer.plainText().serialize(clickedItem.getItemMeta().displayName());
        }

        if (clickedItem.getType() == Material.HOPPER && plainName.contains("COSECHAR")) {
            int cantidad = minion.getDna().storedItems();
            if (cantidad <= 0) {
                crossplayUtils.sendMessage(player, configManager.getMessages().manager().faucesVacias());
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            boolean tieneCompactador = minion.tieneMejoraActiva("ITEM_UPGRADES");
            HashMap<Integer, ItemStack> sobrante = new HashMap<>();

            if (tieneCompactador) {
                int bloques = cantidad / 9;
                int sueltos = cantidad % 9;
                var matBase = snapshotDna.type().getTargetMaterial();
                var matCompactado = obtenerBloqueCompactado(matBase);

                if (bloques > 0) darItemsSeguros(player, matCompactado, bloques, sobrante);
                if (sueltos > 0) darItemsSeguros(player, matBase, sueltos, sobrante);
            } else {
                darItemsSeguros(player, snapshotDna.type().getTargetMaterial(), cantidad, sobrante);
            }

            for (ItemStack drop : sobrante.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }

            String tipoMinion = snapshotDna.type().name();
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
                crossplayUtils.sendMessage(player, msg);
            }

            if (collectionManager != null) {
                collectionManager.addProgress(player, snapshotDna.type().getTargetMaterial().name(), cantidad);
            }

            // 🌟 ENCAPSULAMIENTO AAA: Modificamos RAM y guardamos en una sola línea segura.
            minion.setDna(minion.getDna().withUpdatedState(0, minion.getDna().nextActionTime()));

            String compactText = tieneCompactador ? configManager.getMessages().manager().textoCompactadas() : "";
            crossplayUtils.sendMessage(player, configManager.getMessages().manager().tributoCosechado().replace("%compactadas%", compactText));
            player.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1f, 2f);

            player.closeInventory();
        }

        if (clickedItem.getType() == Material.BARRIER && plainName.contains("DESTERRAR")) {
            player.closeInventory();
            minionManager.recogerMinion(player, minion.getEntity().getUniqueId());
        }

        if (clickedItem.getType() == Material.NETHER_STAR) {
            int sigNivel = snapshotDna.tier() + 1;
            if (sigNivel > 12) return;

            var costo = tiersConfig.getCostoEvolucion(snapshotDna.type(), sigNivel);
            if (costo == null) {
                crossplayUtils.sendMessage(player, configManager.getMessages().manager().errorArcano());
                return;
            }

            if (!cobrarItems(player, costo.getInt("cantidad"), costo.getString("material", ""), costo.getString("nexo_id", ""))) {
                crossplayUtils.sendMessage(player, configManager.getMessages().manager().ritualFallido());
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            MinionDNA curDna = minion.getDna();

            // 🌟 ENCAPSULAMIENTO AAA: Inyectamos el nuevo nivel de forma limpia y directa
            MinionDNA mutatedDna = new MinionDNA(curDna.ownerId(), curDna.type(), sigNivel, curDna.speedMutation(), curDna.strikeProbability(), curDna.fatigueResistance(), curDna.storedItems(), curDna.nextActionTime());
            minion.setDna(mutatedDna);

            player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5f, 1.5f);
            crossplayUtils.sendMessage(player, configManager.getMessages().manager().ritualCompletado().replace("%tier%", String.valueOf(sigNivel)));
            minion.getEntity().getWorld().spawnParticle(org.bukkit.Particle.SCULK_SOUL, minion.getEntity().getLocation().add(0, 1, 0), 50, 0.5, 0.5, 0.5, 0.1);

            player.closeInventory();
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
        var nexoKey = new NamespacedKey("nexo", "id");

        for (var item : player.getInventory().getContents()) {
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
            var item = player.getInventory().getItem(i);
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