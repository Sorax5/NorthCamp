package fr.phylisiumstudio.app.view;

import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.activity.Activity;
import fr.phylisiumstudio.logic.activity.ActivityType;
import fr.phylisiumstudio.logic.activity.fabric.ActivityDataFabric;
import fr.phylisiumstudio.logic.client.Client;
import fr.phylisiumstudio.logic.mapper.PositionMapper;
import fr.phylisiumstudio.logic.plot.Plot;
import fr.phylisiumstudio.logic.plot.PlotType;
import fr.phylisiumstudio.logic.plot.fabric.PlotDataFabric;
import fr.phylisiumstudio.logic.service.CampsiteService;
import fr.phylisiumstudio.logic.service.InstanceService;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.instance.InstanceTickEvent;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CampsiteView {
    private List<ClientStateView> clientsStateViews;
    private final CampsiteService campsiteService;
    private final PlotDataFabric plotDataFabric;
    private final InstanceService instanceService;
    private final ActivityDataFabric activityDataFabric;

    public CampsiteView(CampsiteService campsiteService,
                        PlotDataFabric plotDataFabric,
                        InstanceService instanceService,
                        ActivityDataFabric activityDataFabric) {
        this.clientsStateViews = new ArrayList<>();
        this.campsiteService = campsiteService;
        this.plotDataFabric = plotDataFabric;
        this.instanceService = instanceService;
        this.activityDataFabric = activityDataFabric;

        GlobalEventHandler eventHandler = MinecraftServer.getGlobalEventHandler();

        eventHandler.addListener(AsyncPlayerConfigurationEvent.class, this::AddCamping);
        eventHandler.addListener(InstanceTickEvent.class, this::Update);
    }

    public void AddCamping(AsyncPlayerConfigurationEvent event) {
        final var player = event.getPlayer();

        var campsite = campsiteService.getCampsiteByOwner(player.getUuid())
                .orElseGet(() -> {
                    var newCampsite = new Campsite(player.getUuid());
                    campsiteService.addCampsite(newCampsite);
                    return newCampsite;
                });

        var spawnPoint = new Vector3d(0, 69, 0);
        if(campsite.getPlots().isEmpty()) {
            var random = new Random();
            var campData = plotDataFabric.getPlotData(PlotType.CAMPSITE);
            var carData = plotDataFabric.getPlotData(PlotType.CARAVAN);

            for (var i = 0; i < 20; i++) {
                var plotData = random.nextBoolean() ? campData : carData;

                var row = i / 5;
                var col = i % 5;
                var xOffset = col * 20;
                var zOffset = row * (plotData.area().getSize().z + 5);

                var offset = new Vector3d(spawnPoint);
                var plot = new Plot(plotData, offset.add(xOffset, 0, zOffset));
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
                var activityData = activityDataFabric.getActivityData(activityType);

                if (activityData == null) {
                    continue;
                }

                var row = i / 5;
                var col = i % 5;
                var xOffset = col * 20;
                var zOffset = row * (activityData.area().getSize().z + 5);

                var offset = new Vector3d(activitySpawnPoint).add(xOffset, 0, zOffset);
                var activity = new Activity(activityData, offset, 15, 5, 2);
                campsite.addActivity(activity);
            }
        }

        var instanceContainer = instanceService.getInstance(campsite);

        event.setSpawningInstance(instanceContainer);
        player.setRespawnPoint(PositionMapper.toMinestomPos(spawnPoint));

        this.clientsStateViews.add(new ClientStateView(campsite, instanceContainer));
    }

    public void Update(InstanceTickEvent event) {
        var deltaTime = event.getDuration() / 1000f;

        for (var clientsStateView : clientsStateViews) {
            var isLinked = instanceService.IsLinked(
                    clientsStateView.getCampsite(),
                    event.getInstance()
            );
            if (isLinked) {
                clientsStateView.Update(deltaTime);
            }
        }
    }
}
