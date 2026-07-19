package fr.phylisiumstudio.logic.builder;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import fr.phylisiumstudio.logic.amenity.AmenityInstance;
import fr.phylisiumstudio.logic.mapper.PositionMapper;
import fr.phylisiumstudio.logic.schematic.SchematicFactory;
import net.hollowcube.schem.util.Rotation;
import net.minestom.server.instance.InstanceContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Pose le bâtiment d'une commodité (schématique .nbt du type) à la position de son
 * instance. Si le schématique est absent de {@link SchematicFactory}, la
 * construction est un no-op (rien ne pop), comme pour les emplacements sans schem.
 */
@Singleton
public class AmenityBuilder {
    private static final Logger logger = LoggerFactory.getLogger(AmenityBuilder.class);

    private final SchematicFactory schematicFactory;

    @Inject
    public AmenityBuilder(SchematicFactory schematicFactory) {
        this.schematicFactory = schematicFactory;
    }

    public CompletableFuture<Void> BuildAsync(AmenityInstance amenity, InstanceContainer instance) {
        var future = new CompletableFuture<Void>();
        try {
            var schematic = schematicFactory.getSchematic(amenity.type().schem());
            var batch = schematic.createBatch(Rotation.NONE);
            batch.apply(instance, PositionMapper.toMinestomPos(amenity.position()), _ -> future.complete(null));
        } catch (Exception e) {
            // Schématique absent (assets non fournis) : on ne bloque pas, le slot
            // reste marqué occupé mais aucun bâtiment n'est posé.
            logger.debug("No schematic for amenity {} ({}), skipping build", amenity.type(), amenity.type().schem());
            future.complete(null);
        }
        return future;
    }
}
