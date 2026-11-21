package com.itsm.system.pattern.state;

import com.itsm.system.model.entity.Incident;
import com.itsm.system.model.entity.User;
import com.itsm.system.model.enums.IncidentStatus;

import java.time.LocalDateTime;

/**
 * Concrete state representing a CLOSED incident.
 * Permitted transition: reopen -> IN_PROGRESS.
 */
public class ClosedState extends AbstractIncidentState {

    @Override
    public IncidentStatus getStatus() {
        return IncidentStatus.CLOSED;
    }

    @Override
    public void reopen(Incident incident, User actor, String reason) {
        LocalDateTime now = LocalDateTime.now();
        incident.setStatus(IncidentStatus.IN_PROGRESS);
        incident.setResolvedAt(null);
        incident.setClosedAt(null);

        if (incident.getAssignedTechnician() != null) {
            incident.getAssignedTechnician().incrementWorkload();
        }

        String entry = "[" + now + " - " + actor.getUserIdCode() + "] REOPENED: " +
                (reason != null && !reason.trim().isEmpty() ? reason.trim() : "Incident reopened for further troubleshooting.");
        String existingNotes = incident.getInvestigationNotes();
        incident.setInvestigationNotes(existingNotes == null || existingNotes.isEmpty() ? entry : existingNotes + "\n" + entry);
    }
}
