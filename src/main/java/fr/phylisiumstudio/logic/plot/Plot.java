package fr.phylisiumstudio.logic.plot;

import com.fasterxml.jackson.annotation.*;
import lombok.Data;
import org.joml.Vector3d;

import java.util.UUID;

@Data
@JsonIdentityInfo(
        generator = ObjectIdGenerators.PropertyGenerator.class,
        property = "uniqueID",
        scope = Plot.class
)
public class Plot {
    private final UUID uniqueID;
    private final PlotType plotType;
    private final Vector3d position;
    private int level = 0;

    /** Prix de location courant fixé par le propriétaire. */
    private double price = 0;

    /** Emplacement laissé sale après le départ d'un client ; indisponible tant que non nettoyé. */
    private boolean dirty = false;

    @JsonCreator
    public Plot(
            @JsonProperty("uniqueID") UUID uniqueID,
            @JsonProperty("position") Vector3d position,
            @JsonProperty("plotType") PlotType plotType,
            @JsonProperty("level") int level,
            @JsonProperty("price") double price,
            @JsonProperty("dirty") boolean dirty
    ) {
        this.uniqueID = uniqueID;
        this.position = position;
        this.plotType = plotType;
        this.level = level;
        this.price = price;
        this.dirty = dirty;
    }

    public Plot(Vector3d position, PlotType plotType) {
        this.uniqueID = UUID.randomUUID();
        this.position = position;
        this.plotType = plotType;
    }
}
