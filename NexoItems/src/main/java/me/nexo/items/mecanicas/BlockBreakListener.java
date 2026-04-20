package me.nexo.items.mecanicas;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.user.NexoAPI;
import me.nexo.core.user.NexoUser;
import me.nexo.core.utils.NexoColor;
import me.nexo.items.NexoItems;
import me.nexo.items.dtos.ArmorDTO;
import me.nexo.items.dtos.EnchantDTO;
import me.nexo.items.dtos.ToolDTO;
import me.nexo.items.managers.FileManager;
import me.nexo.items.managers.ItemManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🎒 NexoItems - Motor de Recolección y Minería RPG (Arquitectura Enterprise)
 */
@Singleton
public class BlockBreakListener implements Listener {

    private final NexoItems plugin;
    private final FileManager fileManager;
    // 🌟 FIX: Declaramos el ItemManager inyectado
    private final ItemManager itemManager;

    private final HashMap<UUID, Long> cooldownRecoleccion = new HashMap<>();
    private final String MUNDO_RPG = "Mina";
    private final Random random = new Random();

    public static final ConcurrentHashMap<Location, BlockData> bloquesRegenerando = new ConcurrentHashMap<>();

    // ⚡ CACHÉ DE RENDIMIENTO
    private final NamespacedKey keyBendicion;
    private final NamespacedKey keyExperiencia;
    private final NamespacedKey keyMidas;
    private final NamespacedKey keyAura;

    // 💉 Inyección de Dependencias
    @Inject
    public BlockBreakListener(NexoItems plugin, ItemManager itemManager) { // 🌟 FIX: Inyectamos ItemManager
        this.plugin = plugin;
        this.fileManager = plugin.getFileManager();
        this.itemManager = itemManager; // 🌟 FIX: Guardamos la instancia

        this.keyBendicion = new NamespacedKey(plugin, "nexo_enchant_bendicion_nexo");
        this.keyExperiencia = new NamespacedKey(plugin, "nexo_enchant_experiencia_divina");
        this.keyMidas = new NamespacedKey(plugin, "nexo_enchant_toque_de_midas");
        this.keyAura = new NamespacedKey(plugin, "nexo_enchant_aura_recolectora");
    }

    public static void restaurarBloquesRotos() {
        for (Map.Entry<Location, BlockData> entry : bloquesRegenerando.entrySet()) {
            entry.getKey().getBlock().setBlockData(entry.getValue());
        }
        bloquesRegenerando.clear();
    }

    @EventHandler
    public void alPonerBloque(BlockPlaceEvent event) {
        if (event.getPlayer().getWorld().getName().equalsIgnoreCase(MUNDO_RPG)) {
            if (!event.getPlayer().hasPermission("nexo.admin")) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void alRomperBloque(BlockBreakEvent event) {
        Player jugador = event.getPlayer();
        Block bloque = event.getBlock();
        UUID uuid = jugador.getUniqueId();

        if (!jugador.getWorld().getName().equalsIgnoreCase(MUNDO_RPG)) return;

        Material tipoOriginal = bloque.getType();
        BlockData dataOriginal = bloque.getBlockData();

        int xpGanada = 0;
        int costeEnergia = 0;
        ItemStack recompensa = null;
        boolean esCultivo = false, esMineral = false, esTronco = false;

        // 1. RECONOCIMIENTO DEL BLOQUE
        if (tipoOriginal == Material.COAL_ORE || tipoOriginal == Material.DEEPSLATE_COAL_ORE) {
            xpGanada = 2; costeEnergia = 5; recompensa = new ItemStack(Material.COAL, 1); esMineral = true;
        } else if (tipoOriginal == Material.IRON_ORE || tipoOriginal == Material.DEEPSLATE_IRON_ORE) {
            xpGanada = 5; costeEnergia = 10; recompensa = new ItemStack(Material.RAW_IRON, 1); esMineral = true;
        } else if (tipoOriginal == Material.DIAMOND_ORE || tipoOriginal == Material.DEEPSLATE_DIAMOND_ORE) {
            xpGanada = 25; costeEnergia = 30; recompensa = new ItemStack(Material.DIAMOND, 1); esMineral = true;
        } else if (tipoOriginal == Material.OAK_LOG || tipoOriginal == Material.BIRCH_LOG || tipoOriginal == Material.SPRUCE_LOG) {
            xpGanada = 3; costeEnergia = 4; recompensa = new ItemStack(tipoOriginal, 1); esTronco = true;
        } else if (tipoOriginal == Material.WHEAT || tipoOriginal == Material.CARROTS || tipoOriginal == Material.POTATOES) {
            if (dataOriginal instanceof Ageable cultivo) {
                if (cultivo.getAge() == cultivo.getMaximumAge()) {
                    xpGanada = 1; costeEnergia = 1; esCultivo = true;
                    recompensa = new ItemStack(tipoOriginal == Material.WHEAT ? Material.WHEAT : (tipoOriginal == Material.CARROTS ? Material.CARROT : Material.POTATO), 1);
                } else {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        // 2. PROCESAMIENTO DEL RECURSO
        if (xpGanada > 0) {
            event.setCancelled(true);

            long ahora = System.currentTimeMillis();
            if (cooldownRecoleccion.containsKey(uuid) && (ahora - cooldownRecoleccion.get(uuid)) < 300) return;

            NexoUser user = NexoAPI.getInstance().getUserLocal(uuid);
            if (user == null) {
                CrossplayUtils.sendMessage(jugador, "&#FF5555[!] Enlace neuronal con el Nexo perdido.");
                return;
            }

            int energiaActual = user.getEnergiaMineria();
            if (energiaActual < costeEnergia) {
                CrossplayUtils.sendActionBar(jugador, "&#FF5555[!] Energía de Minería agotada.");
                return;
            }

            double fortunaExtra = 0.0;
            String habilidadHerramienta = "ninguna";
            ItemStack itemMano = jugador.getInventory().getItemInMainHand();
            double suerteTotal = 0.0;

            if (itemMano != null && itemMano.hasItemMeta()) {
                ItemMeta metaTool = itemMano.getItemMeta();
                var pdc = metaTool.getPersistentDataContainer();

                // 🌟 FIX: Cambiamos ItemManager estático a inyectado
                if (pdc.has(itemManager.llaveHerramientaId, PersistentDataType.STRING)) {
                    String toolId = pdc.get(itemManager.llaveHerramientaId, PersistentDataType.STRING);
                    ToolDTO toolData = fileManager.getToolDTO(toolId);

                    if (toolData != null) {
                        fortunaExtra = toolData.multiplicadorFortuna();
                        habilidadHerramienta = toolData.habilidadId();

                        if (pdc.has(keyBendicion, PersistentDataType.INTEGER)) {
                            EnchantDTO ench = fileManager.getEnchantDTO("bendicion_nexo");
                            if (ench != null) fortunaExtra += ench.getValorPorNivel(pdc.get(keyBendicion, PersistentDataType.INTEGER));
                        }

                        if (pdc.has(keyExperiencia, PersistentDataType.INTEGER)) {
                            EnchantDTO ench = fileManager.getEnchantDTO("experiencia_divina");
                            if (ench != null) xpGanada = (int) (xpGanada * ench.getValorPorNivel(pdc.get(keyExperiencia, PersistentDataType.INTEGER)));
                        }

                        if (pdc.has(keyMidas, PersistentDataType.INTEGER)) {
                            EnchantDTO ench = fileManager.getEnchantDTO("toque_de_midas");
                            if (ench != null && (random.nextDouble() * 100 <= ench.getValorPorNivel(pdc.get(keyMidas, PersistentDataType.INTEGER)))) {
                                ItemStack oro = new ItemStack(Material.GOLD_INGOT);
                                ItemMeta oroMeta = oro.getItemMeta();
                                if (oroMeta != null) {
                                    oroMeta.displayName(CrossplayUtils.parseCrossplay(jugador, "&#FFAA00Oro Sintético"));
                                    oro.setItemMeta(oroMeta);
                                }
                                jugador.getInventory().addItem(oro);
                                jugador.playSound(jugador.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 2f);
                            }
                        }

                        // 🌟 FIX: Cambiamos ItemManager estático a inyectado
                        Integer rotosGuardados = pdc.get(itemManager.llaveBloquesRotos, PersistentDataType.INTEGER);
                        int rotos = (rotosGuardados != null ? rotosGuardados : 0) + 1;
                        pdc.set(itemManager.llaveBloquesRotos, PersistentDataType.INTEGER, rotos);

                        List<String> lore = metaTool.getLore();
                        if (lore != null) {
                            for (int i = 0; i < lore.size(); i++) {
                                if (org.bukkit.ChatColor.stripColor(lore.get(i)).contains("Bloques Rotos:")) {
                                    lore.set(i, CrossplayUtils.getChat(jugador, "&#E6CCFFBloques Rotos: &#ff00ff" + String.format("%,d", rotos)));
                                    break;
                                }
                            }
                            metaTool.setLore(lore);
                        }
                        itemMano.setItemMeta(metaTool);
                    }
                }
            }

            suerteTotal = fortunaExtra;
            for (ItemStack armor : jugador.getInventory().getArmorContents()) {
                if (armor == null || !armor.hasItemMeta()) continue;
                var pdc = armor.getItemMeta().getPersistentDataContainer();

                // 🌟 FIX: Cambiamos ItemManager estático a inyectado
                if (pdc.has(itemManager.llaveArmaduraId, PersistentDataType.STRING)) {
                    ArmorDTO armorDTO = fileManager.getArmorDTO(pdc.get(itemManager.llaveArmaduraId, PersistentDataType.STRING));
                    if (armorDTO != null) {
                        if (esMineral) suerteTotal += armorDTO.suerteMinera();
                        if (esCultivo) suerteTotal += armorDTO.suerteAgricola();
                        if (esTronco) suerteTotal += armorDTO.suerteTala();
                    }
                }

                if (pdc.has(keyAura, PersistentDataType.INTEGER)) {
                    EnchantDTO ench = fileManager.getEnchantDTO("aura_recolectora");
                    if (ench != null) suerteTotal += ench.getValorPorNivel(pdc.get(keyAura, PersistentDataType.INTEGER));
                }
            }

            int cantidad = (random.nextDouble() * 100 <= suerteTotal) ? 2 : 1;
            if (cantidad > 1) {
                CrossplayUtils.sendActionBar(jugador, "&#55FF55✨ ¡Doble Drop! &8(&f%suerte%%&8)".replace("%suerte%", String.format("%.1f", suerteTotal)));
            }

            if (recompensa != null) {
                recompensa.setAmount(cantidad);
                jugador.getInventory().addItem(recompensa);
            }

            user.setEnergiaMineria(Math.max(0, energiaActual - costeEnergia));
            cooldownRecoleccion.put(uuid, ahora);

            int nivelActual = user.getNexoNivel();
            int xpActual = user.getNexoXp() + xpGanada;

            while (xpActual >= (nivelActual * 100)) {
                xpActual -= (nivelActual * 100);
                nivelActual++;
                CrossplayUtils.sendTitle(jugador,
                        "&#FF55FF¡ASCENSO CÉNIT!",
                        "&#FFAA00Nivel %level%".replace("%level%", String.valueOf(nivelActual)));
            }

            user.setNexoNivel(nivelActual);
            user.setNexoXp(xpActual);

            ejecutarHabilidadHerramienta(jugador, bloque, tipoOriginal, dataOriginal, habilidadHerramienta, recompensa.clone(), suerteTotal, esTronco);

            if (esCultivo) {
                if (!habilidadHerramienta.equalsIgnoreCase("replante_auto")) {
                    Ageable cultivoNuevo = (Ageable) dataOriginal.clone();
                    cultivoNuevo.setAge(0);
                    bloque.setBlockData(cultivoNuevo);
                }
            } else {
                bloquesRegenerando.put(bloque.getLocation(), dataOriginal);
                bloque.setType(esTronco ? Material.AIR : Material.BEDROCK);

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (bloquesRegenerando.containsKey(bloque.getLocation())) {
                        bloque.setBlockData(dataOriginal);
                        bloquesRegenerando.remove(bloque.getLocation());
                    }
                }, 200L);
            }

        } else if (!jugador.hasPermission("nexo.admin")) {
            event.setCancelled(true);
        }
    }

    private void ejecutarHabilidadHerramienta(Player p, Block bloqueCentral, Material tipoOriginal, BlockData dataOriginal, String habilidad, ItemStack recompensaBase, double suerteTotal, boolean esTronco) {
        if (habilidad == null) return;

        switch (habilidad.toLowerCase()) {
            case "treecapitator":
                if (!esTronco) break;
                int tumbados = 0;
                Block actual = bloqueCentral.getRelative(BlockFace.UP);
                while (actual.getType() == tipoOriginal && tumbados < 8) {
                    procesarBloqueExtra(p, actual, tipoOriginal, dataOriginal, recompensaBase, suerteTotal, true);
                    actual = actual.getRelative(BlockFace.UP);
                    tumbados++;
                }
                if (tumbados > 0) p.playSound(p.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 0.5f, 1.5f);
                break;

            case "vein_miner":
                int minados = 0;
                for (int x = -1; x <= 1; x++) {
                    for (int y = -1; y <= 1; y++) {
                        for (int z = -1; z <= 1; z++) {
                            if (x == 0 && y == 0 && z == 0) continue;
                            if (minados >= 6) break;

                            Block cercano = bloqueCentral.getRelative(x, y, z);
                            if (cercano.getType() == tipoOriginal) {
                                procesarBloqueExtra(p, cercano, tipoOriginal, dataOriginal, recompensaBase, suerteTotal, false);
                                minados++;
                            }
                        }
                    }
                }
                if (minados > 0) p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1f, 1f);
                break;

            case "replante_auto":
                if (dataOriginal instanceof Ageable) {
                    Ageable cultivoNuevo = (Ageable) dataOriginal.clone();
                    cultivoNuevo.setAge(0);
                    bloqueCentral.setBlockData(cultivoNuevo);
                    p.playSound(p.getLocation(), Sound.ITEM_CROP_PLANT, 1f, 1f);
                }
                break;

            case "rompe_3x3":
                int rotos = 0;
                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        if (x == 0 && z == 0) continue;
                        Block cercano = bloqueCentral.getRelative(x, 0, z);
                        if (cercano.getType() == tipoOriginal) {
                            procesarBloqueExtra(p, cercano, tipoOriginal, dataOriginal, recompensaBase, suerteTotal, false);
                            rotos++;
                        }
                    }
                }
                if (rotos > 0) p.playSound(p.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 2f);
                break;
        }
    }

    private void procesarBloqueExtra(Player p, Block b, Material tipoOriginal, BlockData dataOriginal, ItemStack recompensaBase, double suerteTotal, boolean esTronco) {
        int cantidad = (random.nextDouble() * 100 <= suerteTotal) ? 2 : 1;
        ItemStack recompensaFinal = recompensaBase.clone();
        recompensaFinal.setAmount(cantidad);

        p.getInventory().addItem(recompensaFinal);

        bloquesRegenerando.put(b.getLocation(), dataOriginal);
        b.setType(esTronco ? Material.AIR : Material.BEDROCK);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (bloquesRegenerando.containsKey(b.getLocation())) {
                b.setBlockData(dataOriginal);
                bloquesRegenerando.remove(b.getLocation());
            }
        }, 200L);
    }
}