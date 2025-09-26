package fr.phylisiumstudio.logic.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.activity.Activity;
import fr.phylisiumstudio.logic.activity.ActivityData;
import fr.phylisiumstudio.logic.builder.ActivityBuilder;
import fr.phylisiumstudio.logic.builder.Builder;
import fr.phylisiumstudio.logic.builder.fabric.BuilderFabric;
import fr.phylisiumstudio.logic.plot.PlotData;
import net.minestom.server.instance.InstanceContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Singleton
public class BuilderService {
    private final InstanceContainer instanceContainer;
    private final BuilderFabric builderFabric;

    @Inject
    public BuilderService(InstanceContainer instanceContainer, BuilderFabric builderFabric) {
        this.instanceContainer = instanceContainer;
        this.builderFabric = builderFabric;
    }

    public CompletableFuture<Void> buildCampsite(Campsite campsite) {
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (Activity activity : campsite.getActivities()) {
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }
}
