package fr.phylisiumstudio.logic.economy;

import com.google.inject.Singleton;
import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.client.Client;
import fr.phylisiumstudio.logic.plot.Plot;

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
        double amount = Math.min(plot.getPrice(), client.getBudget());
        return transfer(campsite, client, amount);
    }

    /**
     * Encaisse le revenu d'une activité consommée par un client, plafonné à son budget.
     *
     * @return le montant réellement encaissé.
     */
    public double earnActivityIncome(Campsite campsite, Client client, double income) {
        double amount = Math.min(income, client.getBudget());
        return transfer(campsite, client, amount);
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
