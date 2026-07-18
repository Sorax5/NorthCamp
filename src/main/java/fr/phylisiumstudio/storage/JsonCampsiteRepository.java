package fr.phylisiumstudio.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import fr.phylisiumstudio.app.inject.annotation.CampsiteRepositoryFile;
import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.repository.ICampsiteRepository;

import java.io.File;
import java.util.UUID;

@Singleton
public class JsonCampsiteRepository extends AbstractFileRepository<Campsite, UUID>
        implements ICampsiteRepository {

    @Inject
    public JsonCampsiteRepository(@CampsiteRepositoryFile File folder, ObjectMapper mapper) {
        super(folder, mapper, Campsite.class, ".json", Campsite::getUniqueID, UUID::toString);
    }
}
