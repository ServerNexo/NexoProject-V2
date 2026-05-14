package me.aeroxis.islas.menus;

import me.aeroxis.core.crossplay.CrossplayUtils;
import me.aeroxis.core.menus.AeroxisMenu;
import me.aeroxis.islas.AeroxisIslas;
import me.aeroxis.islas.data.IslandProfile;
import me.aeroxis.islas.managers.IslandManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 🏝️ AeroxisIslas - Árbol de Mejoras (Arquitectura Enterprise)
 * Rendimiento: Economía de Stat Points, Validaciones O(1) y Actualización Folia-Ready.
 */
public class IslandUpgradesMenu extends AeroxisMenu {

    private final AeroxisIslas plugin;
    private final IslandManager islandManager;
    private final IslandProfile profile;

    public IslandUpgradesMenu(Player player, CrossplayUtils crossplayUtils, AeroxisIslas plugin, IslandManager islandManager, IslandProfile profile) {
        super(player, crossplayUtils);
        this.plugin = plugin;
        this.islandManager = islandManager;
        this.profile = profile;
    }

    @Override
    public String getMenuName() {
        return "&#00f5ff✧ &#55FF55Árbol de Mejoras";
    }

    @Override
    public int getSlots() {
        return 54; // Diseño AAA
    }

    @Override
    public void setMenuItems() {
        // 🔲 FONDO INMERSIVO
        ItemStack bg = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        bg.editMeta(meta -> meta.displayName(Component.empty()));
        for (int i = 0; i < getSlots(); i++) inventory.setItem(i, bg);

        // 🌟 PANEL DE PUNTOS (Slot 4 - Arriba al centro)
        ItemStack pointsInfo = new ItemStack(Material.NETHER_STAR);
        pointsInfo.editMeta(meta -> {
            meta.displayName(crossplayUtils.parseCrossplay(player, "&#ff00ff<bold>✧ Puntos del Nexo: " + profile.getUpgradePoints() + "</bold>"));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(crossplayUtils.parseCrossplay(player, "&#E6CCFFLos Puntos de Isla se obtienen al"));
            lore.add(crossplayUtils.parseCrossplay(player, "&#E6CCFFsubir de nivel el núcleo de tu territorio."));
            meta.lore(lore);
        });
        inventory.setItem(4, pointsInfo);

        // ==========================================
        // 🏗️ CATEGORÍA 1: LÍMITES FÍSICOS Y DE ENTIDADES (Fila 2)
        // ==========================================
        renderUpgrade(11, Material.GRASS_BLOCK, "Límites del Territorio", profile.getBorderLevel(),
                "Expande la barrera física de tu isla.", profile.getRealBorderSize() + "x" + profile.getRealBorderSize());

        renderUpgrade(12, Material.PLAYER_HEAD, "Límite de Miembros", profile.getMemberLimitLevel(),
                "Capacidad máxima de jugadores en el equipo.", String.valueOf(profile.getRealMemberLimit()));

        renderUpgrade(13, Material.ARMOR_STAND, "Límite de Minions", profile.getMinionLimitLevel(),
                "Max. de operarios automatizados permitidos.", String.valueOf(profile.getRealMinionLimit()));

        renderUpgrade(14, Material.SPAWNER, "Límite de Spawners", profile.getSpawnerLimitLevel(),
                "Capacidad máxima de generadores.", String.valueOf(profile.getRealSpawnerLimit()));

        renderUpgrade(15, Material.FURNACE, "Límite de Fábricas", profile.getFactoryLimitLevel(),
                "Capacidad de maquinaria industrial pesada.", String.valueOf(profile.getRealFactoryLimit()));

        // ==========================================
        // 🧪 CATEGORÍA 2: MULTIPLICADORES Y TASAS (Fila 4)
        // ==========================================
        renderUpgrade(28, Material.WHEAT, "Velocidad de Cultivos", profile.getCropGrowthLevel(),
                "Acelera los ticks de crecimiento agrícola.", getRateStr(profile.getCropGrowthLevel(), "CROP") + "x");

        renderUpgrade(29, Material.BLAZE_POWDER, "Eficiencia de Spawners", profile.getSpawnerRateLevel(),
                "Reduce el tiempo entre apariciones.", getRateStr(profile.getSpawnerRateLevel(), "SPAWN") + "x");

        renderUpgrade(30, Material.BONE, "Botín de Monstruos", profile.getMobDropLevel(),
                "Aumenta la probabilidad de loot extra.", getRateStr(profile.getMobDropLevel(), "MOB") + "x");

        renderUpgrade(31, Material.GOLDEN_HOE, "Rendimiento Agrícola", profile.getFarmingDropLevel(),
                "Cosecha mayor cantidad de productos.", getRateStr(profile.getFarmingDropLevel(), "FARM") + "x");

        renderUpgrade(32, Material.IRON_ORE, "Generador de Minerales", profile.getGeneratorLevel(),
                "Probabilidad de menas más valiosas.", "Tier " + profile.getGeneratorLevel());

        renderUpgrade(33, Material.EXPERIENCE_BOTTLE, "Bendición de Sabiduría", profile.getXpBonusLevel(),
                "Bonus de Experiencia de Profesiones (AuraSkills).", "+" + (int)Math.round((profile.getRealXpBonus() - 1.0) * 100) + "%");

        // ❌ BOTÓN REGRESAR
        ItemStack back = new ItemStack(Material.RED_BED);
        back.editMeta(meta -> meta.displayName(crossplayUtils.parseCrossplay(player, "&#FF5555<bold>⬅ Regresar al Menú Principal</bold>")));
        inventory.setItem(49, back);
    }

    private void renderUpgrade(int slot, Material mat, String title, int currentLevel, String desc, String currentEffect) {
        boolean isMaxed = currentLevel >= 5;
        int cost = getCost(currentLevel);
        boolean canAfford = profile.getUpgradePoints() >= cost;

        ItemStack item = new ItemStack(isMaxed ? Material.ENCHANTED_BOOK : mat);
        item.editMeta(meta -> {
            meta.displayName(crossplayUtils.parseCrossplay(player, "&#00f5ff<bold>" + title + "</bold>"));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(crossplayUtils.parseCrossplay(player, "&#AAAAAA" + desc));
            lore.add(Component.empty());

            if (isMaxed) {
                lore.add(crossplayUtils.parseCrossplay(player, "&#55FF55Nivel Actual: &#FFAA00MÁXIMO"));
                lore.add(crossplayUtils.parseCrossplay(player, "&#E6CCFFPoder Actual: &#ff00ff" + currentEffect));
                lore.add(Component.empty());
                lore.add(crossplayUtils.parseCrossplay(player, "&#55FF55¡Has alcanzado el límite de esta rama!"));
            } else {
                lore.add(crossplayUtils.parseCrossplay(player, "&#55FF55Nivel: &#FFAA00" + currentLevel + " ➔ " + (currentLevel + 1)));
                lore.add(crossplayUtils.parseCrossplay(player, "&#E6CCFFPoder Actual: &#ff00ff" + currentEffect));
                lore.add(Component.empty());

                String colorCosto = canAfford ? "&#55FF55" : "&#FF5555";
                lore.add(crossplayUtils.parseCrossplay(player, "&#E6CCFFCosto: " + colorCosto + cost + " ✧ Puntos de Isla"));
                lore.add(Component.empty());
                lore.add(crossplayUtils.parseCrossplay(player, canAfford ? "&#FFAA00▶ Haz clic para mejorar" : "&#FF5555[x] No tienes suficientes puntos."));
            }
            meta.lore(lore);
        });
        inventory.setItem(slot, item);
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        e.setCancelled(true);
        if (e.getClickedInventory() == null || !e.getClickedInventory().equals(inventory)) return;

        if (e.getSlot() == 49) {
            new IslandMainMenu(player, crossplayUtils, plugin, islandManager, profile).open();
            return;
        }

        // 🌟 MAPEO O(1) DE LOS BOTONES
        String upgradeType = switch (e.getSlot()) {
            case 11 -> "SIZE";
            case 12 -> "MEMBERS";
            case 13 -> "MINIONS";
            case 14 -> "SPAWNERS";
            case 15 -> "FACTORIES";
            case 28 -> "CROP";
            case 29 -> "SPAWN_RATE";
            case 30 -> "MOB_DROP";
            case 31 -> "FARM_DROP";
            case 32 -> "GENERATOR";
            case 33 -> "XP_BONUS";
            default -> null;
        };

        if (upgradeType != null) intentarComprar(upgradeType);
    }

    // ==========================================
    // ⚙️ MOTOR DE COMPRA POR PUNTOS
    // ==========================================
    private void intentarComprar(String type) {
        int currentLevel = getLevelByType(type);
        if (currentLevel >= 5) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        int costo = getCost(currentLevel);

        if (profile.getUpgradePoints() < costo) {
            crossplayUtils.sendMessage(player, "&#FF5555[!] Necesitas " + costo + " Puntos de Isla. ¡Sube de nivel tu núcleo!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // 1. Cobramos los puntos en RAM
        profile.removeUpgradePoints(costo);
        incrementarNivel(type);

        // 2. Guardamos en Base de Datos de forma asíncrona
        CompletableFuture.runAsync(() -> {
            islandManager.saveIslandProfileAsync(profile);
        });

        // 3. Efectos Visuales y Refresco de UI (Folia-Ready)
        Bukkit.getRegionScheduler().run(plugin, player.getLocation(), task -> {
            if (player.isOnline() && player.getOpenInventory().getTopInventory().equals(inventory)) {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
                crossplayUtils.sendMessage(player, "&#55FF55[✓] ¡La mejora ha sido canalizada en el núcleo!");
                setMenuItems(); // Redibuja con los nuevos valores instantáneamente

                // 🌟 ANIMACIÓN DE EXPANSIÓN DEL MUNDO EN TIEMPO REAL
                if (type.equals("SIZE")) {
                    org.bukkit.World world = player.getWorld();
                    // Validamos que el jugador esté parado físicamente en SU isla
                    if (world.getName().equals("island_" + profile.getOwnerId())) {
                        org.bukkit.WorldBorder border = world.getWorldBorder();

                        // Expande el borde hacia el nuevo tamaño fluida y lentamente a lo largo de 5 segundos
                        border.setSize(profile.getRealBorderSize(), 5L);

                        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 0.5f);
                        crossplayUtils.sendMessage(player, "&#00f5ff[🌊] Las barreras de tu territorio se están expandiendo...");
                    }
                }
            }
        });
    }

    // ==========================================
    // 📊 DICCIONARIO DE DATOS
    // ==========================================

    /**
     * Define cuánto cuesta el *siguiente* nivel.
     * Curva balanceada: 1, 2, 3 y 5 puntos respectivamente. Total para maxear 1 habilidad: 11 puntos.
     */
    private int getCost(int currentLevel) {
        return switch (currentLevel) { case 1 -> 1; case 2 -> 2; case 3 -> 3; default -> 5; };
    }

    private int getLevelByType(String type) {
        return switch (type) {
            case "SIZE" -> profile.getBorderLevel();
            case "MEMBERS" -> profile.getMemberLimitLevel();
            case "MINIONS" -> profile.getMinionLimitLevel();
            case "SPAWNERS" -> profile.getSpawnerLimitLevel();
            case "FACTORIES" -> profile.getFactoryLimitLevel();
            case "CROP" -> profile.getCropGrowthLevel();
            case "SPAWN_RATE" -> profile.getSpawnerRateLevel();
            case "MOB_DROP" -> profile.getMobDropLevel();
            case "FARM_DROP" -> profile.getFarmingDropLevel();
            case "GENERATOR" -> profile.getGeneratorLevel();
            case "XP_BONUS" -> profile.getXpBonusLevel();
            default -> 5;
        };
    }

    private void incrementarNivel(String type) {
        switch (type) {
            case "SIZE" -> profile.setBorderLevel(profile.getBorderLevel() + 1);
            case "MEMBERS" -> profile.setMemberLimitLevel(profile.getMemberLimitLevel() + 1);
            case "MINIONS" -> profile.setMinionLimitLevel(profile.getMinionLimitLevel() + 1);
            case "SPAWNERS" -> profile.setSpawnerLimitLevel(profile.getSpawnerLimitLevel() + 1);
            case "FACTORIES" -> profile.setFactoryLimitLevel(profile.getFactoryLimitLevel() + 1);
            case "CROP" -> profile.setCropGrowthLevel(profile.getCropGrowthLevel() + 1);
            case "SPAWN_RATE" -> profile.setSpawnerRateLevel(profile.getSpawnerRateLevel() + 1);
            case "MOB_DROP" -> profile.setMobDropLevel(profile.getMobDropLevel() + 1);
            case "FARM_DROP" -> profile.setFarmingDropLevel(profile.getFarmingDropLevel() + 1);
            case "GENERATOR" -> profile.setGeneratorLevel(profile.getGeneratorLevel() + 1);
            case "XP_BONUS" -> profile.setXpBonusLevel(profile.getXpBonusLevel() + 1);
        }
    }

    // Devuelve el texto visual para los multiplicadores (ej. "1.5")
    private String getRateStr(int level, String type) {
        if (type.equals("CROP")) return switch(level) { case 1 -> "1.0"; case 2 -> "1.2"; case 3 -> "1.5"; case 4 -> "2.0"; default -> "2.5"; };
        if (type.equals("SPAWN")) return switch(level) { case 1 -> "1.0"; case 2 -> "1.2"; case 3 -> "1.5"; case 4 -> "2.0"; default -> "3.0"; };
        return switch(level) { case 1 -> "1.0"; case 2 -> "1.2"; case 3 -> "1.5"; case 4 -> "1.8"; default -> "2.0"; }; // MOB y FARM
    }
}