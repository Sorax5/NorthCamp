package fr.phylisiumstudio.logic.amenity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.joml.Vector3d;

import java.util.UUID;

/**
 * Une commodité effectivement construite dans le camping : son type et la
 * position de son bâtiment. Persistée dans le camping.
 */
public record AmenityInstance(
        @JsonProperty("id") UUID id,
        @JsonProperty("type") Amenity type,
        @JsonProperty("position") Vector3d position
) {
    @JsonCreator
    public AmenityInstance {
    }

    public AmenityInstance(Amenity type, Vector3d position) {
        this(UUID.randomUUID(), type, new Vector3d(position));
    }
}
