package com.itsm.system.pattern.state;

import com.itsm.system.exception.InvalidUserInputException;
import com.itsm.system.model.entity.Incident;
import com.itsm.system.model.entity.User;
import com.itsm.system.model.enums.IncidentStatus;
import com.itsm.system.model.enums.SlaStatus;

import java.time.LocalDateTime;

/**
 * Concrete state representing an incident actively investigated by an assigned technician (IN_PROGRESS).
 * Permitted transition: resolveAndClose -> CLOSED.
 */
public class InProgressState extends AbstractIncidentState {

    @Override
    public IncidentStatus getStatus() {
        return IncidentStatus.IN_PROGRESS;
    }

    @Override
    public void resolveAndClose(Incident incident, User actor, String resolutionDetails) {
        if (resolutionDetails == null || resolutionDetails.trim().isEmpty()) {
            throw new InvalidUserInputException("Resolution details must be provided when resolving an incident.");
        }

        LocalDateTime now = LocalDateTime.now();
        incident.setResolutionDetails(resolutionDetails.trim());
        incident.setResolvedAt(now);
        incident.setClosedAt(now);
        incident.setStatus(IncidentStatus.CLOSED);

        if (incident.getAssignedTechnician() != null) {
            incident.getAssignedTechnician().decrementWorkload();
        }

        // Resolution SLA threshold evaluation
        if (incident.getResolutionDeadline() != null && now.isAfter(incident.getResolutionDeadline())) {
            incident.setSlaViolated(true);
            incident.setSlaStatus(SlaStatus.RESOLUTION_VIOLATED);
        } else if (!incident.isSlaViolated()) {
            incident.setSlaStatus(SlaStatus.MET);
        }
    }
}
