package fr.phylisiumstudio.logic.client;

import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.activity.Activity;
import lombok.Data;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.InstanceContainer;

@Data
public class ClientMemory {
    public final InstanceContainer instance;
    public final Client client;
    public final Campsite campsite;
    public Pos targetPosition;
    public ClientEntity playerEntity;

    public Activity choosenActivity;
    public Activity currentActivity;

    /** Point d'accueil où patientent les clients en attente. */
    public Pos receptionPosition;
    /** Point de sortie rejoint en fin de séjour avant despawn. */
    public Pos exitPosition;
}
