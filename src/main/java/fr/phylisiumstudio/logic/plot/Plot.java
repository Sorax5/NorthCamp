package fr.phylisiumstudio.logic.plot;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.joml.Vector3d;

import java.util.UUID;

@EqualsAndHashCode
public class Plot {
    @Getter
    private final UUID uniqueID;

    @Getter
    private final PlotData plotData;

    @Getter
    private final Vector3d position;

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
