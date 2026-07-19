package fr.phylisiumstudio.logic.amenity;

import com.google.inject.Singleton;
import fr.phylisiumstudio.logic.Campsite;

import java.util.Arrays;
import java.util.List;

/**
 * Effet de confort des aménagements du camping. Chaque aménagement construit
 * améliore un peu chaque jour la satisfaction des campeurs en séjour : investir
 * dans les services rend la clientèle plus heureuse (donc meilleure réputation et
 * note).
 *
 * <p>La construction elle-même (placement sur un emplacement dédié, débit du coût,
 * pose du bâtiment) est gérée par {@code SlotService.buyAmenity} + le builder.
 */
@Singleton
public class AmenityService {

    /** Bonus de satisfaction quotidien par aménagement construit, pour un client en séjour. */
    public static final double COMFORT_PER_AMENITY = 1.5;

    /** Types d'aménagement pas encore construits (donc constructibles). */
    public List<Amenity> buildable(Campsite campsite) {
        return Arrays.stream(Amenity.values())
                .filter(a -> !campsite.hasAmenity(a))
                .toList();
    }

    /** Bonus de confort quotidien apporté par l'ensemble des aménagements construits. */
    public double dailyComfortBonus(Campsite campsite) {
        // Brevet « Confort + » : effet des services renforcé de 50 %.
        double factor = campsite.hasPatent(fr.phylisiumstudio.logic.vendor.Patent.COMFORT_PLUS) ? 1.5 : 1.0;
        return campsite.getBuiltAmenities().size() * COMFORT_PER_AMENITY * factor;
    }
}
