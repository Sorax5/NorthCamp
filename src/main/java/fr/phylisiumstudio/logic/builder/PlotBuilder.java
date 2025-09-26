package fr.phylisiumstudio.logic.builder;

import fr.phylisiumstudio.logic.plot.Plot;
import fr.phylisiumstudio.logic.plot.PlotData;
import net.minestom.server.instance.InstanceContainer;

import java.util.concurrent.CompletableFuture;

public class PlotBuilder implements Builder<PlotData, Plot, InstanceContainer> {
    @Override
    public CompletableFuture<Void> Build(PlotData data, Plot state, InstanceContainer instance) {
                return CompletableFuture.completedFuture(null);
    }
}
