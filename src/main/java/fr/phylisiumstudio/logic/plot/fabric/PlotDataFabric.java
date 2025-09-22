package fr.phylisiumstudio.logic.plot.fabric;

import fr.phylisiumstudio.logic.plot.PlotData;
import fr.phylisiumstudio.logic.plot.PlotType;
import lombok.Getter;

import java.util.EnumMap;
import java.util.Map;

/**
 * Fabric for managing different types of PlotData associated with PlotType.
 */
public class PlotDataFabric {
    @Getter
    private final Map<PlotType, PlotData> plotDataMap;

    public PlotDataFabric() {
        this.plotDataMap = new EnumMap<PlotType, PlotData>(PlotType.class);
    }

    public void registerPlotData(PlotType plotType, PlotData plotData) {
        this.plotDataMap.put(plotType, plotData);
    }

    public PlotData getPlotData(PlotType plotType) {
        return this.plotDataMap.get(plotType);
    }
}
