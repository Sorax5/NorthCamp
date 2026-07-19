package fr.phylisiumstudio.logic.staff;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.activity.Activity;
import fr.phylisiumstudio.logic.activity.ActivitySupplyService;
import fr.phylisiumstudio.logic.client.Client;
import fr.phylisiumstudio.logic.client.ClientLifecycle;
import fr.phylisiumstudio.logic.economy.CheckInService;
import fr.phylisiumstudio.logic.economy.EconomyService;
import fr.phylisiumstudio.logic.gameplay.PlotAssignmentService;
import fr.phylisiumstudio.logic.gameplay.ClientStayService;

import java.util.UUID;

/**
 * Gère les employés : recrutement, licenciement, versement des salaires et
 * automatisation des tâches physiques selon leur rôle et leur compétence.
 *
 * <p>La compétence (0–1) module le débit : un employé doué traite plus de tâches
 * par jour qu'un employé médiocre.
 */
@Singleton
public class StaffService {

    /** Tâches supplémentaires qu'un employé parfaitement compétent traite en plus de la base. */
    private static final int MAX_EXTRA_CAPACITY = 4;
    /** Rendement financier quotidien maximal (à compétence 1) d'un employé finance. */
    private static final double FINANCE_MAX_YIELD = 0.02;
    /**
     * Plafond absolu du rendement finance par employé et par jour. Coupe l'effet
     * boule de neige des intérêts composés (sinon : plus on est riche, plus on
     * gagne passivement — stratégie dégénérée).
     */
    private static final double FINANCE_DAILY_CAP = 500.0;
    /** En dessous de ce stock, un employé au ravitaillement réapprovisionne son activité. */
    private static final int SUPPLY_LOW_THRESHOLD = 5;
    /** Stock cible atteint après un réapprovisionnement. */
    private static final int SUPPLY_TARGET_STOCK = 15;

    private final PlotAssignmentService assignmentService;
    private final ClientStayService stayService;
    private final CheckInService checkInService;
    private final EconomyService economyService;
    private final ActivitySupplyService supplyService;

    @Inject
    public StaffService(PlotAssignmentService assignmentService,
                        ClientStayService stayService,
                        CheckInService checkInService,
                        EconomyService economyService,
                        ActivitySupplyService supplyService) {
        this.assignmentService = assignmentService;
        this.stayService = stayService;
        this.checkInService = checkInService;
        this.economyService = economyService;
        this.supplyService = supplyService;
    }

    public void hire(Campsite campsite, Staff staff) {
        campsite.addStaff(staff);
    }

    public void fire(Campsite campsite, UUID staffId) {
        campsite.getStaff().removeIf(s -> s.getUniqueId().equals(staffId));
    }

    /**
     * Prélève le salaire quotidien de chaque employé sur les finances du camping.
     *
     * @return le total des salaires versés.
     */
    public double paySalaries(Campsite campsite) {
        double total = campsite.getStaff().stream().mapToDouble(Staff::getDailySalary).sum();
        economyService.charge(campsite, total);
        return total;
    }

    /**
     * Exécute le travail des employés sur une journée : chaque employé agit selon
     * son rôle assigné, dans la limite de son débit.
     */
    public void runAutomation(Campsite campsite) {
        for (var staff : campsite.getStaff()) {
            var role = staff.getAssignedRole();
            if (role == null) {
                continue;
            }
            int capacity = capacity(staff.skill(role));
            switch (role) {
                case RECEPTION -> welcomeClients(campsite, capacity);
                case CLEANING -> cleanPlots(campsite, capacity);
                case MAINTENANCE -> maintainActivities(campsite, capacity);
                case SUPPLY -> restockAssignedActivity(campsite, staff);
                case FINANCE -> applyFinanceYield(campsite, staff.skill(StaffRole.FINANCE));
            }
        }
    }

    /** Débit quotidien : au moins 1 tâche, jusqu'à {@code 1 + MAX_EXTRA_CAPACITY} selon la compétence. */
    private int capacity(double skill) {
        return 1 + (int) Math.round(Math.max(0.0, Math.min(1.0, skill)) * MAX_EXTRA_CAPACITY);
    }

    private void welcomeClients(Campsite campsite, int capacity) {
        int done = 0;
        for (var client : campsite.getClients()) {
            if (done >= capacity) {
                break;
            }
            if (client.getLifecycle() != ClientLifecycle.WAITING) {
                continue;
            }
            var plot = assignmentService.availablePlots(campsite).stream()
                    .filter(p -> !client.isFamily() || p.getLevel() >= PlotAssignmentService.FAMILY_MIN_LEVEL)
                    .findFirst();
            if (plot.isPresent() && checkInService.checkIn(campsite, client, plot.get()).isSuccess()) {
                done++;
            }
        }
    }

    private void cleanPlots(Campsite campsite, int capacity) {
        int done = 0;
        for (var plot : campsite.getPlots()) {
            if (done >= capacity) {
                break;
            }
            if (plot.isDirty()) {
                stayService.cleanPlot(plot);
                done++;
            }
        }
    }

    /** Rendement financier : une bonne gestion génère un revenu proportionnel au solde. */
    private void applyFinanceYield(Campsite campsite, double skill) {
        double yield = campsite.getMoney() * FINANCE_MAX_YIELD * Math.max(0.0, Math.min(1.0, skill));
        yield = Math.min(yield, FINANCE_DAILY_CAP);
        if (yield > 0) {
            campsite.addMoney(yield);
        }
    }

    private void maintainActivities(Campsite campsite, int capacity) {
        int done = 0;
        for (var activity : campsite.getActivities()) {
            if (done >= capacity) {
                break;
            }
            if (!activity.isOperational()) {
                activity.repair();
                done++;
            }
        }
    }

    /**
     * Ravitaillement ciblé : l'employé réapprovisionne l'unique activité qui lui est
     * assignée si son stock passe sous le seuil bas (achat des fournitures manquantes).
     */
    private void restockAssignedActivity(Campsite campsite, Staff staff) {
        var activityId = staff.getAssignedActivityId();
        if (activityId == null) {
            return;
        }
        Activity activity = campsite.getActivities().stream()
                .filter(a -> a.getUniqueID().equals(activityId))
                .findFirst().orElse(null);
        if (activity == null || !activity.getType().consumesSupplies()) {
            return;
        }
        if (activity.getSupplies() < SUPPLY_LOW_THRESHOLD) {
            supplyService.restock(campsite, activity, SUPPLY_TARGET_STOCK - activity.getSupplies());
        }
    }
}
