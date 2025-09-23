package fr.phylisiumstudio.app.inject;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.*;
import fr.phylisiumstudio.app.inject.annotation.CampsiteRepositoryFile;
import fr.phylisiumstudio.logic.activity.Activity;
import fr.phylisiumstudio.logic.activity.fabric.ActivityDataFabric;
import fr.phylisiumstudio.logic.plot.Plot;
import fr.phylisiumstudio.logic.plot.fabric.PlotDataFabric;
import fr.phylisiumstudio.logic.repository.ICampsiteRepository;
import fr.phylisiumstudio.storage.JsonCampsiteRepository;
import fr.phylisiumstudio.storage.serialize.ActivitySerializer;
import fr.phylisiumstudio.storage.serialize.PlotSerializer;

import java.io.File;
import java.util.logging.Logger;

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
    }

    public Injector getInjector() {
        return Guice.createInjector(this);
    }

    @Provides
    @Singleton
    @CampsiteRepositoryFile
    public File ProvideCampsiteRepositoryFile() {
        return new File("campsites");
    }

    @Provides
    @Singleton
    public GsonBuilder ProvideGsonBuilder() {
        return app.getGsonBuilder();
    }
}
