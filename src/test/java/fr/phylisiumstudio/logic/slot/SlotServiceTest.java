package fr.phylisiumstudio.logic.slot;

import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.economy.EconomyService;
import fr.phylisiumstudio.logic.plot.PlotType;
import fr.phylisiumstudio.logic.schematic.SchematicFactory;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SlotServiceTest {

    private SlotService service() {
        // Aucun layout schématic enregistré -> LayoutService génère la grille par défaut.
        var layout = new LayoutService(new SchematicFactory());
        layout.load();
        return new SlotService(layout, new EconomyService());
    }

    @Test
    void listsDefaultGridSlotsWhenNoneOccupied() {
        var service = service();
        var campsite = new Campsite(UUID.randomUUID());
        assertFalse(service.availablePlotSlots(campsite).isEmpty());
        assertFalse(service.availableActivitySlots(campsite).isEmpty());
    }

    @Test
    void buyingPlotChargesMoneyAndOccupiesSlot() {
        var service = service();
        var campsite = new Campsite(UUID.randomUUID());
        campsite.addMoney(5_000);

        var slots = service.availablePlotSlots(campsite);
        int before = slots.size();
        var target = slots.get(0).position();

        assertNotNull(service.buyPlot(campsite, target, PlotType.CAMPSITE));
        assertEquals(5_000 - SlotService.PLOT_SLOT_PRICE, campsite.getMoney());
        assertEquals(1, campsite.getPlots().size());
        // Le slot acheté n'est plus proposé.
        assertEquals(before - 1, service.availablePlotSlots(campsite).size());
    }

    @Test
    void purchaseFailsWithoutEnoughMoney() {
        var service = service();
        var campsite = new Campsite(UUID.randomUUID());
        campsite.addMoney(10); // insuffisant

        var target = service.availablePlotSlots(campsite).get(0).position();
        assertNull(service.buyPlot(campsite, target, PlotType.CAMPSITE));
        assertTrue(campsite.getPlots().isEmpty());
    }

    @Test
    void cannotBuyUnknownPosition() {
        var service = service();
        var campsite = new Campsite(UUID.randomUUID());
        campsite.addMoney(5_000);
        assertNull(service.buyPlot(campsite, new Vector3d(9_999, 9_999, 9_999), PlotType.CAMPSITE));
    }
}
