package fr.phylisiumstudio.logic.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import fr.phylisiumstudio.app.App;
import fr.phylisiumstudio.logic.Campsite;
import net.minestom.server.instance.*;
import net.minestom.server.instance.anvil.AnvilLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class InstanceService {
    private static final Logger logger = LoggerFactory.getLogger(InstanceService.class);

    /** Marge de chunks autour des éléments pour couvrir les schématics à cheval. */
    private static final int CHUNK_MARGIN = 2;

    private final InstanceManager instanceManager;
    private final CampsiteBuilderService campsiteBuilderService;
    private final int chunkRadius;
    private final File templateFolder;

    private final ConcurrentHashMap<UUID, CompletableFuture<InstanceContainer>> instances = new ConcurrentHashMap<>();

    @Inject
    public InstanceService(InstanceManager instanceManager, App app, CampsiteBuilderService campsiteBuilderService) {
        this.campsiteBuilderService = campsiteBuilderService;
        this.instanceManager = instanceManager;
        this.templateFolder = new File(app.getDataFolder(), "instance");

        var config = app.getMainConfig();
        this.chunkRadius = (config != null) ? config.ChunkRadius : 13;

        if (!templateFolder.exists() && !templateFolder.mkdirs()) {
            logger.warn("Failed to create template folder: {}", templateFolder.getAbsolutePath());
        }
    }

    public CompletableFuture<InstanceContainer> getInstanceAsync(Campsite campsite) {
        return instances.computeIfAbsent(campsite.getUniqueID(), _ -> createInstanceAsync(campsite));
    }

    public InstanceContainer getInstance(Campsite campsite) {
        return getInstanceAsync(campsite).join();
    }

    /**
     * Construit l'instance de façon entièrement asynchrone : préparation du conteneur,
     * chargement des chunks, éclairage, puis pose des schématics — chaque étape chaînée
     * sans blocage de thread ({@code join}) pour préserver les TPS.
     */
    private CompletableFuture<InstanceContainer> createInstanceAsync(Campsite campsite) {
        var start = Instant.now();
        var container = prepareContainer();
        var range = ChunkRange.forCampsite(campsite, CHUNK_MARGIN, chunkRadius);

        return loadChunks(container, range)
                .thenRun(() -> {
                    LightingChunk.relight(container, container.getChunks());
                    logger.info("Loaded {} chunks in {} ms for campsite {}",
                            range.count(), Duration.between(start, Instant.now()).toMillis(), campsite.getUniqueID());
                })
                .thenCompose(_ -> campsiteBuilderService.BuildCampsiteAsync(campsite, container))
                .thenApply(_ -> {
                    logger.info("Instance ready for campsite {}", campsite.getUniqueID());
                    return container;
                })
                .exceptionally(ex -> {
                    instances.remove(campsite.getUniqueID());
                    logger.error("Failed to create instance for campsite {}: {}",
                            campsite.getUniqueID(), ex.getMessage(), ex);
                    throw new RuntimeException(ex);
                });
    }

    private InstanceContainer prepareContainer() {
        var container = instanceManager.createInstanceContainer();
        // World template partagé en lecture seule
        container.setChunkLoader(new AnvilLoader(templateFolder.toPath()));
        container.setChunkSupplier(LightingChunk::new);
        return container;
    }

    private CompletableFuture<Void> loadChunks(InstanceContainer container, ChunkRange range) {
        var futures = new ArrayList<CompletableFuture<Chunk>>(range.count());
        for (int x = range.fromX(); x <= range.toX(); x++) {
            for (int z = range.fromZ(); z <= range.toZ(); z++) {
                futures.add(container.loadChunk(x, z));
            }
        }
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    public boolean isLinked(Campsite campsite, Instance instance) {
        var future = instances.get(campsite.getUniqueID());
        if (future == null || !future.isDone()) {
            return false;
        }
        var linkedInstance = future.join();
        return linkedInstance.getUuid().equals(instance.getUuid());
    }

    public void releaseInstance(Campsite campsite) {
        var future = instances.remove(campsite.getUniqueID());
        if (future == null || !future.isDone()) {
            return;
        }
        var container = future.join();
        instanceManager.unregisterInstance(container);
        logger.info("Released ephemeral instance for campsite {}", campsite.getUniqueID());
    }

    public void shutdown() {
        logger.info("Shutting down InstanceService: releasing {} ephemeral instances...", instances.size());

        for (var entry : instances.entrySet()) {
            var future = entry.getValue();
            if (!future.isDone() || future.isCompletedExceptionally()) {
                continue;
            }
            try {
                var container = future.join();
                instanceManager.unregisterInstance(container);
                logger.info("Released instance for campsite {}", entry.getKey());
            } catch (Exception e) {
                logger.error("Error releasing instance for campsite {}: {}", entry.getKey(), e.getMessage(), e);
            }
        }

        instances.clear();
        logger.info("InstanceService shutdown complete.");
    }
}
