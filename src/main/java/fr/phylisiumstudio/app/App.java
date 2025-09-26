package fr.phylisiumstudio.app;

import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import fr.phylisiumstudio.app.config.MainConfig;
import fr.phylisiumstudio.app.inject.AppModule;
import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.activity.Activity;
import fr.phylisiumstudio.logic.activity.ActivityData;
import fr.phylisiumstudio.logic.activity.ActivityType;
import fr.phylisiumstudio.logic.activity.fabric.ActivityDataFabric;
import fr.phylisiumstudio.logic.area.Area;
import fr.phylisiumstudio.logic.builder.ActivityBuilder;
import fr.phylisiumstudio.logic.builder.PlotBuilder;
import fr.phylisiumstudio.logic.builder.fabric.BuilderFabric;
import fr.phylisiumstudio.logic.plot.Plot;
import fr.phylisiumstudio.logic.plot.PlotData;
import fr.phylisiumstudio.logic.plot.PlotType;
import fr.phylisiumstudio.logic.plot.fabric.PlotDataFabric;
import fr.phylisiumstudio.logic.service.CampsiteService;
import fr.phylisiumstudio.logic.service.InstanceService;
import fr.phylisiumstudio.storage.serialize.ActivitySerializer;
import fr.phylisiumstudio.storage.serialize.PlotSerializer;
import lombok.Getter;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.InstanceManager;
import org.joml.Vector3d;
import org.joml.Vector3i;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@Getter
public class App {

    private AppModule appModule;
    private final GsonBuilder gsonBuilder;
    private MinecraftServer server;
    private InstanceManager instanceManager;

    private final File dataFolder = new File("run");
    private final Logger logger;
    private MainConfig mainConfig;

    @Inject
    private ActivitySerializer activitySerializer;
    @Inject
    private PlotSerializer plotSerializer;
    @Inject
    private PlotDataFabric plotDataFabric;
    @Inject
    private ActivityDataFabric activityDataFabric;
    @Inject
    private BuilderFabric builderFabric;
    @Inject
    private InstanceService instanceService;
    @Inject
    private CampsiteService campsiteService;

    public App() {
        gsonBuilder =  new GsonBuilder()
                .setPrettyPrinting();
        logger = Logger.getLogger("CampsiteApp");

        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            logger.warning("Could not create data folder");
        }
    }

    public void SetupGuice() {
        try {
            this.appModule = new AppModule(this);
            appModule.getInjector().injectMembers(this);

            gsonBuilder.registerTypeAdapter(Plot.class, plotSerializer);
            gsonBuilder.registerTypeAdapter(Activity.class, activitySerializer);
        }
        catch (Exception e) {
            logger.log(Level.WARNING, "Error setting up Guice", e);
        }
    }

    public void SetupServer() {
        try {
            this.server = MinecraftServer.init();
            this.instanceManager = MinecraftServer.getInstanceManager();
        }
        catch (Exception e) {
            logger.log(Level.WARNING, "Error setting up server", e);
        }
    }

    public void LoadConfig() {
        try {
            File dataFolder = getDataFolder();
            if (!dataFolder.exists() && !dataFolder.mkdirs()) {
                throw new RuntimeException("Failed to create data folder: " + dataFolder.getAbsolutePath());
            }

            File configFile = new File(dataFolder, "config.yml");
            if (!configFile.exists() && !configFile.createNewFile()) {
                throw new RuntimeException("Failed to create config file: " + configFile.getAbsolutePath());
            }

            final YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                    .path(configFile.toPath())
                    .nodeStyle(NodeStyle.BLOCK)
                    .build();

            final CommentedConfigurationNode node = loader.load();

            MainConfig mainConfig = node.get(MainConfig.class);

            node.set(MainConfig.class, mainConfig);
            loader.save(node);

            this.mainConfig = mainConfig;
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to load config", e);
        }
    }

    public void LoadData() {
        logger.info("Loading data...");
        try {
            Area defaultArea = new Area(new Vector3d(0,0,0), new Vector3d(8,8,8));

            for (ActivityType value : ActivityType.values()) {
                ActivityData activityData = new ActivityData(value, defaultArea);
                activityDataFabric.registerActivityData(activityData.type(), activityData);
            }

            for (PlotType value : PlotType.values()) {
                PlotData plotData = new PlotData(value);
                plotDataFabric.registerPlotData(plotData.type(), plotData);
            }

            builderFabric.registerBuilder(PlotData.class, new PlotBuilder<>());
            builderFabric.registerBuilder(ActivityData.class, new ActivityBuilder<>());
        }
        catch (Exception e) {
            logger.log(Level.WARNING, "Error loading data", e);
        }
        finally {
            logger.info("Data loaded.");
        }
    }

    public void StartServer() {
        try {
            SocketAddress address = new InetSocketAddress(mainConfig.Host, mainConfig.Port);

            GlobalEventHandler globalEventHandler = MinecraftServer.getGlobalEventHandler();
            globalEventHandler.addListener(AsyncPlayerConfigurationEvent.class, event -> {
                final Player player = event.getPlayer();

                Optional<Campsite> optionalCampsite = campsiteService.getCampsiteByOwner(player.getUuid());
                Campsite campsite = optionalCampsite
                        .orElseGet(() -> {
                            Campsite newCampsite = new Campsite(player.getUuid());
                            campsiteService.addCampsite(newCampsite);
                            return newCampsite;
                        });

                InstanceContainer instanceContainer = instanceService.getInstance(campsite.getUniqueID());

                event.setSpawningInstance(instanceContainer);
                player.setRespawnPoint(new Pos(28, 69, 207));
            });

            globalEventHandler.addListener(PlayerBlockBreakEvent.class, event -> {
                event.setCancelled(true);
            });

            server.start(address);
            logger.info("Server started on " + address.toString());
        }
        catch (Exception e) {
            logger.log(Level.WARNING, "Error starting server", e);
        }
    }
}
