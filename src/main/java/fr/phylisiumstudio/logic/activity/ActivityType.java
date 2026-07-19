package fr.phylisiumstudio.logic.activity;

public enum ActivityType {
    FISHING(TimeAffinity.DAY),
    SWIM(TimeAffinity.DAY),
    BARBECUE(TimeAffinity.NIGHT);

    private final TimeAffinity affinity;

    ActivityType(TimeAffinity affinity) {
        this.affinity = affinity;
    }

    public TimeAffinity affinity() {
        return affinity;
    }
}
