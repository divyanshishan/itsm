package com.itsm.system.controller;

import com.itsm.system.dto.ApiResponse;
import com.itsm.system.model.entity.AuditLog;
import com.itsm.system.model.entity.IncidentCategory;
import com.itsm.system.model.entity.ItService;
import com.itsm.system.model.entity.Technician;
import com.itsm.system.repository.AuditLogRepository;
import com.itsm.system.repository.IncidentCategoryRepository;
import com.itsm.system.repository.ItServiceRepository;
import com.itsm.system.repository.TechnicianRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST Controller providing read-only master data lookup for services, categories, technicians, and audit trail logs.
 */
@RestController
@RequestMapping("/api/lookup")
public class SystemLookupRestController {

    private final ItServiceRepository itServiceRepository;
    private final IncidentCategoryRepository incidentCategoryRepository;
    private final TechnicianRepository technicianRepository;
    private final AuditLogRepository auditLogRepository;

    public SystemLookupRestController(ItServiceRepository itServiceRepository,
                                    IncidentCategoryRepository incidentCategoryRepository,
                                    TechnicianRepository technicianRepository,
                                    AuditLogRepository auditLogRepository) {
        this.itServiceRepository = itServiceRepository;
        this.incidentCategoryRepository = incidentCategoryRepository;
        this.technicianRepository = technicianRepository;
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Retrieves all configured IT business and technical services.
     */
    @GetMapping("/services")
    public ApiResponse<List<ItService>> getAllServices() {
        List<ItService> services = itServiceRepository.findAll();
        return ApiResponse.ok("IT services retrieved successfully", services);
    }

    /**
     * Retrieves all incident classification categories.
     */
    @GetMapping("/categories")
    public ApiResponse<List<IncidentCategory>> getAllCategories() {
        List<IncidentCategory> categories = incidentCategoryRepository.findAll();
        return ApiResponse.ok("Incident categories retrieved successfully", categories);
    }

    /**
     * Retrieves all IT support technicians along with their skills, experience, and workload.
     */
    @GetMapping("/technicians")
    public ApiResponse<List<Technician>> getAllTechnicians() {
        List<Technician> technicians = technicianRepository.findAll();
        return ApiResponse.ok("Technicians retrieved successfully", technicians);
    }

    /**
     * Retrieves audit trail logs, optionally filtered by incident code.
     */
    @GetMapping("/audit-logs")
    public ApiResponse<List<AuditLog>> getAuditLogs(@RequestParam(required = false) String incidentCode) {
        List<AuditLog> logs;
        if (incidentCode != null && !incidentCode.trim().isEmpty()) {
            logs = auditLogRepository.findByIncidentIncidentCodeOrderByTimestampDesc(incidentCode.trim());
        } else {
            logs = auditLogRepository.findAll().stream()
                    .sorted((a, b) -> {
                        if (a.getTimestamp() == null || b.getTimestamp() == null) return 0;
                        return b.getTimestamp().compareTo(a.getTimestamp());
                    })
                    .toList();
        }
        return ApiResponse.ok("Audit logs retrieved successfully", logs);
    }
}
