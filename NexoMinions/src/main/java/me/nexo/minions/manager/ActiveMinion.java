package me.nexo.minions.manager;

import me.nexo.colecciones.colecciones.CollectionManager;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.islas.data.IslandProfile;
import me.nexo.islas.managers.IslandManager; // 🌟 IMPORT DEL GESTOR DE ISLAS
import me.nexo.minions.NexoMinions;
import me.nexo.minions.data.MinionDNA;
import me.nexo.minions.data.MinionKeys;
import me.nexo.minions.data.MinionTier;
import me.nexo.minions.data.UpgradesConfig;
import org.bukkit.Bukkit;
import org.bukkit.Color;
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
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.concurrent.CompletableFuture;

/**
 * 🤖 NexoMinions - Modelo de Minion Activo (Arquitectura Enterprise Java 25)
 * Rendimiento: Híbrido. Matemáticas en RAM asíncrona, FSM de Fatiga/Huelgas,
 * y persistencia mediante Custom Binary PersistentData.
 */
public class ActiveMinion {

    // ==========================================
    // 🚦 FSM (Finite State Machine) DE SINDICATOS
    // ==========================================
    public enum MinionState {
        WORKING,   // Producción al 100%
        FATIGUED,  // Producción al 50%
        ON_STRIKE  // Sindicato activo: Producción detenida
    }

    private final NexoMinions plugin;
    private final ItemDisplay entity;
    private final Interaction hitbox;
    private final TextDisplay holograma;

    // 🌟 Sinergias inyectadas
    private final UpgradesConfig upgradesConfig;
    private final MinionManager minionManager;
    private final CrossplayUtils crossplayUtils;
    private final CollectionManager collectionManager;
    private final IslandManager islandManager; // 🌟 GESTOR DE ISLAS PARA EL ESPEJO DE PROGRESO

    // ==========================================
    // 🧬 EL GENOMA (Lectura Concurrente Thread-Safe)
    // ==========================================
    private volatile MinionDNA dna;
    private volatile MinionState state = MinionState.WORKING;

    private final ItemStack[] upgrades = new ItemStack[4];
    private int trabajosRealizados = 0;

    private InventoryHolder cachedStorage = null;
    private long lastStorageCheckTime = 0;

    public ActiveMinion(NexoMinions plugin, ItemDisplay entity, Interaction hitbox, TextDisplay holograma,
                        MinionDNA initialDna, UpgradesConfig upgradesConfig, MinionManager minionManager,
                        CrossplayUtils crossplayUtils, CollectionManager collectionManager,
                        IslandManager islandManager) { // 🌟 ISLAND MANAGER AÑADIDO
        this.plugin = plugin;
        this.entity = entity;
        this.hitbox = hitbox;
        this.holograma = holograma;

        this.dna = initialDna;

        this.upgradesConfig = upgradesConfig;
        this.minionManager = minionManager;
        this.crossplayUtils = crossplayUtils;
        this.collectionManager = collectionManager;
        this.islandManager = islandManager;

        // Upgrades se mantienen en el PDC de la entidad
        for (int i = 0; i < 4; i++) {
            byte[] bytes = entity.getPersistentDataContainer().get(MinionKeys.UPGRADES[i], PersistentDataType.BYTE_ARRAY);
            if (bytes != null) this.upgrades[i] = ItemStack.deserializeBytes(bytes);
        }
    }

    public int getRealMaxStorage() {
        int base = MinionTier.getMaxStorage(dna.tier());
        int bonus = 0;

        for (ItemStack item : upgrades) {
            if (item == null || item.isEmpty()) continue;
            var datos = upgradesConfig.getUpgradeData(item);
            if (datos != null && "UPGRADE".equals(datos.getString("category")) && "STORAGE".equals(datos.getString("type"))) {
                bonus += datos.getInt("bonus_capacidad", 0);
            }
        }
        return base + bonus;
    }

    // ==========================================
    // 🪞 ESPEJO DE PROGRESO (Riqueza de la Isla)
    // ==========================================
    private void inyectarValorActividadIsla(int cantidadProducida) {
        if (cantidadProducida <= 0 || islandManager == null || collectionManager == null) return;

        CompletableFuture.runAsync(() -> {
            try {
                // 1. Buscamos el perfil de la isla por el Owner del minion
                IslandProfile perfilIsla = islandManager.getIslandByOwner(dna.ownerId());
                if (perfilIsla == null) return;

                // 2. Extraemos el valor monetario/colección del bloque que pica
                String materialName = dna.type().getTargetMaterial().name();
                int pdcPorUnidad = collectionManager.getItemPDCValue(materialName);

                // 3. MATEMÁTICA: 25% del total producido va a la isla
                long totalGenerado = (long) pdcPorUnidad * cantidadProducida;
                long diezmoActividad = (long) (totalGenerado * 0.25);

                // 4. Se lo inyectamos al Top Valor
                if (diezmoActividad > 0) {
                    perfilIsla.addValorActividad(diezmoActividad);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error en el Espejo de Progreso del Minion: " + e.getMessage());
            }
        }); // Usa hilo virtual global
    }

    // ==========================================
    // 🧠 MOTOR LÓGICO ASÍNCRONO (Virtual Threads)
    // ==========================================
    public void calcularTrabajoOffline(long currentTimeMillis) {
        if (currentTimeMillis <= dna.nextActionTime()) return;

        boolean modoInfinito = tieneMejoraActiva("AUTO_SELL") || tieneMejoraActiva("STORAGE_LINK");
        int maxStorage = getRealMaxStorage();

        if (dna.storedItems() >= maxStorage && !modoInfinito) return;

        long tiempoTranscurrido = currentTimeMillis - dna.nextActionTime();
        long tiempoPorCiclo = (long) (MinionTier.getDelayMillis(dna.tier()) * getSpeedMultiplier());

        if (tiempoPorCiclo <= 0) tiempoPorCiclo = 1000;

        int ciclosPosibles = (int) (tiempoTranscurrido / tiempoPorCiclo);
        if (ciclosPosibles <= 0) return;

        int itemsProducidos;

        if (modoInfinito) {
            itemsProducidos = ciclosPosibles;
        } else {
            int espacioLibre = maxStorage - dna.storedItems();
            itemsProducidos = Math.min(ciclosPosibles, espacioLibre);
        }

        // 🧬 Mutamos el ADN
        long nextTime = currentTimeMillis + (tiempoPorCiclo - (tiempoTranscurrido % tiempoPorCiclo));
        this.dna = this.dna.withUpdatedState(dna.storedItems() + itemsProducidos, nextTime);
        this.trabajosRealizados += itemsProducidos;

        // 🌟 ESPEJO DE PROGRESO: Sumar a la Isla por trabajo offline
        inyectarValorActividadIsla(itemsProducidos);

        consumirCombustiblesFisico();
        saveData();
    }

    public void tick(long currentTimeMillis) {
        // 🚦 1. EVALUAR SINDICATO
        evaluarEstadoLaboral();

        if (state == MinionState.ON_STRIKE) {
            despacharRenderizado(getRealMaxStorage(), false, false);
            return;
        }

        int maxStorage = getRealMaxStorage();
        boolean estaLleno = dna.storedItems() >= maxStorage;
        boolean tieneEnlaceCofre = tieneMejoraPorTipo("STORAGE_LINK");

        boolean debeTrabajar = (currentTimeMillis >= dna.nextActionTime()) && (!estaLleno || tieneEnlaceCofre);

        if (debeTrabajar) {
            long tiempoBase = MinionTier.getDelayMillis(dna.tier());
            double penalty = state == MinionState.FATIGUED ? 2.0 : 1.0;
            long nuevoTiempo = currentTimeMillis + (long) (tiempoBase * getSpeedMultiplier() * penalty);

            this.dna = this.dna.withUpdatedState(dna.storedItems(), nuevoTiempo);
        }

        despacharRenderizado(maxStorage, estaLleno, tieneEnlaceCofre);
    }

    private void evaluarEstadoLaboral() {
        if (state == MinionState.ON_STRIKE) return;

        double roll = Math.random();
        if (state == MinionState.WORKING) {
            if (roll < (0.01 / dna.fatigueResistance())) {
                state = MinionState.FATIGUED;
            }
        } else if (state == MinionState.FATIGUED) {
            if (roll < dna.strikeProbability()) {
                state = MinionState.ON_STRIKE;
            }
        }
    }

    // ==========================================
    // 🔨 EJECUCIÓN FÍSICA (Entity/Chunk Thread)
    // ==========================================
    private void despacharRenderizado(int maxStorage, boolean estaLleno, boolean tieneEnlaceCofre) {
        entity.getScheduler().run(plugin, scheduledTask -> {

            if (!entity.isValid() || entity.isDead()) {
                if (hitbox != null && hitbox.isValid()) hitbox.remove();
                if (holograma != null && holograma.isValid()) holograma.remove();
                minionManager.getMinionsActivos().remove(entity.getUniqueId());
                return;
            }

            actualizarHolograma(maxStorage, estaLleno, tieneEnlaceCofre);

            if (System.currentTimeMillis() >= dna.nextActionTime() && state != MinionState.ON_STRIKE) {
                if (!estaLleno || tieneEnlaceCofre) {
                    realizarTrabajoFisico();
                }
            }

            animarFisica();
        }, null);
    }

    private void realizarTrabajoFisico() {
        Location loc = entity.getLocation();
        boolean jugadorCerca = !loc.getNearbyPlayers(32).isEmpty();

        if (jugadorCerca) {
            loc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc.clone().add(0, 1, 0), 2, 0.2, 0.2, 0.2, 0.01);
            loc.getWorld().playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.3f, 1.5f);
        }

        boolean guardadoEnCofre = false;
        if (tieneMejoraPorTipo("STORAGE_LINK")) {
            guardadoEnCofre = guardarEnCofreAdyacenteFisico(new ItemStack(dna.type().getTargetMaterial(), 1));
        }

        if (!guardadoEnCofre) {
            var autoSellData = getMejoraActiva("AUTO_SELL");
            if (autoSellData != null) {
                double precio = autoSellData.getDouble("precio_por_unidad", 1.0);

                Player owner = Bukkit.getPlayer(dna.ownerId());
                if (owner != null && owner.isOnline()) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "eco give " + owner.getName() + " " + precio);
                    if (collectionManager != null) {
                        collectionManager.addProgress(owner, dna.type().getTargetMaterial().name(), 1);
                    }
                }

                // 🌟 ESPEJO DE PROGRESO (Autosell)
                inyectarValorActividadIsla(1);

                this.trabajosRealizados++;
                consumirCombustiblesFisico();
                return;
            }

            if (this.dna.storedItems() < getRealMaxStorage()) {
                this.dna = this.dna.withUpdatedState(this.dna.storedItems() + 1, this.dna.nextActionTime());

                // 🌟 ESPEJO DE PROGRESO (Normal)
                inyectarValorActividadIsla(1);
            }
        } else {
            // 🌟 ESPEJO DE PROGRESO (Cofre)
            inyectarValorActividadIsla(1);
        }

        this.trabajosRealizados++;
        consumirCombustiblesFisico();
    }

    private boolean guardarEnCofreAdyacenteFisico(ItemStack item) {
        long currentTime = System.currentTimeMillis();

        if (cachedStorage != null) {
            if (cachedStorage.getInventory().getLocation() != null &&
                    cachedStorage.getInventory().getLocation().getBlock().getState() instanceof InventoryHolder) {
                var sobrante = cachedStorage.getInventory().addItem(item);
                if (sobrante.isEmpty()) return true;
            }
            cachedStorage = null;
        }

        if (currentTime - lastStorageCheckTime > 10000) {
            lastStorageCheckTime = currentTime;
            int[][] offsets = {{1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}};

            for (int[] offset : offsets) {
                Block b = entity.getLocation().clone().add(offset[0], 0, offset[2]).getBlock();
                if (b.getState() instanceof InventoryHolder holder) {
                    var sobrante = holder.getInventory().addItem(item);
                    if (sobrante.isEmpty()) {
                        cachedStorage = holder;
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

        if (state == MinionState.ON_STRIKE) {
            holograma.text(crossplayUtils.parseCrossplay(null, "&#FF0000[!] ¡EN HUELGA!\n&#FFAA00Interactúa para negociar."));
            return;
        }

        if (state == MinionState.FATIGUED) {
            holograma.text(crossplayUtils.parseCrossplay(null, "&#FFAA00[Zzz] Trabajador Fatigado (50% Producción)"));
            return;
        }

        if (estaLleno && !tieneEnlaceCofre) {
            holograma.text(crossplayUtils.parseCrossplay(null, "&#FF5555[!] Inventario Lleno (" + dna.storedItems() + " / " + maxStorage + ")"));
        } else {
            String nombreBonito = dna.type().name().replace("MINION_", "").replace("_", " ");
            holograma.text(crossplayUtils.parseCrossplay(null, "&#FFAA00" + nombreBonito + " (Tier " + dna.tier() + ")\n&#E6CCFFÍtems: &#55FF55" + dna.storedItems() + " / " + maxStorage));
        }
    }

    private void animarFisica() {
        if (state == MinionState.ON_STRIKE) return;

        entity.setInterpolationDuration(20);
        entity.setInterpolationDelay(0);

        Transformation trans = entity.getTransformation();
        float nuevoAngulo = (System.currentTimeMillis() % 4000) / 4000f * (float) Math.PI * 2;
        trans.getLeftRotation().set(new AxisAngle4f(nuevoAngulo, new Vector3f(0, 1, 0)));
        entity.setTransformation(trans);
    }

    // ==========================================
    // ⚙️ UTILIDADES
    // ==========================================
    public double getSpeedMultiplier() {
        double multiplicador = dna.speedMutation();
        for (ItemStack item : upgrades) {
            if (item == null || item.isEmpty()) continue;
            var datos = upgradesConfig.getUpgradeData(item);
            if (datos != null && "FUEL".equals(datos.getString("category")) && "SPEED".equals(datos.getString("type"))) {
                multiplicador -= datos.getDouble("multiplier", 0.0);
            }
        }
        return Math.max(multiplicador, 0.1);
    }

    private void consumirCombustiblesFisico() {
        for (int i = 0; i < 4; i++) {
            ItemStack item = upgrades[i];
            if (item == null || item.isEmpty()) continue;

            var datos = upgradesConfig.getUpgradeData(item);
            if (datos != null && "FUEL".equals(datos.getString("category", ""))) {
                if (datos.getBoolean("unbreakable", false)) continue;

                int duracionSegundos = datos.getInt("duration", 600);
                if (duracionSegundos <= 0) continue;

                long tiempoPorTrabajo = (long) (MinionTier.getDelayMillis(dna.tier()) * getSpeedMultiplier());
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
            var datos = upgradesConfig.getUpgradeData(item);
            if (datos != null && datos.getString("type", "").equals(tipoBuscado)) return datos;
        }
        return null;
    }

    public boolean tieneMejoraPorTipo(String tipoBuscado) { return getMejoraActiva(tipoBuscado) != null; }
    public boolean tieneMejoraActiva(String tipoBuscado) { return getMejoraActiva(tipoBuscado) != null; }

    public MinionDNA getDna() { return dna; }

    public void setDna(MinionDNA nuevoDna) {
        this.dna = nuevoDna;
        this.saveData();
    }

    public MinionState getState() { return state; }

    public void cureFatigue() { this.state = MinionState.WORKING; }

    public ItemStack[] getUpgrades() { return upgrades; }

    public void setUpgrade(int slot, ItemStack item) {
        upgrades[slot] = item;
        if (item == null || item.isEmpty()) {
            entity.getPersistentDataContainer().remove(MinionKeys.UPGRADES[slot]);
        } else {
            entity.getPersistentDataContainer().set(MinionKeys.UPGRADES[slot], PersistentDataType.BYTE_ARRAY, item.serializeAsBytes());
        }
    }

    public ItemDisplay getEntity() { return entity; }
    public Interaction getHitbox() { return hitbox; }
    public TextDisplay getHolograma() { return holograma; }

    public void saveData() {
        if (entity == null || !entity.isValid()) return;

        entity.getPersistentDataContainer().set(MinionKeys.DNA_KEY, MinionKeys.DNA_TYPE, this.dna);

        var pdc = entity.getPersistentDataContainer();
        for (int i = 0; i < 4; i++) {
            if (upgrades[i] != null && !upgrades[i].isEmpty()) {
                pdc.set(MinionKeys.UPGRADES[i], PersistentDataType.BYTE_ARRAY, upgrades[i].serializeAsBytes());
            } else {
                pdc.remove(MinionKeys.UPGRADES[i]);
            }
        }
    }
}