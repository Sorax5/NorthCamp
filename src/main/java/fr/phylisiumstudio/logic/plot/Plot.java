package fr.phylisiumstudio.logic.plot;

import com.fasterxml.jackson.annotation.*;
import fr.phylisiumstudio.logic.plot.fabric.PlotDataFabric;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.joml.Vector3d;

import java.util.UUID;

@EqualsAndHashCode
@JsonIdentityInfo(
        generator = ObjectIdGenerators.PropertyGenerator.class,
        property = "uniqueID",
        scope = Plot.class
)
public class Plot {
    @Getter
    private final UUID uniqueID;

    @Getter
    @JsonIgnore
    private final PlotData plotData;

    @Getter
    private final Vector3d position;

    @JsonProperty("type")
    public PlotType getType() {
        return plotData != null ? plotData.type() : null; // Serialize type
    }

    @JsonCreator
    public Plot(
        @JsonProperty("uniqueID") UUID uniqueID,
        @JsonProperty("position") Vector3d position,
        @JsonProperty("type") PlotType type,
        @JacksonInject PlotDataFabric plotDataFabric) {
        this.uniqueID = uniqueID;
        this.position = position;
        this.plotData = plotDataFabric.getPlotData(type);
    }

    public Plot(PlotData plotData, Vector3d position) {
        this.plotData = plotData;
        this.position = position;
        this.uniqueID = UUID.randomUUID();
    }

    public Plot(UUID uniqueID, PlotData plotData, Vector3d position) {
        this.uniqueID = uniqueID;
        this.plotData = plotData;
        this.position = position;
    }
}
