package fr.phylisiumstudio.app.inject;

import com.fasterxml.jackson.annotation.ObjectIdGenerator;
import com.fasterxml.jackson.annotation.ObjectIdResolver;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.cfg.HandlerInstantiator;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.google.inject.Injector;

/**
 * A Jackson HandlerInstantiator that delegates instance creation to Guice.
 * The injector can be set after construction, allowing the instantiator to be
 * registered on the ObjectMapper before the Guice injector is created.
 */
public class GuiceHandlerInstantiator extends HandlerInstantiator {

    private Injector injector;

    public void setInjector(Injector injector) {
        this.injector = injector;
    }

    private <T> T getInstance(Class<?> clazz) {
        if (injector == null) {
            throw new IllegalStateException("Guice injector has not been set yet");
        }
        @SuppressWarnings("unchecked")
        T instance = (T) injector.getInstance(clazz);
        return instance;
    }

    @Override
    public JsonDeserializer<?> deserializerInstance(DeserializationConfig config, Annotated annotated, Class<?> deserClass) {
        return getInstance(deserClass);
    }

    @Override
    public KeyDeserializer keyDeserializerInstance(DeserializationConfig config, Annotated annotated, Class<?> keyDeserClass) {
        return getInstance(keyDeserClass);
    }

    @Override
    public JsonSerializer<?> serializerInstance(com.fasterxml.jackson.databind.SerializationConfig config, Annotated annotated, Class<?> serClass) {
        return getInstance(serClass);
    }

    @Override
    public TypeResolverBuilder<?> typeResolverBuilderInstance(MapperConfig<?> config, Annotated annotated, Class<?> builderClass) {
        return getInstance(builderClass);
    }

    @Override
    public TypeIdResolver typeIdResolverInstance(MapperConfig<?> config, Annotated annotated, Class<?> resolverClass) {
        return getInstance(resolverClass);
    }

    @Override
    public ObjectIdGenerator<?> objectIdGeneratorInstance(MapperConfig<?> config, Annotated annotated, Class<?> implClass) {
        return getInstance(implClass);
    }

    @Override
    public ObjectIdResolver resolverIdGeneratorInstance(MapperConfig<?> config, Annotated annotated, Class<?> implClass) {
        return getInstance(implClass);
    }
}



