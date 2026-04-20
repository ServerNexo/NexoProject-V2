package me.nexo.items.managers;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.items.NexoItems;
import me.nexo.items.dtos.*;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

/**
 * ⚔️ NexoItems - Gestor de Ítems (Arquitectura Enterprise Purificada)
 * 🌟 MEJORA: Eliminación de Estáticos para Inyección de Dependencias Limpia
 */
@Singleton
public class ItemManager {

    private final NexoItems plugin;
    private final FileManager fileManager;

    // 🌟 Llaves de Datos (Agrupadas para eficiencia)
    public final NamespacedKey
            llaveNivelMejora, llaveMaterialMejora, llaveVidaExtra, llaveElemento, llaveSoulbound,
            llaveSuerteMinera, llaveVelocidadMineria, llaveSuerteAgricola, llaveVelocidadMovimiento,
            llaveSuerteTala, llaveFuerzaHacha, llaveVelocidadPesca, llaveCriaturaMarina,
            llaveArmaduraId, llaveWeaponId, llaveWeaponPrestige, llaveHerramientaId,
            llaveBloquesRotos, llaveReforja, llaveEnchantId, llaveEnchantNivel,
            llaveNivelEvolucion, llaveEsencia, llaveFragmento,
            llaveArmaClase, llaveArmaReqCombate, llaveArmaDanioBase, llaveArmaMitica;

    @Inject
    public ItemManager(NexoItems plugin, FileManager fileManager) {
        this.plugin = plugin;
        this.fileManager = fileManager;

        final String p = "nexo_";
        this.llaveNivelMejora = new NamespacedKey(plugin, p + "upgrade");
        this.llaveMaterialMejora = new NamespacedKey(plugin, p + "material_polvo");
        this.llaveVidaExtra = new NamespacedKey(plugin, p + "vida_extra");
        this.llaveElemento = new NamespacedKey(plugin, p + "elemento");
        this.llaveSoulbound = new NamespacedKey(plugin, p + "soulbound");
        this.llaveSuerteMinera = new NamespacedKey(plugin, p + "suerte_minera");
        this.llaveVelocidadMineria = new NamespacedKey(plugin, p + "velocidad_mineria");
        this.llaveSuerteAgricola = new NamespacedKey(plugin, p + "suerte_agricola");
        this.llaveVelocidadMovimiento = new NamespacedKey(plugin, p + "velocidad_movimiento");
        this.llaveSuerteTala = new NamespacedKey(plugin, p + "suerte_tala");
        this.llaveFuerzaHacha = new NamespacedKey(plugin, p + "fuerza_hacha");
        this.llaveVelocidadPesca = new NamespacedKey(plugin, p + "velocidad_pesca");
        this.llaveCriaturaMarina = new NamespacedKey(plugin, p + "criatura_marina");
        this.llaveArmaduraId = new NamespacedKey(plugin, p + "armadura_id");
        this.llaveWeaponId = new NamespacedKey(plugin, p + "weapon_id");
        this.llaveWeaponPrestige = new NamespacedKey(plugin, p + "weapon_prestige");
        this.llaveHerramientaId = new NamespacedKey(plugin, p + "herramienta_id");
        this.llaveBloquesRotos = new NamespacedKey(plugin, p + "bloques_rotos");
        this.llaveReforja = new NamespacedKey(plugin, p + "reforja");
        this.llaveEnchantId = new NamespacedKey(plugin, p + "enchant_id");
        this.llaveEnchantNivel = new NamespacedKey(plugin, p + "enchant_nivel");
        this.llaveNivelEvolucion = new NamespacedKey(plugin, p + "nivel_evolucion");
        this.llaveEsencia = new NamespacedKey(plugin, p + "esencia");
        this.llaveFragmento = new NamespacedKey(plugin, p + "fragmento");
        this.llaveArmaClase = new NamespacedKey(plugin, p + "arma_clase");
        this.llaveArmaReqCombate = new NamespacedKey(plugin, p + "arma_req_combate");
        this.llaveArmaDanioBase = new NamespacedKey(plugin, p + "arma_danio_base");
        this.llaveArmaMitica = new NamespacedKey(plugin, p + "arma_mitica");
    }

    public void sincronizarItemAsync(ItemStack item) {
        if (item == null || item.isEmpty()) return;

        CompletableFuture.runAsync(() -> {
            item.editPersistentDataContainer(pdc -> {
                var weaponId = pdc.get(llaveWeaponId, PersistentDataType.STRING);
                var toolId = pdc.get(llaveHerramientaId, PersistentDataType.STRING);
                var nivelEvo = pdc.getOrDefault(llaveNivelEvolucion, PersistentDataType.INTEGER, 1);
                var reforja = pdc.getOrDefault(llaveReforja, PersistentDataType.STRING, "");

                org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                    if (weaponId != null) {
                        var dto = fileManager.getWeaponDTO(weaponId);
                        if (dto != null) aplicarEvolucionVisual(item, dto.nombre(), dto.danioBase(), nivelEvo, reforja);
                    } else if (toolId != null) {
                        var dto = fileManager.getToolDTO(toolId);
                        if (dto != null) aplicarEvolucionVisual(item, dto.nombre(), 0, nivelEvo, reforja);
                    }
                });
            });
        }, Executors.newVirtualThreadPerTaskExecutor());
    }

    private void aplicarEvolucionVisual(ItemStack item, String nombreBase, double danioBase, int nivel, String reforja) {
        String prefijoReforja = reforja.isEmpty() ? "" : "&#ff00ff" + reforja + " ";
        String nombreFinal = prefijoReforja + nombreBase + " &#E6CCFF[Nv. " + nivel + "]";

        item.editMeta(meta -> {
            meta.displayName(CrossplayUtils.parseCrossplay(null, nombreFinal));

            if (danioBase > 0) {
                meta.removeAttributeModifier(Attribute.ATTACK_DAMAGE);
                var mod = new AttributeModifier(llaveWeaponId, danioBase, Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND);
                meta.addAttributeModifier(Attribute.ATTACK_DAMAGE, mod);
            }

            List<String> nuevoLore = new ArrayList<>();
            nuevoLore.add(CrossplayUtils.getChat(null, "&#E6CCFFEstadísticas de Evolución:"));
            nuevoLore.add(CrossplayUtils.getChat(null, "&#E6CCFFNivel Cénit: &#ff00ff" + nivel + "&#E6CCFF/60"));

            if (meta.hasLore()) {
                for (String linea : meta.getLore()) {
                    if (linea.contains("✦") || linea.contains("Nivel Cénit")) continue;
                    nuevoLore.add(linea);
                }
            }
            meta.setLore(nuevoLore);
        });
    }

    public ItemStack generarArmaRPG(String id_yml) {
        var dto = fileManager.getWeaponDTO(id_yml);
        if (dto == null) {
            return new ItemStack(Material.IRON_SWORD);
        }

        String matString = fileManager.getArmas().getString("armas_rpg." + id_yml + ".material", "IRON_SWORD");
        Material mat = Material.matchMaterial(matString);
        ItemStack item = new ItemStack(mat != null ? mat : Material.IRON_SWORD);

        item.editMeta(meta -> {
            meta.displayName(CrossplayUtils.parseCrossplay(null, dto.nombre() + " &#E6CCFF[&#ff00ff+0&#E6CCFF]"));

            List<String> lore = new ArrayList<>();
            lore.add(CrossplayUtils.getChat(null, "&#E6CCFFClase: &#ff00ff" + dto.claseRequerida()));
            lore.add(CrossplayUtils.getChat(null, "&#E6CCFFElemento: " + dto.elemento()));
            lore.add(" ");
            lore.add(CrossplayUtils.getChat(null, "&#E6CCFFDaño Base: &#8b0000" + dto.danioBase() + " ⚔"));
            lore.add(CrossplayUtils.getChat(null, "&#E6CCFFVelocidad: &#ff00ff" + dto.velocidadAtaque() + " ⚡"));
            lore.add(" ");
            if (!dto.habilidadId().equalsIgnoreCase("ninguna")) {
                lore.add(CrossplayUtils.getChat(null, "&#ff00ff✦ Habilidad: &#E6CCFF" + dto.habilidadId().toUpperCase() + " &#ff00ff<bold>(CLIC DERECHO)</bold>"));
                lore.add(" ");
            }
            lore.add(CrossplayUtils.getChat(null, "&#E6CCFFRequisito de Combate: Nivel " + dto.nivelRequerido()));
            meta.setLore(lore);

            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

            var pdc = meta.getPersistentDataContainer();
            pdc.set(llaveWeaponId, PersistentDataType.STRING, dto.id());
            pdc.set(llaveWeaponPrestige, PersistentDataType.INTEGER, 0);
            pdc.set(llaveNivelEvolucion, PersistentDataType.INTEGER, 1);

            NamespacedKey dmgKey = new NamespacedKey(plugin, "nexo_dmg_" + dto.id());
            var dmgMod = new AttributeModifier(dmgKey, dto.danioBase(), Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND);
            meta.addAttributeModifier(Attribute.ATTACK_DAMAGE, dmgMod);

            NamespacedKey spdKey = new NamespacedKey(plugin, "nexo_spd_" + dto.id());
            double speedOffset = dto.velocidadAtaque() - 4.0;
            var spdMod = new AttributeModifier(spdKey, speedOffset, Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND);
            meta.addAttributeModifier(Attribute.ATTACK_SPEED, spdMod);
        });

        sincronizarItemAsync(item);
        return item;
    }

    public ItemStack generarHerramientaProfesion(String id_yml) {
        var dto = fileManager.getToolDTO(id_yml);
        if (dto == null) return new ItemStack(Material.IRON_PICKAXE);

        String nexoId = fileManager.getHerramientas().getString("herramientas." + id_yml + ".nexo_id");
        ItemStack item;

        try {
            if (nexoId != null && com.nexomc.nexo.api.NexoItems.itemFromId(nexoId) != null) {
                item = com.nexomc.nexo.api.NexoItems.itemFromId(nexoId).build();
            } else {
                String matString = fileManager.getHerramientas().getString("herramientas." + id_yml + ".material", "IRON_PICKAXE");
                Material mat = Material.matchMaterial(matString);
                item = new ItemStack(mat != null ? mat : Material.IRON_PICKAXE);
            }
        } catch (NoClassDefFoundError | Exception e) {
            item = new ItemStack(Material.IRON_PICKAXE);
        }

        item.editMeta(meta -> {
            meta.displayName(CrossplayUtils.parseCrossplay(null, dto.nombre()));

            List<String> lore = new ArrayList<>();
            lore.add(CrossplayUtils.getChat(null, dto.rareza()));
            lore.add(CrossplayUtils.getChat(null, "&#E6CCFFProfesión: &#ff00ff" + dto.profesion()));
            lore.add(CrossplayUtils.getChat(null, "&#E6CCFFTier: &#ff00ff" + dto.tier()));
            lore.add(" ");
            lore.add(CrossplayUtils.getChat(null, "&#E6CCFFVelocidad Base: &#00f5ff+" + dto.velocidadBase()));
            lore.add(CrossplayUtils.getChat(null, "&#E6CCFFBonus Drops: &#00f5ff+" + dto.multiplicadorFortuna() + "%"));
            lore.add(" ");
            lore.add(CrossplayUtils.getChat(null, "&#E6CCFFBloques Rotos: &#ff00ff0"));
            lore.add(" ");
            lore.add(CrossplayUtils.getChat(null, "&#E6CCFFRequisito de " + dto.profesion() + ": Nivel " + dto.nivelRequerido()));
            meta.setLore(lore);

            meta.setUnbreakable(true);

            var pdc = meta.getPersistentDataContainer();
            pdc.set(llaveHerramientaId, PersistentDataType.STRING, dto.id());
            pdc.set(llaveBloquesRotos, PersistentDataType.INTEGER, 0);
            pdc.set(llaveNivelEvolucion, PersistentDataType.INTEGER, 1);
        });

        sincronizarItemAsync(item);
        return item;
    }

    public ItemStack aplicarReforja(ItemStack item, String idReforja) {
        if (item == null || item.isEmpty()) return item;
        var reforge = fileManager.getReforgeDTO(idReforja);
        if (reforge == null) return item;

        item.editPersistentDataContainer(pdc -> pdc.set(llaveReforja, PersistentDataType.STRING, reforge.id()));
        sincronizarItemAsync(item);

        return item;
    }

    public ItemStack aplicarEncantamiento(ItemStack item, String idEnchant, int nivel) {
        if (item == null || item.isEmpty()) return item;
        var enchant = fileManager.getEnchantDTO(idEnchant);
        if (enchant == null) return item;

        item.editMeta(meta -> {
            NamespacedKey keyEnchant = new NamespacedKey(plugin, "nexo_enchant_" + idEnchant);
            meta.getPersistentDataContainer().set(keyEnchant, PersistentDataType.INTEGER, nivel);

            String nombreRomanos = switch (nivel) {
                case 2 -> "II";
                case 3 -> "III";
                case 4 -> "IV";
                case 5 -> "V";
                default -> "I";
            };

            String lineaEncantamiento = CrossplayUtils.getChat(null, enchant.nombre() + " " + nombreRomanos);
            List<String> lore = meta.getLore();
            if (lore == null) lore = new ArrayList<>();
            lore.add(lineaEncantamiento);
            meta.setLore(lore);
        });

        return item;
    }
}