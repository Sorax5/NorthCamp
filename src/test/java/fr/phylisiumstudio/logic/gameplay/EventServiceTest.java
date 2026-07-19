package fr.phylisiumstudio.logic.gameplay;

import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.activity.Activity;
import fr.phylisiumstudio.logic.activity.ActivityType;
import fr.phylisiumstudio.logic.client.Client;
import fr.phylisiumstudio.logic.client.ClientLifecycle;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EventServiceTest {

    /** Random qui force toujours le déclenchement puis choisit un index d'événement fixe. */
    private static EventService servicePicking(int eventIndex) {
        return new EventService(new Random() {
            private int call = 0;
            @Override
            public double nextDouble() {
                return 0.0; // < EVENT_CHANCE -> déclenche toujours
            }
            @Override
            public int nextInt(int bound) {
                return eventIndex % bound;
            }
        });
    }

    @Test
    void calmDayReturnsNullAndChangesNothing() {
        var service = new EventService(new Random() {
            @Override public double nextDouble() { return 1.0; } // >= chance -> jamais
        });
        var campsite = new Campsite(UUID.randomUUID());
        double rep = campsite.getReputation();
        assertNull(service.maybeTrigger(campsite));
        assertEquals(rep, campsite.getReputation());
    }

    @Test
    void stormDisablesActivities() {
        var service = servicePicking(CampEvent.STORM.ordinal());
        var campsite = new Campsite(UUID.randomUUID());
        var activity = new Activity(new Vector3d(), 15, 5, 4, ActivityType.FISHING);
        activity.setOperational(true);
        campsite.addActivity(activity);

        assertEquals(CampEvent.STORM, service.maybeTrigger(campsite));
        assertFalse(activity.isOperational());
    }

    @Test
    void bearLowersReputationAndScaresStayingClients() {
        var service = servicePicking(CampEvent.BEAR.ordinal());
        var campsite = new Campsite(UUID.randomUUID());
        var client = new Client(1, 2, 100);
        client.setLifecycle(ClientLifecycle.STAYING);
        double sat = client.getSatisfaction();
        campsite.addClient(client);
        double rep = campsite.getReputation();

        assertEquals(CampEvent.BEAR, service.maybeTrigger(campsite));
        assertTrue(campsite.getReputation() < rep);
        assertTrue(client.getSatisfaction() < sat);
    }

    @Test
    void festivalRaisesReputation() {
        var service = servicePicking(CampEvent.FESTIVAL.ordinal());
        var campsite = new Campsite(UUID.randomUUID());
        double rep = campsite.getReputation();
        assertEquals(CampEvent.FESTIVAL, service.maybeTrigger(campsite));
        assertTrue(campsite.getReputation() > rep);
    }
}
