package fr.phylisiumstudio.logic.client.Action;

import com.badlogic.gdx.ai.btree.LeafTask;
import com.badlogic.gdx.ai.btree.Task;
import fr.phylisiumstudio.logic.client.Client;
import fr.phylisiumstudio.logic.client.ClientEntity;

public class UpdateClientState extends LeafTask<ClientEntity> {
    @Override
    public Status execute() {
        var memory = getObject().getMemory();
        var client = memory.getClient();

        // La routine suit l'heure : de jour on préfère les activités (BORED), la
        // nuit on rentre dormir (SLEEPY). Ce n'est qu'une tendance, pas une règle.
        boolean isDay = isDay(memory.getInstance().getTime());
        double r = Math.random();
        Client.ClientState newState;
        if (isDay) {
            if (r < 0.65) {
                newState = Client.ClientState.BORED;   // activités en journée
            } else if (r < 0.90) {
                newState = Client.ClientState.CHILL;
            } else {
                newState = Client.ClientState.SLEEPY;
            }
        } else {
            if (r < 0.60) {
                newState = Client.ClientState.SLEEPY;  // dodo la nuit
            } else if (r < 0.85) {
                newState = Client.ClientState.CHILL;
            } else {
                newState = Client.ClientState.BORED;   // barbecue nocturne, etc.
            }
        }

        var current = client.getAction();
        if (current != null && current == newState) {
            for (var state : Client.ClientState.values()) {
                if (state != current) {
                    newState = state;
                    break;
                }
            }
        }

        client.setAction(newState);

        return Status.SUCCEEDED;
    }

    /** Jour si le tick de temps Minecraft est dans la première moitié (0–12000). */
    private static boolean isDay(long minecraftTime) {
        long t = ((minecraftTime % 24000) + 24000) % 24000;
        return t < 12000;
    }

    @Override
    protected Task<ClientEntity> copyTo(Task<ClientEntity> task) {
        return task;
    }
}
