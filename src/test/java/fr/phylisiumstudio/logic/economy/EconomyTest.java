package fr.phylisiumstudio.logic.economy;

import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.client.Client;
import fr.phylisiumstudio.logic.gameplay.AssignmentOutcome;
import fr.phylisiumstudio.logic.gameplay.PlotAssignmentService;
import fr.phylisiumstudio.logic.plot.Plot;
import fr.phylisiumstudio.logic.plot.PlotType;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EconomyTest {

    private static Plot plot(double price) {
        var p = new Plot(new Vector3d(0, 69, 0), PlotType.CAMPSITE);
        p.setPrice(price);
        return p;
    }

    @Test
    void checkInAssignsChargesRentAndReducesBudget() {
        var campsite = new Campsite(UUID.randomUUID());
        var p = plot(50); // aligné sur le prix juste CAMPSITE (50)
        campsite.addPlot(p);
        var client = new Client(1, 2, 200);
        campsite.addClient(client);

        var checkIn = new CheckInService(
                new PlotAssignmentService(), new EconomyService(),
                new MarketService(new Random(1)), new SatisfactionService());

        var outcome = checkIn.checkIn(campsite, client, p);

        assertEquals(AssignmentOutcome.SUCCESS, outcome);
        assertEquals(50.0, campsite.getMoney());
        assertEquals(150.0, client.getBudget());
    }

    @Test
    void rentIsCappedByClientBudget() {
        var campsite = new Campsite(UUID.randomUUID());
        var client = new Client(1, 1, 30);
        var economy = new EconomyService();

        double charged = economy.chargeRent(campsite, client, plot(50));

        assertEquals(30.0, charged);
        assertEquals(0.0, client.getBudget());
        assertEquals(30.0, campsite.getMoney());
    }

    @Test
    void overpricingLowersSatisfaction() {
        var sat = new SatisfactionService();
        var client = new Client(1, 1, 100);
        double before = client.getSatisfaction();

        sat.evaluatePricing(client, 2.0); // deux fois le prix juste
        assertTrue(client.getSatisfaction() < before);
    }

    @Test
    void unhappyWaitingClientAbandonsQueue() {
        var sat = new SatisfactionService();
        var client = new Client(1, 1, 100);
        client.setSatisfaction(20.0);
        assertTrue(sat.shouldAbandonQueue(client));

        var campsite = new Campsite(UUID.randomUUID());
        double repBefore = campsite.getReputation();
        sat.applyQueueAbandonment(campsite);
        assertTrue(campsite.getReputation() < repBefore);
    }

    @Test
    void happyDepartureRaisesReputation() {
        var sat = new SatisfactionService();
        var campsite = new Campsite(UUID.randomUUID());
        var client = new Client(1, 1, 100);
        client.setSatisfaction(90.0);

        double before = campsite.getReputation();
        sat.applyDeparture(campsite, client);
        assertTrue(campsite.getReputation() > before);
    }

    @Test
    void marketRatioReflectsPricing() {
        var market = new MarketService(new Random(1));
        double fair = market.fairPrice(PlotType.CAMPSITE);
        var p = plot(fair * 2);
        assertEquals(2.0, market.priceRatio(p), 1e-9);
    }
}
