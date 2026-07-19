package fr.phylisiumstudio.logic.slot;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.activity.Activity;
import fr.phylisiumstudio.logic.activity.ActivityType;
import fr.phylisiumstudio.logic.economy.EconomyService;
import fr.phylisiumstudio.logic.economy.MarketService;
import fr.phylisiumstudio.logic.plot.Plot;
import fr.phylisiumstudio.logic.plot.PlotType;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Gère les emplacements constructibles : liste ceux encore disponibles et
 * applique l'achat puis la définition d'un type (camping, activité…).
 *
 * <p>Un slot n'est ni un camping ni une activité tant que le joueur ne l'a pas
 * acheté et défini.
 */
@Singleton
public class SlotService {

    /** Coût d'acquisition d'un emplacement de camping. */
    public static final double PLOT_SLOT_PRICE = 1_000.0;
    /** Coût d'acquisition d'un emplacement d'activité. */
    public static final double ACTIVITY_SLOT_PRICE = 1_500.0;

    private final LayoutService layoutService;
    private final EconomyService economyService;
    private final MarketService marketService;

    @Inject
    public SlotService(LayoutService layoutService, EconomyService economyService, MarketService marketService) {
        this.layoutService = layoutService;
        this.economyService = economyService;
        this.marketService = marketService;
    }

    /** Emplacements de camping encore libres (non déjà occupés par un plot défini). */
    public List<Slot> availablePlotSlots(Campsite campsite) {
        var occupied = roundedPositions(campsite.getPlots().stream().map(Plot::getPosition).toList());
        return filterAvailable(layoutService.plotSlotPositions(), occupied, SlotKind.PLOT);
    }

    /** Emplacements d'activité encore libres. */
    public List<Slot> availableActivitySlots(Campsite campsite) {
        var occupied = roundedPositions(campsite.getActivities().stream().map(Activity::getPosition).toList());
        return filterAvailable(layoutService.activitySlotPositions(), occupied, SlotKind.ACTIVITY);
    }

    /**
     * Achète et définit un emplacement de camping du type choisi.
     *
     * @return le {@link Plot} créé, ou {@code null} si l'achat a échoué
     *         (solde insuffisant ou slot déjà pris).
     */
    public Plot buyPlot(Campsite campsite, Vector3d position, PlotType type) {
        if (campsite.getMoney() < PLOT_SLOT_PRICE || !isAvailable(availablePlotSlots(campsite), position)) {
            return null;
        }
        economyService.charge(campsite, PLOT_SLOT_PRICE);
        var plot = new Plot(new Vector3d(position), type);
        // Tarif initial aligné sur le marché : le joueur encaisse dès la 1re nuit
        // (satisfaction neutre) et ajuste ensuite à la hausse ou à la baisse.
        plot.setPrice(marketService.fairPrice(type));
        campsite.addPlot(plot);
        return plot;
    }

    /**
     * Achète et définit un emplacement d'activité du type choisi.
     *
     * @return l'{@link Activity} créée, ou {@code null} si l'achat a échoué.
     */
    public Activity buyActivity(Campsite campsite, Vector3d position, ActivityType type) {
        if (campsite.getMoney() < ACTIVITY_SLOT_PRICE || !isAvailable(availableActivitySlots(campsite), position)) {
            return null;
        }
        economyService.charge(campsite, ACTIVITY_SLOT_PRICE);
        var activity = new Activity(new Vector3d(position), 15, 5, 4, type);
        campsite.addActivity(activity);
        return activity;
    }

    private List<Slot> filterAvailable(List<Vector3d> positions, Set<Vector3d> occupied, SlotKind kind) {
        var slots = new ArrayList<Slot>();
        int index = 0;
        for (var position : positions) {
            if (!occupied.contains(round(position))) {
                slots.add(new Slot(index++, kind, position));
            }
        }
        return slots;
    }

    private boolean isAvailable(List<Slot> slots, Vector3d position) {
        var target = round(position);
        return slots.stream().anyMatch(slot -> round(slot.position()).equals(target));
    }

    private Set<Vector3d> roundedPositions(List<Vector3d> positions) {
        return positions.stream().map(this::round).collect(Collectors.toSet());
    }

    /** Arrondit au bloc pour comparer des positions issues de sources différentes. */
    private Vector3d round(Vector3d position) {
        return new Vector3d(Math.floor(position.x), Math.floor(position.y), Math.floor(position.z));
    }
}
