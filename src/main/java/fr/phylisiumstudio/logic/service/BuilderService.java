package fr.phylisiumstudio.logic.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.builder.fabric.BuilderFabric;
import fr.phylisiumstudio.logic.plot.PlotDataService;
import net.minestom.server.instance.InstanceContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

@Singleton
public class BuilderService {
    private final BuilderFabric builderFabric;
    private final PlotDataService plotDataService;
    private final Logger logger = LoggerFactory.getLogger(BuilderService.class);

    @Inject
    public BuilderService(BuilderFabric builderFabric, PlotDataService plotDataService) {
        this.builderFabric = builderFabric;
        this.plotDataService = plotDataService;
    }

    public CompletableFuture<Void> BuildCampsiteAsync(Campsite campsite, InstanceContainer instanceContainer) {
        var futures = new ArrayList<CompletableFuture<Void>>();

        for (var activity : campsite.getActivities()) {
            var builder = builderFabric.create("activity");
            if (builder != null) {
                futures.add(builder.BuildAsync(activity.getActivityData(), activity, instanceContainer));
            }
        }

        for (var plot : campsite.getPlots()) {
            var builder = builderFabric.create("plot");
            var plotData = plotDataService.getPlotData(plot.getPlotType());

            if (builder != null) {
                futures.add(builder.BuildAsync(plotData, plot, instanceContainer));
            }
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }
}
