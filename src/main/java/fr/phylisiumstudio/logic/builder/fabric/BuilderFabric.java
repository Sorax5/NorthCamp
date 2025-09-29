package fr.phylisiumstudio.logic.builder.fabric;

import com.google.inject.Singleton;
import fr.phylisiumstudio.logic.builder.Builder;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

@Singleton
public class BuilderFabric {
    protected final Map<String, Builder<?,?, ?>> registry = new HashMap<>();

    public <T, U, W> void register(
            String key,
            Supplier<Builder<T, U, W>> supplier
    ) {
        registry.put(key, supplier.get());
    }

    @SuppressWarnings("unchecked")
    public <T, U, W> Builder<T, U, W> create(String key) {
        return (Builder<T, U, W>) registry.get(key);
    }
}
