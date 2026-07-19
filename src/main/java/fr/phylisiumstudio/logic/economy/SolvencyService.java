package fr.phylisiumstudio.logic.economy;

import com.google.inject.Singleton;
import fr.phylisiumstudio.logic.Campsite;

/**
 * Applique les conséquences d'un solde négatif : intérêts de dette qui creusent
 * le trou et réputation qui s'érode tant que le camping est insolvable. Passé un
 * seuil, un renflouement forcé remet le solde à zéro contre une lourde perte de
 * réputation — le joueur n'est jamais bloqué, mais paie cher ses excès.
 *
 * <p>Donne un vrai risque aux décisions : dépenser à crédit a un coût.
 */
@Singleton
public class SolvencyService {

    /** Intérêt quotidien appliqué à une dette (solde négatif). */
    public static final double DEBT_INTEREST = 0.05;
    /** Perte de réputation par jour passé dans le rouge. */
    public static final double INSOLVENCY_REPUTATION_PENALTY = 2.0;
    /** Seuil de dette au-delà duquel un renflouement forcé intervient. */
    public static final double BANKRUPTCY_LIMIT = -5_000.0;
    /** Perte de réputation lors d'un renflouement (faillite). */
    public static final double BANKRUPTCY_REPUTATION_HIT = 15.0;

    /**
     * Règle la situation financière au lever du jour.
     *
     * @return {@code true} si une faillite (renflouement à zéro) a été déclenchée.
     */
    public boolean settle(Campsite campsite) {
        if (campsite.getMoney() >= 0) {
            return false;
        }

        // Intérêts : la dette se creuse (money négatif * taux = ajout négatif).
        campsite.addMoney(campsite.getMoney() * DEBT_INTEREST);
        campsite.adjustReputation(-INSOLVENCY_REPUTATION_PENALTY);

        if (campsite.getMoney() < BANKRUPTCY_LIMIT) {
            // Renflouement forcé : solde ramené à zéro, réputation salement touchée.
            campsite.addMoney(-campsite.getMoney());
            campsite.adjustReputation(-BANKRUPTCY_REPUTATION_HIT);
            return true;
        }
        return false;
    }
}
