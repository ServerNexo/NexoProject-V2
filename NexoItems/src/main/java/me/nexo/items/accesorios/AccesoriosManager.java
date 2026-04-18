package me.nexo.items.accesorios;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.NexoCore;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.items.NexoItems;
import me.nexo.core.utils.Base64Util;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 🎒 NexoItems - Manager Central de Accesorios (Arquitectura Enterprise)
 */
@Singleton
public class AccesoriosManager {

    private final NexoItems plugin;
    public final Map<String, AccessoryDTO> registro = new HashMap<>();
    public final NamespacedKey llaveAccesorio;

    public final Set<UUID> usuariosCorazonNexo = new HashSet<>();

    // 💉 PILAR 3: Inyección de Dependencias
    @Inject
    public AccesoriosManager(NexoItems plugin) {
        this.plugin = plugin;
        this.llaveAccesorio = new NamespacedKey(plugin, "nexo_accesorio_id");
        cargarBaseDeDatosAccesorios();
    }

    private void cargarBaseDeDatosAccesorios() {
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
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            NexoCore nexoCore = (NexoCore) Bukkit.getPluginManager().getPlugin("NexoCore");
            if (nexoCore == null || nexoCore.getDatabaseManager() == null) {
                CrossplayUtils.sendMessage(p, "&#8b0000[!] Error crítico: Enlace caído con la Base de Datos Central.");
                return;
            }

            String sql = "SELECT contenido FROM nexo_storage WHERE uuid = ? AND tipo = 'accesorios'";
            String base64Data = null;

            try (Connection conn = nexoCore.getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, p.getUniqueId().toString());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) base64Data = rs.getString("contenido");
            } catch (Exception e) {
                e.printStackTrace();
            }

            String finalData = base64Data;

            Bukkit.getScheduler().runTask(plugin, () -> {
                int slotsDesbloqueados = getSlotsDesbloqueados(p);
                int filas = Math.max(1, (int) Math.ceil(slotsDesbloqueados / 9.0));
                int size = filas * 9;

                // 🌟 FIX: Creación del menú usando el Holder Seguro del Listener
                net.kyori.adventure.text.Component tituloFormat = CrossplayUtils.parseCrossplay(p, "&#ff00ff💍 <bold>BÓVEDA DE ACCESORIOS</bold>");
                AccesoriosListener.AccesoriosMenuHolder holder = new AccesoriosListener.AccesoriosMenuHolder(tituloFormat, size);
                Inventory inv = holder.getInventory();

                if (finalData != null && !finalData.isEmpty()) {
                    inv.setContents(Base64Util.itemStackArrayFromBase64(finalData));
                }

                // 🌟 FIX: Textos de los Slots Bloqueados directamente en código
                ItemStack cristalBloqueado = new ItemStack(Material.RED_STAINED_GLASS_PANE);
                ItemMeta meta = cristalBloqueado.getItemMeta();
                meta.displayName(CrossplayUtils.parseCrossplay(p, "&#FF5555🔒 <bold>RANURA BLOQUEADA</bold>"));

                List<String> loreRaw = Arrays.asList(
                        "&#E6CCFFDesbloquea esta ranura subiendo",
                        "&#E6CCFFtu nivel de prestigio o con",
                        "&#E6CCFFun expansor de bóveda."
                );
                meta.lore(loreRaw.stream().map(line -> CrossplayUtils.parseCrossplay(p, line)).collect(Collectors.toList()));
                cristalBloqueado.setItemMeta(meta);

                // Rellenar lo bloqueado
                for (int i = slotsDesbloqueados; i < inv.getSize(); i++) {
                    inv.setItem(i, cristalBloqueado);
                }

                p.openInventory(inv);
                p.playSound(p.getLocation(), org.bukkit.Sound.ITEM_ARMOR_EQUIP_GOLD, 1f, 1f);
            });
        });
    }

    public int getSlotsDesbloqueados(Player p) {
        // Todo: Conectar a tu sistema de rangos/niveles (Por ahora da 12)
        return 12;
    }

    public boolean cumpleRequisito(Player p, AccessoryDTO dto) {
        // Todo: Verificaciones extra si fueran necesarias
        return true;
    }

    public void procesarYGuardarBolsa(Player p, Inventory inv) {
        String base64Data = Base64Util.itemStackArrayToBase64(inv.getContents());
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            NexoCore nexoCore = (NexoCore) Bukkit.getPluginManager().getPlugin("NexoCore");
            if (nexoCore == null || nexoCore.getDatabaseManager() == null) return;

            String sql = "INSERT INTO nexo_storage (uuid, tipo, contenido) VALUES (?, 'accesorios', ?) " +
                    "ON CONFLICT (uuid, tipo) DO UPDATE SET contenido = EXCLUDED.contenido;";
            try (Connection conn = nexoCore.getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, p.getUniqueId().toString());
                ps.setString(2, base64Data);
                ps.executeUpdate();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // 🛡️ LÓGICA DE PODER Y FAMILIAS
        Map<AccessoryDTO.Familia, AccessoryDTO> accesoriosActivos = new EnumMap<>(AccessoryDTO.Familia.class);
        for (ItemStack item : inv.getContents()) {
            if (item == null || item.getType() == Material.RED_STAINED_GLASS_PANE || !item.hasItemMeta()) continue;
            var pdc = item.getItemMeta().getPersistentDataContainer();

            if (pdc.has(llaveAccesorio, PersistentDataType.STRING)) {
                String id = pdc.get(llaveAccesorio, PersistentDataType.STRING);
                AccessoryDTO dto = registro.get(id);

                if (dto != null && cumpleRequisito(p, dto)) {
                    if (accesoriosActivos.containsKey(dto.family())) {
                        AccessoryDTO existente = accesoriosActivos.get(dto.family());
                        // Solo equipamos el de mayor poder dentro de la misma familia
                        if (dto.rarity().getPoderNexo() > existente.rarity().getPoderNexo()) {
                            accesoriosActivos.put(dto.family(), dto);
                        }
                    } else {
                        accesoriosActivos.put(dto.family(), dto);
                    }
                }
            }
        }

        // 📊 SUMA DE ESTADÍSTICAS
        Map<AccessoryDTO.StatType, Double> statsTotales = new EnumMap<>(AccessoryDTO.StatType.class);
        int poderTotal = 0;
        boolean corazon = false;

        for (AccessoryDTO activo : accesoriosActivos.values()) {
            poderTotal += activo.rarity().getPoderNexo();
            statsTotales.put(activo.statType(), statsTotales.getOrDefault(activo.statType(), 0.0) + activo.statValue());
            if (activo.id().equals("corazon_nexo")) corazon = true;
        }

        if (corazon) usuariosCorazonNexo.add(p.getUniqueId());
        else usuariosCorazonNexo.remove(p.getUniqueId());

        // Lanzamos el evento para que el Listener asigne los Stats
        AccessoryStatsUpdateEvent evento = new AccessoryStatsUpdateEvent(p, statsTotales, poderTotal, corazon);
        Bukkit.getPluginManager().callEvent(evento);
    }

    public ItemStack generarAccesorio(String id) {
        AccessoryDTO dto = registro.get(id.toLowerCase());
        if (dto == null) return null;

        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(CrossplayUtils.parseCrossplay(null, "&#ff00ff<bold>💍 " + dto.id().toUpperCase().replace("_", " ") + "</bold>"));
        meta.lore(Arrays.asList(
                CrossplayUtils.parseCrossplay(null, "&#E6CCFFFamilia: &#00f5ff" + dto.family().name()),
                CrossplayUtils.parseCrossplay(null, "&#E6CCFFRareza: &#00f5ff" + dto.rarity().name()),
                CrossplayUtils.parseCrossplay(null, " "),
                CrossplayUtils.parseCrossplay(null, "&#E6CCFFBono: &#ff00ff+" + dto.statValue() + " " + dto.statType().name()),
                CrossplayUtils.parseCrossplay(null, "&#E6CCFFAtributo Único: &#00f5ff" + dto.abilityDescription())
        ));

        meta.getPersistentDataContainer().set(llaveAccesorio, PersistentDataType.STRING, dto.id());

        item.setItemMeta(meta);
        return item;
    }
}