package fr.phylisiumstudio.logic.gameplay;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.client.Client;
import fr.phylisiumstudio.logic.client.ClientLifecycle;
import fr.phylisiumstudio.logic.clock.GamePhase;
import fr.phylisiumstudio.logic.clock.event.PhaseChangeEvent;
import net.minestom.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;

/**
 * Orchestre la boucle de gameplay quotidienne en réagissant aux transitions de
 * phase du cycle jour/nuit (pattern Observer via l'EventHandler global).
 *
 * <p>Au lever du jour : les clients partis la veille quittent définitivement les
 * lieux, les séjours en cours avancent d'un jour (départs → emplacements sales),
 * puis de nouveaux clients arrivent à l'accueil.
 */
@Singleton
public class GameplayLoopService {
    private static final Logger logger = LoggerFactory.getLogger(GameplayLoopService.class);

    private static final int MIN_ARRIVALS = 1;
    private static final int MAX_ARRIVALS = 5;

    private final ClientStayService stayService;
    private final ArrivalGenerator arrivalGenerator;
    private final Random random;

    @Inject
    public GameplayLoopService(ClientStayService stayService,
                               ArrivalGenerator arrivalGenerator,
                               Random random) {
        this.stayService = stayService;
        this.arrivalGenerator = arrivalGenerator;
        this.random = random;

        MinecraftServer.getGlobalEventHandler()
                .addListener(PhaseChangeEvent.class, this::onPhaseChange);
    }

    private void onPhaseChange(PhaseChangeEvent event) {
        if (event.phase() == GamePhase.DAY) {
            openNewDay(event.campsite(), event.dayNumber());
        }
    }

    /**
     * Applique la routine du lever du jour à un camping. Extrait de l'écoute
     * d'événement pour rester directement testable.
     *
     * @return les clients nouvellement arrivés à l'accueil.
     */
    public List<Client> openNewDay(Campsite campsite, long dayNumber) {
        // Les clients en départ la veille quittent définitivement les lieux.
        campsite.getClients().stream()
                .filter(c -> c.getLifecycle() == ClientLifecycle.LEAVING)
                .toList()
                .forEach(stayService::depart);
        stayService.removeDeparted(campsite);

        var departing = stayService.advanceDay(campsite);

        int count = MIN_ARRIVALS + random.nextInt(MAX_ARRIVALS - MIN_ARRIVALS + 1);
        var arrivals = arrivalGenerator.generate(count);
        campsite.getClients().addAll(arrivals);

        logger.info("Day {} for campsite {}: {} departures, {} new arrivals",
                dayNumber, campsite.getUniqueID(), departing.size(), arrivals.size());
        return arrivals;
    }
}
