package me.nexo.items.managers;

import io.papermc.paper.datacomponent.DataComponentTypes;
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
 * ⚔️ NexoItems - Gestor de Ítems (Arquitectura Enterprise Java 25)
 * Paper 26.1+ | Data Components Nativos O(1) | Sin bloqueos en Main Thread
 */
public class ItemManager {

    // 🌟 Agrupamos las llaves por categoría para mejor lectura
    public static NamespacedKey
            llaveNivelMejora, llaveMaterialMejora, llaveVidaExtra, llaveElemento, llaveSoulbound,
            llaveSuerteMinera, llaveVelocidadMineria, llaveSuerteAgricola, llaveVelocidadMovimiento,
            llaveSuerteTala, llaveFuerzaHacha, llaveVelocidadPesca, llaveCriaturaMarina,
            llaveArmaduraId, llaveWeaponId, llaveWeaponPrestige, llaveHerramientaId,
            llaveBloquesRotos, llaveReforja, llaveEnchantId, llaveEnchantNivel,
            llaveNivelEvolucion, llaveEsencia, llaveFragmento,
            llaveArmaClase, llaveArmaReqCombate, llaveArmaDanioBase, llaveArmaMitica;

    public static NexoItems pluginMemoria;

    public static void init(NexoItems plugin) {
        pluginMemoria = plugin;

        // 💡 En Java moderno, podemos extraer el prefijo para ahorrar String overhead en memoria
        final String p = "nexo_";
        llaveNivelMejora = new NamespacedKey(plugin, p + "upgrade");
        llaveMaterialMejora = new NamespacedKey(plugin, p + "material_polvo");
        llaveVidaExtra = new NamespacedKey(plugin, p + "vida_extra");
        llaveElemento = new NamespacedKey(plugin, p + "elemento");
        llaveSoulbound = new NamespacedKey(plugin, p + "soulbound");
        llaveSuerteMinera = new NamespacedKey(plugin, p + "suerte_minera");
        llaveVelocidadMineria = new NamespacedKey(plugin, p + "velocidad_mineria");
        llaveSuerteAgricola = new NamespacedKey(plugin, p + "suerte_agricola");
        llaveVelocidadMovimiento = new NamespacedKey(plugin, p + "velocidad_movimiento");
        llaveSuerteTala = new NamespacedKey(plugin, p + "suerte_tala");
        llaveFuerzaHacha = new NamespacedKey(plugin, p + "fuerza_hacha");
        llaveVelocidadPesca = new NamespacedKey(plugin, p + "velocidad_pesca");
        llaveCriaturaMarina = new NamespacedKey(plugin, p + "criatura_marina");
        llaveArmaduraId = new NamespacedKey(plugin, p + "armadura_id");
        llaveWeaponId = new NamespacedKey(plugin, p + "weapon_id");
        llaveWeaponPrestige = new NamespacedKey(plugin, p + "weapon_prestige");
        llaveHerramientaId = new NamespacedKey(plugin, p + "herramienta_id");
        llaveBloquesRotos = new NamespacedKey(plugin, p + "bloques_rotos");
        llaveReforja = new NamespacedKey(plugin, p + "reforja");
        llaveEnchantId = new NamespacedKey(plugin, p + "enchant_id");
        llaveEnchantNivel = new NamespacedKey(plugin, p + "enchant_nivel");
        llaveNivelEvolucion = new NamespacedKey(plugin, p + "nivel_evolucion");
        llaveEsencia = new NamespacedKey(plugin, p + "esencia");
        llaveFragmento = new NamespacedKey(plugin, p + "fragmento");
        llaveArmaClase = new NamespacedKey(plugin, p + "arma_clase");
        llaveArmaReqCombate = new NamespacedKey(plugin, p + "arma_req_combate");
        llaveArmaDanioBase = new NamespacedKey(plugin, p + "arma_danio_base");
        llaveArmaMitica = new NamespacedKey(plugin, p + "arma_mitica");
    }

    public static void sincronizarItemAsync(ItemStack item) {
        if (item == null || item.isEmpty()) return; // 🌟 isEmpty() es el estándar 1.21 en vez de != null (Data Components)

        CompletableFuture.runAsync(() -> {
            // 🌟 O(1) LECTURA: Leemos directamente el Custom Data Component sin parsear el Meta entero
            item.editPersistentDataContainer(pdc -> {
                var weaponId = pdc.get(llaveWeaponId, PersistentDataType.STRING);
                var toolId = pdc.get(llaveHerramientaId, PersistentDataType.STRING);
                var nivelEvo = pdc.getOrDefault(llaveNivelEvolucion, PersistentDataType.INTEGER, 1);
                var reforja = pdc.getOrDefault(llaveReforja, PersistentDataType.STRING, "");

                org.bukkit.Bukkit.getScheduler().runTask(pluginMemoria, () -> {
                    if (weaponId != null) {
                        var dto = pluginMemoria.getFileManager().getWeaponDTO(weaponId);
                        if (dto != null) aplicarEvolucionVisual(item, dto.nombre(), dto.danioBase(), nivelEvo, reforja);
                    } else if (toolId != null) {
                        var dto = pluginMemoria.getFileManager().getToolDTO(toolId);
                        if (dto != null) aplicarEvolucionVisual(item, dto.nombre(), 0, nivelEvo, reforja);
                    }
                });
            });
        }, Executors.newVirtualThreadPerTaskExecutor());
    }

    private static void aplicarEvolucionVisual(ItemStack item, String nombreBase, double danioBase, int nivel, String reforja) {
        String prefijoReforja = reforja.isEmpty() ? "" : "&#ff00ff" + reforja + " ";
        String nombreFinal = prefijoReforja + nombreBase + " &#E6CCFF[Nv. " + nivel + "]";

        // 🌟 EDICIÓN MUTABLE O(1): No extraemos el Meta, lo editamos in-place usando Lambdas
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

    public static ItemStack generarArmaRPG(String id_yml) {
        var dto = pluginMemoria.getFileManager().getWeaponDTO(id_yml); // 🌟 Uso de var para código limpio (Java 21+)
        if (dto == null) {
            org.bukkit.Bukkit.getLogger().warning("¡No se encontró el arma " + id_yml + " en caché!");
            return new ItemStack(Material.WOODEN_SWORD);
        }

        String matString = pluginMemoria.getFileManager().getArmas().getString("armas_rpg." + id_yml + ".material", "IRON_SWORD");
        Material mat = Material.matchMaterial(matString);
        ItemStack item = new ItemStack(mat != null ? mat : Material.IRON_SWORD);

        // 🌟 EDICIÓN IN-PLACE (Data Components):
        // En Paper 1.21.4, hacer esto evita la creación de un mapa NBT pesado en el servidor.
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

            // Paper Data Components nativos
            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

            // Escribimos directo al Custom Data Component (PersistentDataContainer)
            var pdc = meta.getPersistentDataContainer();
            pdc.set(llaveWeaponId, PersistentDataType.STRING, dto.id());
            pdc.set(llaveWeaponPrestige, PersistentDataType.INTEGER, 0);
            pdc.set(llaveNivelEvolucion, PersistentDataType.INTEGER, 1);

            // Registro de Atributos Modernos con llave exacta
            NamespacedKey dmgKey = new NamespacedKey(pluginMemoria, "nexo_dmg_" + dto.id());
            var dmgMod = new AttributeModifier(dmgKey, dto.danioBase(), Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND);
            meta.addAttributeModifier(Attribute.ATTACK_DAMAGE, dmgMod);

            NamespacedKey spdKey = new NamespacedKey(pluginMemoria, "nexo_spd_" + dto.id());
            double speedOffset = dto.velocidadAtaque() - 4.0;
            var spdMod = new AttributeModifier(spdKey, speedOffset, Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND);
            meta.addAttributeModifier(Attribute.ATTACK_SPEED, spdMod);
        });

        sincronizarItemAsync(item);
        return item;
    }

    public static ItemStack generarHerramientaProfesion(String id_yml) {
        var dto = pluginMemoria.getFileManager().getToolDTO(id_yml);
        if (dto == null) {
            org.bukkit.Bukkit.getLogger().warning("¡No se encontró la herramienta " + id_yml + " en caché!");
            return new ItemStack(Material.WOODEN_PICKAXE);
        }

        String nexoId = pluginMemoria.getFileManager().getHerramientas().getString("herramientas." + id_yml + ".nexo_id");
        ItemStack item;

        try {
            if (nexoId != null && com.nexomc.nexo.api.NexoItems.itemFromId(nexoId) != null) {
                item = com.nexomc.nexo.api.NexoItems.itemFromId(nexoId).build();
            } else {
                String matString = pluginMemoria.getFileManager().getHerramientas().getString("herramientas." + id_yml + ".material", "IRON_PICKAXE");
                Material mat = Material.matchMaterial(matString);
                item = new ItemStack(mat != null ? mat : Material.IRON_PICKAXE);
            }
        } catch (NoClassDefFoundError e) {
            String matString = pluginMemoria.getFileManager().getHerramientas().getString("herramientas." + id_yml + ".material", "IRON_PICKAXE");
            Material mat = Material.matchMaterial(matString);
            item = new ItemStack(mat != null ? mat : Material.IRON_PICKAXE);
        }

        // 🌟 EDICIÓN IN-PLACE: Nada de sacar el meta, lo alteramos nativamente
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

            if (dto.esTaladro()) {
                var tool = meta.getTool();
                tool.addRule(org.bukkit.Tag.MINEABLE_SHOVEL, (float) dto.velocidadBase(), true);
                tool.addRule(org.bukkit.Tag.MINEABLE_PICKAXE, (float) dto.velocidadBase(), true);
                meta.setTool(tool);
            }
        });

        sincronizarItemAsync(item);
        return item;
    }

    public static ItemStack generarArmaduraProfesion(String id_yml, String tipoPieza) {
        var dto = pluginMemoria.getFileManager().getArmorDTO(id_yml);
        if (dto == null) {
            org.bukkit.Bukkit.getLogger().warning("¡No se encontró la armadura " + id_yml + " en caché!");
            return new ItemStack(Material.STONE);
        }

        String matString = pluginMemoria.getFileManager().getArmaduras().getString("armaduras_profesion." + id_yml + ".material", "LEATHER_CHESTPLATE");
        String prefijoMat = matString.contains("_") ? matString.split("_")[0] : matString;
        Material mat;
        try {
            mat = Material.valueOf(prefijoMat + "_" + tipoPieza.toUpperCase());
        } catch (Exception e) {
            mat = Material.LEATHER_CHESTPLATE;
        }

        ItemStack item = new ItemStack(mat);

        // 🌟 PATTERN MATCHING SWITCH (Java 25): Adiós a los 'break;' y al código redundante
        String etiquetaPieza = switch (tipoPieza.toUpperCase()) {
            case "HELMET" -> " &#E6CCFF(Casco)";
            case "CHESTPLATE" -> " &#E6CCFF(Peto)";
            case "LEGGINGS" -> " &#E6CCFF(Pantalones)";
            case "BOOTS" -> " &#E6CCFF(Botas)";
            default -> "";
        };

        item.editMeta(meta -> {
            meta.displayName(CrossplayUtils.parseCrossplay(null, dto.nombre() + etiquetaPieza));

            List<String> lore = new ArrayList<>();
            lore.add(CrossplayUtils.getChat(null, "&#E6CCFFClase: &#ff00ff" + dto.claseRequerida()));
            lore.add(" ");
            if (dto.vidaExtra() > 0) lore.add(CrossplayUtils.getChat(null, "&#E6CCFFVida Extra: &#8b0000+" + dto.vidaExtra() + " ❤"));
            if (dto.velocidadMovimiento() > 0) lore.add(CrossplayUtils.getChat(null, "&#E6CCFFVelocidad: &#00f5ff+" + dto.velocidadMovimiento() + " 🍃"));
            if (dto.suerteMinera() > 0) lore.add(CrossplayUtils.getChat(null, "&#E6CCFFFortuna Minera: &#00f5ff+" + dto.suerteMinera() + "% ✨"));
            if (dto.velocidadMineria() > 0) lore.add(CrossplayUtils.getChat(null, "&#E6CCFFPrisa Minera: &#ff00ff+" + dto.velocidadMineria() + " ⚡"));
            if (dto.suerteAgricola() > 0) lore.add(CrossplayUtils.getChat(null, "&#E6CCFFFortuna Agrícola: &#00f5ff+" + dto.suerteAgricola() + "% 🌾"));
            if (dto.suerteTala() > 0) lore.add(CrossplayUtils.getChat(null, "&#E6CCFFDoble Caída (Tala): &#00f5ff+" + dto.suerteTala() + "% 🪓"));
            if (dto.criaturaMarina() > 0) lore.add(CrossplayUtils.getChat(null, "&#E6CCFFProb. Criatura Marina: &#00f5ff+" + dto.criaturaMarina() + "% 🦑"));
            if (dto.velocidadPesca() > 0) lore.add(CrossplayUtils.getChat(null, "&#E6CCFFVelocidad Pesca: &#00f5ff+" + dto.velocidadPesca() + "% 🎣"));

            List<String> loreCustom = pluginMemoria.getFileManager().getArmaduras().getStringList("armaduras_profesion." + id_yml + ".lore_custom");
            if (loreCustom != null && !loreCustom.isEmpty()) {
                lore.add(" ");
                // 💡 Lambda forEach: Más rápido en lectura O(1)
                loreCustom.forEach(linea -> lore.add(CrossplayUtils.getChat(null, linea)));
            }

            lore.add(" ");
            lore.add(CrossplayUtils.getChat(null, "&#E6CCFFRequisito de " + dto.skillRequerida() + ": Nivel " + dto.nivelRequerido()));
            meta.setLore(lore);
            meta.setUnbreakable(true);

            var pdc = meta.getPersistentDataContainer();
            pdc.set(llaveArmaduraId, PersistentDataType.STRING, dto.id());
            if (dto.vidaExtra() > 0) pdc.set(llaveVidaExtra, PersistentDataType.DOUBLE, dto.vidaExtra());
        });

        return item;
    }

    public static ItemStack crearPolvoEstelar() {
        ItemStack item = new ItemStack(Material.GLOWSTONE_DUST);
        item.editMeta(meta -> {
            meta.displayName(CrossplayUtils.parseCrossplay(null, "&#ff00ff✨ Polvo Estelar"));
            meta.getPersistentDataContainer().set(llaveMaterialMejora, PersistentDataType.BYTE, (byte) 1);
        });
        return item;
    }

    public static ItemStack crearHojaVacio() {
        ItemStack item = new ItemStack(Material.DIAMOND_SWORD);
        item.editMeta(meta -> {
            meta.displayName(CrossplayUtils.parseCrossplay(null, "&#ff00ff🌌 Hoja del Vacío"));
            // 🌟 List.of(): Arrays inmutables ultra rápidos para Lore estático
            meta.setLore(List.of(
                    CrossplayUtils.getChat(null, "&#E6CCFFArtefacto de Utilidad"),
                    " ",
                    CrossplayUtils.getChat(null, "&#ff00ffHabilidad: Transmisión Instantánea <bold>(CLIC DERECHO)</bold>"),
                    CrossplayUtils.getChat(null, "&#E6CCFFCosto: &#ff00ff40 Energía ⚡"),
                    CrossplayUtils.getChat(null, "&#8b0000🔒 Ligado al Alma")
            ));
            meta.setUnbreakable(true);
            meta.getPersistentDataContainer().set(llaveSoulbound, PersistentDataType.BYTE, (byte) 1);
        });
        return item;
    }

    public static ItemStack aplicarReforja(ItemStack item, String idReforja) {
        if (item == null || item.isEmpty()) return item;
        var reforge = pluginMemoria.getFileManager().getReforgeDTO(idReforja);
        if (reforge == null) return item;

        // 🌟 LECTURA DIRECTA O(1): Evitamos generar el ItemMeta completo solo para leer
        var pdcItem = item.getPersistentDataContainer();
        var esArma = pdcItem.has(llaveWeaponId, PersistentDataType.STRING);
        var esHerramienta = pdcItem.has(llaveHerramientaId, PersistentDataType.STRING);

        if (!esArma && !esHerramienta) return item;

        String claseOriginal = "Cualquiera";
        if (esArma) {
            String idBase = pdcItem.get(llaveWeaponId, PersistentDataType.STRING);
            var arma = pluginMemoria.getFileManager().getWeaponDTO(idBase);
            if (arma == null) return item;
            claseOriginal = arma.claseRequerida();
        } else {
            String idBase = pdcItem.get(llaveHerramientaId, PersistentDataType.STRING);
            var tool = pluginMemoria.getFileManager().getToolDTO(idBase);
            if (tool == null) return item;
            claseOriginal = tool.profesion();
        }

        if (!reforge.aplicaAClase(claseOriginal) && !reforge.aplicaAClase("Cualquiera")) return item;

        // Escribimos directamente en el Data Component
        item.editPersistentDataContainer(pdc -> pdc.set(llaveReforja, PersistentDataType.STRING, reforge.id()));
        sincronizarItemAsync(item);

        return item;
    }

    public static ItemStack generarLibroEncantamiento(String idEnchant, int nivel) {
        var dto = pluginMemoria.getFileManager().getEnchantDTO(idEnchant);
        if (dto == null) {
            org.bukkit.Bukkit.getLogger().warning("¡No se encontró el encantamiento " + idEnchant + " en la caché!");
            return new ItemStack(Material.BOOK);
        }

        int nivelReal = Math.min(nivel, dto.nivelMaximo());
        ItemStack libro = new ItemStack(Material.ENCHANTED_BOOK);

        String nombreRomanos = switch (nivelReal) {
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> "I";
        };

        libro.editMeta(meta -> {
            meta.displayName(CrossplayUtils.parseCrossplay(null, dto.nombre() + " " + nombreRomanos));

            double valorActual = dto.getValorPorNivel(nivelReal);
            String descReemplazada = dto.descripcion().replace("{val}", String.valueOf(valorActual));

            meta.setLore(List.of(
                    CrossplayUtils.getChat(null, "&#E6CCFFLibro de Encantamiento Mágico"),
                    " ",
                    CrossplayUtils.getChat(null, descReemplazada),
                    " ",
                    CrossplayUtils.getChat(null, "&#E6CCFFAplica a: " + String.join(", ", dto.aplicaA())),
                    CrossplayUtils.getChat(null, "&#ff00ffLlévalo a un Yunque Mágico para aplicarlo.")
            ));

            var pdc = meta.getPersistentDataContainer();
            pdc.set(llaveEnchantId, PersistentDataType.STRING, dto.id());
            pdc.set(llaveEnchantNivel, PersistentDataType.INTEGER, nivelReal);
        });

        return libro;
    }

    public static ItemStack aplicarEncantamiento(ItemStack item, String idEnchant, int nivel) {
        if (item == null || item.isEmpty()) return item;
        var enchant = pluginMemoria.getFileManager().getEnchantDTO(idEnchant);
        if (enchant == null) return item;

        item.editMeta(meta -> {
            NamespacedKey keyEnchant = new NamespacedKey(pluginMemoria, "nexo_enchant_" + idEnchant);
            meta.getPersistentDataContainer().set(keyEnchant, PersistentDataType.INTEGER, nivel);

            String nombreRomanos = switch (nivel) {
                case 2 -> "II";
                case 3 -> "III";
                case 4 -> "IV";
                case 5 -> "V";
                default -> "I";
            };

            String lineaEncantamiento = CrossplayUtils.getChat(null, enchant.nombre() + " " + nombreRomanos);
            String nombrePuro = org.bukkit.ChatColor.stripColor(CrossplayUtils.getChat(null, enchant.nombre()));

            List<String> lore = meta.getLore();
            if (lore != null) {
                boolean encontrado = false;
                for (int i = 0; i < lore.size(); i++) {
                    if (org.bukkit.ChatColor.stripColor(lore.get(i)).startsWith(nombrePuro)) {
                        lore.set(i, lineaEncantamiento);
                        encontrado = true;
                        break;
                    }
                }
                if (!encontrado) lore.add(lineaEncantamiento);
                meta.setLore(lore);
            }
        });

        return item;
    }

    public static ItemStack generarArmadura(String id) {
        return generarArmaduraProfesion(id, "CHESTPLATE");
    }

    public static ItemStack generarHerramienta(String id) {
        return generarHerramientaProfesion(id);
    }
}