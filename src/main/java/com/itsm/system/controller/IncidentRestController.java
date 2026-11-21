package com.itsm.system.controller;

import com.itsm.system.dto.ApiResponse;
import com.itsm.system.dto.FeedbackRequest;
import com.itsm.system.dto.IncidentNoteRequest;
import com.itsm.system.dto.IncidentReportRequest;
import com.itsm.system.dto.IncidentResolveRequest;
import com.itsm.system.exception.InvalidIncidentException;
import com.itsm.system.exception.InvalidUserInputException;
import com.itsm.system.exception.ServiceNotFoundException;
import com.itsm.system.model.entity.Employee;
import com.itsm.system.model.entity.Incident;
import com.itsm.system.model.entity.ItService;
import com.itsm.system.model.entity.User;
import com.itsm.system.model.enums.IncidentStatus;
import com.itsm.system.model.enums.PriorityLevel;
import com.itsm.system.repository.EmployeeRepository;
import com.itsm.system.repository.IncidentCategoryRepository;
import com.itsm.system.repository.IncidentRepository;
import com.itsm.system.repository.ItServiceRepository;
import com.itsm.system.repository.UserRepository;
import com.itsm.system.service.IncidentLifecycleService;
import com.itsm.system.service.IncidentManagementService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST Controller managing the Incident lifecycle, submission pipeline, and audit tracking.
 */
@RestController
@RequestMapping("/api/incidents")
public class IncidentRestController {

    private final IncidentManagementService incidentManagementService;
    private final IncidentLifecycleService incidentLifecycleService;
    private final IncidentRepository incidentRepository;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final ItServiceRepository itServiceRepository;
    private final IncidentCategoryRepository incidentCategoryRepository;

    public IncidentRestController(IncidentManagementService incidentManagementService,
                                  IncidentLifecycleService incidentLifecycleService,
                                  IncidentRepository incidentRepository,
                                  UserRepository userRepository,
                                  EmployeeRepository employeeRepository,
                                  ItServiceRepository itServiceRepository,
                                  IncidentCategoryRepository incidentCategoryRepository) {
        this.incidentManagementService = incidentManagementService;
        this.incidentLifecycleService = incidentLifecycleService;
        this.incidentRepository = incidentRepository;
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
        this.itServiceRepository = itServiceRepository;
        this.incidentCategoryRepository = incidentCategoryRepository;
    }

    /**
     * Reports an incident and automatically transitions it to IN_PROGRESS (5B Choice 1)
     * through either duplicate co-assignment or intelligent strategy-based technician selection.
     */
    @PostMapping("/report")
    public ApiResponse<Incident> reportIncident(@RequestBody IncidentReportRequest request) {
        if (request == null) {
            throw new InvalidUserInputException("Incident report request body cannot be null.");
        }

        Long reporterId = request.getReporterId();
        if (reporterId == null && request.getReporterIdCode() != null && !request.getReporterIdCode().trim().isEmpty()) {
            Employee emp = employeeRepository.findByUserIdCode(request.getReporterIdCode().trim())
                    .orElseThrow(() -> new InvalidUserInputException("Reporter not found with code: " + request.getReporterIdCode()));
            reporterId = emp.getId();
        }

        Long serviceId = request.getServiceId();
        if (serviceId == null && request.getServiceCode() != null && !request.getServiceCode().trim().isEmpty()) {
            ItService svc = itServiceRepository.findByServiceCode(request.getServiceCode().trim())
                    .orElseThrow(() -> new ServiceNotFoundException("Service not found with code: " + request.getServiceCode()));
            serviceId = svc.getId();
        }

        Long categoryId = request.getCategoryId();
        if (categoryId == null && request.getCategoryCode() != null && !request.getCategoryCode().trim().isEmpty()) {
            incidentCategoryRepository.findByNameIgnoreCase(request.getCategoryCode().trim())
                    .ifPresent(cat -> request.setCategoryId(cat.getId()));
            categoryId = request.getCategoryId();
        }

        Incident incident = incidentManagementService.reportAndAutoAssignIncident(
                request.getTitle(),
                request.getDescription(),
                reporterId,
                serviceId,
                categoryId,
                request.getImpact(),
                request.getUrgency()
        );

        return ApiResponse.ok("Incident reported and assigned successfully", incident);
    }

    /**
     * Retrieves all incidents matching optional filtering criteria, sorted by createdAt descending.
     */
    @GetMapping
    public ApiResponse<List<Incident>> getIncidents(
            @RequestParam(required = false) IncidentStatus status,
            @RequestParam(required = false) PriorityLevel priority,
            @RequestParam(required = false) Long serviceId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long technicianId,
            @RequestParam(required = false) Long reporterId,
            @RequestParam(required = false) String technicianCode,
            @RequestParam(required = false) String reporterCode
    ) {
        List<Incident> allIncidents = incidentRepository.findAll();
        List<Incident> filtered = allIncidents.stream()
                .filter(inc -> status == null || inc.getStatus() == status)
                .filter(inc -> priority == null || inc.getPriority() == priority)
                .filter(inc -> serviceId == null || (inc.getAffectedService() != null && inc.getAffectedService().getId().equals(serviceId)))
                .filter(inc -> categoryId == null || (inc.getCategory() != null && inc.getCategory().getId().equals(categoryId)))
                .filter(inc -> technicianId == null || (inc.getAssignedTechnician() != null && inc.getAssignedTechnician().getId().equals(technicianId)))
                .filter(inc -> reporterId == null || (inc.getReporter() != null && inc.getReporter().getId().equals(reporterId)))
                .filter(inc -> technicianCode == null || (inc.getAssignedTechnician() != null && inc.getAssignedTechnician().getUserIdCode().equalsIgnoreCase(technicianCode.trim())))
                .filter(inc -> reporterCode == null || (inc.getReporter() != null && inc.getReporter().getUserIdCode().equalsIgnoreCase(reporterCode.trim())))
                .sorted((a, b) -> {
                    if (a.getCreatedAt() == null || b.getCreatedAt() == null) return 0;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .toList();

        return ApiResponse.ok("Incidents retrieved successfully", filtered);
    }

    /**
     * Retrieves full details of a specific incident by ticket code.
     */
    @GetMapping("/{incidentCode}")
    public ApiResponse<Incident> getIncidentByCode(@PathVariable String incidentCode) {
        if (incidentCode == null || incidentCode.trim().isEmpty()) {
            throw new InvalidIncidentException("Incident code cannot be blank.");
        }

        Incident incident = incidentRepository.findByIncidentCode(incidentCode.trim())
                .orElseThrow(() -> new InvalidIncidentException("Incident not found with code: " + incidentCode));

        return ApiResponse.ok("Incident details retrieved successfully", incident);
    }

    /**
     * Appends an investigation note to an active IN_PROGRESS incident.
     */
    @PostMapping("/{incidentCode}/notes")
    public ApiResponse<Incident> addInvestigationNote(
            @PathVariable String incidentCode,
            @RequestBody IncidentNoteRequest request
    ) {
        if (request == null || request.getActorUserCode() == null) {
            throw new InvalidUserInputException("Actor user code and note are required.");
        }

        User actor = userRepository.findByUserIdCode(request.getActorUserCode().trim())
                .orElseThrow(() -> new InvalidUserInputException("Actor user not found with code: " + request.getActorUserCode()));

        Incident updated = incidentLifecycleService.addInvestigationNote(incidentCode, actor, request.getNote());
        return ApiResponse.ok("Investigation note added successfully", updated);
    }

    /**
     * Resolves and closes an IN_PROGRESS incident, cascading closure to all active duplicate tickets.
     */
    @PostMapping("/{incidentCode}/resolve")
    public ApiResponse<Incident> resolveIncident(
            @PathVariable String incidentCode,
            @RequestBody IncidentResolveRequest request
    ) {
        if (request == null || request.getActorUserCode() == null) {
            throw new InvalidUserInputException("Actor user code and resolution details are required.");
        }

        User actor = userRepository.findByUserIdCode(request.getActorUserCode().trim())
                .orElseThrow(() -> new InvalidUserInputException("Actor user not found with code: " + request.getActorUserCode()));

        Incident resolved = incidentLifecycleService.resolveAndCloseIncident(incidentCode, actor, request.getResolutionDetails());
        return ApiResponse.ok("Incident resolved and closed successfully", resolved);
    }

    /**
     * Reopens a previously CLOSED incident back to IN_PROGRESS.
     */
    @PostMapping("/{incidentCode}/reopen")
    public ApiResponse<Incident> reopenIncident(
            @PathVariable String incidentCode,
            @RequestBody IncidentNoteRequest request
    ) {
        if (request == null || request.getActorUserCode() == null) {
            throw new InvalidUserInputException("Actor user code and reason are required.");
        }

        User actor = userRepository.findByUserIdCode(request.getActorUserCode().trim())
                .orElseThrow(() -> new InvalidUserInputException("Actor user not found with code: " + request.getActorUserCode()));

        Incident reopened = incidentLifecycleService.reopenIncident(incidentCode, actor, request.getNote());
        return ApiResponse.ok("Incident reopened successfully", reopened);
    }

    /**
     * Submits customer satisfaction rating and comments on a CLOSED incident.
     */
    @PostMapping("/{incidentCode}/feedback")
    public ApiResponse<Incident> submitCustomerFeedback(
            @PathVariable String incidentCode,
            @RequestBody FeedbackRequest request
    ) {
        if (request == null || request.getReporterUserCode() == null) {
            throw new InvalidUserInputException("Reporter user code and rating are required.");
        }

        Employee reporter = employeeRepository.findByUserIdCode(request.getReporterUserCode().trim())
                .orElseThrow(() -> new InvalidUserInputException("Reporter employee not found with code: " + request.getReporterUserCode()));

        Incident updated = incidentLifecycleService.submitCustomerFeedback(
                incidentCode, reporter, request.getComments(), request.getRating()
        );
        return ApiResponse.ok("Customer feedback submitted successfully", updated);
    }
}
