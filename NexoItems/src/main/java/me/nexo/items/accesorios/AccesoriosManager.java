package me.nexo.items.accesorios;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.database.DatabaseManager; // Asumido desde el Core
import me.nexo.core.utils.Base64Util;
import me.nexo.items.NexoItems;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 🎒 NexoItems - Manager Central de Accesorios (Arquitectura Enterprise Java 21)
 * Rendimiento: Virtual Threads para DB, Folia Region Sync y O(1) Reads.
 */
@Singleton
public class AccesoriosManager {

    // 🌟 DEPENDENCIAS PROPAGADAS
    private final NexoItems plugin;
    private final DatabaseManager dbManager;
    private final CrossplayUtils crossplayUtils;
    private final Base64Util base64Util;

    // 🚀 MOTOR I/O: Hilos Virtuales para Base de Datos
    private final ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();

    public final Map<String, AccessoryDTO> registro = new HashMap<>();
    public final NamespacedKey llaveAccesorio;

    public final Set<UUID> usuariosCorazonNexo = ConcurrentHashMap.newKeySet();

    // 💉 PILAR 1: Inyección Estricta de Dependencias
    @Inject
    public AccesoriosManager(NexoItems plugin, DatabaseManager dbManager, CrossplayUtils crossplayUtils, Base64Util base64Util) {
        this.plugin = plugin;
        this.dbManager = dbManager;
        this.crossplayUtils = crossplayUtils;
        this.base64Util = base64Util;
        this.llaveAccesorio = new NamespacedKey(plugin, "nexo_accesorio_id");
        cargarBaseDeDatosAccesorios();
    }

    private void cargarBaseDeDatosAccesorios() {
        // Se mantienen tus registros duros como caché inmutable O(1)
        registro.put("guijarro_magnetico", new AccessoryDTO("guijarro_magnetico", AccessoryDTO.Familia.MINERIA, AccessoryDTO.Rareza.COMUN, AccessoryDTO.StatType.FUERZA, 2, "Atrae minerales ligeramente."));
        registro.put("amuleto_espeleologo", new AccessoryDTO("amuleto_espeleologo", AccessoryDTO.Familia.MINERIA, AccessoryDTO.Rareza.EPICO, AccessoryDTO.StatType.VELOCIDAD, 0.05, "Visión en la oscuridad pura."));
        registro.put("reliquia_nucleo", new AccessoryDTO("reliquia_nucleo", AccessoryDTO.Familia.MINERIA, AccessoryDTO.Rareza.MITICO, AccessoryDTO.StatType.VIDA, 20, "Inmunidad parcial a la lava."));
        registro.put("brote_magico", new AccessoryDTO("brote_magico", AccessoryDTO.Familia.TALA, AccessoryDTO.Rareza.COMUN, AccessoryDTO.StatType.ENERGIA_CUSTOM, 10, "Regenera energía en bosques."));
        registro.put("rama_viva", new AccessoryDTO("rama_viva", AccessoryDTO.Familia.TALA, AccessoryDTO.Rareza.RARO, AccessoryDTO.StatType.FUERZA, 5, "+Daño a mobs de madera."));
        registro.put("raiz_arbol_mundo", new AccessoryDTO("raiz_arbol_mundo", AccessoryDTO.Familia.TALA, AccessoryDTO.Rareza.LEGENDARIO, AccessoryDTO.StatType.VIDA, 40, "Resistencia extrema al empuje."));
        registro.put("herradura_oxidada", new AccessoryDTO("herradura_oxidada", AccessoryDTO.Familia.COSECHA, AccessoryDTO.Rareza.COMUN, AccessoryDTO.StatType.VELOCIDAD, 0.02, "Ligeramente más rápido en pasto."));
        registro.put("trebol_4_hojas", new AccessoryDTO("trebol_4_hojas", AccessoryDTO.Familia.COSECHA, AccessoryDTO.Rareza.RARO, AccessoryDTO.StatType.ENERGIA_CUSTOM, 20, "Suerte en drops de cultivos."));
        registro.put("bendicion_demeter", new AccessoryDTO("bendicion_demeter", AccessoryDTO.Familia.COSECHA, AccessoryDTO.Rareza.MITICO, AccessoryDTO.StatType.VIDA, 30, "Aura de crecimiento automático."));
        registro.put("anzuelo_oxidado", new AccessoryDTO("anzuelo_oxidado", AccessoryDTO.Familia.PESCA, AccessoryDTO.Rareza.COMUN, AccessoryDTO.StatType.FUERZA, 1, "Poco útil."));
        registro.put("cebo_plata", new AccessoryDTO("cebo_plata", AccessoryDTO.Familia.PESCA, AccessoryDTO.Rareza.RARO, AccessoryDTO.StatType.VELOCIDAD, 0.03, "Atrae peces raros."));
        registro.put("esfera_leviatan", new AccessoryDTO("esfera_leviatan", AccessoryDTO.Familia.PESCA, AccessoryDTO.Rareza.LEGENDARIO, AccessoryDTO.StatType.ARMADURA, 8, "Respiración acuática infinita."));
        registro.put("escudo_roto", new AccessoryDTO("escudo_roto", AccessoryDTO.Familia.TANQUE, AccessoryDTO.Rareza.COMUN, AccessoryDTO.StatType.ARMADURA, 2, "Bloqueo básico."));
        registro.put("egida", new AccessoryDTO("egida", AccessoryDTO.Familia.TANQUE, AccessoryDTO.Rareza.EPICO, AccessoryDTO.StatType.VIDA, 25, "Absorbe 10% del daño."));
        registro.put("coraza_titan", new AccessoryDTO("coraza_titan", AccessoryDTO.Familia.TANQUE, AccessoryDTO.Rareza.MITICO, AccessoryDTO.StatType.ARMADURA, 15, "Inmune al primer golpe."));
        registro.put("garra", new AccessoryDTO("garra", AccessoryDTO.Familia.MELEE, AccessoryDTO.Rareza.RARO, AccessoryDTO.StatType.FUERZA, 8, "Ataques sangrantes."));
        registro.put("colmillo", new AccessoryDTO("colmillo", AccessoryDTO.Familia.MELEE, AccessoryDTO.Rareza.LEGENDARIO, AccessoryDTO.StatType.FUERZA, 18, "Robo de vida leve."));
        registro.put("pluma", new AccessoryDTO("pluma", AccessoryDTO.Familia.RANGO, AccessoryDTO.Rareza.RARO, AccessoryDTO.StatType.VELOCIDAD, 0.05, "Caída lenta."));
        registro.put("astrolabio", new AccessoryDTO("astrolabio", AccessoryDTO.Familia.RANGO, AccessoryDTO.Rareza.EPICO, AccessoryDTO.StatType.ENERGIA_CUSTOM, 30, "Precisión astral."));
        registro.put("prisma", new AccessoryDTO("prisma", AccessoryDTO.Familia.ENERGIA, AccessoryDTO.Rareza.EPICO, AccessoryDTO.StatType.ENERGIA_CUSTOM, 50, "-5% Costo de Energía."));
        registro.put("ojo_intelecto", new AccessoryDTO("ojo_intelecto", AccessoryDTO.Familia.ENERGIA, AccessoryDTO.Rareza.MITICO, AccessoryDTO.StatType.ENERGIA_CUSTOM, 100, "Regeneración de Maná x2."));
        registro.put("vendaje", new AccessoryDTO("vendaje", AccessoryDTO.Familia.MOVILIDAD, AccessoryDTO.Rareza.COMUN, AccessoryDTO.StatType.VIDA, 5, "Cura pasiva leve."));
        registro.put("espuela", new AccessoryDTO("espuela", AccessoryDTO.Familia.MOVILIDAD, AccessoryDTO.Rareza.EPICO, AccessoryDTO.StatType.VELOCIDAD, 0.10, "Doble salto permitido."));
        registro.put("sello", new AccessoryDTO("sello", AccessoryDTO.Familia.RIQUEZA, AccessoryDTO.Rareza.LEGENDARIO, AccessoryDTO.StatType.ARMADURA, 5, "+15% Monedas obtenidas."));
        registro.put("moneda", new AccessoryDTO("moneda", AccessoryDTO.Familia.RIQUEZA, AccessoryDTO.Rareza.COMUN, AccessoryDTO.StatType.FUERZA, 0, "+1% Monedas obtenidas."));
        registro.put("talisman", new AccessoryDTO("talisman", AccessoryDTO.Familia.CAZAJEFES, AccessoryDTO.Rareza.EPICO, AccessoryDTO.StatType.FUERZA, 10, "+10% Daño a Jefes."));
        registro.put("corazon_nexo", new AccessoryDTO("corazon_nexo", AccessoryDTO.Familia.CAZAJEFES, AccessoryDTO.Rareza.COSMICO, AccessoryDTO.StatType.VIDA, 100, "Te revive una vez cada hora al recibir daño letal."));
    }

    public void abrirBolsa(Player p) {
        // 🚀 Despachamos al pool Virtual de I/O
        virtualExecutor.execute(() -> {
            String sql = "SELECT contenido FROM nexo_storage WHERE uuid = ? AND tipo = 'accesorios'";
            String base64Data = null;

            try (var conn = dbManager.getConnection();
                 var ps = conn.prepareStatement(sql)) {
                ps.setString(1, p.getUniqueId().toString());
                
                try (var rs = ps.executeQuery()) {
                    if (rs.next()) base64Data = rs.getString("contenido");
                }
            } catch (Exception e) {
                plugin.getLogger().severe("❌ Error cargando Bóveda de Accesorios: " + e.getMessage());
            }

            final String finalData = base64Data;

            // 🛡️ FOLIA SYNC: Generación y apertura de inventario en la Región del Jugador
            p.getScheduler().run(plugin, task -> {
                int slotsDesbloqueados = getSlotsDesbloqueados(p);
                int filas = Math.max(1, (int) Math.ceil(slotsDesbloqueados / 9.0));
                int size = Math.min(54, filas * 9); // Límite seguro

                var tituloFormat = crossplayUtils.parseCrossplay(p, "&#ff00ff💍 <bold>BÓVEDA DE ACCESORIOS</bold>");
                var holder = new AccesoriosListener.AccesoriosMenuHolder(tituloFormat, size);
                var inv = holder.getInventory();

                if (finalData != null && !finalData.isEmpty()) {
                    try {
                        inv.setContents(base64Util.itemStackArrayFromBase64(finalData));
                    } catch (Exception e) {
                        plugin.getLogger().severe("❌ Error Base64 en Bóveda de " + p.getName() + ": " + e.getMessage());
                    }
                }

                var cristalBloqueado = new ItemStack(Material.RED_STAINED_GLASS_PANE);
                cristalBloqueado.editMeta(meta -> {
                    meta.displayName(crossplayUtils.parseCrossplay(p, "&#FF5555🔒 <bold>RANURA BLOQUEADA</bold>"));
                    meta.lore(List.of(
                            crossplayUtils.parseCrossplay(p, "&#E6CCFFDesbloquea esta ranura subiendo"),
                            crossplayUtils.parseCrossplay(p, "&#E6CCFFtu nivel de prestigio o con"),
                            crossplayUtils.parseCrossplay(p, "&#E6CCFFun expansor de bóveda.")
                    ));
                });

                for (int i = slotsDesbloqueados; i < inv.getSize(); i++) {
                    inv.setItem(i, cristalBloqueado);
                }

                p.openInventory(inv);
                p.playSound(p.getLocation(), org.bukkit.Sound.ITEM_ARMOR_EQUIP_GOLD, 1f, 1f);
            }, null);
        });
    }

    public int getSlotsDesbloqueados(Player p) {
        return 12;
    }

    public boolean cumpleRequisito(Player p, AccessoryDTO dto) {
        return true;
    }

    public void procesarYGuardarBolsa(Player p, Inventory inv) {
        String base64Data;
        try {
            base64Data = base64Util.itemStackArrayToBase64(inv.getContents());
        } catch (Exception e) {
            plugin.getLogger().severe("❌ Error guardando inventario Base64 de " + p.getName() + ": " + e.getMessage());
            return;
        }

        // 🚀 Guardado asíncrono
        virtualExecutor.execute(() -> {
            String sql = "INSERT INTO nexo_storage (uuid, tipo, contenido) VALUES (?, 'accesorios', ?) " +
                    "ON CONFLICT (uuid, tipo) DO UPDATE SET contenido = EXCLUDED.contenido;";
            try (var conn = dbManager.getConnection();
                 var ps = conn.prepareStatement(sql)) {
                ps.setString(1, p.getUniqueId().toString());
                ps.setString(2, base64Data);
                ps.executeUpdate();
            } catch (Exception e) {
                plugin.getLogger().severe("❌ Error guardando BD Accesorios: " + e.getMessage());
            }
        });

        // 🛡️ LÓGICA DE PODER Y FAMILIAS (Síncrono para cálculos inmediatos)
        Map<AccessoryDTO.Familia, AccessoryDTO> accesoriosActivos = new EnumMap<>(AccessoryDTO.Familia.class);
        
        for (var item : inv.getContents()) {
            if (item == null || item.isEmpty() || item.getType() == Material.RED_STAINED_GLASS_PANE || !item.hasItemMeta()) continue;
            
            var pdc = item.getItemMeta().getPersistentDataContainer();

            if (pdc.has(llaveAccesorio, PersistentDataType.STRING)) {
                String id = pdc.get(llaveAccesorio, PersistentDataType.STRING);
                var dto = registro.get(id);

                if (dto != null && cumpleRequisito(p, dto)) {
                    if (accesoriosActivos.containsKey(dto.family())) {
                        var existente = accesoriosActivos.get(dto.family());
                        if (dto.rarity().getPoderNexo() > existente.rarity().getPoderNexo()) {
                            accesoriosActivos.put(dto.family(), dto);
                        }
                    } else {
                        accesoriosActivos.put(dto.family(), dto);
                    }
                }
            }
        }

        Map<AccessoryDTO.StatType, Double> statsTotales = new EnumMap<>(AccessoryDTO.StatType.class);
        int poderTotal = 0;
        boolean corazon = false;

        for (var activo : accesoriosActivos.values()) {
            poderTotal += activo.rarity().getPoderNexo();
            statsTotales.put(activo.statType(), statsTotales.getOrDefault(activo.statType(), 0.0) + activo.statValue());
            if (activo.id().equals("corazon_nexo")) corazon = true;
        }

        if (corazon) usuariosCorazonNexo.add(p.getUniqueId());
        else usuariosCorazonNexo.remove(p.getUniqueId());

        // Lanzamos el evento inmutable (Síncrono por contrato Bukkit)
        var evento = new AccessoryStatsUpdateEvent(p, statsTotales, poderTotal, corazon);
        plugin.getServer().getPluginManager().callEvent(evento);
    }

    public ItemStack generarAccesorio(String id) {
        var dto = registro.get(id.toLowerCase());
        if (dto == null) return null;

        var item = new ItemStack(Material.PAPER);

        item.editMeta(meta -> {
            meta.displayName(crossplayUtils.parseCrossplay(null, "&#ff00ff<bold>💍 " + dto.id().toUpperCase().replace("_", " ") + "</bold>"));
            
            meta.lore(List.of(
                    crossplayUtils.parseCrossplay(null, "&#E6CCFFFamilia: &#00f5ff" + dto.family().name()),
                    crossplayUtils.parseCrossplay(null, "&#E6CCFFRareza: &#00f5ff" + dto.rarity().name()),
                    net.kyori.adventure.text.Component.empty(),
                    crossplayUtils.parseCrossplay(null, "&#E6CCFFBono: &#ff00ff+" + dto.statValue() + " " + dto.statType().name()),
                    crossplayUtils.parseCrossplay(null, "&#E6CCFFAtributo Único: &#00f5ff" + dto.abilityDescription())
            ));

            meta.getPersistentDataContainer().set(llaveAccesorio, PersistentDataType.STRING, dto.id());
        });

        return item;
    }
}