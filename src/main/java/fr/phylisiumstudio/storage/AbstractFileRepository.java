package fr.phylisiumstudio.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.phylisiumstudio.repository.IRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Repository générique persistant un fichier par entité dans un dossier.
 *
 * <p>Toute la logique CRUD (création/lecture/mise à jour/suppression/listing) est
 * factorisée ici. Les sous-classes ne fournissent que le format (mapper + extension),
 * le type d'entité et la façon de dériver la clé d'un fichier/entité.
 *
 * @param <T> type de l'entité
 * @param <K> type de la clé
 */
public abstract class AbstractFileRepository<T, K> implements IRepository<T, K> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final File folder;
    private final ObjectMapper mapper;
    private final Class<T> type;
    private final String extension;
    private final Function<T, K> keyOf;
    private final Function<K, String> keyToName;

    /**
     * @param folder     dossier de stockage (créé si absent)
     * @param mapper     mapper Jackson (JSON ou YAML)
     * @param type       classe de l'entité, pour la désérialisation
     * @param extension  extension de fichier avec le point, ex. {@code ".yml"}
     * @param keyOf      extrait la clé d'une entité
     * @param keyToName  convertit une clé en nom de fichier (sans extension)
     */
    protected AbstractFileRepository(File folder, ObjectMapper mapper, Class<T> type,
                                     String extension, Function<T, K> keyOf, Function<K, String> keyToName) {
        this.folder = folder;
        this.mapper = mapper;
        this.type = type;
        this.extension = extension;
        this.keyOf = keyOf;
        this.keyToName = keyToName;

        if (!folder.exists() && !folder.mkdirs()) {
            logger.warn("Unable to create folder: {}", folder.getAbsolutePath());
        }
    }

    @Override
    public CompletableFuture<T> create(T entity) {
        return CompletableFuture.supplyAsync(() -> {
            var file = fileOf(keyOf.apply(entity));
            if (file.exists()) {
                throw new IllegalArgumentException("Entity already exists: " + file.getName());
            }
            return write(file, entity);
        });
    }

    @Override
    public CompletableFuture<T> read(K id) {
        return CompletableFuture.supplyAsync(() -> {
            var file = fileOf(id);
            return file.exists() ? readFile(file) : null;
        });
    }

    @Override
    public CompletableFuture<T> update(T entity) {
        return CompletableFuture.supplyAsync(() -> {
            var file = fileOf(keyOf.apply(entity));
            if (!file.exists()) {
                throw new IllegalArgumentException("Entity does not exist: " + file.getName());
            }
            return write(file, entity);
        });
    }

    @Override
    public CompletableFuture<Void> delete(K id) {
        return CompletableFuture.runAsync(() -> {
            var file = fileOf(id);
            if (file.exists() && !file.delete()) {
                throw new RuntimeException("Failed to delete " + file.getName());
            }
        });
    }

    @Override
    public CompletableFuture<List<T>> list() {
        return CompletableFuture.supplyAsync(() -> {
            var files = folder.listFiles((dir, name) -> name.endsWith(extension));
            if (files == null) {
                throw new RuntimeException("Failed to list files in " + folder.getAbsolutePath());
            }
            return Stream.of(files)
                    .map(this::readSafely)
                    .filter(Objects::nonNull)
                    .toList();
        });
    }

    @Override
    public CompletableFuture<Boolean> exists(K id) {
        return CompletableFuture.supplyAsync(() -> fileOf(id).exists());
    }

    private T write(File file, T entity) {
        try {
            mapper.writeValue(file, entity);
            return entity;
        } catch (Exception e) {
            throw new RuntimeException("Error writing " + file.getName(), e);
        }
    }

    private T readFile(File file) {
        try {
            return mapper.readValue(file, type);
        } catch (Exception e) {
            throw new RuntimeException("Error reading " + file.getName(), e);
        }
    }

    private T readSafely(File file) {
        try {
            return mapper.readValue(file, type);
        } catch (Exception e) {
            logger.error("Skipping unreadable file {}: {}", file.getName(), e.getMessage());
            return null;
        }
    }

    private File fileOf(K id) {
        return new File(folder, keyToName.apply(id) + extension);
    }
}
