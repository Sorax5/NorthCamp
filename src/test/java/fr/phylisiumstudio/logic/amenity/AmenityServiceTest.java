package fr.phylisiumstudio.logic.amenity;

import fr.phylisiumstudio.logic.Campsite;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AmenityServiceTest {

    private final AmenityService service = new AmenityService();

    @Test
    void buildableExcludesAlreadyBuiltTypes() {
        var campsite = new Campsite(UUID.randomUUID());
        assertEquals(Amenity.values().length, service.buildable(campsite).size());

        campsite.addAmenity(new AmenityInstance(Amenity.SHOWERS, new Vector3d()));
        assertFalse(service.buildable(campsite).contains(Amenity.SHOWERS));
        assertEquals(Amenity.values().length - 1, service.buildable(campsite).size());
    }

    @Test
    void comfortBonusScalesWithBuiltAmenities() {
        var campsite = new Campsite(UUID.randomUUID());
        assertEquals(0.0, service.dailyComfortBonus(campsite));

        campsite.addAmenity(new AmenityInstance(Amenity.SHOWERS, new Vector3d()));
        campsite.addAmenity(new AmenityInstance(Amenity.SHOP, new Vector3d()));
        assertEquals(2 * AmenityService.COMFORT_PER_AMENITY, service.dailyComfortBonus(campsite));
    }
}
