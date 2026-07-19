package fr.phylisiumstudio.logic.economy;

import com.google.inject.Singleton;
import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.client.Client;
import fr.phylisiumstudio.logic.plot.Plot;
import fr.phylisiumstudio.logic.rating.RatingService;

/**
 * Centralise les mouvements d'argent du camping : location des emplacements et
 * revenus des activités. Le budget du client borne ce qu'il peut dépenser.
 */
@Singleton
public class EconomyService {

    /**
     * Encaisse la location d'un emplacement, plafonnée au budget restant du client.
     *
     * @return le montant réellement facturé.
     */
    public double chargeRent(Campsite campsite, Client client, Plot plot) {
        // Le loyer effectif grimpe avec la note du camping (premium étoiles).
        double rent = plot.getPrice() * RatingService.priceMultiplier(campsite);
        double amount = Math.min(rent, client.getBudget());
        return transfer(campsite, client, amount);
    }

    /**
     * Encaisse le revenu d'une activité consommée par un client, plafonné à son budget.
     *
     * @return le montant réellement encaissé.
     */
    public double earnActivityIncome(Campsite campsite, Client client, double income) {
        return collectActivityIncome(campsite, client, income);
    }

    /**
     * Encaisse le revenu d'une activité, plafonné au budget restant du client.
     * Statique pour être appelable depuis l'arbre de comportement (hors DI).
     *
     * @return le montant réellement encaissé.
     */
    public static double collectActivityIncome(Campsite campsite, Client client, double income) {
        // Revenu d'activité majoré par la note du camping (premium étoiles).
        double gross = income * RatingService.priceMultiplier(campsite);
        double amount = Math.min(gross, client.getBudget());
        if (amount <= 0) {
            return 0.0;
        }
        client.setBudget(client.getBudget() - amount);
        campsite.addMoney(amount);
        return amount;
    }

    private double transfer(Campsite campsite, Client client, double amount) {
        if (amount <= 0) {
            return 0.0;
        }
        client.setBudget(client.getBudget() - amount);
        campsite.addMoney(amount);
        return amount;
    }

    /** Prélève une dépense du camping (ex. salaires), pouvant rendre le solde négatif. */
    public void charge(Campsite campsite, double amount) {
        if (amount > 0) {
            campsite.addMoney(-amount);
        }
    }
}
