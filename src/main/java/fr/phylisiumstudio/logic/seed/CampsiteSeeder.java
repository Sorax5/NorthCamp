package fr.phylisiumstudio.logic.seed;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.activity.Activity;
import fr.phylisiumstudio.logic.activity.ActivityType;
import fr.phylisiumstudio.logic.client.Client;
import fr.phylisiumstudio.logic.economy.MarketService;
import fr.phylisiumstudio.logic.plot.Plot;
import fr.phylisiumstudio.logic.plot.PlotType;
import fr.phylisiumstudio.logic.service.PlotDataService;
import fr.phylisiumstudio.logic.staff.StaffFactory;
import fr.phylisiumstudio.logic.staff.StaffRole;
import org.joml.Vector3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Remplit un camping neuf avec un contenu de démonstration cohérent pour tester
 * toutes les fonctionnalités immédiatement : emplacements variés et améliorés,
 * activités, clients (en attente et en séjour), employés et trésorerie.
 */
@Singleton
public class CampsiteSeeder {
    private static final Logger logger = LoggerFactory.getLogger(CampsiteSeeder.class);

    private static final Vector3d PLOT_ORIGIN = new Vector3d(0, 69, 0);
    private static final Vector3d ACTIVITY_ORIGIN = new Vector3d(0, 69, -20);
    private static final int PLOTS_PER_TYPE = 4;
    private static final int PLOT_SPACING = 12;
    private static final int ACTIVITY_SPACING = 12;
    private static final double STARTING_MONEY = 2_000.0;

    private final PlotDataService plotDataService;
    private final MarketService marketService;
    private final StaffFactory staffFactory;

    @Inject
    public CampsiteSeeder(PlotDataService plotDataService, MarketService marketService, StaffFactory staffFactory) {
        this.plotDataService = plotDataService;
        this.marketService = marketService;
        this.staffFactory = staffFactory;
    }

    /** Remplit le camping s'il est vide ; ne fait rien sinon (idempotent). */
    public void seedIfEmpty(Campsite campsite) {
        if (!campsite.getPlots().isEmpty()) {
            return;
        }
        seedPlots(campsite);
        seedActivities(campsite);
        seedClients(campsite);
        seedStaff(campsite);
        campsite.addMoney(STARTING_MONEY);

        logger.info("Seeded demo campsite {}: {} plots, {} activities, {} clients, {} staff",
                campsite.getUniqueID(), campsite.getPlots().size(), campsite.getActivities().size(),
                campsite.getClients().size(), campsite.getStaff().size());
    }

    private void seedPlots(Campsite campsite) {
        int column = 0;
        for (var type : PlotType.values()) {
            double fair = marketService.fairPrice(type);
            for (int row = 0; row < PLOTS_PER_TYPE; row++) {
                var position = new Vector3d(PLOT_ORIGIN)
                        .add(column * PLOT_SPACING, 0, row * PLOT_SPACING);
                var plot = new Plot(position, type);
                // Quelques emplacements améliorés pour accueillir les familles.
                plot.setLevel(row % 3);
                plot.setPrice(Math.round(fair));
                campsite.addPlot(plot);
            }
            column++;
        }
    }

    private void seedActivities(Campsite campsite) {
        int i = 0;
        for (var type : ActivityType.values()) {
            var position = new Vector3d(ACTIVITY_ORIGIN).add(i * ACTIVITY_SPACING, 0, 0);
            var activity = new Activity(position, 15, 5, 4, type);
            campsite.addActivity(activity);
            i++;
        }
    }

    private void seedClients(Campsite campsite) {
        // Tous les emplacements démarrent vides : c'est la file d'attente et les
        // employés qui remplissent le camping au fil de la simulation.
        campsite.addClient(new Client(1, 2, 150));
        campsite.addClient(new Client(1, 4, 320));
        campsite.addClient(new Client(2, 3, 450));
        campsite.addClient(new Client(3, 3, 600));
        campsite.addClient(new Client(4, 2, 700));
        campsite.addClient(new Client(1, 5, 400));
    }

    private void seedStaff(Campsite campsite) {
        hire(campsite, StaffRole.RECEPTION);
        hire(campsite, StaffRole.CLEANING);
        hire(campsite, StaffRole.MAINTENANCE);
    }

    private void hire(Campsite campsite, StaffRole role) {
        var staff = staffFactory.generateCandidate();
        staff.setAssignedRole(role);
        campsite.addStaff(staff);
    }
}
