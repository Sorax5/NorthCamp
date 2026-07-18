package fr.phylisiumstudio.logic.gameplay;

import com.google.inject.Singleton;
import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.client.Client;
import fr.phylisiumstudio.logic.client.ClientLifecycle;
import fr.phylisiumstudio.logic.plot.Plot;

import java.util.ArrayList;
import java.util.List;

/**
 * Gère la progression du séjour des clients au fil des jours ainsi que
 * l'entretien des emplacements (étapes « Départ » et « Maintenance »).
 *
 * <p>Service pur, sans dépendance moteur.
 */
@Singleton
public class ClientStayService {

    /**
     * Fait avancer d'un jour tous les séjours en cours. Les clients dont le séjour
     * se termine passent en {@link ClientLifecycle#LEAVING} et leur emplacement est
     * marqué sale.
     *
     * @return les clients dont le séjour vient de se terminer.
     */
    public List<Client> advanceDay(Campsite campsite) {
        var departing = new ArrayList<Client>();
        for (var client : campsite.getClients()) {
            if (client.getLifecycle() != ClientLifecycle.STAYING) {
                continue;
            }
            client.setRemainingDays(client.getRemainingDays() - 1);
            if (client.getRemainingDays() <= 0) {
                client.setLifecycle(ClientLifecycle.LEAVING);
                if (client.getPlot() != null) {
                    client.getPlot().setDirty(true);
                }
                departing.add(client);
            }
        }
        return departing;
    }

    /**
     * Finalise le départ d'un client : il quitte le camping et libère son emplacement
     * (qui reste sale jusqu'au nettoyage).
     */
    public void depart(Client client) {
        client.setLifecycle(ClientLifecycle.GONE);
        client.setPlot(null);
    }

    /** Retire du camping les clients ayant définitivement quitté les lieux. */
    public void removeDeparted(Campsite campsite) {
        campsite.getClients().removeIf(c -> c.getLifecycle() == ClientLifecycle.GONE);
    }

    /** Remet un emplacement en état après le départ d'un client. */
    public void cleanPlot(Plot plot) {
        plot.setDirty(false);
    }
}
