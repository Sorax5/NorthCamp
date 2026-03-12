package fr.phylisiumstudio.app.json;

import com.fasterxml.jackson.databind.module.SimpleModule;
import org.joml.Vector3d;

public class Vector3dModule extends SimpleModule {
    public Vector3dModule() {
        super("Vector3dModule");
        addSerializer(Vector3d.class, new Vector3dSerializer());
        addDeserializer(Vector3d.class, new Vector3dDeserializer());
    }
}

