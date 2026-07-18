package fr.phylisiumstudio.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import fr.phylisiumstudio.app.inject.annotation.ContentMapper;
import fr.phylisiumstudio.app.inject.annotation.PlotDataRepositoryFile;
import fr.phylisiumstudio.logic.plot.PlotData;
import fr.phylisiumstudio.logic.plot.PlotType;
import fr.phylisiumstudio.logic.repository.IPlotDataRepository;

import java.io.File;

@Singleton
public class YamlPlotDataRepository extends AbstractFileRepository<PlotData, PlotType>
        implements IPlotDataRepository {

    @Inject
    public YamlPlotDataRepository(@PlotDataRepositoryFile File folder, @ContentMapper ObjectMapper mapper) {
        super(folder, mapper, PlotData.class, ".yml", PlotData::type, PlotType::name);
    }
}
