package fr.phylisiumstudio.app.view;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.activity.Activity;
import fr.phylisiumstudio.logic.service.ActivityDataService;
import fr.phylisiumstudio.logic.activity.ActivityType;
import fr.phylisiumstudio.logic.client.Client;
import fr.phylisiumstudio.logic.mapper.PositionMapper;
import fr.phylisiumstudio.logic.plot.Plot;
import fr.phylisiumstudio.logic.service.PlotDataService;
import fr.phylisiumstudio.logic.plot.PlotType;
import fr.phylisiumstudio.logic.service.CampsiteService;
import fr.phylisiumstudio.logic.service.InstanceService;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.instance.InstanceTickEvent;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import org.joml.Vector3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

@Singleton
public class CampsiteView {
    private static final Logger logger = LoggerFactory.getLogger(CampsiteView.class);

    private final List<ClientStateView> clientsStateViews;
    private final CampsiteService campsiteService;
    private final InstanceService instanceService;
    private final ActivityDataService activityDataService;
    private final PlotDataService plotDataService;
    private final Random random;

    @Inject
    public CampsiteView(CampsiteService campsiteService,
                        InstanceService instanceService,
                        ActivityDataService activityDataService,
                        PlotDataService plotDataService,
                        Random random) {
        this.clientsStateViews = new CopyOnWriteArrayList<>();
        this.campsiteService = campsiteService;
        this.instanceService = instanceService;
        this.activityDataService = activityDataService;
        this.plotDataService = plotDataService;
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

        var campsite = campsiteService.getCampsiteByOwner(player.getUuid())
                .orElseGet(() -> {
                    var newCampsite = new Campsite(player.getUuid());
                    campsiteService.addCampsite(newCampsite);
                    return newCampsite;
                });

        var spawnPoint = new Vector3d(0, 69, 0);
        if(campsite.getPlots().isEmpty()) {

            for (var i = 0; i < 20; i++) {
                var randomPlotType = PlotType.values()[random.nextInt(PlotType.values().length)];
                var campData = plotDataService.getPlotData(randomPlotType);
                var row = i / 5;
                var col = i % 5;
                var xOffset = col * 20;
                var zOffset = row * (campData.area().getSize().z + 5);

                var offset = new Vector3d(spawnPoint);
                var jitterX = (random.nextDouble() * 2.0) - 1.0;
                var jitterZ = (random.nextDouble() * 2.0) - 1.0;
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
                var xOffset = col * 20;
                var zOffset = row * (activityData.area().getSize().z + 5);

                var offset = new Vector3d(activitySpawnPoint).add(xOffset, 0, zOffset);
                var activity = new Activity(offset, 15, 5, 2, activityType);
                campsite.addActivity(activity);
            }
        }

        var instanceContainer = instanceService.getInstance(campsite);

        event.setSpawningInstance(instanceContainer);
        player.setRespawnPoint(PositionMapper.toMinestomPos(spawnPoint));

        this.clientsStateViews.add(new ClientStateView(campsite, instanceContainer));
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
