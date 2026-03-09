package fr.phylisiumstudio.logic.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import fr.phylisiumstudio.app.App;
import fr.phylisiumstudio.logic.Campsite;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.InstanceManager;
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

    private final InstanceManager instanceManager;
    private final BuilderService builderService;
    private final int chunkRadius;

    private final ConcurrentHashMap<UUID, CompletableFuture<InstanceContainer>> instances = new ConcurrentHashMap<>();

    private final File templateFolder;

    @Inject
    public InstanceService(InstanceManager instanceManager, App app, BuilderService builderService) {
        this.builderService = builderService;
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

    private CompletableFuture<InstanceContainer> createInstanceAsync(Campsite campsite) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var instanceContainer = instanceManager.createInstanceContainer();
                // Chargement depuis le world template partagé (lecture seule)
                var loader = new AnvilLoader(templateFolder.toPath());
                instanceContainer.setChunkLoader(loader);

                var now = Instant.now();

                var futures = new ArrayList<CompletableFuture<Chunk>>();
                var halfRadius = chunkRadius / 2;
                for (var x = -halfRadius; x <= halfRadius; x++) {
                    for (var z = -halfRadius; z <= halfRadius; z++) {
                        futures.add(instanceContainer.loadChunk(x, z));
                    }
                }

                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

                var duration = Duration.between(now, Instant.now());
                logger.info("Loaded {} chunks in {} ms for campsite {}", futures.size(), duration.toMillis(), campsite.getUniqueID());

                builderService.BuildCampsiteAsync(campsite, instanceContainer).join();

                logger.info("Instance ready for campsite {}", campsite.getUniqueID());
                return instanceContainer;
            } catch (Exception e) {
                instances.remove(campsite.getUniqueID());
                logger.error("Failed to create instance for campsite {}: {}", campsite.getUniqueID(), e.getMessage(), e);
                throw new RuntimeException(e);
            }
        });
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
