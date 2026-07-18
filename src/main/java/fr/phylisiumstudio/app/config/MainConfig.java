package fr.phylisiumstudio.app.config;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public class MainConfig {
    public String Host = "0.0.0.0";
    public int Port = 25565;
    public int ChunkRadius = 13;

    /** Durée réelle (secondes) d'une journée de jeu. */
    public int DayDurationSeconds = 600;
    /** Durée réelle (secondes) d'une nuit de jeu. */
    public int NightDurationSeconds = 300;

    /** Remplit automatiquement un camping neuf avec du contenu de démonstration. */
    public boolean SeedTestCampsite = true;
}
