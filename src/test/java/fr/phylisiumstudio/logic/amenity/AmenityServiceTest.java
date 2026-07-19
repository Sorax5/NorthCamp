package fr.phylisiumstudio.logic.amenity;

import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.economy.EconomyService;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AmenityServiceTest {

    private final AmenityService service = new AmenityService(new EconomyService());

    @Test
    void buildChargesCostAndMarksBuilt() {
        var campsite = new Campsite(UUID.randomUUID());
        campsite.addMoney(5_000);

        assertTrue(service.build(campsite, Amenity.SHOWERS));
        assertTrue(campsite.hasAmenity(Amenity.SHOWERS));
        assertEquals(5_000 - Amenity.SHOWERS.cost(), campsite.getMoney());
        assertFalse(service.buildable(campsite).contains(Amenity.SHOWERS));
    }

    @Test
    void cannotBuildTwiceOrWithoutFunds() {
        var campsite = new Campsite(UUID.randomUUID());
        campsite.addMoney(Amenity.WIFI.cost());

        assertTrue(service.build(campsite, Amenity.WIFI));
        assertFalse(service.build(campsite, Amenity.WIFI));           // déjà construit
        assertFalse(service.build(campsite, Amenity.SHOP));           // solde à 0
    }

    @Test
    void comfortBonusScalesWithBuiltAmenities() {
        var campsite = new Campsite(UUID.randomUUID());
        assertEquals(0.0, service.dailyComfortBonus(campsite));

        campsite.addMoney(100_000);
        service.build(campsite, Amenity.SHOWERS);
        service.build(campsite, Amenity.SHOP);
        assertEquals(2 * AmenityService.COMFORT_PER_AMENITY, service.dailyComfortBonus(campsite));
    }
}
