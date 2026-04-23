package me.nexo.protections.core;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 🛡️ NexoProtections - Modelo de Monolito (Arquitectura Enterprise)
 * Rendimiento: 100% Desacoplado, Lock-Free Hilos Virtuales y Cero I/O.
 */
public class ProtectionStone {

    private final UUID stoneId;
    private final UUID ownerId;
    private final UUID clanId;
    private final ClaimBox box;

    private double currentEnergy;
    private double maxEnergy;
    
    // 🌟 FIX JAVA 21: ReentrantLock previene Carrier Thread Pinning en el UpkeepManager
    private final ReentrantLock energyLock = new ReentrantLock();

    private final Set<UUID> trustedFriends = new HashSet<>();
    private final Map<String, Boolean> environmentFlags = new ConcurrentHashMap<>();

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

    /**
     * 🌟 FIX DESACOPLADO: El modelo solo compara. El Manager le provee el rol.
     */
    public boolean hasPermission(UUID playerId, ClaimAction action, String targetClanRole) {
        if (playerId.equals(ownerId)) return true;
        if (currentEnergy <= 0) return true;

        if (clanId == null || targetClanRole == null) {
            return trustedFriends.contains(playerId);
        } else {
            String role = targetClanRole.toUpperCase();
            if ("LIDER".equals(role) || "OFICIAL".equals(role)) return true;
            return "MIEMBRO".equals(role) && (action == ClaimAction.INTERACT || action == ClaimAction.OPEN_CONTAINER);
        }
    }
    
    // Sobrecarga para comprobaciones donde el jugador NO tiene clan
    public boolean hasPermission(UUID playerId, ClaimAction action) {
        return hasPermission(playerId, action, null);
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

    /**
     * 🌟 FIX I/O: El Holograma recibe el componente de texto ya pre-computado.
     * El modelo de datos NO debe parsear colores ni llamar a bases de datos.
     */
    public void updateHologram(NamespacedKey holoKey, net.kyori.adventure.text.Component parsedText) {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("Hologramas solo pueden actualizarse en el Hilo Principal");
        }

        Location loc = getCenterLocationIfLoaded();
        if (loc == null) return; // Anti-Lag

        Location holoLoc = loc.clone().add(0.5, 1.2, 0.5);
        ArmorStand hologram = null;

        for (Entity e : loc.getWorld().getNearbyEntities(loc, 2, 3, 2)) {
            if (e instanceof ArmorStand && e.getPersistentDataContainer().has(holoKey, PersistentDataType.STRING)) {
                String id = e.getPersistentDataContainer().get(holoKey, PersistentDataType.STRING);
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
                as.getPersistentDataContainer().set(holoKey, PersistentDataType.STRING, stoneId.toString());
            });
        }

        hologram.customName(parsedText);
    }

    public void removeHologram(NamespacedKey holoKey) {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("Hologramas solo pueden eliminarse en el Hilo Principal");
        }

        World w = Bukkit.getWorld(box.world());
        if (w == null) return;

        int cx = (box.minX() + box.maxX()) / 2;
        int cz = (box.minZ() + box.maxZ()) / 2;
        Location centerCol = new Location(w, cx + 0.5, 100, cz + 0.5);

        for (Entity e : w.getNearbyEntities(centerCol, 2, 320, 2)) {
            if (e instanceof ArmorStand && e.getPersistentDataContainer().has(holoKey, PersistentDataType.STRING)) {
                String id = e.getPersistentDataContainer().get(holoKey, PersistentDataType.STRING);
                if (stoneId.toString().equals(id)) {
                    e.remove();
                }
            }
        }
    }

    // ==========================================
    // 🔋 GETTERS Y SETTERS (THREAD-SAFE ENERGY)
    // ==========================================
    public ClaimBox getBox() { return box; }
    public UUID getStoneId() { return stoneId; }
    public UUID getOwnerId() { return ownerId; }
    public UUID getClanId() { return clanId; }

    public double getCurrentEnergy() { return currentEnergy; }
    public double getMaxEnergy() { return maxEnergy; }
    public void setMaxEnergy(double maxEnergy) { this.maxEnergy = maxEnergy; }

    public void addEnergy(double amount) {
        energyLock.lock();
        try {
            this.currentEnergy = Math.min(maxEnergy, this.currentEnergy + amount);
        } finally {
            energyLock.unlock();
        }
    }
    
    public void drainEnergy(double amount) {
        energyLock.lock();
        try {
            this.currentEnergy = Math.max(0, this.currentEnergy - amount);
        } finally {
            energyLock.unlock();
        }
    }

    public boolean getFlag(String flagName) { return environmentFlags.getOrDefault(flagName, false); }
    public void setFlag(String flagName, boolean value) { environmentFlags.put(flagName, value); }
    public Map<String, Boolean> getFlags() { return environmentFlags; }

    public Set<UUID> getTrustedFriends() { return trustedFriends; }
    public void addFriend(UUID friendId) { trustedFriends.add(friendId); }
    public void removeFriend(UUID friendId) { trustedFriends.remove(friendId); }
}