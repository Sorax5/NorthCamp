package fr.phylisiumstudio.logic.activity;

import lombok.Getter;

import java.util.UUID;

public class Activity {
    @Getter
    private final UUID uniqueID;

    @Getter
    private final ActivityData activityData;

    public Activity(ActivityData activityData) {
        this.activityData = activityData;
        this.uniqueID = UUID.randomUUID();
    }

    public Activity(UUID uniqueID, ActivityData activityData) {
        this.uniqueID = uniqueID;
        this.activityData = activityData;
    }
}
