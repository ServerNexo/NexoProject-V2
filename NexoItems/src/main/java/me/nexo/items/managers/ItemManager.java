package me.nexo.items.managers;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.items.NexoItems;
import me.nexo.items.dtos.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ⚔️ NexoItems - Gestor de Ítems (Arquitectura Enterprise Java 21)
 * Rendimiento: Singleton Puro, Data Components O(1), List<Component> Nativas e Hilos Virtuales.
 */
@Singleton
public class ItemManager {

    // 🌟 DEPENDENCIAS PROPAGADAS
    private final NexoItems plugin;
    private final FileManager fileManager;
    private final CrossplayUtils crossplayUtils;

    // 🚀 MOTOR VIRTUAL (Instanciado con el Singleton, no de forma global)
    private final ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();

    // 🌟 LLAVES INMUTABLES (Sin prefijos mágicos en memoria)
    public final NamespacedKey llaveNivelMejora, llaveMaterialMejora, llaveVidaExtra, llaveElemento, llaveSoulbound,
            llaveSuerteMinera, llaveVelocidadMineria, llaveSuerteAgricola, llaveVelocidadMovimiento,
            llaveSuerteTala, llaveFuerzaHacha, llaveVelocidadPesca, llaveCriaturaMarina,
            llaveArmaduraId, llaveWeaponId, llaveWeaponPrestige, llaveHerramientaId,
            llaveBloquesRotos, llaveReforja, llaveEnchantId, llaveEnchantNivel,
            llaveNivelEvolucion, llaveEsencia, llaveFragmento,
            llaveArmaClase, llaveArmaReqCombate, llaveArmaDanioBase, llaveArmaMitica;

    // 💉 PILAR 1: Inyección Estricta
    @Inject
    public ItemManager(NexoItems plugin, FileManager fileManager, CrossplayUtils crossplayUtils) {
        this.plugin = plugin;
        this.fileManager = fileManager;
        this.crossplayUtils = crossplayUtils;

        this.llaveNivelMejora = new NamespacedKey(plugin, "nexo_upgrade");
        this.llaveMaterialMejora = new NamespacedKey(plugin, "nexo_material_polvo");
        this.llaveVidaExtra = new NamespacedKey(plugin, "nexo_vida_extra");
        this.llaveElemento = new NamespacedKey(plugin, "nexo_elemento");
        this.llaveSoulbound = new NamespacedKey(plugin, "nexo_soulbound");
        this.llaveSuerteMinera = new NamespacedKey(plugin, "nexo_suerte_minera");
        this.llaveVelocidadMineria = new NamespacedKey(plugin, "nexo_velocidad_mineria");
        this.llaveSuerteAgricola = new NamespacedKey(plugin, "nexo_suerte_agricola");
        this.llaveVelocidadMovimiento = new NamespacedKey(plugin, "nexo_velocidad_movimiento");
        this.llaveSuerteTala = new NamespacedKey(plugin, "nexo_suerte_tala");
        this.llaveFuerzaHacha = new NamespacedKey(plugin, "nexo_fuerza_hacha");
        this.llaveVelocidadPesca = new NamespacedKey(plugin, "nexo_velocidad_pesca");
        this.llaveCriaturaMarina = new NamespacedKey(plugin, "nexo_criatura_marina");
        this.llaveArmaduraId = new NamespacedKey(plugin, "nexo_armadura_id");
        this.llaveWeaponId = new NamespacedKey(plugin, "nexo_weapon_id");
        this.llaveWeaponPrestige = new NamespacedKey(plugin, "nexo_weapon_prestige");
        this.llaveHerramientaId = new NamespacedKey(plugin, "nexo_herramienta_id");
        this.llaveBloquesRotos = new NamespacedKey(plugin, "nexo_bloques_rotos");
        this.llaveReforja = new NamespacedKey(plugin, "nexo_reforja");
        this.llaveEnchantId = new NamespacedKey(plugin, "nexo_enchant_id");
        this.llaveEnchantNivel = new NamespacedKey(plugin, "nexo_enchant_nivel");
        this.llaveNivelEvolucion = new NamespacedKey(plugin, "nexo_nivel_evolucion");
        this.llaveEsencia = new NamespacedKey(plugin, "nexo_esencia");
        this.llaveFragmento = new NamespacedKey(plugin, "nexo_fragmento");
        this.llaveArmaClase = new NamespacedKey(plugin, "nexo_arma_clase");
        this.llaveArmaReqCombate = new NamespacedKey(plugin, "nexo_arma_req_combate");
        this.llaveArmaDanioBase = new NamespacedKey(plugin, "nexo_arma_danio_base");
        this.llaveArmaMitica = new NamespacedKey(plugin, "nexo_arma_mitica");
    }

    public CompletableFuture<ItemStack> sincronizarItemAsync(ItemStack item) {
        if (item == null || item.isEmpty()) return CompletableFuture.completedFuture(item);

        return CompletableFuture.supplyAsync(() -> {
            var clon = item.clone();

            clon.editPersistentDataContainer(pdc -> {
                var weaponId = pdc.get(llaveWeaponId, PersistentDataType.STRING);
                var toolId = pdc.get(llaveHerramientaId, PersistentDataType.STRING);
                var nivelEvo = pdc.getOrDefault(llaveNivelEvolucion, PersistentDataType.INTEGER, 1);
                var reforja = pdc.getOrDefault(llaveReforja, PersistentDataType.STRING, "");

                if (weaponId != null) {
                    var dto = fileManager.getWeaponDTO(weaponId);
                    if (dto != null) aplicarEvolucionVisual(clon, dto.nombre(), dto.danioBase(), nivelEvo, reforja);
                } else if (toolId != null) {
                    var dto = fileManager.getToolDTO(toolId);
                    if (dto != null) aplicarEvolucionVisual(clon, dto.nombre(), 0, nivelEvo, reforja);
                }
            });

            return clon;
        }, virtualExecutor);
    }

    private void aplicarEvolucionVisual(ItemStack item, String nombreBase, double danioBase, int nivel, String reforja) {
        String prefijoReforja = reforja.isEmpty() ? "" : "&#ff00ff" + reforja + " ";
        String nombreFinal = prefijoReforja + nombreBase + " &#E6CCFF[Nv. " + nivel + "]";

        item.editMeta(meta -> {
            meta.displayName(crossplayUtils.parseCrossplay(null, nombreFinal));

            if (danioBase > 0) {
                meta.removeAttributeModifier(Attribute.ATTACK_DAMAGE);
                var mod = new AttributeModifier(llaveWeaponId, danioBase, Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND);
                meta.addAttributeModifier(Attribute.ATTACK_DAMAGE, mod);
            }

            // 🌟 FIX: List<Component>
            List<Component> nuevoLore = new ArrayList<>();
            nuevoLore.add(crossplayUtils.parseCrossplay(null, "&#E6CCFFEstadísticas de Evolución:"));
            nuevoLore.add(crossplayUtils.parseCrossplay(null, "&#E6CCFFNivel Cénit: &#ff00ff" + nivel + "&#E6CCFF/60"));

            if (meta.hasLore()) {
                for (var component : meta.lore()) {
                    String linea = PlainTextComponentSerializer.plainText().serialize(component);
                    if (linea.contains("✦") || linea.contains("Nivel Cénit")) continue;

                    // Re-parseamos la linea plana usando Kyori
                    nuevoLore.add(crossplayUtils.parseCrossplay(null, linea));
                }
            }
            // 🌟 FIX: Asignación directa, sin map ni streams extras
            meta.lore(nuevoLore);
        });
    }

    public ItemStack generarArmaRPG(String id_yml) {
        var dto = fileManager.getWeaponDTO(id_yml);
        if (dto == null) {
            plugin.getLogger().warning("¡No se encontró el arma " + id_yml + " en caché!");
            return new ItemStack(Material.WOODEN_SWORD);
        }

        String matString = fileManager.getArmas().getString("armas_rpg." + id_yml + ".material", "IRON_SWORD");
        Material mat = Material.matchMaterial(matString);
        var item = new ItemStack(mat != null ? mat : Material.IRON_SWORD);

        item.editMeta(meta -> {
            meta.displayName(crossplayUtils.parseCrossplay(null, dto.nombre() + " &#E6CCFF[&#ff00ff+0&#E6CCFF]"));

            meta.lore(List.of(
                    crossplayUtils.parseCrossplay(null, "&#E6CCFFClase: &#ff00ff" + dto.claseRequerida()),
                    crossplayUtils.parseCrossplay(null, "&#E6CCFFElemento: " + dto.elemento()),
                    Component.empty(),
                    crossplayUtils.parseCrossplay(null, "&#E6CCFFDaño Base: &#8b0000" + dto.danioBase() + " ⚔"),
                    crossplayUtils.parseCrossplay(null, "&#E6CCFFVelocidad: &#ff00ff" + dto.velocidadAtaque() + " ⚡"),
                    Component.empty(),
                    (!dto.habilidadId().equalsIgnoreCase("ninguna") ? crossplayUtils.parseCrossplay(null, "&#ff00ff✦ Habilidad: &#E6CCFF" + dto.habilidadId().toUpperCase() + " &#ff00ff<bold>(CLIC DERECHO)</bold>") : Component.empty()),
                    (!dto.habilidadId().equalsIgnoreCase("ninguna") ? Component.empty() : Component.empty()),
                    crossplayUtils.parseCrossplay(null, "&#E6CCFFRequisito de Combate: Nivel " + dto.nivelRequerido())
            ));

            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

            var pdc = meta.getPersistentDataContainer();
            pdc.set(llaveWeaponId, PersistentDataType.STRING, dto.id());
            pdc.set(llaveWeaponPrestige, PersistentDataType.INTEGER, 0);
            pdc.set(llaveNivelEvolucion, PersistentDataType.INTEGER, 1);

            var dmgKey = new NamespacedKey(plugin, "nexo_dmg_" + dto.id());
            var dmgMod = new AttributeModifier(dmgKey, dto.danioBase(), Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND);
            meta.addAttributeModifier(Attribute.ATTACK_DAMAGE, dmgMod);

            var spdKey = new NamespacedKey(plugin, "nexo_spd_" + dto.id());
            double speedOffset = dto.velocidadAtaque() - 4.0;
            var spdMod = new AttributeModifier(spdKey, speedOffset, Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND);
            meta.addAttributeModifier(Attribute.ATTACK_SPEED, spdMod);
        });

        return sincronizarItemAsync(item).join();
    }

    public ItemStack generarHerramientaProfesion(String id_yml) {
        var dto = fileManager.getToolDTO(id_yml);
        if (dto == null) {
            plugin.getLogger().warning("¡No se encontró la herramienta " + id_yml + " en caché!");
            return new ItemStack(Material.WOODEN_PICKAXE);
        }

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
        } catch (NoClassDefFoundError e) {
            String matString = fileManager.getHerramientas().getString("herramientas." + id_yml + ".material", "IRON_PICKAXE");
            Material mat = Material.matchMaterial(matString);
            item = new ItemStack(mat != null ? mat : Material.IRON_PICKAXE);
        }

        item.editMeta(meta -> {
            meta.displayName(crossplayUtils.parseCrossplay(null, dto.nombre()));

            meta.lore(List.of(
                    crossplayUtils.parseCrossplay(null, dto.rareza()),
                    crossplayUtils.parseCrossplay(null, "&#E6CCFFProfesión: &#ff00ff" + dto.profesion()),
                    crossplayUtils.parseCrossplay(null, "&#E6CCFFTier: &#ff00ff" + dto.tier()),
                    Component.empty(),
                    crossplayUtils.parseCrossplay(null, "&#E6CCFFVelocidad Base: &#00f5ff+" + dto.velocidadBase()),
                    crossplayUtils.parseCrossplay(null, "&#E6CCFFBonus Drops: &#00f5ff+" + dto.multiplicadorFortuna() + "%"),
                    Component.empty(),
                    crossplayUtils.parseCrossplay(null, "&#E6CCFFBloques Rotos: &#ff00ff0"),
                    Component.empty(),
                    crossplayUtils.parseCrossplay(null, "&#E6CCFFRequisito de " + dto.profesion() + ": Nivel " + dto.nivelRequerido())
            ));

            meta.setUnbreakable(true);

            var pdc = meta.getPersistentDataContainer();
            pdc.set(llaveHerramientaId, PersistentDataType.STRING, dto.id());
            pdc.set(llaveBloquesRotos, PersistentDataType.INTEGER, 0);
            pdc.set(llaveNivelEvolucion, PersistentDataType.INTEGER, 1);

            if (dto.esTaladro()) {
                var tool = meta.getTool();
                tool.addRule(org.bukkit.Tag.MINEABLE_SHOVEL, (float) dto.velocidadBase(), true);
                tool.addRule(org.bukkit.Tag.MINEABLE_PICKAXE, (float) dto.velocidadBase(), true);
                meta.setTool(tool);
            }
        });

        return sincronizarItemAsync(item).join();
    }

    public ItemStack generarArmaduraProfesion(String id_yml, String tipoPieza) {
        var dto = fileManager.getArmorDTO(id_yml);
        if (dto == null) {
            plugin.getLogger().warning("¡No se encontró la armadura " + id_yml + " en caché!");
            return new ItemStack(Material.STONE);
        }

        String matString = fileManager.getArmaduras().getString("armaduras_profesion." + id_yml + ".material", "LEATHER_CHESTPLATE");
        String prefijoMat = matString.contains("_") ? matString.split("_")[0] : matString;
        Material mat;
        try {
            mat = Material.valueOf(prefijoMat + "_" + tipoPieza.toUpperCase());
        } catch (Exception e) {
            mat = Material.LEATHER_CHESTPLATE;
        }

        var item = new ItemStack(mat);

        String etiquetaPieza = switch (tipoPieza.toUpperCase()) {
            case "HELMET" -> " &#E6CCFF(Casco)";
            case "CHESTPLATE" -> " &#E6CCFF(Peto)";
            case "LEGGINGS" -> " &#E6CCFF(Pantalones)";
            case "BOOTS" -> " &#E6CCFF(Botas)";
            default -> "";
        };

        item.editMeta(meta -> {
            meta.displayName(crossplayUtils.parseCrossplay(null, dto.nombre() + etiquetaPieza));

            // 🌟 FIX: List<Component>
            List<Component> lore = new ArrayList<>();
            lore.add(crossplayUtils.parseCrossplay(null, "&#E6CCFFClase: &#ff00ff" + dto.claseRequerida()));
            lore.add(crossplayUtils.parseCrossplay(null, " "));
            if (dto.vidaExtra() > 0) lore.add(crossplayUtils.parseCrossplay(null, "&#E6CCFFVida Extra: &#8b0000+" + dto.vidaExtra() + " ❤"));
            if (dto.velocidadMovimiento() > 0) lore.add(crossplayUtils.parseCrossplay(null, "&#E6CCFFVelocidad: &#00f5ff+" + dto.velocidadMovimiento() + " 🍃"));
            if (dto.suerteMinera() > 0) lore.add(crossplayUtils.parseCrossplay(null, "&#E6CCFFFortuna Minera: &#00f5ff+" + dto.suerteMinera() + "% ✨"));
            if (dto.velocidadMineria() > 0) lore.add(crossplayUtils.parseCrossplay(null, "&#E6CCFFPrisa Minera: &#ff00ff+" + dto.velocidadMineria() + " ⚡"));
            if (dto.suerteAgricola() > 0) lore.add(crossplayUtils.parseCrossplay(null, "&#E6CCFFFortuna Agrícola: &#00f5ff+" + dto.suerteAgricola() + "% 🌾"));
            if (dto.suerteTala() > 0) lore.add(crossplayUtils.parseCrossplay(null, "&#E6CCFFDoble Caída (Tala): &#00f5ff+" + dto.suerteTala() + "% 🪓"));
            if (dto.criaturaMarina() > 0) lore.add(crossplayUtils.parseCrossplay(null, "&#E6CCFFProb. Criatura Marina: &#00f5ff+" + dto.criaturaMarina() + "% 🦑"));
            if (dto.velocidadPesca() > 0) lore.add(crossplayUtils.parseCrossplay(null, "&#E6CCFFVelocidad Pesca: &#00f5ff+" + dto.velocidadPesca() + "% 🎣"));

            List<String> loreCustom = fileManager.getArmaduras().getStringList("armaduras_profesion." + id_yml + ".lore_custom");
            if (loreCustom != null && !loreCustom.isEmpty()) {
                lore.add(crossplayUtils.parseCrossplay(null, " "));
                loreCustom.forEach(linea -> lore.add(crossplayUtils.parseCrossplay(null, linea)));
            }

            lore.add(crossplayUtils.parseCrossplay(null, " "));
            lore.add(crossplayUtils.parseCrossplay(null, "&#E6CCFFRequisito de " + dto.skillRequerida() + ": Nivel " + dto.nivelRequerido()));

            // 🌟 FIX: Asignación directa
            meta.lore(lore);

            meta.setUnbreakable(true);

            var pdc = meta.getPersistentDataContainer();
            pdc.set(llaveArmaduraId, PersistentDataType.STRING, dto.id());
            if (dto.vidaExtra() > 0) pdc.set(llaveVidaExtra, PersistentDataType.DOUBLE, dto.vidaExtra());
        });

        return item;
    }

    public ItemStack crearPolvoEstelar() {
        var item = new ItemStack(Material.GLOWSTONE_DUST);
        item.editMeta(meta -> {
            meta.displayName(crossplayUtils.parseCrossplay(null, "&#ff00ff✨ Polvo Estelar"));
            meta.getPersistentDataContainer().set(llaveMaterialMejora, PersistentDataType.BYTE, (byte) 1);
        });
        return item;
    }

    public ItemStack crearHojaVacio() {
        var item = new ItemStack(Material.DIAMOND_SWORD);
        item.editMeta(meta -> {
            meta.displayName(crossplayUtils.parseCrossplay(null, "&#ff00ff🌌 Hoja del Vacío"));
            meta.lore(List.of(
                    crossplayUtils.parseCrossplay(null, "&#E6CCFFArtefacto de Utilidad"),
                    Component.empty(),
                    crossplayUtils.parseCrossplay(null, "&#ff00ffHabilidad: Transmisión Instantánea <bold>(CLIC DERECHO)</bold>"),
                    crossplayUtils.parseCrossplay(null, "&#E6CCFFCosto: &#ff00ff40 Energía ⚡"),
                    crossplayUtils.parseCrossplay(null, "&#8b0000🔒 Ligado al Alma")
            ));
            meta.setUnbreakable(true);
            meta.getPersistentDataContainer().set(llaveSoulbound, PersistentDataType.BYTE, (byte) 1);
        });
        return item;
    }

    public ItemStack aplicarReforja(ItemStack item, String idReforja) {
        if (item == null || item.isEmpty()) return item;
        var reforge = fileManager.getReforgeDTO(idReforja);
        if (reforge == null) return item;

        var pdcItem = item.getPersistentDataContainer();
        var esArma = pdcItem.has(llaveWeaponId, PersistentDataType.STRING);
        var esHerramienta = pdcItem.has(llaveHerramientaId, PersistentDataType.STRING);

        if (!esArma && !esHerramienta) return item;

        String claseOriginal = "Cualquiera";
        if (esArma) {
            String idBase = pdcItem.get(llaveWeaponId, PersistentDataType.STRING);
            var arma = fileManager.getWeaponDTO(idBase);
            if (arma == null) return item;
            claseOriginal = arma.claseRequerida();
        } else {
            String idBase = pdcItem.get(llaveHerramientaId, PersistentDataType.STRING);
            var tool = fileManager.getToolDTO(idBase);
            if (tool == null) return item;
            claseOriginal = tool.profesion();
        }

        if (!reforge.aplicaAClase(claseOriginal) && !reforge.aplicaAClase("Cualquiera")) return item;

        item.editPersistentDataContainer(pdc -> pdc.set(llaveReforja, PersistentDataType.STRING, reforge.id()));
        return sincronizarItemAsync(item).join();
    }

    public ItemStack generarLibroEncantamiento(String idEnchant, int nivel) {
        var dto = fileManager.getEnchantDTO(idEnchant);
        if (dto == null) {
            plugin.getLogger().warning("¡No se encontró el encantamiento " + idEnchant + " en la caché!");
            return new ItemStack(Material.BOOK);
        }

        int nivelReal = Math.min(nivel, dto.nivelMaximo());
        var libro = new ItemStack(Material.ENCHANTED_BOOK);

        String nombreRomanos = switch (nivelReal) {
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> "I";
        };

        libro.editMeta(meta -> {
            meta.displayName(crossplayUtils.parseCrossplay(null, dto.nombre() + " " + nombreRomanos));

            double valorActual = dto.getValorPorNivel(nivelReal);
            String descReemplazada = dto.descripcion().replace("{val}", String.valueOf(valorActual));

            meta.lore(List.of(
                    crossplayUtils.parseCrossplay(null, "&#E6CCFFLibro de Encantamiento Mágico"),
                    Component.empty(),
                    crossplayUtils.parseCrossplay(null, descReemplazada),
                    Component.empty(),
                    crossplayUtils.parseCrossplay(null, "&#E6CCFFAplica a: " + String.join(", ", dto.aplicaA())),
                    crossplayUtils.parseCrossplay(null, "&#ff00ffLlévalo a un Yunque Mágico para aplicarlo.")
            ));

            var pdc = meta.getPersistentDataContainer();
            pdc.set(llaveEnchantId, PersistentDataType.STRING, dto.id());
            pdc.set(llaveEnchantNivel, PersistentDataType.INTEGER, nivelReal);
        });

        return libro;
    }

    public ItemStack aplicarEncantamiento(ItemStack item, String idEnchant, int nivel) {
        if (item == null || item.isEmpty()) return item;
        var enchant = fileManager.getEnchantDTO(idEnchant);
        if (enchant == null) return item;

        item.editMeta(meta -> {
            var keyEnchant = new NamespacedKey(plugin, "nexo_enchant_" + idEnchant);
            meta.getPersistentDataContainer().set(keyEnchant, PersistentDataType.INTEGER, nivel);

            String nombreRomanos = switch (nivel) {
                case 2 -> "II";
                case 3 -> "III";
                case 4 -> "IV";
                case 5 -> "V";
                default -> "I";
            };

            Component lineaEncantamiento = crossplayUtils.parseCrossplay(null, enchant.nombre() + " " + nombreRomanos);
            String nombrePuro = PlainTextComponentSerializer.plainText().serialize(crossplayUtils.parseCrossplay(null, enchant.nombre()));

            // 🌟 FIX: List<Component> pura
            List<Component> nuevoLore = new ArrayList<>();
            boolean encontrado = false;

            if (meta.hasLore()) {
                for (var component : meta.lore()) {
                    String lineaPlana = PlainTextComponentSerializer.plainText().serialize(component);
                    if (lineaPlana.startsWith(nombrePuro)) {
                        nuevoLore.add(lineaEncantamiento);
                        encontrado = true;
                    } else {
                        nuevoLore.add(component); // 🌟 PRESERVA EL COMPONENTE ORIGINAL INTACTO
                    }
                }
            }
            if (!encontrado) nuevoLore.add(lineaEncantamiento);

            meta.lore(nuevoLore);
        });

        return item;
    }

    public ItemStack generarArmadura(String id) {
        return generarArmaduraProfesion(id, "CHESTPLATE");
    }

    public ItemStack generarHerramienta(String id) {
        return generarHerramientaProfesion(id);
    }
}