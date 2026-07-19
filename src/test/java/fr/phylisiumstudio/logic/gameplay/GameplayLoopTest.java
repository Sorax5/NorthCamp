package fr.phylisiumstudio.logic.gameplay;

import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.client.Client;
import fr.phylisiumstudio.logic.client.ClientLifecycle;
import fr.phylisiumstudio.logic.economy.SatisfactionService;
import fr.phylisiumstudio.logic.plot.Plot;
import fr.phylisiumstudio.logic.plot.PlotType;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class GameplayLoopTest {

    private final PlotAssignmentService assignment = new PlotAssignmentService();
    private final ClientStayService stay = new ClientStayService();

    private static Plot plot(int level) {
        var p = new Plot(new Vector3d(0, 69, 0), PlotType.CAMPSITE);
        p.setLevel(level);
        return p;
    }

    private static Client solo(int stayDays) {
        return new Client(1, stayDays, 100);
    }

    @Test
    void assignsWaitingClientToCleanFreePlot() {
        var campsite = new Campsite(UUID.randomUUID());
        var p = plot(0);
        campsite.addPlot(p);
        var client = solo(3);
        campsite.addClient(client);

        var outcome = assignment.assign(campsite, client, p);

        assertEquals(AssignmentOutcome.SUCCESS, outcome);
        assertEquals(ClientLifecycle.STAYING, client.getLifecycle());
        assertSame(p, client.getPlot());
        assertTrue(assignment.availablePlots(campsite).isEmpty());
    }

    @Test
    void plotTypeShapesStayLengthAndComfort() {
        var campsite = new Campsite(UUID.randomUUID());
        var caravan = new Plot(new Vector3d(0, 69, 0), PlotType.CARAVAN);
        caravan.setLevel(1);
        campsite.addPlot(caravan);
        var client = solo(4); // séjour de base 4 jours
        double baseSatisfaction = client.getSatisfaction();
        campsite.addClient(client);

        assertEquals(AssignmentOutcome.SUCCESS, assignment.assign(campsite, client, caravan));

        // Caravane : séjour allongé (×1,4 -> 6) et bonus de confort à l'installation.
        assertEquals(6, client.getTotalStayDays());
        assertEquals(6, client.getRemainingDays());
        assertTrue(client.getSatisfaction() > baseSatisfaction);
    }

    @Test
    void tentGivesShorterStay() {
        var campsite = new Campsite(UUID.randomUUID());
        var tent = plot(0); // CAMPSITE
        campsite.addPlot(tent);
        var client = solo(4);
        campsite.addClient(client);

        assignment.assign(campsite, client, tent);
        // Tente : ×0,7 -> 3 jours.
        assertEquals(3, client.getTotalStayDays());
    }

    @Test
    void familyRequiresHigherLevelPlot() {
        var campsite = new Campsite(UUID.randomUUID());
        var basic = plot(0);
        campsite.addPlot(basic);
        var family = new Client(3, 2, 300);
        campsite.addClient(family);

        assertEquals(AssignmentOutcome.NEEDS_HIGHER_LEVEL, assignment.assign(campsite, family, basic));

        var upgraded = plot(1);
        campsite.addPlot(upgraded);
        assertEquals(AssignmentOutcome.SUCCESS, assignment.assign(campsite, family, upgraded));
    }

    @Test
    void dirtyPlotIsUnavailableUntilCleaned() {
        var campsite = new Campsite(UUID.randomUUID());
        var p = plot(0);
        p.setDirty(true);
        campsite.addPlot(p);
        var client = solo(1);
        campsite.addClient(client);

        assertEquals(AssignmentOutcome.PLOT_DIRTY, assignment.assign(campsite, client, p));
        assertTrue(assignment.availablePlots(campsite).isEmpty());

        stay.cleanPlot(p);
        assertEquals(1, assignment.availablePlots(campsite).size());
    }

    @Test
    void stayEndsMarkPlotDirtyAndClientLeaving() {
        var campsite = new Campsite(UUID.randomUUID());
        var p = plot(0);
        campsite.addPlot(p);
        var client = solo(1);
        campsite.addClient(client);
        assignment.assign(campsite, client, p);

        var departing = stay.advanceDay(campsite);

        assertEquals(1, departing.size());
        assertEquals(ClientLifecycle.LEAVING, client.getLifecycle());
        assertTrue(p.isDirty());
    }

    @Test
    void departFreesPlotAndRemovalClearsClient() {
        var campsite = new Campsite(UUID.randomUUID());
        var p = plot(0);
        campsite.addPlot(p);
        var client = solo(1);
        campsite.addClient(client);
        assignment.assign(campsite, client, p);
        stay.advanceDay(campsite);

        stay.depart(client);
        assertNull(client.getPlot());
        assertEquals(ClientLifecycle.GONE, client.getLifecycle());

        stay.removeDeparted(campsite);
        assertTrue(campsite.getClients().isEmpty());
    }

    @Test
    void waitingClientLosesPatienceAndEventuallyAbandonsQueue() {
        var satisfaction = new SatisfactionService();
        var client = solo(2); // satisfaction de départ = 70
        assertFalse(satisfaction.shouldAbandonQueue(client), "ne doit pas abandonner dès l'arrivée");

        // Applique l'impatience jusqu'à ce que la satisfaction franchisse le seuil.
        int days = 0;
        while (!satisfaction.shouldAbandonQueue(client) && days < 100) {
            satisfaction.applyWaitingImpatience(client);
            days++;
        }

        assertTrue(satisfaction.shouldAbandonQueue(client), "doit finir par abandonner");
        assertTrue(days >= 3 && days <= 6, "abandon après ~4 jours d'attente, obtenu : " + days);
    }

    @Test
    void arrivalGeneratorProducesWaitingClients() {
        var generator = new ArrivalGenerator(new Random(42));
        var arrivals = generator.generate(10);
        assertEquals(10, arrivals.size());
        for (var c : arrivals) {
            assertEquals(ClientLifecycle.WAITING, c.getLifecycle());
            assertTrue(c.getGroupSize() >= 1);
            assertTrue(c.getTotalStayDays() >= 1);
            assertTrue(c.getBudget() >= 0);
        }
    }
}
