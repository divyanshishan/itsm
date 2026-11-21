package com.itsm.system.pattern.state;

import com.itsm.system.exception.TechnicianUnavailableException;
import com.itsm.system.model.entity.Incident;
import com.itsm.system.model.entity.Technician;
import com.itsm.system.model.entity.User;
import com.itsm.system.model.enums.IncidentStatus;
import com.itsm.system.model.enums.SlaStatus;

import java.time.LocalDateTime;

/**
 * Concrete state representing an OPEN incident awaiting technician assignment.
 * Permitted transition: assignTechnician -> IN_PROGRESS.
 */
public class OpenState extends AbstractIncidentState {

    @Override
    public IncidentStatus getStatus() {
        return IncidentStatus.OPEN;
    }

    @Override
    public void assignTechnician(Incident incident, Technician technician, User actor) {
        if (technician == null) {
            throw new TechnicianUnavailableException("Cannot assign a null technician to incident " + incident.getIncidentCode());
        }

        incident.setAssignedTechnician(technician);
        technician.incrementWorkload();
        incident.setStatus(IncidentStatus.IN_PROGRESS);

        LocalDateTime now = LocalDateTime.now();
        incident.setAcknowledgedAt(now);

        // Response SLA threshold evaluation
        if (incident.getResponseDeadline() != null && now.isAfter(incident.getResponseDeadline())) {
            incident.setSlaViolated(true);
            incident.setSlaStatus(SlaStatus.RESPONSE_VIOLATED);
        }
    }
}
