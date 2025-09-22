package fr.phylisiumstudio.logic.plot;

import lombok.Getter;

import java.util.UUID;

public class Plot {
    @Getter
    private final UUID uniqueID;

    @Getter
    private final PlotData plotData;

    public Plot(PlotData plotData) {
        this.plotData = plotData;
        this.uniqueID = UUID.randomUUID();
    }

    public Plot(UUID uniqueID, PlotData plotData) {
        this.uniqueID = uniqueID;
        this.plotData = plotData;
    }
}
