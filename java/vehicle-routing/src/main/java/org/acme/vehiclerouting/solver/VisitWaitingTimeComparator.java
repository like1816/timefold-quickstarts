package org.acme.vehiclerouting.solver;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import org.acme.vehiclerouting.domain.Visit;

/**
 * Sorts visits by waiting time in descending order (longer waiting time first).
 * Calculates waiting time using Visit's existing arrivalTime and minStartTime.
 */
public class VisitWaitingTimeComparator implements Comparator<Visit> {

    @Override
    public int compare(Visit a, Visit b) {
        long waitingTimeA = calculateWaitingTimeSeconds(a);
        long waitingTimeB = calculateWaitingTimeSeconds(b);
        // Reverse order: longer waiting time comes first
        return Long.compare(waitingTimeB, waitingTimeA);
    }

    private long calculateWaitingTimeSeconds(Visit visit) {
        LocalDateTime arrivalTime = visit.getArrivalTime();
        if (arrivalTime == null) {
            return 0;
        }
        LocalDateTime minStartTime = visit.getMinStartTime();
        if (arrivalTime.isBefore(minStartTime)) {
            return Duration.between(arrivalTime, minStartTime).getSeconds();
        }
        return 0;
    }
}
