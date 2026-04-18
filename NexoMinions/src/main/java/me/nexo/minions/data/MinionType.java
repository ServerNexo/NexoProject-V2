package me.nexo.minions.data;

import dev.aurelium.auraskills.api.skill.Skill;
import dev.aurelium.auraskills.api.skill.Skills;
import org.bukkit.Material;

// ❌ Borramos el @Getter de aquí arriba

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

    // ✅ Escribimos los métodos a mano para que IntelliJ los vea
    public String getDisplayName() { return displayName; }
    public Material getTargetMaterial() { return targetMaterial; }
    public String getNexoModelID() { return nexoModelID; }
    public Skill getAuraSkill() { return auraSkill; }
}