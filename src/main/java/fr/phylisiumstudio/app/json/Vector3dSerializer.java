package fr.phylisiumstudio.app.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.joml.Vector3d;

import java.io.IOException;

public class Vector3dSerializer extends JsonSerializer<Vector3d> {
    @Override
    public void serialize(Vector3d v, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (v == null) {
            gen.writeNull();
            return;
        }

        gen.writeStartObject();
        gen.writeNumberField("x", v.x);
        gen.writeNumberField("y", v.y);
        gen.writeNumberField("z", v.z);
        gen.writeEndObject();
    }
}

