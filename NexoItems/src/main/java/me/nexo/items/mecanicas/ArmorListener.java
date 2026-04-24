package me.nexo.items.mecanicas;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.aurelium.auraskills.api.AuraSkillsApi;
import dev.aurelium.auraskills.api.skill.Skills;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.user.NexoUser;
import me.nexo.core.user.UserManager;
import me.nexo.items.NexoItems;
import me.nexo.items.dtos.ArmorDTO;
import me.nexo.items.managers.FileManager;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * 🎒 NexoItems - Motor de Armaduras RPG (Arquitectura Enterprise Java 21)
 * Rendimiento: Eventos Nativos Paper, O(1) PDC Reads, Folia Region Scheduler.
 */
@Singleton
public class ArmorListener implements Listener {

    // 🌟 DEPENDENCIAS PROPAGADAS
    private final NexoItems plugin;
    private final FileManager fileManager;
    private final UserManager userManager;
    private final CrossplayUtils crossplayUtils;
    private final AuraSkillsApi auraSkillsApi;

    // 🌟 LLAVES DESACOPLADAS O(1)
    private final NamespacedKey armorIdKey;
    private final NamespacedKey extraHealthKey;

    // 💉 PILAR 1: Inyección de Dependencias Estricta
    @Inject
    public ArmorListener(NexoItems plugin, FileManager fileManager, UserManager userManager, CrossplayUtils crossplayUtils) {
        this.plugin = plugin;
        this.fileManager = fileManager;
        this.userManager = userManager;
        this.crossplayUtils = crossplayUtils;

        this.auraSkillsApi = AuraSkillsApi.get(); // Cacheado al inicio
        this.armorIdKey = new NamespacedKey("nexoitems", "armor_id");
        this.extraHealthKey = new NamespacedKey("nexoitems", "vida_extra");
    }

    // 🌟 1. EVENTO EXACTO: Solo evaluamos cuando realmente cambian una pieza
    @EventHandler(priority = EventPriority.HIGH)
    public void onArmorChange(PlayerArmorChangeEvent event) {
        var p = event.getPlayer();
        // 🛡️ FOLIA SYNC: Retrasamos 1 tick en el Hilo de la Región del jugador
        p.getScheduler().runDelayed(plugin, task -> evaluarArmadura(p), null, 1L);
    }

    // 🌟 2. EVENTO INICIAL: Evaluamos al entrar
    @EventHandler(priority = EventPriority.HIGH)
    public void onJoin(PlayerJoinEvent event) {
        evaluarArmadura(event.getPlayer());
    }

    /**
     * 🧠 Evalúa el set de armadura actual, aplica stats o rechaza la pieza si no tiene nivel
     */
    public void evaluarArmadura(Player p) {
        if (p == null || !p.isOnline()) return;

        var armor = p.getInventory().getArmorContents();
        double extraVida = 0;
        int velMineria = 0; // Usamos enteros para los niveles de poción
        int velMovimiento = 0;

        String claseDominante = "Cualquiera";

        NexoUser user = userManager.getUserOrNull(p.getUniqueId());

        // ==========================================
        // 1. DETECTAR CLASE DOMINANTE
        // ==========================================
        for (var item : armor) {
            // 🌟 GHOST-ITEM PROOF Y LECTURA O(1) (Sin crear ItemMeta extra)
            if (item == null || item.isEmpty() || !item.hasItemMeta()) continue;
            var pdc = item.getPersistentDataContainer();

            if (pdc.has(armorIdKey, PersistentDataType.STRING)) {
                String id = pdc.get(armorIdKey, PersistentDataType.STRING);
                ArmorDTO dto = fileManager.getArmorDTO(id);

                if (dto != null && !dto.claseRequerida().equalsIgnoreCase("Cualquiera") && !dto.claseRequerida().equalsIgnoreCase("Ninguna")) {
                    claseDominante = dto.claseRequerida();
                    break; // La primera pieza con clase define el set entero
                }
            }
        }

        if (user != null) {
            user.setClaseJugador(claseDominante);
        }

        // ==========================================
        // 2. APLICAR STATS Y RESTRICCIONES (Por pieza)
        // ==========================================
        for (int i = 0; i < armor.length; i++) {
            var item = armor[i];
            if (item == null || item.isEmpty() || !item.hasItemMeta()) continue;
            var pdc = item.getPersistentDataContainer();

            if (pdc.has(armorIdKey, PersistentDataType.STRING)) {
                String id = pdc.get(armorIdKey, PersistentDataType.STRING);
                ArmorDTO dto = fileManager.getArmorDTO(id);

                if (dto != null) {
                    boolean cumpleRequisitos = true;
                    String razonFallo = "";

                    // A) Validación de Clase
                    if (!dto.claseRequerida().equalsIgnoreCase("Cualquiera") &&
                            !dto.claseRequerida().equalsIgnoreCase("Ninguna") &&
                            !dto.claseRequerida().equalsIgnoreCase(claseDominante)) {
                        razonFallo = "Choque de Matrices (" + dto.claseRequerida() + ")";
                        cumpleRequisitos = false;
                    }

                    // B) Validación de Nivel (AuraSkills y Core)
                    if (cumpleRequisitos) {
                        int nivelJugador = 1;
                        String skill = dto.skillRequerida();

                        if (user != null) {
                            if (skill.equalsIgnoreCase("Combate")) nivelJugador = Math.max(1, user.getCombateNivel());
                            else if (skill.equalsIgnoreCase("Minería")) nivelJugador = Math.max(1, user.getMineriaNivel());
                            else if (skill.equalsIgnoreCase("Agricultura")) nivelJugador = Math.max(1, user.getAgriculturaNivel());
                        }

                        try {
                            if (skill.equalsIgnoreCase("Pesca")) nivelJugador = Math.max(1, auraSkillsApi.getUser(p.getUniqueId()).getSkillLevel(Skills.FISHING));
                            else if (skill.equalsIgnoreCase("Tala")) nivelJugador = Math.max(1, auraSkillsApi.getUser(p.getUniqueId()).getSkillLevel(Skills.FORAGING));
                        } catch (Exception ignored) {}

                        if (nivelJugador < dto.nivelRequerido()) {
                            razonFallo = skill + " Nv." + dto.nivelRequerido();
                            cumpleRequisitos = false;
                        }
                    }

                    // C) Aplicación o Rechazo
                    if (cumpleRequisitos) {
                        extraVida += dto.vidaExtra();
                        velMineria += dto.velocidadMineria();
                        velMovimiento += dto.velocidadMovimiento();
                    } else {
                        quitarArmadura(p, item, i, razonFallo);
                    }
                }
            }
            else if (pdc.has(extraHealthKey, PersistentDataType.DOUBLE)) {
                // Soporte para ítems viejos o customizados
                extraVida += pdc.getOrDefault(extraHealthKey, PersistentDataType.DOUBLE, 0.0);
            }
        }

        // ==========================================
        // 3. APLICAR VIDA Y EFECTOS FINALES
        // ==========================================
        double vidaBaseVanilla = 20.0;
        double vidaTotal = vidaBaseVanilla + extraVida;

        // 🌟 FIX PAPER 1.21: Attribute.MAX_HEALTH
        var healthAttr = p.getAttribute(Attribute.MAX_HEALTH);
        if (healthAttr != null && healthAttr.getBaseValue() != vidaTotal) {
            healthAttr.setBaseValue(vidaTotal);
            // Evitamos que el jugador se quede con vida extra si se quita la armadura
            if (p.getHealth() > vidaTotal) {
                p.setHealth(vidaTotal);
            }
        }

        // 🌟 FIX: Actualizado al nombre moderno de la API de Paper (HASTE en lugar de FAST_DIGGING)
        gestionarEfecto(p, PotionEffectType.HASTE, velMineria / 20);
        gestionarEfecto(p, PotionEffectType.SPEED, velMovimiento / 20);
    }

    /**
     * 🛡️ Gestor limpio de efectos (No parpadea la pantalla de Bedrock)
     */
    private void gestionarEfecto(Player p, PotionEffectType tipo, int nivel) {
        if (nivel > 0) {
            // Aplicamos un efecto infinito y escondemos las partículas
            p.addPotionEffect(new PotionEffect(tipo, PotionEffect.INFINITE_DURATION, nivel - 1, false, false, false));
        } else {
            p.removePotionEffect(tipo);
        }
    }

    /**
     * ❌ Des-equipa una pieza ilegal y la manda al inventario (o al suelo)
     */
    private void quitarArmadura(Player p, ItemStack item, int slotIndex, String razon) {
        var clon = item.clone();

        // Limpiamos el slot real usando el índice que nos dio el array getArmorContents()
        var armor = p.getInventory().getArmorContents();
        armor[slotIndex] = null;
        p.getInventory().setArmorContents(armor);

        var sobrantes = p.getInventory().addItem(clon);
        for (var drop : sobrantes.values()) {
            p.getWorld().dropItemNaturally(p.getLocation(), drop);
        }

        crossplayUtils.sendMessage(p, "&#FF5555[!] <bold>INCOMPATIBILIDAD NEURAL</bold> | La armadura ha sido rechazada por tu sistema. Requisito: &#FFAA00" + razon);
        p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
    }
}