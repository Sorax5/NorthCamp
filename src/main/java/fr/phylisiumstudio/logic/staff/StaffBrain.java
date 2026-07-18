package fr.phylisiumstudio.logic.staff;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.activity.Activity;
import fr.phylisiumstudio.logic.client.Client;
import fr.phylisiumstudio.logic.client.ClientLifecycle;
import fr.phylisiumstudio.logic.economy.CheckInService;
import fr.phylisiumstudio.logic.gameplay.ClientStayService;
import fr.phylisiumstudio.logic.gameplay.PlotAssignmentService;
import fr.phylisiumstudio.logic.plot.Plot;
import org.joml.Vector3d;

import java.util.Optional;

/**
 * Logique de travail d'un employé, calquée sur celle des clients : trouver la
 * tâche la plus prioritaire de son rôle, s'y rendre, puis l'accomplir sur place
 * dès que possible.
 *
 * <p>Service pur (hors moteur) partagé par les entités employés.
 */
@Singleton
public class StaffBrain {

    private final PlotAssignmentService assignmentService;
    private final ClientStayService stayService;
    private final CheckInService checkInService;

    @Inject
    public StaffBrain(PlotAssignmentService assignmentService, ClientStayService stayService,
                      CheckInService checkInService) {
        this.assignmentService = assignmentService;
        this.stayService = stayService;
        this.checkInService = checkInService;
    }

    /** Position vers laquelle l'employé doit se rendre pour travailler, selon son rôle. */
    public Vector3d target(Staff staff, Campsite campsite, Vector3d reception) {
        var role = staff.getAssignedRole();
        if (role == null) {
            return reception;
        }
        return switch (role) {
            case CLEANING -> firstDirtyPlot(campsite).map(Plot::getPosition).orElse(reception);
            case MAINTENANCE -> firstBrokenActivity(campsite).map(Activity::getPosition).orElse(reception);
            case RECEPTION, FINANCE -> reception;
        };
    }

    /**
     * Accomplit une unité de travail à l'endroit courant, selon le rôle.
     *
     * @return {@code true} si une tâche a réellement été effectuée.
     */
    public boolean work(Staff staff, Campsite campsite) {
        var role = staff.getAssignedRole();
        if (role == null) {
            return false;
        }
        return switch (role) {
            case CLEANING -> clean(campsite);
            case MAINTENANCE -> maintain(campsite);
            case RECEPTION -> welcome(campsite);
            case FINANCE -> false; // rendement financier appliqué quotidiennement
        };
    }

    private boolean clean(Campsite campsite) {
        var plot = firstDirtyPlot(campsite).orElse(null);
        if (plot == null) {
            return false;
        }
        stayService.cleanPlot(plot);
        return true;
    }

    private boolean maintain(Campsite campsite) {
        var activity = firstBrokenActivity(campsite).orElse(null);
        if (activity == null) {
            return false;
        }
        activity.setOperational(true);
        return true;
    }

    private boolean welcome(Campsite campsite) {
        var client = campsite.getClients().stream()
                .filter(c -> c.getLifecycle() == ClientLifecycle.WAITING)
                .findFirst().orElse(null);
        if (client == null) {
            return false;
        }
        var plot = assignmentService.availablePlots(campsite).stream()
                .filter(p -> !client.isFamily() || p.getLevel() >= PlotAssignmentService.FAMILY_MIN_LEVEL)
                .findFirst().orElse(null);
        if (plot == null) {
            return false;
        }
        return checkInService.checkIn(campsite, client, plot).isSuccess();
    }

    private Optional<Plot> firstDirtyPlot(Campsite campsite) {
        return campsite.getPlots().stream().filter(Plot::isDirty).findFirst();
    }

    private Optional<Activity> firstBrokenActivity(Campsite campsite) {
        return campsite.getActivities().stream().filter(a -> !a.isOperational()).findFirst();
    }

    /** Vrai s'il reste du travail pour ce rôle (utile pour l'animation/feedback). */
    public boolean hasWork(Staff staff, Campsite campsite) {
        var role = staff.getAssignedRole();
        if (role == null) {
            return false;
        }
        return switch (role) {
            case CLEANING -> firstDirtyPlot(campsite).isPresent();
            case MAINTENANCE -> firstBrokenActivity(campsite).isPresent();
            case RECEPTION -> campsite.getClients().stream()
                    .anyMatch(c -> c.getLifecycle() == ClientLifecycle.WAITING);
            case FINANCE -> false;
        };
    }
}
