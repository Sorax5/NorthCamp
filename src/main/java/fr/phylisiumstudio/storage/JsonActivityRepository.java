package fr.phylisiumstudio.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import fr.phylisiumstudio.app.inject.annotation.ActivityRepositoryFile;
import fr.phylisiumstudio.logic.activity.ActivityData;
import fr.phylisiumstudio.logic.activity.ActivityType;
import fr.phylisiumstudio.logic.repository.IActivityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

@Singleton
public class JsonActivityRepository implements IActivityRepository {
    private static final Logger logger = LoggerFactory.getLogger(JsonActivityRepository.class);
    private final File folder;
    private final ObjectMapper objectMapper;

    @Inject
    public JsonActivityRepository(@ActivityRepositoryFile File folder, ObjectMapper objectMapper) {
        this.folder = folder;
        this.objectMapper = objectMapper;

        if (!this.folder.exists() && !this.folder.mkdirs()) {
            logger.warn("Unable to create activity data folder: {}", folder.getAbsolutePath());
        }
    }

    @Override
    public CompletableFuture<ActivityData> create(ActivityData entity) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                File file = getFile(entity.type());
                if (file.exists()) {
                    throw new IllegalArgumentException("ActivityData with type " + entity.type() + " already exists.");
                }

                if (file.createNewFile()) {
                    objectMapper.writeValue(file, entity);
                    return entity;
                } else {
                    throw new RuntimeException("Failed to create file for ActivityData with type " + entity.type());
                }
            } catch (Exception e) {
                logger.error("Error creating ActivityData: {}", e.getMessage(), e);
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public CompletableFuture<ActivityData> read(ActivityType id) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                File file = getFile(id);
                if (!file.exists()) {
                    return null;
                }

                return objectMapper.readValue(file, ActivityData.class);
            } catch (Exception e) {
                logger.error("Error reading ActivityData: {}", e.getMessage(), e);
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public CompletableFuture<ActivityData> update(ActivityData entity) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                File file = getFile(entity.type());
                if (!file.exists()) {
                    throw new IllegalArgumentException("ActivityData with type " + entity.type() + " does not exist.");
                }

                objectMapper.writeValue(file, entity);
                return entity;
            } catch (Exception e) {
                logger.error("Error updating ActivityData: {}", e.getMessage(), e);
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> delete(ActivityType id) {
        return CompletableFuture.runAsync(() -> {
            try {
                File file = getFile(id);
                if (file.exists() && !file.delete()) {
                    throw new RuntimeException("Failed to delete ActivityData with type " + id);
                }
            } catch (Exception e) {
                logger.error("Error deleting ActivityData: {}", e.getMessage(), e);
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public CompletableFuture<List<ActivityData>> list() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                File[] files = folder.listFiles((dir, name) -> name.endsWith(".json"));
                if (files == null) {
                    throw new RuntimeException("Failed to list ActivityData files.");
                }

                return Stream.of(files).map(file -> {
                    try {
                        return objectMapper.readValue(file, ActivityData.class);
                    } catch (Exception e) {
                        logger.error("Error reading ActivityData file: {}", e.getMessage(), e);
                        return null;
                    }
                }).filter(Objects::nonNull).toList();
            } catch (Exception e) {
                logger.error("Error listing ActivityData: {}", e.getMessage(), e);
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> exists(ActivityType id) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                File file = getFile(id);
                return file.exists();
            } catch (Exception e) {
                logger.error("Error checking ActivityData existence: {}", e.getMessage(), e);
                throw new RuntimeException(e);
            }
        });
    }

    private File getFile(ActivityType type) {
        return new File(folder, type.name() + ".json");
    }
}

