package fr.phylisiumstudio.storage;

import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import fr.phylisiumstudio.app.inject.annotation.CampsiteRepositoryFile;
import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.activity.fabric.ActivityDataFabric;
import fr.phylisiumstudio.logic.plot.fabric.PlotDataFabric;
import fr.phylisiumstudio.logic.repository.ICampsiteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

@Singleton
public class JsonCampsiteRepository implements ICampsiteRepository {
    private static final Logger logger = LoggerFactory.getLogger(JsonCampsiteRepository.class);
    private final File folder;
    private final ObjectMapper objectMapper;

    @Inject
    public JsonCampsiteRepository(@CampsiteRepositoryFile File folder, ObjectMapper objectMapper, ActivityDataFabric activityDataFabric, PlotDataFabric plotDataFabric)
    {
        this.folder = folder;
        this.objectMapper = objectMapper.copy();

        InjectableValues.Std injectableValues = new InjectableValues.Std();
        injectableValues.addValue(ActivityDataFabric.class, activityDataFabric);
        injectableValues.addValue(ActivityDataFabric.class.getName(), activityDataFabric);
        injectableValues.addValue(PlotDataFabric.class, plotDataFabric);
        injectableValues.addValue(PlotDataFabric.class.getName(), plotDataFabric);
        this.objectMapper.setInjectableValues(injectableValues);

        if (!this.folder.exists() && !this.folder.mkdirs()) {
            logger.warn("Unable to create campsite folder.");
        }
    }

    @Override
    public CompletableFuture<Campsite> create(Campsite entity) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                File file = getFile(entity.getUniqueID());
                if (file.exists()) {
                    throw new IllegalArgumentException("Campsite with ID " + entity.getUniqueID() + " already exists.");
                }

                if (file.createNewFile()) {
                    objectMapper.writeValue(file, entity);
                    return entity;
                } else {
                    throw new RuntimeException("Failed to create file for campsite with ID " + entity.getUniqueID());
                }
            } catch (Exception e) {
                logger.error("Error creating campsite: {}", e.getMessage(), e);
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public CompletableFuture<Campsite> read(UUID id) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                File file = getFile(id);
                if (!file.exists()) {
                    return null;
                }

                return objectMapper.readValue(file, Campsite.class);
            } catch (Exception e) {
                logger.error("Error reading campsite: {}", e.getMessage(), e);
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public CompletableFuture<Campsite> update(Campsite entity) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                File file = getFile(entity.getUniqueID());
                if (!file.exists()) {
                    throw new IllegalArgumentException("Campsite with ID " + entity.getUniqueID() + " does not exist.");
                }

                objectMapper.writeValue(file, entity);
                return entity;
            } catch (Exception e) {
                logger.error("Error updating campsite: {}", e.getMessage(), e);
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> delete(UUID id) {
        return CompletableFuture.runAsync(() -> {
            try {
                File file = getFile(id);
                if (file.exists() && !file.delete()) {
                    throw new RuntimeException("Failed to delete campsite with ID " + id);
                }
            } catch (Exception e) {
                logger.error("Error deleting campsite: {}", e.getMessage(), e);
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public CompletableFuture<List<Campsite>> list() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                File[] files = folder.listFiles((dir, name) -> name.endsWith(".json"));
                if (files == null) {
                    throw new RuntimeException("Failed to list campsite files.");
                }

                return Stream.of(files).map(file -> {
                    try {
                        return objectMapper.readValue(file, Campsite.class);
                    } catch (Exception e) {
                        logger.error("Error reading campsite file: {}", e.getMessage(), e);
                        return null;
                    }
                }).filter(Objects::nonNull).toList();
            } catch (Exception e) {
                logger.error("Error listing campsites: {}", e.getMessage(), e);
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> exists(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                File file = getFile(uuid);
                return file.exists();
            } catch (Exception e) {
                logger.error("Error checking campsite existence: {}", e.getMessage(), e);
                throw new RuntimeException(e);
            }
        });
    }

    private File getFile(UUID uniqueId) {
        return new File(folder, uniqueId.toString() + ".json");
    }
}
