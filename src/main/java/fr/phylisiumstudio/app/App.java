package fr.phylisiumstudio.app;

import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import fr.phylisiumstudio.app.commands.MoneyCommand;
import fr.phylisiumstudio.app.commands.ShutdownCommand;
import fr.phylisiumstudio.app.config.MainConfig;
import fr.phylisiumstudio.app.inject.AppModule;
import fr.phylisiumstudio.app.view.CampsiteView;
import fr.phylisiumstudio.logic.IApplication;
import fr.phylisiumstudio.logic.activity.ActivityData;
import fr.phylisiumstudio.logic.activity.ActivityType;
import fr.phylisiumstudio.logic.activity.fabric.ActivityDataFabric;
import fr.phylisiumstudio.logic.area.Area;
import fr.phylisiumstudio.logic.builder.ActivityBuilder;
import fr.phylisiumstudio.logic.builder.PlotBuilder;
import fr.phylisiumstudio.logic.builder.fabric.BuilderFabric;
import fr.phylisiumstudio.logic.plot.PlotData;
import fr.phylisiumstudio.logic.plot.PlotType;
import fr.phylisiumstudio.logic.plot.fabric.PlotDataFabric;
import fr.phylisiumstudio.logic.schematic.SchematicFactory;
import fr.phylisiumstudio.logic.service.BuilderService;
import fr.phylisiumstudio.logic.service.CampsiteService;
import fr.phylisiumstudio.logic.service.InstanceService;
import lombok.Getter;
import net.hollowcube.schem.reader.SchematicReader;
import net.minestom.server.MinecraftServer;
import net.minestom.server.instance.InstanceManager;
import org.joml.Vector3d;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@Getter
public class App implements IApplication {

    private AppModule appModule;
    private final ObjectMapper objectMapper;
    private MinecraftServer server;
    private InstanceManager instanceManager;

    private final File dataFolder = new File("run");
    private final Logger logger;
    private MainConfig mainConfig;

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

    @Inject
    private MoneyCommand moneyCommand;

    private CampsiteView campsiteView;

    public App() {
        objectMapper = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
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
            var schematicsFolder = new File(dataFolder, "schem");
            if (!schematicsFolder.exists() && !schematicsFolder.mkdirs()) {
                throw new RuntimeException("Failed to create schematics folder: " + schematicsFolder.getAbsolutePath());
            }

            try (var stream = Files.walk(schematicsFolder.toPath(), 1)) {
                var schematicFiles = stream
                        .filter(Files::isRegularFile)
                        .map(Path::toFile)
                        .toList();

                for (var schematicFile : schematicFiles) {
                    var bytes = Files.readAllBytes(schematicFile.toPath());
                    var schem = SchematicReader.detecting().read(bytes);
                    schematicFactory.registerSchematic(schematicFile.getName(), schem);
                }
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

            SimpleModule module = new SimpleModule();
            objectMapper.registerModule(module);

            InjectableValues.Std injectableValues = new InjectableValues.Std();
            injectableValues.addValue(PlotDataFabric.class, plotDataFabric);
            objectMapper.setInjectableValues(injectableValues);
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
            var dataFolder = getDataFolder();
            if (!dataFolder.exists() && !dataFolder.mkdirs()) {
                throw new RuntimeException("Failed to create data folder: " + dataFolder.getAbsolutePath());
            }

            var configFile = new File(dataFolder, "config.yml");
            if (!configFile.exists() && !configFile.createNewFile()) {
                throw new RuntimeException("Failed to create config file: " + configFile.getAbsolutePath());
            }

            final var loader = YamlConfigurationLoader.builder()
                    .path(configFile.toPath())
                    .nodeStyle(NodeStyle.BLOCK)
                    .build();

            final var node = loader.load();

            var mainConfig = node.get(MainConfig.class);

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
            var defaultArea = new Area(new Vector3d(0,0,0), new Vector3d(8,6,8));

            for (var value : ActivityType.values()) {
                var activityData = new ActivityData(value, defaultArea);
                activityDataFabric.registerActivityData(activityData.type(), activityData);
            }

            for (var value : PlotType.values()) {
                var plotData = new PlotData(value, defaultArea);
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
            var address = new InetSocketAddress(mainConfig.Host, mainConfig.Port);

            this.campsiteView = new CampsiteView(campsiteService, plotDataFabric, instanceService, activityDataFabric);

            MinecraftServer.getCommandManager().register(new ShutdownCommand());
            MinecraftServer.getCommandManager().register(moneyCommand);
            server.start(address);
            logger.info("Server started on " + address);
        }
        catch (Exception e) {
            logger.log(Level.WARNING, "Error starting server", e);
        }
    }
}
