package com.itsm.system.pattern.state;

import com.itsm.system.model.entity.Incident;
import com.itsm.system.model.entity.Technician;
import com.itsm.system.model.entity.User;
import com.itsm.system.model.enums.IncidentStatus;

/**
 * State Pattern interface defining valid lifecycle operations for an Incident:
 * assignTechnician (OPEN -> IN_PROGRESS), resolveAndClose (IN_PROGRESS -> CLOSED), and reopen (CLOSED -> IN_PROGRESS).
 */
public interface IncidentState {

    /**
     * Returns the incident status represented by this concrete state.
     */
    IncidentStatus getStatus();

    /**
     * Assigns a qualified technician to an OPEN incident, transitioning it to IN_PROGRESS.
     */
    void assignTechnician(Incident incident, Technician technician, User actor);

    /**
     * Resolves and closes an IN_PROGRESS incident upon completion of troubleshooting.
     */
    void resolveAndClose(Incident incident, User actor, String resolutionDetails);

    /**
     * Reopens a previously CLOSED incident back to IN_PROGRESS.
     */
    void reopen(Incident incident, User actor, String reason);
}
