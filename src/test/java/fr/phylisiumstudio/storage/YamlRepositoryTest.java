package fr.phylisiumstudio.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.phylisiumstudio.app.json.JacksonConfig;
import fr.phylisiumstudio.logic.area.Area;
import fr.phylisiumstudio.logic.plot.PlotData;
import fr.phylisiumstudio.logic.plot.PlotLevel;
import fr.phylisiumstudio.logic.plot.PlotType;
import fr.phylisiumstudio.logic.repository.IPlotDataRepository;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class YamlRepositoryTest {

    private static IPlotDataRepository repo(File folder) {
        ObjectMapper yaml = JacksonConfig.createYaml();
        return new YamlPlotDataRepository(folder, yaml);
    }

    @Test
    void roundTripsPlotDataThroughYaml(@TempDir Path dir) {
        var repo = repo(dir.toFile());
        var data = new PlotData(
                PlotType.CAMPSITE,
                new Area(new Vector3d(0, 0, 0), new Vector3d(8, 6, 8)),
                "campsite.nbt",
                List.of(new PlotLevel(0, "campsite.nbt", 10),
                        new PlotLevel(500, "campsite.nbt", 20)));

        repo.create(data).join();

        // Fichier YAML écrit sur disque avec la bonne extension
        assertTrue(new File(dir.toFile(), "CAMPSITE.yml").exists());

        var loaded = repo.read(PlotType.CAMPSITE).join();
        assertNotNull(loaded);
        assertEquals(PlotType.CAMPSITE, loaded.type());
        assertEquals("campsite.nbt", loaded.schem());
        assertEquals(2, loaded.levels().size());
        assertEquals(20, loaded.levels().get(1).income());
        // Vector3d sérialisé/désérialisé correctement
        assertEquals(new Vector3d(8, 6, 8), loaded.area().secondCorner());
    }

    @Test
    void listReturnsOnlyYamlFiles(@TempDir Path dir) {
        var repo = repo(dir.toFile());
        repo.create(new PlotData(PlotType.CARAVAN,
                new Area(new Vector3d(0, 0, 0), new Vector3d(1, 1, 1)),
                "caravan.nbt", List.of())).join();

        List<PlotData> all = repo.list().join();
        assertEquals(1, all.size());
        assertEquals(PlotType.CARAVAN, all.get(0).type());
    }
}
