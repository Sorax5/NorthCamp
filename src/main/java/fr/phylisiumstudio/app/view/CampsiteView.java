package fr.phylisiumstudio.app.view;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import fr.phylisiumstudio.app.App;
import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.clock.GameClockService;
import fr.phylisiumstudio.logic.mapper.PositionMapper;
import fr.phylisiumstudio.logic.marker.MarkerRegistry;
import fr.phylisiumstudio.logic.seed.CampsiteSeeder;
import fr.phylisiumstudio.logic.service.CampsiteService;
import fr.phylisiumstudio.logic.service.InstanceService;
import fr.phylisiumstudio.logic.skin.SkinLibrary;
import fr.phylisiumstudio.logic.slot.LayoutService;
import fr.phylisiumstudio.logic.staff.StaffBrain;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.GameMode;
import net.minestom.server.event.EventNode;
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

    private static final Vector3d DEFAULT_SPAWN_POINT = new Vector3d(0, 69, 0);
    private static final Vector3d STAFF_ORIGIN = new Vector3d(0, 69, -5);

    private final List<ClientView> clientsStateViews;
    private final List<StaffView> staffViews;
    private final List<PlaceInfoView> placeInfoViews;
    private final CampsiteService campsiteService;
    private final InstanceService instanceService;
    private final GameClockService gameClockService;
    private final CampsiteSeeder campsiteSeeder;
    private final SkinLibrary skinLibrary;
    private final MarkerRegistry markerRegistry;
    private final LayoutService layoutService;
    private final StaffBrain staffBrain;
    private final Random random;
    private final boolean seedTestCampsite;

    @Inject
    public CampsiteView(CampsiteService campsiteService,
                        InstanceService instanceService,
                        GameClockService gameClockService,
                        CampsiteSeeder campsiteSeeder,
                        SkinLibrary skinLibrary,
                        MarkerRegistry markerRegistry,
                        LayoutService layoutService,
                        StaffBrain staffBrain,
                        Random random,
                        App app) {
        this.clientsStateViews = new CopyOnWriteArrayList<>();
        this.staffViews = new CopyOnWriteArrayList<>();
        this.placeInfoViews = new CopyOnWriteArrayList<>();
        this.campsiteService = campsiteService;
        this.instanceService = instanceService;
        this.gameClockService = gameClockService;
        this.campsiteSeeder = campsiteSeeder;
        this.skinLibrary = skinLibrary;
        this.markerRegistry = markerRegistry;
        this.layoutService = layoutService;
        this.staffBrain = staffBrain;
        this.random = random;
        var config = app.getMainConfig();
        this.seedTestCampsite = config == null || config.SeedTestCampsite;

        // Nœud dédié au cycle de vie joueur ↔ camping, attaché à la racine.
        var node = EventNode.all("campsite-view");
        node.addListener(AsyncPlayerConfigurationEvent.class, this::addCamping);
        node.addListener(PlayerDisconnectEvent.class, this::onPlayerDisconnect);
        node.addListener(PlayerSpawnEvent.class, event -> {
            var player = event.getPlayer();
            // Mode aventure : le joueur observe et gère, il ne casse ni ne pose de bloc.
            player.setGameMode(GameMode.ADVENTURE);
            player.setAllowFlying(true);
        });
        MinecraftServer.getGlobalEventHandler().addChild(node);
    }

    public void addCamping(AsyncPlayerConfigurationEvent event) {
        final var player = event.getPlayer();

        var campsite = campsiteService.getCampsiteByOwner(player.getUuid())
                .orElseGet(() -> {
                    var newCampsite = new Campsite(player.getUuid());
                    campsiteService.addCampsite(newCampsite);
                    return newCampsite;
                });

        var spawnPoint = new Vector3d(DEFAULT_SPAWN_POINT);
        if (seedTestCampsite) {
            campsiteSeeder.seedIfEmpty(campsite);
        }

        var instanceContainer = instanceService.getInstance(campsite);

        event.setSpawningInstance(instanceContainer);
        player.setRespawnPoint(PositionMapper.toMinestomPos(spawnPoint));

        gameClockService.start(campsite, instanceContainer);
        var reception = layoutService.receptionPosition();
        var exit = layoutService.exitPosition();
        this.clientsStateViews.add(new ClientView(campsite, instanceContainer, reception, exit, skinLibrary, random));
        this.staffViews.add(new StaffView(campsite, instanceContainer, new Vector3d(STAFF_ORIGIN), reception, skinLibrary, staffBrain));
        this.placeInfoViews.add(new PlaceInfoView(campsite, instanceContainer, markerRegistry, layoutService));
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

            clientsStateViews.removeIf(view -> {
                if (view.getCampsite().getUniqueID().equals(campsite.getUniqueID())) {
                    view.dispose();
                    return true;
                }
                return false;
            });

            staffViews.removeIf(view -> {
                if (view.getCampsite().getUniqueID().equals(campsite.getUniqueID())) {
                    view.dispose();
                    return true;
                }
                return false;
            });

            placeInfoViews.removeIf(view -> {
                if (view.getCampsite().getUniqueID().equals(campsite.getUniqueID())) {
                    view.dispose();
                    return true;
                }
                return false;
            });

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
