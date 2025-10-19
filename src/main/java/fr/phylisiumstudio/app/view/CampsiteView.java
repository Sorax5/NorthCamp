package fr.phylisiumstudio.app.view;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.client.Client;
import fr.phylisiumstudio.logic.mapper.PositionMapper;
import fr.phylisiumstudio.logic.plot.Plot;
import fr.phylisiumstudio.logic.plot.PlotData;
import fr.phylisiumstudio.logic.plot.PlotType;
import fr.phylisiumstudio.logic.plot.fabric.PlotDataFabric;
import fr.phylisiumstudio.logic.service.CampsiteService;
import fr.phylisiumstudio.logic.service.InstanceService;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.instance.InstanceTickEvent;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.instance.InstanceContainer;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public class CampsiteView {
    private List<ClientStateView> clientsStateViews;
    private final CampsiteService campsiteService;
    private final PlotDataFabric plotDataFabric;
    private final InstanceService instanceService;

    public CampsiteView(CampsiteService campsiteService,
                        PlotDataFabric plotDataFabric,
                        InstanceService instanceService) {
        this.clientsStateViews = new ArrayList<>();
        this.campsiteService = campsiteService;
        this.plotDataFabric = plotDataFabric;
        this.instanceService = instanceService;

        GlobalEventHandler eventHandler = MinecraftServer.getGlobalEventHandler();

        eventHandler.addListener(AsyncPlayerConfigurationEvent.class, this::AddCamping);
        eventHandler.addListener(InstanceTickEvent.class, this::Update);
    }

    public void AddCamping(AsyncPlayerConfigurationEvent event) {
        final Player player = event.getPlayer();

        Optional<Campsite> optionalCampsite = campsiteService.getCampsiteByOwner(player.getUuid());
        Campsite campsite = optionalCampsite
                .orElseGet(() -> {
                    Campsite newCampsite = new Campsite(player.getUuid());
                    campsiteService.addCampsite(newCampsite);
                    return newCampsite;
                });

        Vector3d spawnPoint = new Vector3d(0, 69, 0);
        /*if(campsite.getPlots().isEmpty()) {
            Random random = new Random();
            PlotData campData = plotDataFabric.getPlotData(PlotType.CAMPSITE);
            PlotData carData = plotDataFabric.getPlotData(PlotType.CARAVAN);

            PlotData plotData = random.nextBoolean() ? campData : carData;
            for (int i = 0; i < 5; i++) {
                Vector3d offset = new Vector3d(spawnPoint);
                Plot plot = new Plot(plotData, offset.add(0,-1, i * (plotData.area().getSize().z + 5)));
                campsite.addPlot(plot);
            }
        }*/

        if(campsite.getClients().isEmpty()) {
            campsite.addClient(new Client());
        }

        InstanceContainer instanceContainer = instanceService.getInstance(campsite);

        event.setSpawningInstance(instanceContainer);
        player.setRespawnPoint(PositionMapper.toMinestomPos(spawnPoint));

        this.clientsStateViews.add(new ClientStateView(campsite));
    }

    public void Update(InstanceTickEvent event) {
        float deltaTime = event.getDuration() / 1000f;

        for (ClientStateView clientsStateView : clientsStateViews) {
            boolean isLinked = instanceService.IsLinked(
                    clientsStateView.getCampsite(),
                    event.getInstance()
            );
            if (isLinked) {
                clientsStateView.Update(deltaTime);
            }
        }
    }
}
