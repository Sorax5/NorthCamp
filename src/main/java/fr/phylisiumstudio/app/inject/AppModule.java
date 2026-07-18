package fr.phylisiumstudio.app.inject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.*;
import fr.phylisiumstudio.app.App;
import fr.phylisiumstudio.app.config.MainConfig;
import fr.phylisiumstudio.app.inject.annotation.CampsiteRepositoryFile;
import fr.phylisiumstudio.app.inject.annotation.ContentMapper;
import fr.phylisiumstudio.app.inject.annotation.PlotDataRepositoryFile;
import fr.phylisiumstudio.app.inject.annotation.ActivityRepositoryFile;
import fr.phylisiumstudio.app.json.JacksonConfig;
import fr.phylisiumstudio.logic.repository.IPlotDataRepository;
import fr.phylisiumstudio.logic.repository.ICampsiteRepository;
import fr.phylisiumstudio.logic.repository.IActivityRepository;
import fr.phylisiumstudio.storage.JsonCampsiteRepository;
import fr.phylisiumstudio.storage.YamlPlotDataRepository;
import fr.phylisiumstudio.storage.YamlActivityRepository;
import net.minestom.server.MinecraftServer;
import net.minestom.server.instance.InstanceManager;

import java.io.File;
import java.util.Random;

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
        bind(IPlotDataRepository.class).to(YamlPlotDataRepository.class).in(Singleton.class);
        bind(IActivityRepository.class).to(YamlActivityRepository.class).in(Singleton.class);
        bind(ObjectMapper.class).annotatedWith(ContentMapper.class).toInstance(JacksonConfig.createYaml());
        bind(MinecraftServer.class).toInstance(app.getServer());
        bind(InstanceManager.class).toInstance(app.getInstanceManager());
        bind(App.class).toInstance(app);
        bind(MainConfig.class).toInstance(app.getMainConfig());
        bind(ObjectMapper.class).toInstance(app.getObjectMapper());
        bind(Random.class).toInstance(new Random());
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

    @Provides
    @Singleton
    @PlotDataRepositoryFile
    public File ProvidePlotDataRepositoryFile() {
        return new File(app.getDataFolder(), "plotdata");
    }

    @Provides
    @Singleton
    @ActivityRepositoryFile
    public File ProvideActivityRepositoryFile() {
        return new File(app.getDataFolder(), "activitydata");
    }
}
