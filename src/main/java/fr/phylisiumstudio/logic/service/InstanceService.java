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

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Singleton
public class InstanceService {
    private final InstanceManager instanceManager;
    private final App app;
    private final BuilderService builderService;
    private Map<UUID, InstanceContainer> instances = new HashMap<>();

    private File instanceFolder;

    @Inject
    public InstanceService(InstanceManager instanceManager, App app, BuilderService builderService) {
        this.builderService = builderService;
        this.instanceManager = instanceManager;
        this.app = app;
        this.instanceFolder = new File(app.getDataFolder(), "instance");
        if (!instanceFolder.exists() && !instanceFolder.mkdirs()) {
            app.getLogger().warning("Failed to create instance folder: " + instanceFolder.getAbsolutePath());
        }
    }

    public InstanceContainer getInstance(Campsite campsite) {
        try {
            InstanceContainer instanceContainer = instances.get(campsite.getUniqueID());
            if (instanceContainer == null) {
                instanceContainer = instanceManager.createInstanceContainer();
                instanceContainer.setChunkLoader(new AnvilLoader(this.instanceFolder.getAbsolutePath()));
                instances.put(campsite.getUniqueID(), instanceContainer);

                List<CompletableFuture<Chunk>> futures = new ArrayList<>();
                for (int x = 0; x < 15; x++) {
                    for (int z = 0; z < 15; z++) {
                        futures.add(instanceContainer.loadChunk(x, z));
                    }
                }

                Instant now = Instant.now();
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
                Instant after = Instant.now();
                Duration duration = Duration.between(now, after);
                app.getLogger().info("Loaded " + futures.size() + " chunks in " + duration.toMillis() + " ms for instance " + campsite.getUniqueID());

                CompletableFuture<Void> future = builderService.BuildCampsiteAsync(campsite, instanceContainer);
                future.join();
            }

            return instanceContainer;
        }
        catch (Exception e) {
            app.getLogger().severe("Failed to get or create instance for campsite " + campsite.getUniqueID() + ": " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public boolean IsLinked(Campsite campsite, Instance instance) {
        InstanceContainer linkedInstance = instances.get(campsite.getUniqueID());
        return linkedInstance != null && linkedInstance.getUuid().equals(instance.getUuid());
    }
}
