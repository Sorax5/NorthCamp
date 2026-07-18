package fr.phylisiumstudio.app.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.joml.Vector3d;

import java.io.IOException;

public class Vector3dDeserializer extends JsonDeserializer<Vector3d> {
    @Override
    public Vector3d deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (p.currentToken() == JsonToken.VALUE_NULL) return null;

        double x = 0, y = 0, z = 0;

        if (p.currentToken() != JsonToken.START_OBJECT) {
            // try to handle array [x,y,z]
            if (p.currentToken() == JsonToken.START_ARRAY) {
                p.nextToken(); x = p.getDoubleValue();
                p.nextToken(); y = p.getDoubleValue();
                p.nextToken(); z = p.getDoubleValue();
                while (p.currentToken() != JsonToken.END_ARRAY) p.nextToken();
                return new Vector3d(x, y, z);
            }
        }

        while (p.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = p.currentName();
            p.nextToken();
            if ("x".equals(fieldName)) {
                x = p.getDoubleValue();
            } else if ("y".equals(fieldName)) {
                y = p.getDoubleValue();
            } else if ("z".equals(fieldName)) {
                z = p.getDoubleValue();
            } else {
                p.skipChildren();
            }
        }

        return new Vector3d(x, y, z);
    }
}

