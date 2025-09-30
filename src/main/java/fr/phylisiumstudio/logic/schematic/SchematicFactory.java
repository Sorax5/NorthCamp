package fr.phylisiumstudio.logic.schematic;

import com.google.inject.Singleton;
import net.hollowcube.schem.Schematic;

import java.util.HashMap;
import java.util.Map;

@Singleton
public class SchematicFactory {
    private final Map<String, Schematic> schematics;

    public SchematicFactory() {
        this.schematics = new HashMap<>();
    }

    public void registerSchematic(String name, Schematic schematic) {
        schematics.put(name, schematic);
    }

    public Schematic getSchematic(String name) {
        return schematics.get(name);
    }
}
