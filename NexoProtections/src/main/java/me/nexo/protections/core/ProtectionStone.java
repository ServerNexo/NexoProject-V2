package me.nexo.protections.core;

import me.nexo.core.NexoCore;
import me.nexo.core.user.NexoUser;
import me.nexo.core.utils.NexoColor;
import me.nexo.protections.NexoProtections;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🛡️ NexoProtections - Modelo de Monolito (Arquitectura Enterprise)
 */
public class ProtectionStone {

    private final UUID stoneId;
    private final UUID ownerId;
    private final UUID clanId;
    private final ClaimBox box;

    private double currentEnergy;
    private double maxEnergy;

    private final Set<UUID> trustedFriends = new HashSet<>();
    private final Map<String, Boolean> environmentFlags = new ConcurrentHashMap<>();

    // 💡 Cachés transitorias (no se guardan en BD) para evitar llamadas constantes
    private transient NexoProtections pluginCache;
    private transient NexoCore coreCache;
    private transient NamespacedKey holoKey;

    public ProtectionStone(UUID stoneId, UUID ownerId, UUID clanId, ClaimBox box) {
        this.stoneId = stoneId;
        this.ownerId = ownerId;
        this.clanId = clanId;
        this.box = box;
        this.currentEnergy = 100.0;
        this.maxEnergy = 1000.0;

        this.environmentFlags.put("pvp", false);
        this.environmentFlags.put("mob-spawning", false);
        this.environmentFlags.put("tnt-damage", false);
        this.environmentFlags.put("fire-spread", false);
        this.environmentFlags.put("interact", false);
        this.environmentFlags.put("containers", false);
        this.environmentFlags.put("item-pickup", false);
        this.environmentFlags.put("item-drop", false);
        this.environmentFlags.put("animal-damage", false);
    }

    // 💡 Localizador seguro de Plugins (Reemplaza al viejo getInstance())
    private NexoProtections getPlugin() {
        if (pluginCache == null) pluginCache = JavaPlugin.getPlugin(NexoProtections.class);
        return pluginCache;
    }

    private NexoCore getCore() {
        if (coreCache == null) coreCache = JavaPlugin.getPlugin(NexoCore.class);
        return coreCache;
    }

    private NamespacedKey getHoloKey() {
        if (holoKey == null) holoKey = new NamespacedKey(getPlugin(), "nexo_holo");
        return holoKey;
    }

    public boolean hasPermission(UUID playerId, ClaimAction action) {
        if (playerId.equals(ownerId)) return true;
        if (currentEnergy <= 0) return true;

        if (clanId == null) {
            return trustedFriends.contains(playerId);
        } else {
            NexoUser user = getCore().getUserManager().getUserOrNull(playerId);
            if (user != null && user.hasClan() && clanId.equals(user.getClanId())) {
                String role = user.getClanRole().toUpperCase();
                if (role.equals("LIDER") || role.equals("OFICIAL")) return true;
                if (role.equals("MIEMBRO") && (action == ClaimAction.INTERACT || action == ClaimAction.OPEN_CONTAINER)) return true;
            }
            return false;
        }
    }

    // ==========================================
    // 🔮 SISTEMA DE HOLOGRAMAS TIER S
    // ==========================================

    public Location getCenterLocationIfLoaded() {
        World w = Bukkit.getWorld(box.world());
        if (w == null) return null;

        int cx = (box.minX() + box.maxX()) / 2;
        int cz = (box.minZ() + box.maxZ()) / 2;

        if (!w.isChunkLoaded(cx >> 4, cz >> 4)) return null;

        for (int y = 319; y >= -64; y--) {
            Block b = w.getBlockAt(cx, y, cz);
            if (b.getType() == Material.LODESTONE) {
                return b.getLocation();
            }
        }
        return null;
    }

    public void updateHologram() {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(getPlugin(), this::updateHologram);
            return;
        }

        Location loc = getCenterLocationIfLoaded();
        if (loc == null) return; // Anti-Lag

        Location holoLoc = loc.clone().add(0.5, 1.2, 0.5);
        ArmorStand hologram = null;

        for (Entity e : loc.getWorld().getNearbyEntities(loc, 2, 3, 2)) {
            if (e instanceof ArmorStand && e.getPersistentDataContainer().has(getHoloKey(), PersistentDataType.STRING)) {
                String id = e.getPersistentDataContainer().get(getHoloKey(), PersistentDataType.STRING);
                if (stoneId.toString().equals(id)) {
                    hologram = (ArmorStand) e;
                    break;
                }
            }
        }

        if (hologram == null) {
            hologram = loc.getWorld().spawn(holoLoc, ArmorStand.class, as -> {
                as.setVisible(false);
                as.setMarker(true);
                as.setGravity(false);
                as.setCustomNameVisible(true);
                as.getPersistentDataContainer().set(getHoloKey(), PersistentDataType.STRING, stoneId.toString());
            });
        }

        String ownerName = Bukkit.getOfflinePlayer(ownerId).getName();
        if (ownerName == null) ownerName = "Desconocido";

        double percentage = (currentEnergy / maxEnergy) * 100;
        String color = percentage > 50 ? "&#CC66FF" : (percentage > 20 ? "&#9933FF" : "&#FF3366");

        net.kyori.adventure.text.Component text = NexoColor.parse("&#9933FF<bold>MONOLITO</bold> &#FFFFFF| &#E6CCFF" + ownerName + " &#FFFFFF| " + color + String.format("%.0f", currentEnergy) + " ✦");
        hologram.customName(text);
    }

    public void removeHologram() {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(getPlugin(), this::removeHologram);
            return;
        }

        World w = Bukkit.getWorld(box.world());
        if (w == null) return;

        int cx = (box.minX() + box.maxX()) / 2;
        int cz = (box.minZ() + box.maxZ()) / 2;

        Location centerCol = new Location(w, cx + 0.5, 100, cz + 0.5);

        for (Entity e : w.getNearbyEntities(centerCol, 2, 320, 2)) {
            if (e instanceof ArmorStand && e.getPersistentDataContainer().has(getHoloKey(), PersistentDataType.STRING)) {
                String id = e.getPersistentDataContainer().get(getHoloKey(), PersistentDataType.STRING);
                if (stoneId.toString().equals(id)) {
                    e.remove();
                }
            }
        }
    }

    // ==========================================
    // 🔋 GETTERS Y SETTERS
    // ==========================================
    public ClaimBox getBox() { return box; }
    public UUID getStoneId() { return stoneId; }
    public UUID getOwnerId() { return ownerId; }
    public UUID getClanId() { return clanId; }

    public double getCurrentEnergy() { return currentEnergy; }
    public double getMaxEnergy() { return maxEnergy; }
    public void setMaxEnergy(double maxEnergy) { this.maxEnergy = maxEnergy; }

    public void addEnergy(double amount) {
        this.currentEnergy = Math.min(maxEnergy, this.currentEnergy + amount);
        updateHologram();
    }
    public void drainEnergy(double amount) {
        this.currentEnergy = Math.max(0, this.currentEnergy - amount);
        updateHologram();
    }

    public boolean getFlag(String flagName) { return environmentFlags.getOrDefault(flagName, false); }
    public void setFlag(String flagName, boolean value) { environmentFlags.put(flagName, value); }
    public Map<String, Boolean> getFlags() { return environmentFlags; }

    public Set<UUID> getTrustedFriends() { return trustedFriends; }
    public void addFriend(UUID friendId) { trustedFriends.add(friendId); }
    public void removeFriend(UUID friendId) { trustedFriends.remove(friendId); }
}