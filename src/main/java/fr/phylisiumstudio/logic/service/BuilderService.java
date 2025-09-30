package fr.phylisiumstudio.logic.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.activity.Activity;
import fr.phylisiumstudio.logic.activity.ActivityData;
import fr.phylisiumstudio.logic.builder.Builder;
import fr.phylisiumstudio.logic.builder.fabric.BuilderFabric;
import fr.phylisiumstudio.logic.plot.Plot;
import fr.phylisiumstudio.logic.plot.PlotData;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.InstanceManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Singleton
public class BuilderService {
    private final InstanceManager instanceManager;
    private final BuilderFabric builderFabric;

    @Inject
    public BuilderService(InstanceManager instanceManager, BuilderFabric builderFabric) {
        this.instanceManager = instanceManager;
        this.builderFabric = builderFabric;
    }

    public CompletableFuture<Void> BuildCampsiteAsync(Campsite campsite, InstanceContainer instanceContainer) {
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (Activity activity : campsite.getActivities()) {
            Builder<ActivityData, Activity, InstanceContainer> builder = builderFabric.create("activity");
            if (builder != null) {
                futures.add(builder.BuildAsync(activity.getActivityData(), activity, instanceContainer));
            }
        }

        for (Plot plot : campsite.getPlots()) {
            Builder<PlotData, Plot, InstanceContainer> builder = builderFabric.create("plot");
            if (builder != null) {
                futures.add(builder.BuildAsync(plot.getPlotData(), plot, instanceContainer));
            }
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }


}
