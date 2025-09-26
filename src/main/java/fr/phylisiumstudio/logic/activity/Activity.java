package fr.phylisiumstudio.logic.activity;

import lombok.Getter;
import org.joml.Vector3d;

import java.util.UUID;

public class Activity {
    @Getter
    private final UUID uniqueID;

    @Getter
    private final ActivityData activityData;

    @Getter
    private final Vector3d position;

    public Activity(ActivityData activityData, Vector3d position) {
        this.activityData = activityData;
        this.uniqueID = UUID.randomUUID();
        this.position = position;
    }

    public Activity(UUID uniqueID, ActivityData activityData, Vector3d position) {
        this.uniqueID = uniqueID;
        this.activityData = activityData;
        this.position = position;
    }
}
