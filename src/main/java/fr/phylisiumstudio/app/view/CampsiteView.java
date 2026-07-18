package fr.phylisiumstudio.app.view;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.activity.Activity;
import fr.phylisiumstudio.logic.activity.ActivityData;
import fr.phylisiumstudio.logic.activity.ActivityLevel;
import fr.phylisiumstudio.logic.area.Area;
import fr.phylisiumstudio.logic.service.ActivityDataService;
import fr.phylisiumstudio.logic.activity.ActivityType;
import fr.phylisiumstudio.logic.clock.GameClockService;
import fr.phylisiumstudio.logic.client.Client;
import fr.phylisiumstudio.logic.mapper.PositionMapper;
import fr.phylisiumstudio.logic.plot.Plot;
import fr.phylisiumstudio.logic.service.PlotDataService;
import fr.phylisiumstudio.logic.plot.PlotType;
import fr.phylisiumstudio.logic.service.CampsiteService;
import fr.phylisiumstudio.logic.service.InstanceService;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;

import org.joml.Vector3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@Singleton
public class CampsiteView {
    private static final Logger logger = LoggerFactory.getLogger(CampsiteView.class);

    // Constants to control spawn/generation behaviour (plots only)
    private static final int DEFAULT_PLOT_COUNT = 500;
    private static final int PLOT_COLUMNS = 20;
    private static final double PLOT_PADDING = 5.0; // padding added to plot size when laying out grid
    private static final double PLOT_BASE_X_SPACING = 10.0; // fallback spacing in X if plot size unavailable
    private static final double PLOT_JITTER = 1.0; // jitter range in blocks (+/-)

    private static final Vector3d DEFAULT_SPAWN_POINT = new Vector3d(0, 69, 0);

    private final List<ClientView> clientsStateViews;
    private final CampsiteService campsiteService;
    private final InstanceService instanceService;
    private final ActivityDataService activityDataService;
    private final PlotDataService plotDataService;
    private final GameClockService gameClockService;
    private final Random random;

    @Inject
    public CampsiteView(CampsiteService campsiteService,
                        InstanceService instanceService,
                        ActivityDataService activityDataService,
                        PlotDataService plotDataService,
                        GameClockService gameClockService,
                        Random random) {
        this.clientsStateViews = new CopyOnWriteArrayList<>();
        this.campsiteService = campsiteService;
        this.instanceService = instanceService;
        this.activityDataService = activityDataService;
        this.plotDataService = plotDataService;
        this.gameClockService = gameClockService;
        this.random = random;

        var eventHandler = MinecraftServer.getGlobalEventHandler();
        eventHandler.addListener(AsyncPlayerConfigurationEvent.class, this::addCamping);
        eventHandler.addListener(PlayerDisconnectEvent.class, this::onPlayerDisconnect);
        eventHandler.addListener(PlayerSpawnEvent.class, event -> {
            var player = event.getPlayer();
            player.setAllowFlying(true);
        });
    }

    public void addCamping(AsyncPlayerConfigurationEvent event) {
        final var player = event.getPlayer();

        // TODO: This is just for testing, we should have a better way to initialize activity data
        if (activityDataService.listActivityData().isEmpty()) {
            var defaultArea = new Area(new Vector3d(0, 68, 0), new Vector3d(10, 74, 10));
            for (var value : ActivityType.values()) {
                var activityData = activityDataService.getActivityData(value);
                if (activityData == null) {
                    activityData = new ActivityData(value, defaultArea, new ArrayList<>());
                    activityDataService.addActivityData(activityData);

                    var schem = activityData.type() + ".nbt";
                    for (int i = 0; i < 5; i++) {
                        var level = new ActivityLevel(i + 1, schem, 1);
                        activityData.levels().add(level);
                    }
                }
            }
            activityDataService.save();
        }

        var campsite = campsiteService.getCampsiteByOwner(player.getUuid())
                .orElseGet(() -> {
                    var newCampsite = new Campsite(player.getUuid());
                    campsiteService.addCampsite(newCampsite);
                    return newCampsite;
                });

        var spawnPoint = new Vector3d(DEFAULT_SPAWN_POINT);
        if(campsite.getPlots().isEmpty()) {

            for (var i = 0; i < DEFAULT_PLOT_COUNT; i++) {
                var randomPlotType = PlotType.values()[random.nextInt(PlotType.values().length)];
                var campData = plotDataService.getPlotData(randomPlotType);

                var row = i / PLOT_COLUMNS;
                var col = i % PLOT_COLUMNS;

                var spacingX = PLOT_BASE_X_SPACING;
                if (campData != null && campData.area() != null) {
                    spacingX = Math.max(campData.area().getSize().x + PLOT_PADDING, PLOT_BASE_X_SPACING);
                }
                var spacingZ = (campData != null && campData.area() != null)
                        ? campData.area().getSize().z + PLOT_PADDING
                        : PLOT_BASE_X_SPACING;

                var xOffset = col * spacingX;
                var zOffset = row * spacingZ;

                var offset = new Vector3d(spawnPoint);
                var jitterX = (random.nextDouble() * 2.0 * PLOT_JITTER) - PLOT_JITTER;
                var jitterZ = (random.nextDouble() * 2.0 * PLOT_JITTER) - PLOT_JITTER;
                var plot = new Plot(offset.add(xOffset + jitterX, 0, zOffset + jitterZ), randomPlotType);
                campsite.addPlot(plot);

                var client = new Client(plot);
                campsite.addClient(client);
            }
        }

        var activitySpawnPoint = new Vector3d(0, 69, -20);
        if (campsite.getActivities().isEmpty()) {
            var activityTypes = ActivityType.values();

            for (var i = 0; i < activityTypes.length; i++) {
                var activityType = activityTypes[i];
                var activityData = activityDataService.getActivityData(activityType);
                if (activityData == null) {
                    continue;
                }

                var row = i / 5;
                var col = i % 5;

                var xOffset = col * (activityData.area().getSize().x + 5.0);
                var zOffset = row * (activityData.area().getSize().z + 5.0);

                var offset = new Vector3d(activitySpawnPoint).add(xOffset, 0, zOffset);
                var activity = new Activity(UUID.randomUUID() ,offset, 15, 5, 2, activityType); // literal defaults
                campsite.addActivity(activity);
            }
        }

        var instanceContainer = instanceService.getInstance(campsite);

        event.setSpawningInstance(instanceContainer);
        player.setRespawnPoint(PositionMapper.toMinestomPos(spawnPoint));

        gameClockService.start(campsite, instanceContainer);
        this.clientsStateViews.add(new ClientView(campsite, instanceContainer));
    }

    private void onPlayerDisconnect(PlayerDisconnectEvent event) {
        var player = event.getPlayer();
        var playerInstance = player.getInstance();
        if (playerInstance == null) {
            return;
        }

        campsiteService.getCampsiteByOwner(player.getUuid()).ifPresent(campsite -> {
            var remainingPlayers = playerInstance.getPlayers().stream()
                    .filter(p -> !p.getUuid().equals(player.getUuid()))
                    .count();

            if (remainingPlayers > 0) {
                return;
            }

            clientsStateViews.removeIf(view ->
                    view.getCampsite().getUniqueID().equals(campsite.getUniqueID()));

            gameClockService.stop(campsite.getUniqueID());

            try {
                instanceService.releaseInstance(campsite);
            } catch (Exception ex) {
                logger.error("Error releasing instance for campsite {}: {}",
                        campsite.getUniqueID(), ex.getMessage(), ex);
            }

            logger.info("Released instance for campsite {} (last player disconnected)", campsite.getUniqueID());
        });
    }
}
