package fr.phylisiumstudio.logic.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import fr.phylisiumstudio.logic.plot.PlotData;
import fr.phylisiumstudio.logic.plot.PlotType;
import fr.phylisiumstudio.logic.repository.IPlotDataRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class PlotDataService {
    private final IPlotDataRepository plotDataRepository;
    private final Map<PlotType, PlotData> plotDataMap;

    @Inject
    public PlotDataService(IPlotDataRepository plotDataRepository) {
        this.plotDataRepository = plotDataRepository;
        this.plotDataMap = new HashMap<>();
    }

    /**
     * Charge les PlotData depuis le repository. Doit être appelé après que
     * l'injecteur Guice et l'ObjectMapper aient été configurés (p.ex. depuis App.LoadData()).
     */
    public void load() {
        for (var plotData : plotDataRepository.list().join()) {
            plotDataMap.put(plotData.type(), plotData);
        }
    }

    public PlotData getPlotData(PlotType type) {
        return plotDataMap.get(type);
    }

    public void addPlotData(PlotData plotData) {
        plotDataMap.put(plotData.type(), plotData);
        plotDataRepository.create(plotData);
    }

    public List<PlotData> listPlotData() {
        return plotDataMap.values().stream().toList();
    }
}
