package fr.phylisiumstudio.storage.serialize;

import com.google.gson.*;
import fr.phylisiumstudio.logic.plot.Plot;
import fr.phylisiumstudio.logic.plot.PlotData;
import fr.phylisiumstudio.logic.plot.PlotType;
import fr.phylisiumstudio.logic.plot.fabric.PlotDataFabric;

import java.lang.reflect.Type;
import java.util.UUID;
import java.util.logging.Logger;

public class PlotSerializer implements JsonSerializer<Plot>, JsonDeserializer<Plot> {

    private final String UNIQUE_ID = "uniqueID";
    private final String TYPE = "type";

    private final PlotDataFabric plotDataFabric;
    private final Logger logger;

    public PlotSerializer(PlotDataFabric plotDataFabric, Logger logger) {
        this.plotDataFabric = plotDataFabric;
        this.logger = logger;
    }

    @Override
    public Plot deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        try {
            JsonObject jsonObject = jsonElement.getAsJsonObject();

            String uniqueID = jsonObject.get(UNIQUE_ID).getAsString();
            UUID plotUUID = UUID.fromString(uniqueID);

            String plotType = jsonObject.get(TYPE).getAsString();
            PlotType plotTypeEnum = PlotType.valueOf(plotType);
            PlotData plotData = plotDataFabric.getPlotData(plotTypeEnum);

            return new Plot(plotUUID, plotData);
        }
        catch (Exception e) {
            logger.warning("Could not deserialize plot");
            throw new JsonParseException(e);
        }
    }

    @Override
    public JsonElement serialize(Plot plot, Type type, JsonSerializationContext jsonSerializationContext) {
        try {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty(UNIQUE_ID, plot.getUniqueID().toString());
            jsonObject.addProperty(TYPE, plot.getPlotData().type().toString());
            return jsonObject;
        }
        catch (Exception e) {
            logger.warning("Could not serialize plot " + plot.getUniqueID().toString());
            throw new JsonParseException(e);
        }
    }
}
