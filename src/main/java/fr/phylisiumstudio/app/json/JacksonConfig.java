package fr.phylisiumstudio.app.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public final class JacksonConfig {
    private JacksonConfig() {}

    public static ObjectMapper create() {
        return configure(new ObjectMapper());
    }

    /**
     * Mapper YAML partageant les mêmes modules et features que le mapper JSON.
     * Utilisé pour les données de contenu (PlotData, ActivityData).
     */
    public static ObjectMapper createYaml() {
        return configure(new ObjectMapper(new YAMLFactory()
                .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)));
    }

    private static ObjectMapper configure(ObjectMapper mapper) {
        // Modules
        mapper.registerModule(new JavaTimeModule());
        mapper.registerModule(new Jdk8Module());
        mapper.registerModule(new Vector3dModule());

        // Features
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        return mapper;
    }
}

