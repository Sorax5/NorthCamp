package fr.phylisiumstudio.logic.activity;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.economy.EconomyService;
import fr.phylisiumstudio.logic.vendor.Patent;

/**
 * Ravitaillement des activités consommant des fournitures (pêche, barbecue). Le
 * joueur rachète du stock ; le coût par unité dépend du type d'activité, ce qui
 * justifie un tarif client plus élevé pour ces activités.
 */
@Singleton
public class ActivitySupplyService {

    /** Stock de départ offert à l'achat d'une activité consommant des fournitures. */
    public static final int STARTING_SUPPLIES = 15;

    private final EconomyService economyService;

    @Inject
    public ActivitySupplyService(EconomyService economyService) {
        this.economyService = economyService;
    }

    /** Coût total pour racheter {@code amount} fournitures pour cette activité. */
    public long restockCost(Activity activity, int amount) {
        return (long) activity.getType().supplyCost() * Math.max(0, amount);
    }

    /**
     * Rachète des fournitures pour l'activité si elle en consomme et que le camping
     * a les fonds.
     *
     * @return la quantité réellement ajoutée (0 si échec ou activité sans consommable).
     */
    public int restock(Campsite campsite, Activity activity, int amount) {
        if (!activity.getType().consumesSupplies() || amount <= 0) {
            return 0;
        }
        // Brevet « Fournitures éco » : coût des fournitures réduit de 30 %.
        double factor = campsite.hasPatent(Patent.ECO_SUPPLIES) ? 0.7 : 1.0;
        long cost = Math.round(restockCost(activity, amount) * factor);
        if (campsite.getMoney() < cost) {
            return 0;
        }
        economyService.charge(campsite, cost);
        activity.setSupplies(activity.getSupplies() + amount);
        return amount;
    }
}
