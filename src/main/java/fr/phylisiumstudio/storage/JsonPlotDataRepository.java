package fr.phylisiumstudio.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import fr.phylisiumstudio.app.inject.annotation.PlotDataRepositoryFile;
import fr.phylisiumstudio.logic.plot.IPlotDataRepository;
import fr.phylisiumstudio.logic.plot.PlotData;
import fr.phylisiumstudio.logic.plot.PlotType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

@Singleton
public class JsonPlotDataRepository implements IPlotDataRepository {
    private static final Logger logger = LoggerFactory.getLogger(JsonPlotDataRepository.class);
    private final File folder;
    private final ObjectMapper objectMapper;

    @Inject
    public JsonPlotDataRepository(@PlotDataRepositoryFile File folder, ObjectMapper objectMapper) {
        this.folder = folder;
        this.objectMapper = objectMapper;

        if (!this.folder.exists() && !this.folder.mkdirs()) {
            logger.warn("Unable to create plot data folder: {}", folder.getAbsolutePath());
        }
    }

    @Override
    public CompletableFuture<PlotData> create(PlotData entity) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                File file = getFile(entity.type());
                if (file.exists()) {
                    throw new IllegalArgumentException("PlotData with type " + entity.type() + " already exists.");
                }

                if (file.createNewFile()) {
                    objectMapper.writeValue(file, entity);
                    return entity;
                } else {
                    throw new RuntimeException("Failed to create file for PlotData with type " + entity.type());
                }
            } catch (Exception e) {
                logger.error("Error creating PlotData: {}", e.getMessage(), e);
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public CompletableFuture<PlotData> read(PlotType id) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                File file = getFile(id);
                if (!file.exists()) {
                    return null;
                }

                return objectMapper.readValue(file, PlotData.class);
            } catch (Exception e) {
                logger.error("Error reading PlotData: {}", e.getMessage(), e);
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public CompletableFuture<PlotData> update(PlotData entity) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                File file = getFile(entity.type());
                if (!file.exists()) {
                    throw new IllegalArgumentException("PlotData with type " + entity.type() + " does not exist.");
                }

                objectMapper.writeValue(file, entity);
                return entity;
            } catch (Exception e) {
                logger.error("Error updating PlotData: {}", e.getMessage(), e);
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> delete(PlotType id) {
        return CompletableFuture.runAsync(() -> {
            try {
                File file = getFile(id);
                if (file.exists() && !file.delete()) {
                    throw new RuntimeException("Failed to delete PlotData with type " + id);
                }
            } catch (Exception e) {
                logger.error("Error deleting PlotData: {}", e.getMessage(), e);
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public CompletableFuture<List<PlotData>> list() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                File[] files = folder.listFiles((dir, name) -> name.endsWith(".json"));
                if (files == null) {
                    throw new RuntimeException("Failed to list PlotData files.");
                }

                return Stream.of(files).map(file -> {
                    try {
                        return objectMapper.readValue(file, PlotData.class);
                    } catch (Exception e) {
                        logger.error("Error reading PlotData file: {}", e.getMessage(), e);
                        return null;
                    }
                }).filter(Objects::nonNull).toList();
            } catch (Exception e) {
                logger.error("Error listing PlotData: {}", e.getMessage(), e);
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> exists(PlotType id) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                File file = getFile(id);
                return file.exists();
            } catch (Exception e) {
                logger.error("Error checking PlotData existence: {}", e.getMessage(), e);
                throw new RuntimeException(e);
            }
        });
    }

    private File getFile(PlotType type) {
        return new File(folder, type.name() + ".json");
    }
}
