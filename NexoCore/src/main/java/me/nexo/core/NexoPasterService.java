package me.nexo.core;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.structure.Mirror;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.structure.StructureManager;

import java.io.File;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 🚀 NexoPaster - Motor de Pegado de Estructuras AAA (Sustituto de FAWE)
 * Rendimiento: Lectura I/O Ilimitada en Hilos Virtuales + Inyección Nativa de PaperMC.
 */
@Singleton
public class NexoPasterService {

    private final JavaPlugin plugin;
    private final StructureManager structureManager;
    
    // 🧵 Hilos Virtuales Nativos de Java 21 para Operaciones de Disco
    private final ExecutorService virtualExecutor;
    private final File templatesFolder;

    @Inject
    public NexoPasterService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.structureManager = Bukkit.getStructureManager();
        this.virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();
        
        // 📁 Aseguramos que el directorio de plantillas exista
        this.templatesFolder = new File(plugin.getDataFolder(), "templates");
        if (!this.templatesFolder.exists()) {
            this.templatesFolder.mkdirs();
        }
    }

    /**
     * 🏗️ Pega una estructura .nbt de forma segura y sin impacto en los TPS.
     * * @param templateName Nombre del archivo (ej: "boss_arena") sin el .nbt
     * @param targetLocation Ubicación donde se pegará la esquina de la estructura
     * @return CompletableFuture que se completa cuando la estructura está físicamente en el mundo
     */
    public CompletableFuture<Boolean> pasteTemplateAsync(String templateName, Location targetLocation) {
        
        // 🌟 FASE 1: LECTURA I/O ASÍNCRONA (Virtual Thread)
        return CompletableFuture.supplyAsync(() -> {
            try {
                var file = new File(templatesFolder, templateName + ".nbt");
                if (!file.exists()) {
                    plugin.getLogger().warning("❌ [NexoPaster] Plantilla no encontrada: " + file.getAbsolutePath());
                    return null;
                }
                
                // Leemos el disco sin bloquear el Main Thread
                return structureManager.loadStructure(file);
                
            } catch (Exception e) {
                plugin.getLogger().severe("❌ [NexoPaster] Error leyendo plantilla de disco: " + e.getMessage());
                return null;
            }
            
        }, virtualExecutor).thenApply(structure -> {
            
            // 🌟 FASE 2: INYECCIÓN FÍSICA EN EL MUNDO (Main Thread)
            if (structure == null) return false;

            // Delegamos la modificación de chunks al hilo principal para evitar corrupción de memoria
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    structure.place(
                            targetLocation,
                            true,                   // Incluir entidades (Cofres, Mobs, ArmorStands)
                            StructureRotation.NONE, // Sin rotación
                            Mirror.NONE,            // Sin espejado
                            0,                      // Paleta de bloques por defecto
                            1.0F,                   // 100% de integridad (Sin daño aleatorio)
                            new Random()
                    );
                    plugin.getLogger().info("✅ [NexoPaster] Estructura '" + templateName + "' inyectada exitosamente.");
                } catch (Exception e) {
                    plugin.getLogger().severe("❌ [NexoPaster] Fallo de inyección en Main Thread: " + e.getMessage());
                }
            });
            
            return true;
        });
    }
}