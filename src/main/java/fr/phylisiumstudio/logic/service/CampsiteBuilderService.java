package fr.phylisiumstudio.logic.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.activity.Activity;
import fr.phylisiumstudio.logic.amenity.AmenityInstance;
import fr.phylisiumstudio.logic.builder.ActivityBuilder;
import fr.phylisiumstudio.logic.builder.AmenityBuilder;
import fr.phylisiumstudio.logic.builder.PlotBuilder;
import fr.phylisiumstudio.logic.plot.Plot;
import net.minestom.server.instance.InstanceContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

@Singleton
public class CampsiteBuilderService {
    private final PlotBuilder plotBuilder;
    private final ActivityBuilder activityBuilder;
    private final AmenityBuilder amenityBuilder;
    private final PlotDataService plotDataService;
    private final ActivityDataService activityDataService;
    private final Logger logger = LoggerFactory.getLogger(CampsiteBuilderService.class);

    @Inject
    public CampsiteBuilderService(PlotBuilder plotBuilder, ActivityBuilder activityBuilder, AmenityBuilder amenityBuilder,
                                  PlotDataService plotDataService, ActivityDataService activityDataService) {
        this.plotBuilder = plotBuilder;
        this.activityBuilder = activityBuilder;
        this.amenityBuilder = amenityBuilder;
        this.activityDataService = activityDataService;
        this.plotDataService = plotDataService;
    }

    public CompletableFuture<Void> BuildCampsiteAsync(Campsite campsite, InstanceContainer instanceContainer) {
        var futures = new ArrayList<CompletableFuture<Void>>();

        for (var activity : campsite.getActivities()) {
            futures.add(buildActivityAsync(activity, instanceContainer));
        }

        for (var plot : campsite.getPlots()) {
            futures.add(buildPlotAsync(plot, instanceContainer));
        }

        for (var amenity : campsite.getBuiltAmenities()) {
            futures.add(buildAmenityAsync(amenity, instanceContainer));
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    /** Construit une seule commodité dans l'instance (achat à chaud). */
    public CompletableFuture<Void> buildAmenityAsync(AmenityInstance amenity, InstanceContainer instanceContainer) {
        if (amenityBuilder == null) {
            return CompletableFuture.completedFuture(null);
        }
        return amenityBuilder.BuildAsync(amenity, instanceContainer);
    }

    /** Construit un seul emplacement de camping dans l'instance (achat à chaud). */
    public CompletableFuture<Void> buildPlotAsync(Plot plot, InstanceContainer instanceContainer) {
        if (plotBuilder == null) {
            return CompletableFuture.completedFuture(null);
        }
        return plotBuilder.BuildAsync(plotDataService.getPlotData(plot.getPlotType()), plot, instanceContainer);
    }

    /** Construit une seule activité dans l'instance (achat à chaud). */
    public CompletableFuture<Void> buildActivityAsync(Activity activity, InstanceContainer instanceContainer) {
        if (activityBuilder == null) {
            return CompletableFuture.completedFuture(null);
        }
        return activityBuilder.BuildAsync(activityDataService.getActivityData(activity.getType()), activity, instanceContainer);
    }
}
