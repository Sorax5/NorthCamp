package fr.phylisiumstudio.logic.gameplay;

/**
 * Événement ponctuel qui secoue une journée de camping, dans le thème du grand
 * nord américain. Certains sont subis (orage, ours), d'autres bénéfiques
 * (festival) : ils cassent la routine et récompensent un camping bien tenu.
 */
public enum CampEvent {
    /** Orage : toutes les activités tombent hors service jusqu'à réparation. */
    STORM("⛈ Orage", "Un orage s'abat : les activités sont hors service.", false),
    /** Ours rôdeur : les campeurs sont effrayés, la réputation en pâtit. */
    BEAR("🐻 Ours rôdeur", "Un ours rôde dans le camping ! Les campeurs sont effrayés.", false),
    /** Festival régional : bouffée de réputation. */
    FESTIVAL("🎉 Festival régional", "Un festival attire l'attention : la réputation grimpe.", true);

    private final String displayName;
    private final String description;
    private final boolean positive;

    CampEvent(String displayName, String description, boolean positive) {
        this.displayName = displayName;
        this.description = description;
        this.positive = positive;
    }

    public String displayName() {
        return displayName;
    }

    public String description() {
        return description;
    }

    public boolean positive() {
        return positive;
    }
}
