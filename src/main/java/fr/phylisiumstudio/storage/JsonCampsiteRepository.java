package fr.phylisiumstudio.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import fr.phylisiumstudio.app.inject.annotation.CampsiteRepositoryFile;
import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.activity.Activity;
import fr.phylisiumstudio.logic.activity.fabric.ActivityDataFabric;
import fr.phylisiumstudio.logic.plot.Plot;
import fr.phylisiumstudio.logic.plot.fabric.PlotDataFabric;
import fr.phylisiumstudio.logic.repository.ICampsiteRepository;
import fr.phylisiumstudio.storage.serialize.ActivitySerializer;
import fr.phylisiumstudio.storage.serialize.PlotSerializer;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

@Singleton
public class JsonCampsiteRepository implements ICampsiteRepository {
    private final File folder;
    private final Gson gson;
    private final Logger logger;

    @Inject
    public JsonCampsiteRepository(@CampsiteRepositoryFile File folder, Logger logger, GsonBuilder gsonBuilder, PlotSerializer plotSerializer, ActivitySerializer activitySerializer)
    {
        this.logger = logger;
        this.folder = folder;
        if (!this.folder.exists() && !this.folder.mkdirs()) {
            this.logger.log(Level.WARNING, "Unable to create campsite folder.");
        }

        gsonBuilder.registerTypeAdapter(Plot.class, plotSerializer);
        gsonBuilder.registerTypeAdapter(Activity.class, activitySerializer);

        this.gson = gsonBuilder.create();
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
                    String json = gson.toJson(entity);
                    Files.writeString(file.toPath(), json);
                    return entity;
                } else {
                    throw new RuntimeException("Failed to create file for campsite with ID " + entity.getUniqueID());
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error creating campsite: " + e.getMessage(), e);
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

                String json = Files.readString(file.toPath());
                return gson.fromJson(json, Campsite.class);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error reading campsite: " + e.getMessage(), e);
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

                String json = gson.toJson(entity);
                Files.writeString(file.toPath(), json);
                return entity;
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error updating campsite: " + e.getMessage(), e);
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
                logger.log(Level.SEVERE, "Error deleting campsite: " + e.getMessage(), e);
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
                        String json = Files.readString(file.toPath());
                        return gson.fromJson(json, Campsite.class);
                    } catch (Exception e) {
                        logger.log(Level.SEVERE, "Error reading campsite file: " + e.getMessage(), e);
                        return null;
                    }
                }).filter(Objects::nonNull).toList();
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error listing campsites: " + e.getMessage(), e);
                throw new RuntimeException(e);
            }
        });
    }

    private File getFile(UUID uniqueId) {
        return new File(folder, uniqueId.toString() + ".json");
    }
}
