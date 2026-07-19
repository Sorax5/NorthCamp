package fr.phylisiumstudio.logic.activity;

import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.area.Area;
import fr.phylisiumstudio.logic.economy.EconomyService;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ActivityUpgradeServiceTest {

    private final ActivityUpgradeService service = new ActivityUpgradeService(new EconomyService());

    private static ActivityData data() {
        return new ActivityData(ActivityType.FISHING, new Area(new Vector3d(), new Vector3d(10, 6, 10)),
                List.of(new ActivityLevel(0, "s.nbt", 2),
                        new ActivityLevel(100, "s.nbt", 5),
                        new ActivityLevel(300, "s.nbt", 8)));
    }

    private static Activity activity() {
        return new Activity(new Vector3d(0, 69, 0), 15, 5, 4, ActivityType.FISHING);
    }

    @Test
    void upgradeChargesCostRaisesCapacityAndIncome() {
        var campsite = new Campsite(UUID.randomUUID());
        campsite.addMoney(1_000);
        var activity = activity();
        var data = data();

        assertEquals(100, service.nextCost(data, activity));
        assertTrue(service.upgrade(campsite, data, activity));

        assertEquals(1, activity.getCurrentLevel());
        assertEquals(5, activity.getMaxClients());           // 4 + 1
        assertEquals(10.0, activity.getPrice());             // 5 + income(5)
        assertEquals(900.0, campsite.getMoney());            // 1000 - 100
    }

    @Test
    void stopsAtMaxLevelAndRespectsFunds() {
        var campsite = new Campsite(UUID.randomUUID());
        campsite.addMoney(10); // < 100
        var activity = activity();
        var data = data();

        assertFalse(service.upgrade(campsite, data, activity));
        assertEquals(0, activity.getCurrentLevel());

        campsite.addMoney(5_000);
        assertTrue(service.upgrade(campsite, data, activity)); // -> 1
        assertTrue(service.upgrade(campsite, data, activity)); // -> 2 (max)
        assertFalse(service.canUpgrade(data, activity));
        assertEquals(-1, service.nextCost(data, activity));
    }
}
