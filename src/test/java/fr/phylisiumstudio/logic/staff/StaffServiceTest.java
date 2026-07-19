package fr.phylisiumstudio.logic.staff;

import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.activity.Activity;
import fr.phylisiumstudio.logic.activity.ActivityType;
import fr.phylisiumstudio.logic.client.Client;
import fr.phylisiumstudio.logic.client.ClientLifecycle;
import fr.phylisiumstudio.logic.economy.CheckInService;
import fr.phylisiumstudio.logic.economy.EconomyService;
import fr.phylisiumstudio.logic.economy.MarketService;
import fr.phylisiumstudio.logic.economy.SatisfactionService;
import fr.phylisiumstudio.logic.gameplay.ClientStayService;
import fr.phylisiumstudio.logic.gameplay.PlotAssignmentService;
import fr.phylisiumstudio.logic.plot.Plot;
import fr.phylisiumstudio.logic.plot.PlotType;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class StaffServiceTest {

    private StaffService service() {
        var assignment = new PlotAssignmentService();
        var stay = new ClientStayService();
        var economy = new EconomyService();
        var checkIn = new CheckInService(assignment, economy,
                new MarketService(new Random(1)), new SatisfactionService());
        return new StaffService(assignment, stay, checkIn, economy,
                new fr.phylisiumstudio.logic.activity.ActivitySupplyService(economy));
    }

    private static Staff staff(StaffRole role, double skill, double salary) {
        Map<StaffRole, Double> skills = new EnumMap<>(StaffRole.class);
        skills.put(role, skill);
        return new Staff(UUID.randomUUID(), "Bob", skills, salary, StaffLook.VARIANT_A, role, null);
    }

    private static Plot plot() {
        var p = new Plot(new Vector3d(0, 69, 0), PlotType.CAMPSITE);
        p.setPrice(50);
        return p;
    }

    @Test
    void salariesAreDeductedFromCampsite() {
        var svc = service();
        var campsite = new Campsite(UUID.randomUUID());
        campsite.addMoney(1000);
        svc.hire(campsite, staff(StaffRole.RECEPTION, 0.5, 120));

        double paid = svc.paySalaries(campsite);
        assertEquals(120, paid);
        assertEquals(880, campsite.getMoney());
    }

    @Test
    void supplyStaffRestocksItsAssignedActivityWhenLow() {
        var svc = service();
        var campsite = new Campsite(UUID.randomUUID());
        campsite.addMoney(10_000);

        var barbecue = new Activity(new Vector3d(), 15, 5, 4, ActivityType.BARBECUE);
        barbecue.setSupplies(2); // sous le seuil bas
        campsite.addActivity(barbecue);

        var supplier = staff(StaffRole.SUPPLY, 0.8, 100);
        supplier.setAssignedActivityId(barbecue.getUniqueID());
        svc.hire(campsite, supplier);

        svc.runAutomation(campsite);
        assertTrue(barbecue.getSupplies() > 2, "l'activité doit avoir été réapprovisionnée");
    }

    @Test
    void skilledReceptionistCapacityExceedsUnskilled() {
        var svc = service();
        var campsite = new Campsite(UUID.randomUUID());
        for (int i = 0; i < 5; i++) {
            campsite.addPlot(plot());
            var c = new Client(1, 2, 200);
            campsite.addClient(c);
        }
        // compétence 1.0 -> capacité 5, doit installer les 5 clients
        svc.hire(campsite, staff(StaffRole.RECEPTION, 1.0, 100));
        svc.runAutomation(campsite);

        long staying = campsite.getClients().stream()
                .filter(c -> c.getLifecycle() == ClientLifecycle.STAYING).count();
        assertEquals(5, staying);
    }

    @Test
    void cleanerRestoresDirtyPlotsUpToCapacity() {
        var svc = service();
        var campsite = new Campsite(UUID.randomUUID());
        for (int i = 0; i < 4; i++) {
            var p = plot();
            p.setDirty(true);
            campsite.addPlot(p);
        }
        // compétence 0 -> capacité 1, un seul emplacement nettoyé
        svc.hire(campsite, staff(StaffRole.CLEANING, 0.0, 80));
        svc.runAutomation(campsite);

        long dirty = campsite.getPlots().stream().filter(Plot::isDirty).count();
        assertEquals(3, dirty);
    }

    @Test
    void maintenanceBringsActivitiesBackOnline() {
        var svc = service();
        var campsite = new Campsite(UUID.randomUUID());
        var activity = new Activity(new Vector3d(0, 69, 0), 10, 5, 2, ActivityType.FISHING);
        activity.setOperational(false);
        campsite.addActivity(activity);

        svc.hire(campsite, staff(StaffRole.MAINTENANCE, 1.0, 90));
        svc.runAutomation(campsite);

        assertTrue(activity.isOperational());
    }
}
