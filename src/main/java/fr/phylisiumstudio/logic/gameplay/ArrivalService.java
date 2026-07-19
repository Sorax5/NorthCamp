package fr.phylisiumstudio.logic.gameplay;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.client.ClientLifecycle;
import fr.phylisiumstudio.logic.clock.GamePhase;
import fr.phylisiumstudio.logic.clock.GameClockService;
import net.minestom.server.MinecraftServer;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fait arriver les clients au compte-goutte pendant la journée plutôt que d'un
 * bloc. Le taux dépend de la disponibilité (emplacements libres), de la
 * réputation et de l'engorgement de la file d'attente.
 */
@Singleton
public class ArrivalService {

    private static final int TICK_SECONDS = 5;
    private static final double BASE_CHANCE = 0.25;

    private final ArrivalGenerator arrivalGenerator;
    private final GameClockService gameClockService;
    private final PlotAssignmentService assignmentService;
    private final java.util.Random random;

    private final ConcurrentHashMap<UUID, Task> tasks = new ConcurrentHashMap<>();

    @Inject
    public ArrivalService(ArrivalGenerator arrivalGenerator, GameClockService gameClockService,
                          PlotAssignmentService assignmentService, java.util.Random random) {
        this.arrivalGenerator = arrivalGenerator;
        this.gameClockService = gameClockService;
        this.assignmentService = assignmentService;
        this.random = random;
    }

    public void start(Campsite campsite) {
        tasks.computeIfAbsent(campsite.getUniqueID(), _ ->
                MinecraftServer.getSchedulerManager()
                        .buildTask(() -> maybeArrive(campsite))
                        .repeat(TaskSchedule.seconds(TICK_SECONDS))
                        .schedule());
    }

    public void stop(UUID campsiteId) {
        var task = tasks.remove(campsiteId);
        if (task != null) {
            task.cancel();
        }
    }

    private void maybeArrive(Campsite campsite) {
        if (gameClockService.getPhase(campsite.getUniqueID()).orElse(GamePhase.NIGHT) != GamePhase.DAY) {
            return;
        }
        if (campsite.getClients().size() >= maxClients(campsite)) {
            return;
        }
        if (random.nextDouble() < arrivalChance(campsite)) {
            campsite.addClient(arrivalGenerator.generate());
        }
    }

    /** Probabilité d'une arrivée sur un tick, combinant réputation et disponibilité. */
    public double arrivalChance(Campsite campsite) {
        int free = assignmentService.availablePlots(campsite).size();
        long waiting = campsite.getClients().stream()
                .filter(c -> c.getLifecycle() == ClientLifecycle.WAITING).count();

        double reputationFactor = 0.5 + campsite.getReputation() / 100.0;
        double availabilityFactor = free > 0 ? 1.0 : 0.3;
        if (waiting > free + 5) {
            availabilityFactor *= 0.3;
        }

        // Brevet « Marketing » : +20 % de chance d'arrivée.
        double marketing = campsite.hasPatent(fr.phylisiumstudio.logic.vendor.Patent.MARKETING) ? 1.2 : 1.0;

        return Math.clamp(BASE_CHANCE * reputationFactor * availabilityFactor * marketing, 0.0, 0.9);
    }

    private int maxClients(Campsite campsite) {
        return campsite.getPlots().size() * 2 + 10;
    }
}
