package fr.phylisiumstudio.storage.serialize;

import com.google.gson.*;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import fr.phylisiumstudio.logic.activity.Activity;
import fr.phylisiumstudio.logic.activity.ActivityData;
import fr.phylisiumstudio.logic.activity.ActivityType;
import fr.phylisiumstudio.logic.activity.fabric.ActivityDataFabric;

import java.lang.reflect.Type;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
public class ActivitySerializer implements JsonSerializer<Activity>, JsonDeserializer<Activity> {

    private final String UNIQUE_ID = "uniqueID";
    private final String TYPE = "type";

    private final ActivityDataFabric activityDataFabric;
    private final Logger logger;

    @Inject
    public ActivitySerializer(ActivityDataFabric activityDataFabric, Logger logger) {
        this.activityDataFabric = activityDataFabric;
        this.logger = logger;
    }

    @Override
    public Activity deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        try {
            JsonObject jsonObject = jsonElement.getAsJsonObject();

            String uniqueID = jsonObject.get(UNIQUE_ID).getAsString();
            UUID activityUUID = UUID.fromString(uniqueID);

            String activityType = jsonObject.get(TYPE).getAsString();
            ActivityType activityTypeEnum = ActivityType.valueOf(activityType);
            ActivityData activityData = activityDataFabric.getActivityData(activityTypeEnum);

            return new Activity(activityUUID, activityData);
        }
        catch (Exception e) {
            logger.log(Level.WARNING, "Could not deserialize activity", e);
            throw new JsonParseException(e);
        }
    }

    @Override
    public JsonElement serialize(Activity activity, Type type, JsonSerializationContext jsonSerializationContext) {
        try {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty(UNIQUE_ID, activity.getUniqueID().toString());
            jsonObject.addProperty(TYPE, activity.getActivityData().type().toString());

            return jsonObject;
        }
        catch (Exception e) {
            logger.log(Level.WARNING, "Could not serialize activity " + activity.getUniqueID().toString(), e);
            throw new JsonParseException(e);
        }
    }
}
