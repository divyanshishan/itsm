package com.itsm.system.repository;

import com.itsm.system.model.entity.Employee;
import com.itsm.system.model.entity.Incident;
import com.itsm.system.model.entity.ItService;
import com.itsm.system.model.entity.Technician;
import com.itsm.system.model.enums.IncidentStatus;
import com.itsm.system.model.enums.PriorityLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for Incident entities.
 */
@Repository
public interface IncidentRepository extends JpaRepository<Incident, Long> {

    Optional<Incident> findByIncidentCode(String incidentCode);

    List<Incident> findByReporter(Employee reporter);

    List<Incident> findByReporterId(Long reporterId);

    List<Incident> findByAssignedTechnician(Technician assignedTechnician);

    List<Incident> findByAssignedTechnicianId(Long technicianId);

    List<Incident> findByStatus(IncidentStatus status);

    List<Incident> findByStatusNot(IncidentStatus status);

    List<Incident> findByPriority(PriorityLevel priority);

    List<Incident> findByAffectedService(ItService affectedService);

    List<Incident> findByAffectedServiceIdAndStatusNot(Long serviceId, IncidentStatus status);

    @org.springframework.data.jpa.repository.Query("SELECT i FROM Incident i WHERE i.status <> com.itsm.system.model.enums.IncidentStatus.CLOSED AND (i.incidentCode = :masterCode OR i.duplicateOfIncidentCode = :masterCode)")
    List<Incident> findActiveByClusterMasterCode(@org.springframework.data.repository.query.Param("masterCode") String masterCode);

    List<Incident> findByStatusAndResolutionDetailsIsNotNull(IncidentStatus status);

    boolean existsByIncidentCode(String incidentCode);
}
