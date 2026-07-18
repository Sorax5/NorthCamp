package fr.phylisiumstudio.logic.gameplay;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.activity.Activity;
import fr.phylisiumstudio.logic.client.Client;
import fr.phylisiumstudio.logic.client.ClientLifecycle;
import fr.phylisiumstudio.logic.clock.GamePhase;
import fr.phylisiumstudio.logic.clock.event.PhaseChangeEvent;
import fr.phylisiumstudio.logic.economy.MarketService;
import fr.phylisiumstudio.logic.economy.SatisfactionService;
import fr.phylisiumstudio.logic.season.SeasonService;
import fr.phylisiumstudio.logic.staff.StaffService;
import net.minestom.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;

/**
 * Orchestre la boucle de gameplay quotidienne en réagissant aux transitions de
 * phase du cycle jour/nuit (pattern Observer via l'EventHandler global).
 *
 * <p>Au lever du jour : le marché fluctue, les activités s'usent, les clients
 * partis la veille quittent les lieux (impact réputation), les clients trop
 * insatisfaits abandonnent la file, les séjours avancent d'un jour (départs →
 * emplacements sales), puis de nouveaux clients arrivent — en nombre modulé par
 * la réputation et la saison. Enfin, les employés travaillent.
 */
@Singleton
public class GameplayLoopService {
    private static final Logger logger = LoggerFactory.getLogger(GameplayLoopService.class);

    private static final int MIN_ARRIVALS = 1;
    private static final int MAX_ARRIVALS = 8;
    /** Probabilité qu'une activité opérationnelle s'use et tombe en panne chaque jour. */
    private static final double ACTIVITY_DEGRADE_CHANCE = 0.25;

    private final ClientStayService stayService;
    private final ArrivalGenerator arrivalGenerator;
    private final MarketService marketService;
    private final SatisfactionService satisfactionService;
    private final SeasonService seasonService;
    private final StaffService staffService;
    private final Random random;

    @Inject
    public GameplayLoopService(ClientStayService stayService,
                               ArrivalGenerator arrivalGenerator,
                               MarketService marketService,
                               SatisfactionService satisfactionService,
                               SeasonService seasonService,
                               StaffService staffService,
                               Random random) {
        this.stayService = stayService;
        this.arrivalGenerator = arrivalGenerator;
        this.marketService = marketService;
        this.satisfactionService = satisfactionService;
        this.seasonService = seasonService;
        this.staffService = staffService;
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
        degradeActivities(campsite);

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

        var arrivals = arrivalGenerator.generate(arrivalCount(campsite, dayNumber));
        campsite.getClients().addAll(arrivals);

        // Les employés travaillent : salaires prélevés, puis accueil/nettoyage/maintenance.
        double salaries = staffService.paySalaries(campsite);
        staffService.runAutomation(campsite);

        logger.info("Day {} ({}{}) for campsite {}: {} departures, {} abandoned, {} arrivals, {} salaries (reputation {})",
                dayNumber, seasonService.seasonOf(dayNumber).displayName(),
                seasonService.isSpecialEvent(dayNumber) ? " - événement spécial" : "",
                campsite.getUniqueID(), departing.size(), abandoned, arrivals.size(),
                salaries, Math.round(campsite.getReputation()));
        return arrivals;
    }

    /** Usure quotidienne : chaque activité opérationnelle peut tomber en panne. */
    private void degradeActivities(Campsite campsite) {
        for (Activity activity : campsite.getActivities()) {
            if (activity.isOperational() && random.nextDouble() < ACTIVITY_DEGRADE_CHANCE) {
                activity.setOperational(false);
            }
        }
    }

    /** Nombre d'arrivées du jour, modulé par la réputation du camping et la saison. */
    private int arrivalCount(Campsite campsite, long dayNumber) {
        double reputationFactor = campsite.getReputation() / 100.0;
        int range = MAX_ARRIVALS - MIN_ARRIVALS;
        int scaledMax = MIN_ARRIVALS + (int) Math.round(range * reputationFactor);
        int base = MIN_ARRIVALS + random.nextInt(Math.max(1, scaledMax - MIN_ARRIVALS + 1));
        return (int) Math.round(base * seasonService.arrivalMultiplier(dayNumber));
    }
}
