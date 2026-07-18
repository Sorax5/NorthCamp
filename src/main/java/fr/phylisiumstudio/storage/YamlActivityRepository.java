package fr.phylisiumstudio.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import fr.phylisiumstudio.app.inject.annotation.ActivityRepositoryFile;
import fr.phylisiumstudio.app.inject.annotation.ContentMapper;
import fr.phylisiumstudio.logic.activity.ActivityData;
import fr.phylisiumstudio.logic.activity.ActivityType;
import fr.phylisiumstudio.logic.repository.IActivityRepository;

import java.io.File;

@Singleton
public class YamlActivityRepository extends AbstractFileRepository<ActivityData, ActivityType>
        implements IActivityRepository {

    @Inject
    public YamlActivityRepository(@ActivityRepositoryFile File folder, @ContentMapper ObjectMapper mapper) {
        super(folder, mapper, ActivityData.class, ".yml", ActivityData::type, ActivityType::name);
    }
}
