package fr.phylisiumstudio.logic.activity;

import fr.phylisiumstudio.logic.area.Area;

import java.util.List;

public record ActivityData(ActivityType type, Area area, List<ActivityLevel> levels) {
}
