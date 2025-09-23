package fr.phylisiumstudio.app.inject;

import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import fr.phylisiumstudio.logic.activity.Activity;
import fr.phylisiumstudio.logic.activity.ActivityData;
import fr.phylisiumstudio.logic.activity.ActivityType;
import fr.phylisiumstudio.logic.activity.fabric.ActivityDataFabric;
import fr.phylisiumstudio.logic.plot.Plot;
import fr.phylisiumstudio.logic.plot.PlotData;
import fr.phylisiumstudio.logic.plot.PlotType;
import fr.phylisiumstudio.logic.plot.fabric.PlotDataFabric;
import fr.phylisiumstudio.storage.serialize.ActivitySerializer;
import fr.phylisiumstudio.storage.serialize.PlotSerializer;
import lombok.Getter;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.InstanceManager;
import net.minestom.server.instance.block.Block;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

@Getter
public class App {

    private final SocketAddress address = new InetSocketAddress("0.0.0.0", 25565);

    private final AppModule appModule;
    private final GsonBuilder gsonBuilder;
    private MinecraftServer server;

    @Inject
    private Logger logger;
    @Inject
    private ActivitySerializer activitySerializer;
    @Inject
    private PlotSerializer plotSerializer;
    @Inject
    private PlotDataFabric plotDataFabric;
    @Inject
    private ActivityDataFabric activityDataFabric;

    public App() {
        gsonBuilder =  new GsonBuilder()
                .setPrettyPrinting();

        this.appModule = new AppModule(this);
        appModule.getInjector().injectMembers(this);

        gsonBuilder.registerTypeAdapter(Plot.class, plotSerializer);
        gsonBuilder.registerTypeAdapter(Activity.class, activitySerializer);
    }

    public void LoadData() {
        logger.info("Loading data...");
        try {
            for (ActivityType value : ActivityType.values()) {
                ActivityData activityData = new ActivityData(value);
                activityDataFabric.registerActivityData(activityData.type(), activityData);
            }

            for (PlotType value : PlotType.values()) {
                PlotData plotData = new PlotData(value);
                plotDataFabric.registerPlotData(plotData.type(), plotData);
            }
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
            this.server = MinecraftServer.init();

            InstanceManager instanceManager = MinecraftServer.getInstanceManager();
            InstanceContainer instanceContainer = instanceManager.createInstanceContainer();

            // Set the ChunkGenerator
            instanceContainer.setGenerator(unit -> unit.modifier().fillHeight(0, 40, Block.GRASS_BLOCK));

            // Add an event callback to specify the spawning instance (and the spawn position)
            GlobalEventHandler globalEventHandler = MinecraftServer.getGlobalEventHandler();
            globalEventHandler.addListener(AsyncPlayerConfigurationEvent.class, event -> {
                final Player player = event.getPlayer();
                event.setSpawningInstance(instanceContainer);
                player.setRespawnPoint(new Pos(0, 42, 0));
            });

            server.start(address);
            logger.info("Server started on " + address.toString());
        }
        catch (Exception e) {
            logger.log(Level.WARNING, "Error starting server", e);
        }
    }
}
