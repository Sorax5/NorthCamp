package fr.phylisiumstudio.app;

import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import fr.phylisiumstudio.app.commands.ActivitiesCommand;
import fr.phylisiumstudio.app.commands.CampCommand;
import fr.phylisiumstudio.app.commands.ClientsCommand;
import fr.phylisiumstudio.app.commands.LeaderboardCommand;
import fr.phylisiumstudio.app.commands.MoneyCommand;
import fr.phylisiumstudio.app.commands.PricingCommand;
import fr.phylisiumstudio.app.commands.ShutdownCommand;
import fr.phylisiumstudio.app.commands.SlotsCommand;
import fr.phylisiumstudio.app.commands.StaffCommand;
import fr.phylisiumstudio.app.config.MainConfig;
import fr.phylisiumstudio.app.inject.AppModule;
import fr.phylisiumstudio.app.inject.GuiceHandlerInstantiator;
import fr.phylisiumstudio.app.json.JacksonConfig;
import fr.phylisiumstudio.app.view.CampsiteView;
import fr.phylisiumstudio.logic.IApplication;
import fr.phylisiumstudio.logic.gameplay.GameplayLoopService;
import fr.phylisiumstudio.logic.service.ActivityDataService;
import fr.phylisiumstudio.logic.builder.ActivityBuilder;
import fr.phylisiumstudio.logic.builder.PlotBuilder;
import fr.phylisiumstudio.logic.service.PlotDataService;
import fr.phylisiumstudio.logic.schematic.SchematicFactory;
import fr.phylisiumstudio.logic.service.CampsiteBuilderService;
import fr.phylisiumstudio.logic.service.CampsiteService;
import fr.phylisiumstudio.logic.service.InstanceService;
import fr.phylisiumstudio.logic.skin.SkinLibrary;
import fr.phylisiumstudio.logic.slot.LayoutService;
import lombok.Getter;
import me.lucko.spark.minestom.SparkMinestom;
import net.hollowcube.schem.reader.SchematicReader;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.server.ServerListPingEvent;
import net.minestom.server.instance.InstanceManager;
import net.minestom.server.ping.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;

@Singleton
@Getter
public class App implements IApplication {

    private AppModule appModule;
    private final ObjectMapper objectMapper;
    private MinecraftServer server;
    private InstanceManager instanceManager;

    private final File dataFolder = new File("run");
    private static final Logger logger = LoggerFactory.getLogger(App.class);
    private MainConfig mainConfig;

    @Inject
    private InstanceService instanceService;
    @Inject
    private CampsiteService campsiteService;
    @Inject
    private CampsiteBuilderService campsiteBuilderService;
    @Inject
    private SchematicFactory schematicFactory;

    @Inject
    private PlotDataService plotDataService;
    @Inject
    private ActivityDataService activityDataService;

    @Inject
    private ActivityBuilder activityBuilder;
    @Inject
    private PlotBuilder plotBuilder;

    @Inject
    private MoneyCommand moneyCommand;
    @Inject
    private LeaderboardCommand leaderboardCommand;
    @Inject
    private CampCommand campCommand;
    @Inject
    private PricingCommand pricingCommand;
    @Inject
    private StaffCommand staffCommand;
    @Inject
    private ClientsCommand clientsCommand;
    @Inject
    private ActivitiesCommand activitiesCommand;
    @Inject
    private SlotsCommand slotsCommand;

    @Inject
    private CampsiteView campsiteView;
    @Inject
    private GameplayLoopService gameplayLoopService;
    @Inject
    private SkinLibrary skinLibrary;
    @Inject
    private LayoutService layoutService;
    private SparkMinestom spark;

    public App() {
        objectMapper = JacksonConfig.create();

        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            logger.warn("Could not create data folder");
        }
    }

    @Override
    public void OnEnable() {
        SetupServer();
        LoadConfig();
        SetupGuice();
        loadSchematics();
        skinLibrary.load();
        layoutService.load();
        LoadData();
        StartServer();
    }

    @Override
    public void OnDisable() {
        try {
            logger.info("Saving data...");

            if (instanceService != null) {
                instanceService.shutdown();
            }

            campsiteService.saveCampsite();

            if (spark != null) {
                spark.shutdown();
                logger.info("Spark profiler disabled.");
            }
        }
        catch (Exception e) {
            logger.warn("Error saving data", e);
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
                    var schem = SchematicReader.structure().read(bytes);
                    schematicFactory.registerSchematic(schematicFile.getName(), schem);
                }
            }
        }
        catch (Exception e) {
            logger.warn("Error loading schematics", e);
        }
    }

    public void SetupGuice() {
        try {
            var handlerInstantiator = new GuiceHandlerInstantiator();
            objectMapper.setHandlerInstantiator(handlerInstantiator);

            this.appModule = new AppModule(this);
            var injector = appModule.getInjector();

            handlerInstantiator.setInjector(injector);
            injector.injectMembers(this);

            var injectableValues = new InjectableValues.Std();
            objectMapper.setInjectableValues(injectableValues);
        }
        catch (Exception e) {
            logger.warn("Error setting up Guice", e);
        }
    }

    public void SetupServer() {
        try {
            this.server = MinecraftServer.init();
            this.instanceManager = MinecraftServer.getInstanceManager();
        }
        catch (Exception e) {
            logger.warn("Error setting up server", e);
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
            logger.error("Failed to load config", e);
        }
    }

    public void LoadData() {
        logger.info("Loading data...");
        try {
            plotDataService.load();
            activityDataService.load();

            campsiteService.loadCampsites();
        }
        catch (Exception e) {
            logger.warn("Error loading data", e);
        }
        finally {
            logger.info("Data loaded.");
        }
    }

    public void StartServer() {
        try {
            var address = new InetSocketAddress(mainConfig.Host, mainConfig.Port);

            var commandManager = MinecraftServer.getCommandManager();
            commandManager.register(new ShutdownCommand());
            commandManager.register(moneyCommand);
            commandManager.register(leaderboardCommand);
            commandManager.register(campCommand);
            commandManager.register(pricingCommand);
            commandManager.register(staffCommand);
            commandManager.register(clientsCommand);
            commandManager.register(activitiesCommand);
            commandManager.register(slotsCommand);

            registerMotd();

            var sparkDirectory = Path.of(dataFolder.getPath(), "spark");
            this.spark = SparkMinestom.builder(sparkDirectory)
                    .commands(true)
                    .permissionHandler((sender, permission) -> true)
                    .enable();
            logger.info("Spark profiler enabled.");

            server.start(address);
            logger.info("Server started on {}", address);
        }
        catch (Exception e) {
            logger.warn("Error starting server", e);
        }
    }

    /** MOTD thématique affiché dans la liste des serveurs. */
    private void registerMotd() {
        var node = EventNode.all("motd");
        node.addListener(ServerListPingEvent.class, event -> {
            var online = MinecraftServer.getConnectionManager().getOnlinePlayerCount();
            event.setStatus(Status.builder()
                    .description(Component.text()
                            .append(Component.text("North Camp", NamedTextColor.GOLD, TextDecoration.BOLD))
                            .append(Component.text(" — le tycoon de camping", NamedTextColor.GREEN))
                            .build())
                    .playerInfo(online, Math.max(online + 1, 100))
                    .build());
        });
        MinecraftServer.getGlobalEventHandler().addChild(node);
    }
}
