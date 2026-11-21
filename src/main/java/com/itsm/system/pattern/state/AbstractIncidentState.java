package com.itsm.system.pattern.state;

import com.itsm.system.exception.InvalidStateTransitionException;
import com.itsm.system.model.entity.Incident;
import com.itsm.system.model.entity.Technician;
import com.itsm.system.model.entity.User;

/**
 * Skeletal base implementation of IncidentState.
 * By default, all lifecycle operations throw InvalidStateTransitionException.
 * Concrete states override only the operations permitted in their respective status.
 */
public abstract class AbstractIncidentState implements IncidentState {

    @Override
    public void assignTechnician(Incident incident, Technician technician, User actor) {
        throw new InvalidStateTransitionException(
                "Invalid state transition from " + getStatus() + " for incident " + incident.getIncidentCode()
        );
    }

    @Override
    public void resolveAndClose(Incident incident, User actor, String resolutionDetails) {
        throw new InvalidStateTransitionException(
                "Invalid state transition from " + getStatus() + " for incident " + incident.getIncidentCode()
        );
    }

    @Override
    public void reopen(Incident incident, User actor, String reason) {
        throw new InvalidStateTransitionException(
                "Invalid state transition from " + getStatus() + " for incident " + incident.getIncidentCode()
        );
    }
}
