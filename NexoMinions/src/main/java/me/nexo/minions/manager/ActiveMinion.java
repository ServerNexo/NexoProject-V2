package me.nexo.minions.manager;

import me.nexo.colecciones.NexoColecciones;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.minions.NexoMinions;
import me.nexo.minions.data.MinionKeys;
import me.nexo.minions.data.MinionTier;
import me.nexo.minions.data.MinionType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.UUID;

/**
 * 🤖 NexoMinions - Modelo de Minion Activo (Arquitectura Enterprise Java 25)
 * Rendimiento: Híbrido. Matemáticas en RAM asíncrona, Física con EntityScheduler (Folia-Ready).
 */
public class ActiveMinion {
    private final NexoMinions plugin;
    private final ItemDisplay entity;
    private final Interaction hitbox;
    private final TextDisplay holograma;
    private final UUID ownerId;
    private final MinionType type;

    // Volátiles para garantizar lectura segura entre Hilos Virtuales y el Main Thread
    private volatile int tier;
    private volatile long nextActionTime;
    private volatile int storedItems;

    private final ItemStack[] upgrades = new ItemStack[4];
    private int trabajosRealizados = 0;

    private InventoryHolder cachedStorage = null;
    private long lastStorageCheckTime = 0;

    public ActiveMinion(NexoMinions plugin, ItemDisplay entity, Interaction hitbox, TextDisplay holograma, UUID ownerId, MinionType type, int tier, long nextActionTime, int storedItems) {
        this.plugin = plugin;
        this.entity = entity;
        this.hitbox = hitbox;
        this.holograma = holograma;
        this.ownerId = ownerId;
        this.type = type;
        this.tier = tier;
        this.nextActionTime = nextActionTime;
        this.storedItems = storedItems;

        for (int i = 0; i < 4; i++) {
            byte[] bytes = entity.getPersistentDataContainer().get(MinionKeys.UPGRADES[i], PersistentDataType.BYTE_ARRAY);
            if (bytes != null) this.upgrades[i] = ItemStack.deserializeBytes(bytes);
        }
    }

    public int getRealMaxStorage() {
        int base = MinionTier.getMaxStorage(tier);
        int bonus = 0;

        for (ItemStack item : upgrades) {
            if (item == null || item.isEmpty()) continue; // 🌟 1.21 Fix
            ConfigurationSection datos = plugin.getUpgradesConfig().getUpgradeData(item);
            if (datos != null && "UPGRADE".equals(datos.getString("category")) && "STORAGE".equals(datos.getString("type"))) {
                bonus += datos.getInt("bonus_capacidad", 0);
            }
        }
        return base + bonus;
    }

    // ==========================================
    // 🧠 MOTOR LÓGICO ASÍNCRONO (Virtual Threads)
    // ==========================================
    public void tick(long currentTimeMillis) {
        int maxStorage = getRealMaxStorage();
        boolean estaLleno = storedItems >= maxStorage;
        boolean tieneEnlaceCofre = tieneMejoraPorTipo("STORAGE_LINK");

        // 1. Planificamos qué tareas físicas se deben hacer
        boolean debeTrabajar = (currentTimeMillis >= nextActionTime) && (!estaLleno || tieneEnlaceCofre);

        // Calculamos tiempo de próxima acción si es necesario
        if (debeTrabajar) {
            long tiempoBase = MinionTier.getDelayMillis(tier);
            this.nextActionTime = currentTimeMillis + (long) (tiempoBase * getSpeedMultiplier());
        }

        // 2. Ejecución Física SEGURA (EntityScheduler)
        // 🌟 PAPER 1.21+ NATIVO: Despacha la tarea al hilo específico de este Chunk.
        entity.getScheduler().run(plugin, scheduledTask -> {

            // Si el minion fue removido o el chunk se descargó, limpiamos y abortamos.
            if (!entity.isValid() || entity.isDead()) {
                if (hitbox != null && hitbox.isValid()) hitbox.remove();
                if (holograma != null && holograma.isValid()) holograma.remove();
                plugin.getMinionManager().getMinionsActivos().remove(entity.getUniqueId());
                return;
            }

            // Actualización visual holográfica
            actualizarHolograma(maxStorage, estaLleno, tieneEnlaceCofre);

            if (debeTrabajar) {
                realizarTrabajoFisico();
                entity.getPersistentDataContainer().set(MinionKeys.NEXT_ACTION, PersistentDataType.LONG, nextActionTime);
            }

            animarFisica();
        }, null);
    }

    // ==========================================
    // 🔨 EJECUCIÓN FÍSICA (Entity/Chunk Thread)
    // ==========================================
    private void realizarTrabajoFisico() {
        Location loc = entity.getLocation();

        // 🌟 OPTIMIZACIÓN VISUAL O(1): Usamos getNearbyPlayers nativo.
        // Paper procesa esto en C++ internamente, no causa lag como los Streams antiguos.
        boolean jugadorCerca = !loc.getNearbyPlayers(32).isEmpty();

        if (jugadorCerca) {
            loc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc.clone().add(0, 1, 0), 2, 0.2, 0.2, 0.2, 0.01);
            loc.getWorld().playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.3f, 1.5f);
        }

        boolean guardadoEnCofre = false;
        if (tieneMejoraPorTipo("STORAGE_LINK")) {
            guardadoEnCofre = guardarEnCofreAdyacenteFisico(new ItemStack(type.getTargetMaterial(), 1));
        }

        if (!guardadoEnCofre) {
            ConfigurationSection autoSellData = getMejoraActiva("AUTO_SELL");
            if (autoSellData != null) {
                double precio = autoSellData.getDouble("precio_por_unidad", 1.0);

                Player owner = Bukkit.getPlayer(ownerId);
                if (owner != null && owner.isOnline()) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "eco give " + owner.getName() + " " + precio);

                    // Sistema de Colecciones
                    if (Bukkit.getPluginManager().isPluginEnabled("NexoColecciones")) {
                        JavaPlugin.getPlugin(NexoColecciones.class).getCollectionManager().addProgress(owner, type.getTargetMaterial().name(), 1);
                    }
                }
                this.trabajosRealizados++;
                consumirCombustiblesFisico();
                return;
            }

            // Guardado Interno
            if (this.storedItems < getRealMaxStorage()) {
                this.storedItems += 1;
                entity.getPersistentDataContainer().set(MinionKeys.STORED_ITEMS, PersistentDataType.INTEGER, this.storedItems);
            }
        }

        this.trabajosRealizados++;
        consumirCombustiblesFisico();
    }

    private boolean guardarEnCofreAdyacenteFisico(ItemStack item) {
        long currentTime = System.currentTimeMillis();

        // Check rápido del caché
        if (cachedStorage != null) {
            if (cachedStorage.getInventory().getLocation() != null &&
                    cachedStorage.getInventory().getLocation().getBlock().getState() instanceof InventoryHolder) {
                var sobrante = cachedStorage.getInventory().addItem(item);
                if (sobrante.isEmpty()) return true;
            }
            cachedStorage = null; // Caché inválido o cofre lleno
        }

        // Búsqueda rate-limited (10 segundos) para no ahogar el TPS
        if (currentTime - lastStorageCheckTime > 10000) {
            lastStorageCheckTime = currentTime;
            int[][] offsets = {{1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}};

            for (int[] offset : offsets) {
                Block b = entity.getLocation().clone().add(offset[0], 0, offset[2]).getBlock();
                if (b.getState() instanceof InventoryHolder holder) {
                    var sobrante = holder.getInventory().addItem(item);
                    if (sobrante.isEmpty()) {
                        cachedStorage = holder; // Cachear nuevo cofre
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // ==========================================
    // 🎨 RENDERIZADO VISUAL
    // ==========================================
    private void actualizarHolograma(int maxStorage, boolean estaLleno, boolean tieneEnlaceCofre) {
        if (holograma == null || holograma.isDead()) return;

        if (estaLleno && !tieneEnlaceCofre) {
            holograma.text(CrossplayUtils.parseCrossplay(null, "&#FF5555[!] Inventario Lleno (" + storedItems + " / " + maxStorage + ")"));
        } else {
            String nombreBonito = type.name().replace("MINION_", "").replace("_", " ");
            holograma.text(CrossplayUtils.parseCrossplay(null, "&#FFAA00" + nombreBonito + " (Tier " + tier + ")\n&#E6CCFFÍtems: &#55FF55" + storedItems + " / " + maxStorage));
        }
    }

    private void animarFisica() {
        entity.setInterpolationDuration(20);
        entity.setInterpolationDelay(0);

        Transformation trans = entity.getTransformation();
        float nuevoAngulo = (System.currentTimeMillis() % 4000) / 4000f * (float) Math.PI * 2;
        trans.getLeftRotation().set(new AxisAngle4f(nuevoAngulo, new Vector3f(0, 1, 0)));
        entity.setTransformation(trans);
    }

    // ==========================================
    // ⚙️ UTILIDADES DE MEJORAS Y COMBUSTIBLE
    // ==========================================
    public double getSpeedMultiplier() {
        double multiplicador = 1.0;
        for (ItemStack item : upgrades) {
            if (item == null || item.isEmpty()) continue;
            ConfigurationSection datos = plugin.getUpgradesConfig().getUpgradeData(item);
            if (datos != null && "FUEL".equals(datos.getString("category")) && "SPEED".equals(datos.getString("type"))) {
                multiplicador -= datos.getDouble("multiplier", 0.0);
            }
        }
        return Math.max(multiplicador, 0.1);
    }

    private void consumirCombustiblesFisico() {
        for (int i = 0; i < 4; i++) {
            ItemStack item = upgrades[i];
            if (item == null || item.isEmpty()) continue; // 🌟 1.21 Fix

            ConfigurationSection datos = plugin.getUpgradesConfig().getUpgradeData(item);
            if (datos != null && "FUEL".equals(datos.getString("category", ""))) {
                if (datos.getBoolean("unbreakable", false)) continue;

                int duracionSegundos = datos.getInt("duration", 600);
                if (duracionSegundos <= 0) continue;

                long tiempoPorTrabajo = (long) (MinionTier.getDelayMillis(this.tier) * getSpeedMultiplier());
                if (tiempoPorTrabajo <= 0) tiempoPorTrabajo = 1000;

                double trabajosTotalesEnDuracion = (duracionSegundos * 1000.0) / tiempoPorTrabajo;
                double probabilidadDeGasto = 1.0 / trabajosTotalesEnDuracion;

                if (Math.random() <= probabilidadDeGasto) {
                    item.setAmount(item.getAmount() - 1);
                    setUpgrade(i, item);
                }
            }
        }
    }

    public ConfigurationSection getMejoraActiva(String tipoBuscado) {
        for (ItemStack item : upgrades) {
            if (item == null || item.isEmpty()) continue;
            ConfigurationSection datos = plugin.getUpgradesConfig().getUpgradeData(item);
            if (datos != null && datos.getString("type", "").equals(tipoBuscado)) return datos;
        }
        return null;
    }

    public boolean tieneMejoraPorTipo(String tipoBuscado) { return getMejoraActiva(tipoBuscado) != null; }
    public boolean tieneMejoraActiva(String tipoBuscado) { return getMejoraActiva(tipoBuscado) != null; }

    // ==========================================
    // 💾 GETTERS, SETTERS Y GUARDADO
    // ==========================================
    public ItemStack[] getUpgrades() { return upgrades; }
    public void setUpgrade(int slot, ItemStack item) {
        upgrades[slot] = item;
        if (item == null || item.isEmpty()) { // 🌟 1.21 Fix
            entity.getPersistentDataContainer().remove(MinionKeys.UPGRADES[slot]);
        } else {
            entity.getPersistentDataContainer().set(MinionKeys.UPGRADES[slot], PersistentDataType.BYTE_ARRAY, item.serializeAsBytes());
        }
    }

    public void setTier(int nuevoTier) {
        this.tier = nuevoTier;
        entity.getPersistentDataContainer().set(MinionKeys.TIER, PersistentDataType.INTEGER, nuevoTier);
    }

    public ItemDisplay getEntity() { return entity; }
    public Interaction getHitbox() { return hitbox; }
    public TextDisplay getHolograma() { return holograma; }
    public UUID getOwnerId() { return ownerId; }
    public MinionType getType() { return type; }
    public int getTier() { return tier; }
    public int getStoredItems() { return storedItems; }
    public void setStoredItems(int storedItems) { this.storedItems = storedItems; }

    public void saveData() {
        if (entity == null || !entity.isValid()) return;
        var pdc = entity.getPersistentDataContainer();

        pdc.set(MinionKeys.STORED_ITEMS, PersistentDataType.INTEGER, this.storedItems);
        pdc.set(MinionKeys.NEXT_ACTION, PersistentDataType.LONG, this.nextActionTime);
        pdc.set(MinionKeys.TIER, PersistentDataType.INTEGER, this.tier);

        for (int i = 0; i < 4; i++) {
            if (upgrades[i] != null && !upgrades[i].isEmpty()) { // 🌟 1.21 Fix
                pdc.set(MinionKeys.UPGRADES[i], PersistentDataType.BYTE_ARRAY, upgrades[i].serializeAsBytes());
            } else {
                pdc.remove(MinionKeys.UPGRADES[i]);
            }
        }
    }
}