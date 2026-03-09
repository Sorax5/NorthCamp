package fr.phylisiumstudio.logic.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import fr.phylisiumstudio.logic.activity.ActivityData;
import fr.phylisiumstudio.logic.activity.ActivityType;
import fr.phylisiumstudio.logic.repository.IActivityRepository;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Singleton
public class ActivityDataService {
    private final IActivityRepository activityRepository;
    private final Map<ActivityType, ActivityData> activityDataMap;

    @Inject
    public ActivityDataService(IActivityRepository activityRepository) {
        this.activityRepository = activityRepository;
        this.activityDataMap = new EnumMap<>(ActivityType.class);
    }

    public void load() {
        for (var activityData : activityRepository.list().join()) {
            activityDataMap.put(activityData.type(), activityData);
        }
    }

    public ActivityData getActivityData(ActivityType type) {
        return activityDataMap.get(type);
    }

    public void addActivityData(ActivityData activityData) {
        activityDataMap.put(activityData.type(), activityData);
        activityRepository.create(activityData);
    }

    public List<ActivityData> listActivityData() {
        return activityDataMap.values().stream().toList();
    }
}
