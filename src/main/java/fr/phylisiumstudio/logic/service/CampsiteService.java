package fr.phylisiumstudio.logic.service;

import com.google.inject.Inject;
import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.repository.ICampsiteRepository;
import lombok.Getter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CampsiteService {
    @Getter
    private final ICampsiteRepository campsiteRepository;

    @Getter
    private Map<UUID, Campsite> campsites = new HashMap<>();

    @Inject
    public CampsiteService(ICampsiteRepository campsiteRepository) {
        this.campsiteRepository = campsiteRepository;

        List<Campsite> loadedCampsites = campsiteRepository.list().join();
        this.campsites = loadedCampsites.stream().collect(
                HashMap::new,
                (map, campsite) -> map.put(campsite.getUniqueID(), campsite),
                HashMap::putAll
        );
    }

    public void addCampsite(Campsite campsite) {
        campsites.put(campsite.getUniqueID(), campsite);
        campsiteRepository.create(campsite);
    }

    public void removeCampsite(UUID campsiteID) {
        campsites.remove(campsiteID);
        campsiteRepository.delete(campsiteID);
    }

    public void saveCampsite() {
        for (Campsite campsite : campsites.values()) {
            campsiteRepository.update(campsite);
        }
    }

    public Campsite getCampsite(UUID campsiteID) {
        return campsites.get(campsiteID);
    }
}
