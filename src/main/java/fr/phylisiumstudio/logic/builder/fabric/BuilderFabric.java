package fr.phylisiumstudio.logic.builder.fabric;

import com.google.inject.Singleton;
import fr.phylisiumstudio.logic.builder.Builder;

import java.util.HashMap;
import java.util.Map;

@Singleton
public class BuilderFabric {
    protected final Map<Class<?>, Builder<?,?, ?>> builders;

    public BuilderFabric() {
        this.builders = new HashMap<>();
    }

    public <T, J, W> void registerBuilder(Class<T> clazz, Builder<T, J, W> builder) {
        builders.put(clazz, builder);
    }

    @SuppressWarnings("unchecked")
    public <T, J, W> Builder<T, J, W> getBuilder(Class<T> clazz) {
        return (Builder<T, J, W>) builders.get(clazz);
    }
}
