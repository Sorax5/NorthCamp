package fr.phylisiumstudio.logic.activity.fabric;

import com.google.inject.Singleton;
import fr.phylisiumstudio.logic.activity.ActivityData;
import fr.phylisiumstudio.logic.activity.ActivityType;
import lombok.Getter;

import java.util.EnumMap;
import java.util.Map;

/**
 * Fabric for managing different types of ActivityData associated with ActivityType.
 */
@Singleton
public class ActivityDataFabric {
    @Getter
    private final Map<ActivityType, ActivityData> activityDataMap;

    public ActivityDataFabric() {
        this.activityDataMap = new EnumMap<ActivityType, ActivityData>(ActivityType.class);
    }

    public void registerActivityData(ActivityType activityType, ActivityData activityData) {
        this.activityDataMap.put(activityType, activityData);
    }

    public ActivityData getActivityData(ActivityType activityType) {
        return this.activityDataMap.get(activityType);
    }
}
