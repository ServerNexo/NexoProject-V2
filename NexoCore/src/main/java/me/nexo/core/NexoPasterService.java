package me.nexo.core;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.api.schematics.NexoSchematic;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 🚀 NexoPaster - Motor de Pegado Procedural AAA (Sustituto de FAWE y NBT)
 * Rendimiento: Particionado de Chunks Matemático + Inyección Nativa en RegionScheduler.
 */
@Singleton
public class NexoPasterService {

    private final JavaPlugin plugin;

    // 🧵 Hilos Virtuales Nativos de Java 21 para Operaciones de Disco futuras
    private final ExecutorService virtualExecutor;
    private final File templatesFolder;

    @Inject
    public NexoPasterService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();

        // 📁 Aseguramos que el directorio de plantillas exista
        this.templatesFolder = new File(plugin.getDataFolder(), "templates");
        if (!this.templatesFolder.exists()) {
            this.templatesFolder.mkdirs();
        }
    }

    /**
     * 🏗️ Pega una estructura gigante de forma segura dividiéndola por Chunks (Folia-Ready).
     * @param schematic La plantilla cacheada en RAM.
     * @param center Ubicación central donde aparecerá la estructura.
     * @return Future que se completa cuando TODOS los hilos han terminado de pegar sus chunks.
     */
    public CompletableFuture<Void> pasteAsynchronously(NexoSchematic schematic, Location center) {
        int width = schematic.width();
        int height = schematic.height();
        int length = schematic.length();

        // Calculamos la esquina inferior izquierda (Offset)
        int startX = center.getBlockX() - (width / 2);
        int startY = center.getBlockY() - (height / 2);
        int startZ = center.getBlockZ() - (length / 2);

        World world = center.getWorld();
        if (world == null) return CompletableFuture.completedFuture(null);

        // ==========================================
        // 🌟 FASE 1: PARTICIONADO MATEMÁTICO
        // Agrupamos los bloques en listas según a qué Chunk pertenecen.
        // ==========================================
        Map<Long, List<BlockOperation>> operationsByChunk = new HashMap<>();

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < length; z++) {
                    int index = schematic.getIndex(x, y, z);
                    BlockData data = schematic.blocks()[index];

                    // Solo registramos si es un bloque válido y no es aire (para no borrar terreno sin querer)
                    if (data != null && !data.getMaterial().isAir()) {
                        int worldX = startX + x;
                        int worldY = startY + y;
                        int worldZ = startZ + z;

                        long chunkKey = getChunkKey(worldX >> 4, worldZ >> 4);
                        operationsByChunk.computeIfAbsent(chunkKey, k -> new ArrayList<>())
                                .add(new BlockOperation(worldX, worldY, worldZ, data));
                    }
                }
            }
        }

        // ==========================================
        // 🌟 FASE 2: INYECCIÓN DISTRIBUIDA (Folia RegionSchedulers)
        // Despachamos las tareas a los hilos dueños de cada Chunk en paralelo.
        // ==========================================
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (Map.Entry<Long, List<BlockOperation>> entry : operationsByChunk.entrySet()) {
            long chunkKey = entry.getKey();
            List<BlockOperation> operations = entry.getValue();

            int chunkX = (int) chunkKey;
            int chunkZ = (int) (chunkKey >>> 32);

            CompletableFuture<Void> chunkFuture = new CompletableFuture<>();
            futures.add(chunkFuture);

            // ⚠️ Ejecutamos EXACTAMENTE en el hilo que controla este Chunk
            Bukkit.getRegionScheduler().execute(plugin, world, chunkX, chunkZ, () -> {
                for (BlockOperation op : operations) {
                    // applyPhysics = false evita actualizaciones de vecinos y caídas de TPS
                    world.getBlockAt(op.x(), op.y(), op.z()).setBlockData(op.data(), false);
                }
                chunkFuture.complete(null);
            });
        }

        // Devolvemos una promesa que se cumple solo cuando el último Chunk termina de pegarse
        // ✅ FIX: Usamos <?> para evitar el Raw Type Warning
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0]));
    }

    // ==========================================
    // 🧮 UTILIDADES Y RECORDS INTERNOS
    // ==========================================

    /**
     * Comprime X y Z de un Chunk en un Long ultra-rápido para usarlo como llave en el HashMap.
     */
    private long getChunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX & 0xFFFFFFFFL) | (((long) chunkZ & 0xFFFFFFFFL) << 32);
    }

    /**
     * Tarea mínima para colocar un bloque en el mundo físico.
     */
    private record BlockOperation(int x, int y, int z, BlockData data) {}
}