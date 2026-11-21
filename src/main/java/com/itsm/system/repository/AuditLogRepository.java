package com.itsm.system.repository;

import com.itsm.system.model.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA Repository for AuditLog entities.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByIncidentIdOrderByTimestampDesc(Long incidentId);

    List<AuditLog> findByIncidentIncidentCodeOrderByTimestampDesc(String incidentCode);

    List<AuditLog> findByPerformedByUserCodeOrderByTimestampDesc(String userCode);
}
