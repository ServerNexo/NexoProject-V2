package me.aeroxis.minions.manager;

import me.aeroxis.core.api.AeroxisFactoriesAPI;
import me.aeroxis.core.user.AeroxisAPI;
import me.aeroxis.colecciones.colecciones.CollectionManager;
import me.aeroxis.core.crossplay.CrossplayUtils;
import me.aeroxis.islas.data.IslandProfile;
import me.aeroxis.islas.managers.IslandLevelEngine;
import me.aeroxis.islas.managers.IslandManager;
import me.aeroxis.minions.AeroxisMinions;
import me.aeroxis.minions.data.MinionDNA;
import me.aeroxis.minions.data.MinionKeys;
import me.aeroxis.minions.data.MinionTier;
import me.aeroxis.minions.data.UpgradesConfig;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
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

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 🤖 AeroxisMinions - Modelo de Minion Activo (Omni-Minion Phase)
 * Rendimiento: Híbrido, Inyección RPG (AuraSkills), NBT EMF Fishing Hacking, Enrutamiento Wi-Fi y XP Individual.
 */
public class ActiveMinion {

    public enum MinionState { WORKING, FATIGUED, ON_STRIKE }

    private final AeroxisMinions plugin;
    private final ItemDisplay entity;
    private final Interaction hitbox;
    private final TextDisplay holograma;

    private final UpgradesConfig upgradesConfig;
    private final MinionManager minionManager;
    private final CrossplayUtils crossplayUtils;
    private final CollectionManager collectionManager;

    private final IslandManager islandManager;
    private final IslandLevelEngine islandLevelEngine;

    private volatile MinionDNA dna;
    private volatile MinionState state = MinionState.WORKING;

    private volatile UUID targetLinkId = null;
    private final NamespacedKey targetLinkKey;

    private volatile double unclaimedXp = 0.0;
    private final NamespacedKey unclaimedXpKey;

    private final ItemStack[] upgrades = new ItemStack[4];
    private int trabajosRealizados = 0;

    private InventoryHolder cachedStorage = null;
    private long lastStorageCheckTime = 0;

    public ActiveMinion(AeroxisMinions plugin, ItemDisplay entity, Interaction hitbox, TextDisplay holograma,
                        MinionDNA initialDna, UpgradesConfig upgradesConfig, MinionManager minionManager,
                        CrossplayUtils crossplayUtils, CollectionManager collectionManager,
                        IslandManager islandManager, IslandLevelEngine islandLevelEngine) {
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
        this.islandLevelEngine = islandLevelEngine;

        this.targetLinkKey = new NamespacedKey(plugin, "target_link");
        this.unclaimedXpKey = new NamespacedKey(plugin, "unclaimed_xp");

        for (int i = 0; i < 4; i++) {
            byte[] bytes = entity.getPersistentDataContainer().get(MinionKeys.UPGRADES[i], PersistentDataType.BYTE_ARRAY);
            if (bytes != null) this.upgrades[i] = ItemStack.deserializeBytes(bytes);
        }

        String linkStr = entity.getPersistentDataContainer().get(targetLinkKey, PersistentDataType.STRING);
        if (linkStr != null) this.targetLinkId = UUID.fromString(linkStr);

        Double savedXp = entity.getPersistentDataContainer().get(unclaimedXpKey, PersistentDataType.DOUBLE);
        if (savedXp != null) this.unclaimedXp = savedXp;
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

    private void acumularValorIsla(int cantidadProducida, ItemStack producedItem) {
        if (cantidadProducida <= 0 || islandLevelEngine == null) return;

        CompletableFuture.runAsync(() -> {
            try {
                String productionId = dna.currentProductionId();
                double xpPorUnidad = 0.0;

                if (productionId.equals("EMF_FISH") && producedItem != null) {
                    xpPorUnidad = islandLevelEngine.getFishXp(producedItem);
                    if (xpPorUnidad <= 0) xpPorUnidad = 15.0;
                } else {
                    xpPorUnidad = islandLevelEngine.getBlockXp(productionId);
                    if (xpPorUnidad <= 0) xpPorUnidad = islandLevelEngine.getMobXp(productionId);
                }

                double totalGenerado = xpPorUnidad * cantidadProducida;
                double diezmoActividad = totalGenerado * 0.25;

                if (diezmoActividad > 0) {
                    this.unclaimedXp += diezmoActividad;
                }

                Player owner = Bukkit.getPlayer(dna.ownerId());
                if (owner != null && owner.isOnline()) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        try {
                            dev.aurelium.auraskills.api.AuraSkillsApi auraApi = dev.aurelium.auraskills.api.AuraSkillsApi.get();
                            dev.aurelium.auraskills.api.user.SkillsUser user = auraApi.getUser(owner.getUniqueId());

                            if (user != null) {
                                dev.aurelium.auraskills.api.skill.Skill skill = dev.aurelium.auraskills.api.skill.Skills.MINING;

                                if (productionId.equals("EMF_FISH")) {
                                    skill = dev.aurelium.auraskills.api.skill.Skills.FISHING;
                                } else if (productionId.contains("LOG") || productionId.contains("WOOD")) {
                                    skill = dev.aurelium.auraskills.api.skill.Skills.FORAGING;
                                } else if (productionId.contains("WHEAT") || productionId.contains("CARROT") || productionId.contains("POTATO") || productionId.contains("CANE") || productionId.contains("BEETROOT")) {
                                    skill = dev.aurelium.auraskills.api.skill.Skills.FARMING;
                                }

                                double xpAmount = cantidadProducida * 0.5;
                                user.addSkillXp(skill, xpAmount);
                            }
                        } catch (Throwable ignored) {}
                    });
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error acumulando XP del Minion: " + e.getMessage());
            }
        });
    }

    public double getUnclaimedXp() { return unclaimedXp; }

    public void reclamarNivelIsla(Player player) {
        if (this.unclaimedXp <= 0) {
            crossplayUtils.sendMessage(player, "&#FF5555[x] El Minion aún no ha generado valor de isla suficiente.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        IslandProfile perfilIsla = islandManager.getIslandByOwner(dna.ownerId());
        if (perfilIsla != null) {
            islandLevelEngine.addXp(perfilIsla, player.getUniqueId(), this.unclaimedXp);
            islandManager.saveIslandProfileAsync(perfilIsla);

            crossplayUtils.sendMessage(player, "&#55FF55[✓] <bold>¡VALOR RECLAMADO!</bold> &#E6CCFFHas sumado &#FFAA00" + String.format("%.1f", this.unclaimedXp) + " &#E6CCFFpuntos de valor a tu isla.");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);

            this.unclaimedXp = 0.0;
            saveData();
        } else {
            crossplayUtils.sendMessage(player, "&#FF5555[x] Hubo un error de sincronización con tu isla.");
        }
    }

    public void calcularTrabajoOffline(long currentTimeMillis) {
        if (currentTimeMillis <= dna.nextActionTime()) return;

        boolean modoInfinito = tieneMejoraActiva("AUTO_SELL") || tieneMejoraActiva("STORAGE_LINK") || this.targetLinkId != null;
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

        long nextTime = currentTimeMillis + (tiempoPorCiclo - (tiempoTranscurrido % tiempoPorCiclo));

        ItemStack dummyItem = dna.currentProductionId().equals("EMF_FISH") ? new ItemStack(Material.COD) : new ItemStack(Material.valueOf(dna.currentProductionId()));

        if (this.targetLinkId != null) {
            AeroxisAPI.getInstance().getServiceManager().get(AeroxisFactoriesAPI.class).ifPresent(api -> {
                try {
                    ItemStack clone = dummyItem.clone();
                    clone.setAmount(itemsProducidos);
                    api.routeItem(entity.getLocation(), this.targetLinkId, clone);
                } catch (Exception ignored) {}
            });
            this.dna = this.dna.withUpdatedState(dna.storedItems(), nextTime);
        } else {
            this.dna = this.dna.withUpdatedState(dna.storedItems() + itemsProducidos, nextTime);
        }

        this.trabajosRealizados += itemsProducidos;
        acumularValorIsla(itemsProducidos, dummyItem);
        consumirCombustiblesFisico();
        saveData();
    }

    public void tick(long currentTimeMillis) {
        evaluarEstadoLaboral();

        if (state == MinionState.ON_STRIKE) {
            despacharRenderizado(getRealMaxStorage(), false, false);
            return;
        }

        int maxStorage = getRealMaxStorage();
        boolean estaLleno = dna.storedItems() >= maxStorage;
        boolean tieneEnlaceCofre = tieneMejoraPorTipo("STORAGE_LINK") || this.targetLinkId != null;

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
            if (roll < (0.01 / dna.fatigueResistance())) state = MinionState.FATIGUED;
        } else if (state == MinionState.FATIGUED) {
            if (roll < dna.strikeProbability()) state = MinionState.ON_STRIKE;
        }
    }

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
                if (!estaLleno || tieneEnlaceCofre) realizarTrabajoFisico();
            }

            animarFisica();
        }, null);
    }

    private void realizarTrabajoFisico() {
        Location loc = entity.getLocation();
        if (!loc.getNearbyPlayers(32).isEmpty()) {
            loc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc.clone().add(0, 1, 0), 2, 0.2, 0.2, 0.2, 0.01);
            loc.getWorld().playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.3f, 1.5f);
        }

        ItemStack itemAEnviar;

        // 🌟 FIX NBT HACKING: Generamos un pez compatible con CUALQUIER versión de EMF
        if (dna.currentProductionId().equals("EMF_FISH")) {
            itemAEnviar = new ItemStack(Material.COD);
            itemAEnviar.editMeta(meta -> {
                // Etiquetamos el pez manualmente con las llaves universales de EvenMoreFish
                NamespacedKey isFishKey = new NamespacedKey("evenmorefish", "emf-fish-name");
                NamespacedKey rarityKey = new NamespacedKey("evenmorefish", "emf-fish-rarity");
                NamespacedKey lengthKey = new NamespacedKey("evenmorefish", "emf-fish-length");

                meta.getPersistentDataContainer().set(isFishKey, PersistentDataType.STRING, "Minion Fish");
                meta.getPersistentDataContainer().set(rarityKey, PersistentDataType.STRING, "Common");
                meta.getPersistentDataContainer().set(lengthKey, PersistentDataType.DOUBLE, 15.5);

                meta.displayName(crossplayUtils.parseCrossplay(null, "&#55FF55Pez del Nexo"));
            });
        } else {
            Material matOutput = Material.COBBLESTONE;
            try { matOutput = Material.valueOf(dna.currentProductionId()); } catch (Exception ignored) {}
            itemAEnviar = new ItemStack(matOutput, 1);
        }

        if (this.targetLinkId != null) {
            ItemStack finalItemAEnviar = itemAEnviar;
            AeroxisAPI.getInstance().getServiceManager().get(AeroxisFactoriesAPI.class).ifPresent(api -> {
                api.routeItem(loc, this.targetLinkId, finalItemAEnviar);
            });

            acumularValorIsla(1, itemAEnviar);
            this.trabajosRealizados++;
            consumirCombustiblesFisico();
            return;
        }

        boolean guardadoEnCofre = false;
        if (tieneMejoraPorTipo("STORAGE_LINK")) {
            guardadoEnCofre = guardarEnCofreAdyacenteFisico(itemAEnviar);
        }

        if (!guardadoEnCofre) {
            var autoSellData = getMejoraActiva("AUTO_SELL");
            if (autoSellData != null) {
                double precio = autoSellData.getDouble("precio_por_unidad", 1.0);
                Player owner = Bukkit.getPlayer(dna.ownerId());
                if (owner != null && owner.isOnline()) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "eco give " + owner.getName() + " " + precio);
                    if (collectionManager != null) collectionManager.addCollectionProgress(owner.getUniqueId(), dna.currentProductionId(), 1);
                }
                acumularValorIsla(1, itemAEnviar);
                this.trabajosRealizados++;
                consumirCombustiblesFisico();
                return;
            }

            if (this.dna.storedItems() < getRealMaxStorage()) {
                this.dna = this.dna.withUpdatedState(this.dna.storedItems() + 1, this.dna.nextActionTime());
                acumularValorIsla(1, itemAEnviar);
            }
        } else {
            acumularValorIsla(1, itemAEnviar);
        }

        this.trabajosRealizados++;
        consumirCombustiblesFisico();
    }

    public void changeProduction(String newProductionId) {
        this.dna = this.dna.withUpdatedProduction(newProductionId);
        saveData();

        entity.getScheduler().run(plugin, scheduledTask -> {
            try {
                Material mat = newProductionId.equals("EMF_FISH") ? Material.COD : Material.valueOf(newProductionId);
                entity.setItemStack(new ItemStack(mat));
                entity.getWorld().spawnParticle(org.bukkit.Particle.TOTEM_OF_UNDYING, entity.getLocation().add(0, 1, 0), 40, 0.4, 0.4, 0.4, 0.2);
                entity.getWorld().playSound(entity.getLocation(), org.bukkit.Sound.BLOCK_BEACON_POWER_SELECT, 1f, 2f);
                actualizarHolograma(getRealMaxStorage(), false, false);
            } catch (Exception ignored) {}
        }, null);
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

    private void actualizarHolograma(int maxStorage, boolean estaLleno, boolean tieneEnlace) {
        if (holograma == null || holograma.isDead()) return;

        if (state == MinionState.ON_STRIKE) {
            holograma.text(crossplayUtils.parseCrossplay(null, "&#FF0000[!] ¡EN HUELGA!\n&#FFAA00Interactúa para negociar."));
            return;
        }

        if (state == MinionState.FATIGUED) {
            holograma.text(crossplayUtils.parseCrossplay(null, "&#FFAA00[Zzz] Trabajador Fatigado (50% Producción)"));
            return;
        }

        if (estaLleno && !tieneEnlace) {
            holograma.text(crossplayUtils.parseCrossplay(null, "&#FF5555[!] Inventario Lleno (" + dna.storedItems() + " / " + maxStorage + ")"));
        } else {
            String nombreBonito = dna.currentProductionId().replace("_", " ");
            holograma.text(crossplayUtils.parseCrossplay(null, "&#00f5ff" + nombreBonito + " (Tier " + dna.tier() + ")\n&#E6CCFFÍtems: &#55FF55" + dna.storedItems() + " / " + maxStorage));
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
    public void setDna(MinionDNA nuevoDna) { this.dna = nuevoDna; this.saveData(); }
    public MinionState getState() { return state; }
    public void cureFatigue() { this.state = MinionState.WORKING; }
    public ItemStack[] getUpgrades() { return upgrades; }

    public UUID getTargetLinkId() { return targetLinkId; }
    public void setTargetLinkId(UUID targetLinkId) {
        this.targetLinkId = targetLinkId;
        saveData();
    }

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

        var pdc = entity.getPersistentDataContainer();
        pdc.set(MinionKeys.DNA_KEY, MinionKeys.DNA_TYPE, this.dna);

        for (int i = 0; i < 4; i++) {
            if (upgrades[i] != null && !upgrades[i].isEmpty()) {
                pdc.set(MinionKeys.UPGRADES[i], PersistentDataType.BYTE_ARRAY, upgrades[i].serializeAsBytes());
            } else {
                pdc.remove(MinionKeys.UPGRADES[i]);
            }
        }

        if (targetLinkId != null) {
            pdc.set(targetLinkKey, PersistentDataType.STRING, targetLinkId.toString());
        } else {
            pdc.remove(targetLinkKey);
        }

        // 🌟 Guardar la XP acumulada
        if (unclaimedXp > 0) {
            pdc.set(unclaimedXpKey, PersistentDataType.DOUBLE, unclaimedXp);
        } else {
            pdc.remove(unclaimedXpKey);
        }
    }
}