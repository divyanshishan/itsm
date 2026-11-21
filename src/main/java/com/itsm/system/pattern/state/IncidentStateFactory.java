package com.itsm.system.pattern.state;

import com.itsm.system.model.entity.Incident;
import com.itsm.system.model.enums.IncidentStatus;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

/**
 * Factory component that resolves the appropriate IncidentState implementation for a given status.
 * Maps the 3 lifecycle states: OPEN -> OpenState, IN_PROGRESS -> InProgressState, CLOSED -> ClosedState.
 */
@Component
public class IncidentStateFactory {

    private final Map<IncidentStatus, IncidentState> stateMap = new EnumMap<>(IncidentStatus.class);

    public IncidentStateFactory() {
        stateMap.put(IncidentStatus.OPEN, new OpenState());
        stateMap.put(IncidentStatus.IN_PROGRESS, new InProgressState());
        stateMap.put(IncidentStatus.CLOSED, new ClosedState());
    }

    /**
     * Resolves the state instance for a given Incident entity.
     */
    public IncidentState getState(Incident incident) {
        if (incident == null || incident.getStatus() == null) {
            throw new IllegalArgumentException("Incident or Incident status cannot be null.");
        }
        return getState(incident.getStatus());
    }

    /**
     * Resolves the state instance for a given IncidentStatus enum value.
     */
    public IncidentState getState(IncidentStatus status) {
        IncidentState state = stateMap.get(status);
        if (state == null) {
            throw new IllegalArgumentException("No state implementation registered for status: " + status);
        }
        return state;
    }
}
