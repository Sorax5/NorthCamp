package fr.phylisiumstudio.logic.gameplay;

import com.google.inject.Singleton;
import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.client.Client;
import fr.phylisiumstudio.logic.client.ClientLifecycle;
import fr.phylisiumstudio.logic.economy.SatisfactionService;
import fr.phylisiumstudio.logic.plot.Plot;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Gère l'affectation des clients en attente aux emplacements disponibles
 * (étape « Affectation » de la boucle de gameplay).
 *
 * <p>Service pur : ne dépend d'aucun moteur, entièrement testable.
 */
@Singleton
public class PlotAssignmentService {

    /** Niveau minimal d'emplacement requis pour héberger une famille/groupe. */
    public static final int FAMILY_MIN_LEVEL = 1;

    /** Emplacements disponibles : ni occupés par un séjour en cours, ni sales. */
    public List<Plot> availablePlots(Campsite campsite) {
        Set<Plot> occupied = occupiedPlots(campsite);
        return campsite.getPlots().stream()
                .filter(plot -> !plot.isDirty())
                .filter(plot -> !occupied.contains(plot))
                .collect(Collectors.toList());
    }

    private Set<Plot> occupiedPlots(Campsite campsite) {
        return campsite.getClients().stream()
                .filter(c -> c.getLifecycle() == ClientLifecycle.STAYING)
                .map(Client::getPlot)
                .filter(p -> p != null)
                .collect(Collectors.toSet());
    }

    /**
     * Affecte un client en attente à un emplacement.
     *
     * @return l'issue de l'affectation ; l'état du client n'est modifié que sur {@code SUCCESS}.
     */
    public AssignmentOutcome assign(Campsite campsite, Client client, Plot plot) {
        if (client.getLifecycle() != ClientLifecycle.WAITING) {
            return AssignmentOutcome.CLIENT_NOT_WAITING;
        }
        if (plot.isDirty()) {
            return AssignmentOutcome.PLOT_DIRTY;
        }
        if (occupiedPlots(campsite).contains(plot)) {
            return AssignmentOutcome.PLOT_OCCUPIED;
        }
        if (client.isFamily() && plot.getLevel() < FAMILY_MIN_LEVEL) {
            return AssignmentOutcome.NEEDS_HIGHER_LEVEL;
        }

        client.setPlot(plot);
        client.setLifecycle(ClientLifecycle.STAYING);
        client.setAction(Client.ClientState.SLEEPY);

        // Le type d'emplacement façonne l'expérience : durée de séjour modulée et
        // bonus de satisfaction à l'installation.
        var type = plot.getPlotType();
        int adjustedStay = Math.max(1, (int) Math.round(client.getTotalStayDays() * type.stayMultiplier()));
        client.setTotalStayDays(adjustedStay);
        client.setRemainingDays(adjustedStay);
        SatisfactionService.applyComfort(client, type.comfortBonus());

        return AssignmentOutcome.SUCCESS;
    }

    /**
     * Affecte automatiquement chaque client en attente au premier emplacement
     * compatible (utilisé par un employé d'accueil).
     *
     * @return le nombre de clients installés.
     */
    public int autoAssign(Campsite campsite) {
        int assigned = 0;
        var available = availablePlots(campsite);

        for (var client : campsite.getClients()) {
            if (client.getLifecycle() != ClientLifecycle.WAITING) {
                continue;
            }
            var match = available.stream()
                    .filter(plot -> !client.isFamily() || plot.getLevel() >= FAMILY_MIN_LEVEL)
                    .findFirst();
            if (match.isPresent()) {
                assign(campsite, client, match.get());
                available.remove(match.get());
                assigned++;
            }
        }
        return assigned;
    }
}
