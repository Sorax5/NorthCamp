package fr.phylisiumstudio.logic.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.builder.ActivityBuilder;
import fr.phylisiumstudio.logic.builder.PlotBuilder;
import net.minestom.server.instance.InstanceContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

@Singleton
public class BuilderService {
    private final PlotBuilder plotBuilder;
    private final ActivityBuilder activityBuilder;
    private final PlotDataService plotDataService;
    private final ActivityDataService activityDataService;
    private final Logger logger = LoggerFactory.getLogger(BuilderService.class);

    @Inject
    public BuilderService(PlotBuilder plotBuilder, ActivityBuilder activityBuilder, PlotDataService plotDataService, ActivityDataService activityDataService) {
        this.plotBuilder = plotBuilder;
        this.activityBuilder = activityBuilder;
        this.activityDataService = activityDataService;
        this.plotDataService = plotDataService;
    }

    public CompletableFuture<Void> BuildCampsiteAsync(Campsite campsite, InstanceContainer instanceContainer) {
        var futures = new ArrayList<CompletableFuture<Void>>();

        for (var activity : campsite.getActivities()) {
            var activityData = activityDataService.getActivityData(activity.getType());
            if (activityBuilder != null) {
                futures.add(activityBuilder.BuildAsync(activityData, activity, instanceContainer));
            }
        }

        for (var plot : campsite.getPlots()) {
            var plotData = plotDataService.getPlotData(plot.getPlotType());
            if (plotBuilder != null) {
                futures.add(plotBuilder.BuildAsync(plotData, plot, instanceContainer));
            }
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }
}
