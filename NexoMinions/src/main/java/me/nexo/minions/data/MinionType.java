package me.nexo.minions.data;

import dev.aurelium.auraskills.api.skill.Skill;
import dev.aurelium.auraskills.api.skill.Skills;
import org.bukkit.Material;

/**
 * 🤖 NexoMinions - Diccionario de Tipos de Minion (Data Enum)
 * Rendimiento: Nativamente Inmutable y Thread-Safe.
 * Nota: Al ser una estructura de constantes estáticas, no interviene Guice.
 */
public enum MinionType {

    COBBLESTONE("Cobblestone Minion", Material.COBBLESTONE, "minion_bee_cobble", Skills.MINING),
    WHEAT("Wheat Minion", Material.WHEAT, "minion_bee_wheat", Skills.FARMING),
    OAK_WOOD("Oak Minion", Material.OAK_LOG, "minion_bee_oak", Skills.FORAGING);

    private final String displayName;
    private final Material targetMaterial;
    private final String nexoModelID;
    private final Skill auraSkill;

    MinionType(String displayName, Material targetMaterial, String nexoModelID, Skill auraSkill) {
        this.displayName = displayName;
        this.targetMaterial = targetMaterial;
        this.nexoModelID = nexoModelID;
        this.auraSkill = auraSkill;
    }

    // Getters manuales (O(1) compilado nativo)
    public String getDisplayName() { return displayName; }
    public Material getTargetMaterial() { return targetMaterial; }
    public String getNexoModelID() { return nexoModelID; }
    public Skill getAuraSkill() { return auraSkill; }
}