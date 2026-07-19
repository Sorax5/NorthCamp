package fr.phylisiumstudio.logic.activity;

import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.economy.EconomyService;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ActivitySupplyServiceTest {

    private final ActivitySupplyService service = new ActivitySupplyService(new EconomyService());

    private static Activity of(ActivityType type) {
        return new Activity(new Vector3d(0, 69, 0), 15, 5, 4, type);
    }

    @Test
    void consumableActivityBlocksWhenEmptyAndConsumesWhenStocked() {
        var barbecue = of(ActivityType.BARBECUE);
        assertFalse(barbecue.hasSupplies());        // stock 0
        assertFalse(barbecue.consumeSupply());

        barbecue.setSupplies(2);
        assertTrue(barbecue.consumeSupply());
        assertEquals(1, barbecue.getSupplies());
        assertTrue(barbecue.consumeSupply());
        assertFalse(barbecue.consumeSupply());      // épuisé
    }

    @Test
    void nonConsumableActivityNeverBlocks() {
        var swim = of(ActivityType.SWIM);
        assertTrue(swim.hasSupplies());
        assertTrue(swim.consumeSupply());
        assertEquals(0, swim.getSupplies());        // inchangé
    }

    @Test
    void restockChargesByTypeCostAndAddsStock() {
        var campsite = new Campsite(UUID.randomUUID());
        campsite.addMoney(1_000);
        var fishing = of(ActivityType.FISHING); // supplyCost 4

        assertEquals(40, service.restockCost(fishing, 10));
        assertEquals(10, service.restock(campsite, fishing, 10));
        assertEquals(10, fishing.getSupplies());
        assertEquals(960.0, campsite.getMoney());
    }

    @Test
    void usageWearsActivityDownUntilItBreaksThenRepairResets() {
        var activity = of(ActivityType.SWIM);
        for (int i = 0; i < Activity.WEAR_THRESHOLD - 1; i++) {
            assertFalse(activity.recordUsage());
            assertTrue(activity.isOperational());
        }
        // Le passage au seuil déclenche la panne.
        assertTrue(activity.recordUsage());
        assertFalse(activity.isOperational());

        activity.repair();
        assertTrue(activity.isOperational());
        assertEquals(0, activity.getUsage());
    }

    @Test
    void ecoSuppliesPatentReducesRestockCost() {
        var campsite = new Campsite(UUID.randomUUID());
        campsite.addMoney(1_000);
        campsite.addPatent(fr.phylisiumstudio.logic.vendor.Patent.ECO_SUPPLIES);
        var fishing = of(ActivityType.FISHING); // supplyCost 4, base 40 pour 10

        service.restock(campsite, fishing, 10);
        // 40 * 0,7 = 28 dépensés (au lieu de 40).
        assertEquals(1_000 - 28, campsite.getMoney());
    }

    @Test
    void restockRejectsNonConsumableOrInsufficientFunds() {
        var campsite = new Campsite(UUID.randomUUID());
        campsite.addMoney(10);
        assertEquals(0, service.restock(campsite, of(ActivityType.SWIM), 10));   // pas de consommable
        assertEquals(0, service.restock(campsite, of(ActivityType.BARBECUE), 10)); // 80 $ requis
    }
}
