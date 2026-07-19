package fr.phylisiumstudio.logic.plot;

import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.area.Area;
import fr.phylisiumstudio.logic.economy.EconomyService;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PlotUpgradeServiceTest {

    private final PlotUpgradeService service = new PlotUpgradeService(new EconomyService());

    private static PlotData data() {
        return new PlotData(PlotType.CAMPSITE, new Area(new Vector3d(), new Vector3d(8, 6, 8)), "s.nbt",
                List.of(new PlotLevel(0, "s.nbt", 10),
                        new PlotLevel(500, "s.nbt", 20),
                        new PlotLevel(1500, "s.nbt", 35)));
    }

    private static Plot plot() {
        return new Plot(new Vector3d(0, 69, 0), PlotType.CAMPSITE);
    }

    @Test
    void upgradeChargesCostAndRaisesLevelAndIncome() {
        var campsite = new Campsite(UUID.randomUUID());
        campsite.addMoney(2_000);
        var plot = plot();
        var data = data();

        assertEquals(10, service.nightlyIncome(data, plot));
        assertEquals(500, service.nextCost(data, plot));

        assertTrue(service.upgrade(campsite, data, plot));
        assertEquals(1, plot.getLevel());
        assertEquals(1_500.0, campsite.getMoney());
        assertEquals(20, service.nightlyIncome(data, plot));
    }

    @Test
    void upgradeFailsWithoutFundsAndStopsAtMaxLevel() {
        var campsite = new Campsite(UUID.randomUUID());
        campsite.addMoney(100); // < 500
        var plot = plot();
        var data = data();

        assertFalse(service.upgrade(campsite, data, plot));
        assertEquals(0, plot.getLevel());

        // Monte au max avec des fonds suffisants.
        campsite.addMoney(5_000);
        assertTrue(service.upgrade(campsite, data, plot));  // -> 1
        assertTrue(service.upgrade(campsite, data, plot));  // -> 2 (dernier niveau)
        assertFalse(service.canUpgrade(data, plot));
        assertEquals(-1, service.nextCost(data, plot));
        assertFalse(service.upgrade(campsite, data, plot));
    }
}
