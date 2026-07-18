package fr.phylisiumstudio.logic.economy;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.client.Client;
import fr.phylisiumstudio.logic.gameplay.AssignmentOutcome;
import fr.phylisiumstudio.logic.gameplay.PlotAssignmentService;
import fr.phylisiumstudio.logic.plot.Plot;

/**
 * Point d'entrée de l'installation d'un client : affecte l'emplacement, encaisse
 * la location et ajuste la satisfaction selon l'alignement du prix sur le marché.
 *
 * <p>Compose les services de placement, d'économie et de satisfaction sans les
 * coupler entre eux (responsabilité unique par service).
 */
@Singleton
public class CheckInService {

    private final PlotAssignmentService assignmentService;
    private final EconomyService economyService;
    private final MarketService marketService;
    private final SatisfactionService satisfactionService;

    @Inject
    public CheckInService(PlotAssignmentService assignmentService,
                          EconomyService economyService,
                          MarketService marketService,
                          SatisfactionService satisfactionService) {
        this.assignmentService = assignmentService;
        this.economyService = economyService;
        this.marketService = marketService;
        this.satisfactionService = satisfactionService;
    }

    /**
     * Installe un client sur un emplacement : placement, ajustement de satisfaction
     * lié au prix, puis encaissement de la location.
     *
     * @return l'issue de l'affectation ; aucun effet monétaire si elle échoue.
     */
    public AssignmentOutcome checkIn(Campsite campsite, Client client, Plot plot) {
        // La satisfaction liée au prix est jugée avant l'installation.
        double ratio = marketService.priceRatio(plot);

        var outcome = assignmentService.assign(campsite, client, plot);
        if (!outcome.isSuccess()) {
            return outcome;
        }

        satisfactionService.evaluatePricing(client, ratio);
        economyService.chargeRent(campsite, client, plot);
        return outcome;
    }
}
