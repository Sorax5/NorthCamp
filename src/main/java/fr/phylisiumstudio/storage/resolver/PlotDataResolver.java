package fr.phylisiumstudio.storage.resolver;

import com.fasterxml.jackson.annotation.ObjectIdGenerator;
import com.fasterxml.jackson.annotation.ObjectIdResolver;
import com.google.inject.Inject;
import fr.phylisiumstudio.logic.plot.PlotType;
import fr.phylisiumstudio.logic.plot.fabric.PlotDataFabric;

public class PlotDataResolver implements ObjectIdResolver {

    private final PlotDataFabric fabric;

    @Inject
    public PlotDataResolver(PlotDataFabric fabric) {
        this.fabric = fabric;
    }

    @Override
    public Object resolveId(ObjectIdGenerator.IdKey id) {
        PlotType type = (PlotType) id.key;
        return fabric.getPlotData(type);
    }

    @Override
    public void bindItem(ObjectIdGenerator.IdKey id, Object pojo) {}

    @Override
    public ObjectIdResolver newForDeserialization(Object context) {
        return this;
    }

    @Override
    public boolean canUseFor(ObjectIdResolver resolverType) {
        return resolverType.getClass() == getClass();
    }
}
