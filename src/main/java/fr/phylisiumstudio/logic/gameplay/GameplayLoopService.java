package fr.phylisiumstudio.logic.gameplay;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.client.Client;
import fr.phylisiumstudio.logic.client.ClientLifecycle;
import fr.phylisiumstudio.logic.clock.GamePhase;
import fr.phylisiumstudio.logic.clock.event.PhaseChangeEvent;
import fr.phylisiumstudio.logic.economy.MarketService;
import fr.phylisiumstudio.logic.economy.SatisfactionService;
import net.minestom.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;

/**
 * Orchestre la boucle de gameplay quotidienne en réagissant aux transitions de
 * phase du cycle jour/nuit (pattern Observer via l'EventHandler global).
 *
 * <p>Au lever du jour : le marché fluctue, les clients partis la veille quittent
 * les lieux (impact réputation), les clients trop insatisfaits abandonnent la
 * file, les séjours avancent d'un jour (départs → emplacements sales), puis de
 * nouveaux clients arrivent — en nombre modulé par la réputation.
 */
@Singleton
public class GameplayLoopService {
    private static final Logger logger = LoggerFactory.getLogger(GameplayLoopService.class);

    private static final int MIN_ARRIVALS = 1;
    private static final int MAX_ARRIVALS = 8;

    private final ClientStayService stayService;
    private final ArrivalGenerator arrivalGenerator;
    private final MarketService marketService;
    private final SatisfactionService satisfactionService;
    private final Random random;

    @Inject
    public GameplayLoopService(ClientStayService stayService,
                               ArrivalGenerator arrivalGenerator,
                               MarketService marketService,
                               SatisfactionService satisfactionService,
                               Random random) {
        this.stayService = stayService;
        this.arrivalGenerator = arrivalGenerator;
        this.marketService = marketService;
        this.satisfactionService = satisfactionService;
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
        marketService.fluctuate();

        // Les clients en départ la veille quittent définitivement les lieux.
        for (var client : campsite.getClients()) {
            if (client.getLifecycle() == ClientLifecycle.LEAVING) {
                satisfactionService.applyDeparture(campsite, client);
                stayService.depart(client);
            }
        }

        // Les clients en attente trop insatisfaits abandonnent la file (perte + réputation).
        int abandoned = 0;
        for (var client : campsite.getClients()) {
            if (client.getLifecycle() == ClientLifecycle.WAITING
                    && satisfactionService.shouldAbandonQueue(client)) {
                satisfactionService.applyQueueAbandonment(campsite);
                client.setLifecycle(ClientLifecycle.GONE);
                abandoned++;
            }
        }

        stayService.removeDeparted(campsite);

        var departing = stayService.advanceDay(campsite);

        var arrivals = arrivalGenerator.generate(arrivalCount(campsite));
        campsite.getClients().addAll(arrivals);

        logger.info("Day {} for campsite {}: {} departures, {} abandoned, {} new arrivals (reputation {})",
                dayNumber, campsite.getUniqueID(), departing.size(), abandoned,
                arrivals.size(), Math.round(campsite.getReputation()));
        return arrivals;
    }

    /** Nombre d'arrivées du jour, modulé par la réputation du camping (0–100). */
    private int arrivalCount(Campsite campsite) {
        double reputationFactor = campsite.getReputation() / 100.0;
        int range = MAX_ARRIVALS - MIN_ARRIVALS;
        int scaledMax = MIN_ARRIVALS + (int) Math.round(range * reputationFactor);
        return MIN_ARRIVALS + random.nextInt(Math.max(1, scaledMax - MIN_ARRIVALS + 1));
    }
}
