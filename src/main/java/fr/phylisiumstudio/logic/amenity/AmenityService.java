package fr.phylisiumstudio.logic.amenity;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.economy.EconomyService;

import java.util.Arrays;
import java.util.List;

/**
 * Gère la construction des aménagements du camping et leur effet de confort.
 * Chaque aménagement présent améliore un peu chaque jour la satisfaction des
 * campeurs en séjour : investir dans les services rend la clientèle plus heureuse
 * (donc meilleure réputation et note).
 */
@Singleton
public class AmenityService {

    /** Bonus de satisfaction quotidien par aménagement construit, pour un client en séjour. */
    public static final double COMFORT_PER_AMENITY = 1.5;

    private final EconomyService economyService;

    @Inject
    public AmenityService(EconomyService economyService) {
        this.economyService = economyService;
    }

    /** Aménagements pas encore construits (donc constructibles). */
    public List<Amenity> buildable(Campsite campsite) {
        return Arrays.stream(Amenity.values())
                .filter(a -> !campsite.hasAmenity(a))
                .toList();
    }

    /**
     * Construit un aménagement s'il n'existe pas déjà et que le camping a les fonds.
     *
     * @return {@code true} si l'aménagement a été construit.
     */
    public boolean build(Campsite campsite, Amenity amenity) {
        if (campsite.hasAmenity(amenity) || campsite.getMoney() < amenity.cost()) {
            return false;
        }
        economyService.charge(campsite, amenity.cost());
        campsite.addAmenity(amenity);
        return true;
    }

    /** Bonus de confort quotidien apporté par l'ensemble des aménagements construits. */
    public double dailyComfortBonus(Campsite campsite) {
        return campsite.getAmenities().size() * COMFORT_PER_AMENITY;
    }
}
