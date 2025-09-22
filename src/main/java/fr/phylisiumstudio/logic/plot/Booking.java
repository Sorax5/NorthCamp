package fr.phylisiumstudio.logic.plot;

import java.util.Date;
import java.util.UUID;

/**
 * A booking of a plot by a client for a specific date range.
 *
 * @param plotID the ID of the plot being booked
 * @param clientID the ID of the client making the booking
 * @param startDate the start date of the booking
 * @param endDate the end date of the booking
 */
public record Booking (UUID plotID, UUID clientID, Date startDate, Date endDate) {
}
