package fr.phylisiumstudio.logic;

import fr.phylisiumstudio.logic.activity.Activity;
import fr.phylisiumstudio.logic.plot.Booking;
import fr.phylisiumstudio.logic.plot.Plot;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Campsite {
    @Getter
    private final UUID uniqueID;
    @Getter
    private final UUID ownerID;

    private List<Activity> activities = new ArrayList<>();
    private List<Plot> plots = new ArrayList<>();
    private List<Booking> bookings = new ArrayList<>();
    private List<Client> clients = new ArrayList<>();

    public Campsite(UUID ownerID) {
        this.uniqueID = UUID.randomUUID();
        this.ownerID = ownerID;
    }
}
