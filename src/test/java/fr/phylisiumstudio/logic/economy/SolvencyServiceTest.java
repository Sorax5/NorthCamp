package fr.phylisiumstudio.logic.economy;

import fr.phylisiumstudio.logic.Campsite;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SolvencyServiceTest {

    private final SolvencyService service = new SolvencyService();

    @Test
    void positiveBalanceIsUntouched() {
        var campsite = new Campsite(UUID.randomUUID());
        campsite.addMoney(1_000);
        double rep = campsite.getReputation();

        assertFalse(service.settle(campsite));
        assertEquals(1_000.0, campsite.getMoney());
        assertEquals(rep, campsite.getReputation());
    }

    @Test
    void debtAccruesInterestAndErodesReputation() {
        var campsite = new Campsite(UUID.randomUUID());
        campsite.addMoney(-1_000); // dette
        double rep = campsite.getReputation();

        assertFalse(service.settle(campsite)); // pas encore la faillite
        assertEquals(-1_050.0, campsite.getMoney()); // -1000 * 1.05
        assertEquals(rep - SolvencyService.INSOLVENCY_REPUTATION_PENALTY, campsite.getReputation());
    }

    @Test
    void bankruptcyResetsBalanceToZeroWithReputationHit() {
        var campsite = new Campsite(UUID.randomUUID());
        campsite.addMoney(-6_000); // sous le seuil de faillite
        double rep = campsite.getReputation();

        assertTrue(service.settle(campsite));
        assertEquals(0.0, campsite.getMoney());
        assertTrue(campsite.getReputation() < rep);
    }
}
