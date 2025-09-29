package fr.phylisiumstudio.logic.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import fr.phylisiumstudio.app.App;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.InstanceManager;
import net.minestom.server.instance.anvil.AnvilLoader;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Singleton
public class InstanceService {
    private final InstanceManager instanceManager;
    private final App app;
    private Map<UUID, InstanceContainer> instances = new HashMap<>();

    private File instanceFolder;

    @Inject
    public InstanceService(InstanceManager instanceManager, App app) {
        this.instanceManager = instanceManager;
        this.app = app;
        this.instanceFolder = new File(app.getDataFolder(), "instance");
        if (!instanceFolder.exists() && !instanceFolder.mkdirs()) {
            app.getLogger().warning("Failed to create instance folder: " + instanceFolder.getAbsolutePath());
        }
    }

    public InstanceContainer getInstance(UUID id) {
        InstanceContainer instanceContainer = instances.get(id);
        if (instanceContainer == null) {
            instanceContainer = instanceManager.createInstanceContainer();
            instanceContainer.setChunkLoader(new AnvilLoader(this.instanceFolder.getAbsolutePath()));
            instances.put(id, instanceContainer);
        }

        return instanceContainer;
    }
}
