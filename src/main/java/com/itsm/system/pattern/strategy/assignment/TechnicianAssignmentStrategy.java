package com.itsm.system.pattern.strategy.assignment;

import com.itsm.system.model.entity.Incident;
import com.itsm.system.model.entity.Technician;

import java.util.List;

/**
 * Strategy interface for assigning incidents to suitable technicians.
 * Allows pluggable assignment strategies (e.g. cascading, round-robin, least-loaded)
 * without affecting surrounding business logic.
 */
public interface TechnicianAssignmentStrategy {

    /**
     * Selects the most appropriate technician from a candidate list for an incident.
     *
     * @param incident             the incident to be assigned
     * @param candidateTechnicians list of potential technician candidates
     * @return the selected Technician
     */
    Technician selectTechnician(Incident incident, List<Technician> candidateTechnicians);
}
