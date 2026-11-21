package com.itsm.system.controller;

import com.itsm.system.dto.ApiResponse;
import com.itsm.system.dto.SuggestedResolutionDTO;
import com.itsm.system.exception.InvalidIncidentException;
import com.itsm.system.model.entity.Incident;
import com.itsm.system.model.entity.User;
import com.itsm.system.model.enums.IncidentStatus;
import com.itsm.system.model.enums.Role;
import com.itsm.system.repository.IncidentRepository;
import com.itsm.system.repository.UserRepository;
import com.itsm.system.service.IncidentLifecycleService;
import com.itsm.system.service.intelligent.ResolutionRecommendationService;
import com.itsm.system.service.intelligent.SlaMonitoringService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST Controller for Intelligent AI-driven features:
 * Historical Resolution Recommendations (Feature G) and SLA Deadline Monitoring (Feature 4C).
 */
@RestController
@RequestMapping("/api/intelligence")
public class IntelligenceRestController {

    private final ResolutionRecommendationService resolutionRecommendationService;
    private final IncidentLifecycleService incidentLifecycleService;
    private final SlaMonitoringService slaMonitoringService;
    private final IncidentRepository incidentRepository;
    private final UserRepository userRepository;

    public IntelligenceRestController(ResolutionRecommendationService resolutionRecommendationService,
                                    IncidentLifecycleService incidentLifecycleService,
                                    SlaMonitoringService slaMonitoringService,
                                    IncidentRepository incidentRepository,
                                    UserRepository userRepository) {
        this.resolutionRecommendationService = resolutionRecommendationService;
        this.incidentLifecycleService = incidentLifecycleService;
        this.slaMonitoringService = slaMonitoringService;
        this.incidentRepository = incidentRepository;
        this.userRepository = userRepository;
    }

    /**
     * Intelligent Feature G: Returns ALL closed historical resolutions ranked in descending order of similarity score.
     */
    @GetMapping("/incidents/{incidentCode}/suggested-resolutions")
    public ApiResponse<List<SuggestedResolutionDTO>> getSuggestedResolutions(@PathVariable String incidentCode) {
        Incident incident = incidentRepository.findByIncidentCode(incidentCode.trim())
                .orElseThrow(() -> new InvalidIncidentException("Incident not found with code: " + incidentCode));

        List<SuggestedResolutionDTO> recommendations = resolutionRecommendationService.getAllAvailableHistoricalSolutions(incident);
        return ApiResponse.ok("Historical resolutions retrieved successfully", recommendations);
    }

    /**
     * Intelligent Feature G: Applies a chosen historical incident's resolution to the target active incident and closes it.
     */
    @PostMapping("/incidents/{incidentCode}/apply-historical-resolution/{historicalCode}")
    public ApiResponse<Incident> applyHistoricalResolution(
            @PathVariable String incidentCode,
            @PathVariable String historicalCode,
            @RequestParam(required = false) String actorUserCode
    ) {
        Incident incident = incidentRepository.findByIncidentCode(incidentCode.trim())
                .orElseThrow(() -> new InvalidIncidentException("Active incident not found with code: " + incidentCode));

        User actor = null;
        if (actorUserCode != null && !actorUserCode.trim().isEmpty()) {
            actor = userRepository.findByUserIdCode(actorUserCode.trim()).orElse(null);
        }
        if (actor == null) {
            actor = incident.getAssignedTechnician();
        }
        if (actor == null) {
            actor = userRepository.findByRole(Role.ADMINISTRATOR).stream().findFirst().orElse(null);
        }

        Incident closed = incidentLifecycleService.applyHistoricalResolutionAndClose(incidentCode, historicalCode, actor);
        return ApiResponse.ok("Historical resolution applied and incident closed successfully", closed);
    }

    /**
     * Continuous SLA Monitoring: Manually triggers immediate SLA check and returns all active incidents with updated statuses.
     */
    @PostMapping("/sla/check-now")
    public ApiResponse<List<Incident>> runSlaCheckNow() {
        slaMonitoringService.runSlaCheckNow();
        List<Incident> activeIncidents = incidentRepository.findByStatusNot(IncidentStatus.CLOSED);
        return ApiResponse.ok("SLA evaluation completed successfully", activeIncidents);
    }
}
