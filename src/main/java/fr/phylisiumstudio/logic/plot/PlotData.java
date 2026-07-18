package fr.phylisiumstudio.logic.plot;

import fr.phylisiumstudio.logic.area.Area;

import java.util.List;

public record PlotData (PlotType type, Area area, String schem, List<PlotLevel> levels) {
}