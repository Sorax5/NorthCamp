package fr.phylisiumstudio.app.inject;
import com.google.gson.GsonBuilder;
import com.google.inject.*;
import fr.phylisiumstudio.app.App;
import fr.phylisiumstudio.app.config.MainConfig;
import fr.phylisiumstudio.app.inject.annotation.CampsiteRepositoryFile;
import fr.phylisiumstudio.logic.repository.ICampsiteRepository;
import fr.phylisiumstudio.storage.JsonCampsiteRepository;
import net.minestom.server.MinecraftServer;
import net.minestom.server.instance.InstanceManager;

import java.io.File;

/**
 * AppModule is a Guice module that configures dependency injection for the application.
 * It provides various dependencies required throughout the app.
 */
public class AppModule extends AbstractModule {

    private final App app;

    public AppModule(App app) {
        this.app = app;
    }

    @Override
    protected void configure() {
        super.configure();

        bind(ICampsiteRepository.class).to(JsonCampsiteRepository.class).in(Singleton.class);
        bind(MinecraftServer.class).toInstance(app.getServer());
        bind(InstanceManager.class).toInstance(app.getInstanceManager());
        bind(App.class).toInstance(app);
        bind(MainConfig.class).toInstance(app.getMainConfig());
        bind(GsonBuilder.class).toInstance(app.getGsonBuilder());
    }

    public Injector getInjector() {
        return Guice.createInjector(this);
    }

    @Provides
    @Singleton
    @CampsiteRepositoryFile
    public File ProvideCampsiteRepositoryFile() {
        return new File(app.getDataFolder(), "campsites");
    }
}
