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

    @JsonCreator
    public Plot(
            @JsonProperty("uniqueID") UUID uniqueID,
            @JsonProperty("position") Vector3d position,
            @JsonProperty("plotType") PlotType plotType
    ) {
        this.uniqueID = uniqueID;
        this.position = position;
        this.plotType = plotType;
    }

    public Plot(Vector3d position, PlotType plotType) {
        this.uniqueID = UUID.randomUUID();
        this.position = position;
        this.plotType = plotType;
    }
}
