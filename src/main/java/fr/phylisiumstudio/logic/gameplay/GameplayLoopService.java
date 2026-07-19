package fr.phylisiumstudio.logic.gameplay;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.activity.Activity;
import fr.phylisiumstudio.logic.client.ClientLifecycle;
import fr.phylisiumstudio.logic.clock.GamePhase;
import fr.phylisiumstudio.logic.clock.event.PhaseChangeEvent;
import fr.phylisiumstudio.logic.economy.MarketService;
import fr.phylisiumstudio.logic.economy.SatisfactionService;
import fr.phylisiumstudio.logic.season.SeasonService;
import fr.phylisiumstudio.logic.staff.StaffService;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.EventNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    /** Probabilité qu'une activité opérationnelle s'use et tombe en panne chaque jour. */
    private static final double ACTIVITY_DEGRADE_CHANCE = 0.25;

    private final ClientStayService stayService;
    private final MarketService marketService;
    private final SatisfactionService satisfactionService;
    private final SeasonService seasonService;
    private final StaffService staffService;
    private final Random random;

    @Inject
    public GameplayLoopService(ClientStayService stayService,
                               MarketService marketService,
                               SatisfactionService satisfactionService,
                               SeasonService seasonService,
                               StaffService staffService,
                               Random random) {
        this.stayService = stayService;
        this.marketService = marketService;
        this.satisfactionService = satisfactionService;
        this.seasonService = seasonService;
        this.staffService = staffService;
        this.random = random;

        // Nœud dédié attaché à la racine : regroupe la logique de la boucle et
        // reflète la structure du serveur plutôt que d'empiler sur le handler global.
        var node = EventNode.all("gameplay-loop");
        node.addListener(PhaseChangeEvent.class, this::onPhaseChange);
        MinecraftServer.getGlobalEventHandler().addChild(node);
    }

    private void onPhaseChange(PhaseChangeEvent event) {
        if (event.phase() == GamePhase.DAY) {
            openNewDay(event.campsite(), event.dayNumber());
        }
    }

    /**
     * Applique la routine du lever du jour à un camping (départs, abandons,
     * usure, réputation, salaires). Les arrivées, elles, se font en continu
     * pendant la journée via {@link ArrivalService}.
     */
    public void openNewDay(Campsite campsite, long dayNumber) {
        marketService.fluctuate();
        degradeActivities(campsite);

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

        // Les clients qui partaient hier et n'ont pas fini de sortir quittent
        // définitivement (leur impact réputation a déjà été appliqué à la fin du séjour).
        for (var client : campsite.getClients()) {
            if (client.getLifecycle() == ClientLifecycle.LEAVING) {
                stayService.depart(client);
            }
        }

        // Retire tous les partis (despawn par l'IA, abandons, sorties forcées).
        stayService.removeDeparted(campsite);

        // Les séjours avancent ; ceux qui se terminent passent en LEAVING et leur
        // satisfaction finale est reportée maintenant sur la réputation.
        var departing = stayService.advanceDay(campsite);
        for (var client : departing) {
            satisfactionService.applyDeparture(campsite, client);
        }

        // Les employés travaillent : salaires prélevés, puis accueil/nettoyage/maintenance.
        double salaries = staffService.paySalaries(campsite);
        staffService.runAutomation(campsite);

        logger.info("Day {} ({}{}) for campsite {}: {} departures, {} abandoned, {} salaries (reputation {})",
                dayNumber, seasonService.seasonOf(dayNumber).displayName(),
                seasonService.isSpecialEvent(dayNumber) ? " - événement spécial" : "",
                campsite.getUniqueID(), departing.size(), abandoned,
                salaries, Math.round(campsite.getReputation()));
    }

    /** Usure quotidienne : chaque activité opérationnelle peut tomber en panne. */
    private void degradeActivities(Campsite campsite) {
        for (Activity activity : campsite.getActivities()) {
            if (activity.isOperational() && random.nextDouble() < ACTIVITY_DEGRADE_CHANCE) {
                activity.setOperational(false);
            }
        }
    }

}
