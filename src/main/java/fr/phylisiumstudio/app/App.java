package fr.phylisiumstudio.app;

import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import fr.phylisiumstudio.app.commands.ShutdownCommand;
import fr.phylisiumstudio.app.config.MainConfig;
import fr.phylisiumstudio.app.inject.AppModule;
import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.IApplication;
import fr.phylisiumstudio.logic.activity.Activity;
import fr.phylisiumstudio.logic.activity.ActivityData;
import fr.phylisiumstudio.logic.activity.ActivityType;
import fr.phylisiumstudio.logic.activity.fabric.ActivityDataFabric;
import fr.phylisiumstudio.logic.area.Area;
import fr.phylisiumstudio.logic.builder.ActivityBuilder;
import fr.phylisiumstudio.logic.builder.PlotBuilder;
import fr.phylisiumstudio.logic.builder.fabric.BuilderFabric;
import fr.phylisiumstudio.logic.mapper.PositionMapper;
import fr.phylisiumstudio.logic.plot.Plot;
import fr.phylisiumstudio.logic.plot.PlotData;
import fr.phylisiumstudio.logic.plot.PlotType;
import fr.phylisiumstudio.logic.plot.fabric.PlotDataFabric;
import fr.phylisiumstudio.logic.schematic.SchematicFactory;
import fr.phylisiumstudio.logic.service.BuilderService;
import fr.phylisiumstudio.logic.service.CampsiteService;
import fr.phylisiumstudio.logic.service.InstanceService;
import fr.phylisiumstudio.storage.serialize.ActivitySerializer;
import fr.phylisiumstudio.storage.serialize.PlotSerializer;
import lombok.Getter;
import net.hollowcube.schem.Schematic;
import net.hollowcube.schem.SchematicReader;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.InstanceManager;
import org.joml.Vector3d;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@Getter
public class App implements IApplication {

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
    @Inject
    private BuilderService builderService;
    @Inject
    private SchematicFactory schematicFactory;

    public App() {
        gsonBuilder =  new GsonBuilder()
                .setPrettyPrinting();
        logger = Logger.getLogger("CampsiteApp");

        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            logger.warning("Could not create data folder");
        }
    }

    @Override
    public void OnEnable() {
        SetupServer();
        LoadConfig();
        SetupGuice();
        loadSchematics();
        LoadData();
        StartServer();
    }

    @Override
    public void OnDisable() {
        try {
            logger.info("Saving data...");
            campsiteService.saveCampsite();
        }
        catch (Exception e) {
            logger.log(Level.WARNING, "Error saving data", e);
        }
        finally {
            logger.info("Data saved.");
        }
    }

    private void loadSchematics() {
        try {
            SchematicReader schematicReader = new SchematicReader();
            File schematicsFolder = new File(dataFolder, "schem");
            if (!schematicsFolder.exists() && !schematicsFolder.mkdirs()) {
                throw new RuntimeException("Failed to create schematics folder: " + schematicsFolder.getAbsolutePath());
            }

            List<File> schematicFiles = Files.walk(schematicsFolder.toPath(), 1)
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .toList();

            for (File schematicFile : schematicFiles) {
                Schematic schematic = schematicReader.read(Paths.get(schematicFile.getAbsolutePath()));
                schematicFactory.registerSchematic(schematicFile.getName(), schematic);
            }
        }
        catch (Exception e) {
            logger.log(Level.WARNING, "Error loading schematics", e);
        }
    }

    public void SetupGuice() {
        try {
            this.appModule = new AppModule(this);
            appModule.getInjector().injectMembers(this);
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
            Area defaultArea = new Area(new Vector3d(0,0,0), new Vector3d(8,6,8));

            for (ActivityType value : ActivityType.values()) {
                ActivityData activityData = new ActivityData(value, defaultArea);
                activityDataFabric.registerActivityData(activityData.type(), activityData);
            }

            for (PlotType value : PlotType.values()) {
                PlotData plotData = new PlotData(value, defaultArea);
                plotDataFabric.registerPlotData(plotData.type(), plotData);
            }

            builderFabric.register("plot", () -> new PlotBuilder(schematicFactory));
            builderFabric.register("activity", ActivityBuilder::new);

            campsiteService.loadCampsites();
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

                Vector3d spawnPoint = new Vector3d(0, 69, 0);
                if(campsite.getPlots().isEmpty()) {
                    Random random = new Random();
                    PlotData campData = plotDataFabric.getPlotData(PlotType.CAMPSITE);
                    PlotData carData = plotDataFabric.getPlotData(PlotType.CARAVAN);

                    PlotData plotData = random.nextBoolean() ? campData : carData;
                    for (int i = 0; i < 5; i++) {
                        Vector3d offset = new Vector3d(spawnPoint);
                        Plot plot = new Plot(plotData, offset.add(0,-1, i * (plotData.area().getSize().z + 5)));
                        campsite.addPlot(plot);
                    }
                }

                InstanceContainer instanceContainer = instanceService.getInstance(campsite);

                event.setSpawningInstance(instanceContainer);
                player.setRespawnPoint(PositionMapper.toMinestomPos(spawnPoint));
            });

            globalEventHandler.addListener(PlayerBlockBreakEvent.class, event -> {
                event.setCancelled(true);
            });

            MinecraftServer.getCommandManager().register(new ShutdownCommand());
            server.start(address);
            logger.info("Server started on " + address.toString());
        }
        catch (Exception e) {
            logger.log(Level.WARNING, "Error starting server", e);
        }
    }
}
