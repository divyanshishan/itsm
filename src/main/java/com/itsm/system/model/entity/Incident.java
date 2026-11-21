package com.itsm.system.model.entity;

import com.itsm.system.model.enums.ImpactLevel;
import com.itsm.system.model.enums.IncidentStatus;
import com.itsm.system.model.enums.PriorityLevel;
import com.itsm.system.model.enums.SlaStatus;
import com.itsm.system.model.enums.UrgencyLevel;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Core domain aggregate root representing an IT Incident lifecycle.
 * Demonstrates Associations, Aggregation, and Composition.
 */
@Entity
@Table(name = "incidents")
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "incident_code", unique = true, nullable = false, length = 50)
    private String incidentCode;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", nullable = false, length = 2000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private Employee reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private IncidentCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "affected_service_id")
    private ItService affectedService;

    @Enumerated(EnumType.STRING)
    @Column(name = "impact", nullable = false, length = 50)
    private ImpactLevel impact;

    @Enumerated(EnumType.STRING)
    @Column(name = "urgency", nullable = false, length = 50)
    private UrgencyLevel urgency;

    @Column(name = "priority_score", nullable = false)
    private double priorityScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 50)
    private PriorityLevel priority;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private IncidentStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_technician_id")
    private Technician assignedTechnician;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applied_sla_id")
    private SlaPolicy appliedSla;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "response_deadline")
    private LocalDateTime responseDeadline;

    @Column(name = "resolution_deadline")
    private LocalDateTime resolutionDeadline;

    @Enumerated(EnumType.STRING)
    @Column(name = "sla_status", nullable = false, length = 50)
    private SlaStatus slaStatus = SlaStatus.ON_TRACK;

    @Column(name = "sla_violated", nullable = false)
    private boolean slaViolated = false;

    @Column(name = "investigation_notes", length = 3000)
    private String investigationNotes;

    @Column(name = "resolution_details", length = 3000)
    private String resolutionDetails;

    @Column(name = "customer_feedback", length = 1000)
    private String customerFeedback;

    @Column(name = "feedback_rating")
    private Integer feedbackRating;

    @Column(name = "duplicate_of_incident_code", length = 50)
    private String duplicateOfIncidentCode;

    @Column(name = "escalated", nullable = false)
    private boolean escalated = false;

    // Composition: Audit logs are managed through and owned by this Incident aggregate
    @OneToMany(mappedBy = "incident", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AuditLog> auditLogs = new ArrayList<>();

    // Default constructor for JPA
    public Incident() {
        this.createdAt = LocalDateTime.now();
        this.status = IncidentStatus.OPEN;
        this.slaStatus = SlaStatus.ON_TRACK;
        this.slaViolated = false;
        this.escalated = false;
    }

    @jakarta.persistence.PrePersist
    protected void onPrePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    // Parameterized constructor
    public Incident(String incidentCode, String title, String description, Employee reporter,
                    IncidentCategory category, ItService affectedService, ImpactLevel impact,
                    UrgencyLevel urgency, double priorityScore, PriorityLevel priority,
                    SlaPolicy appliedSla, LocalDateTime responseDeadline, LocalDateTime resolutionDeadline) {
        this();
        this.incidentCode = incidentCode;
        this.title = title;
        this.description = description;
        this.reporter = reporter;
        this.category = category;
        this.affectedService = affectedService;
        this.impact = impact;
        this.urgency = urgency;
        this.priorityScore = priorityScore;
        this.priority = priority;
        this.appliedSla = appliedSla;
        this.responseDeadline = responseDeadline;
        this.resolutionDeadline = resolutionDeadline;
    }

    // Composition helper methods
    public void addAuditLog(AuditLog auditLog) {
        if (auditLog != null) {
            this.auditLogs.add(auditLog);
            auditLog.setIncident(this);
        }
    }

    public void removeAuditLog(AuditLog auditLog) {
        if (auditLog != null) {
            this.auditLogs.remove(auditLog);
            auditLog.setIncident(null);
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIncidentCode() {
        return incidentCode;
    }

    public void setIncidentCode(String incidentCode) {
        this.incidentCode = incidentCode;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Employee getReporter() {
        return reporter;
    }

    public void setReporter(Employee reporter) {
        this.reporter = reporter;
    }

    public IncidentCategory getCategory() {
        return category;
    }

    public void setCategory(IncidentCategory category) {
        this.category = category;
    }

    public ItService getAffectedService() {
        return affectedService;
    }

    public void setAffectedService(ItService affectedService) {
        this.affectedService = affectedService;
    }

    public ImpactLevel getImpact() {
        return impact;
    }

    public void setImpact(ImpactLevel impact) {
        this.impact = impact;
    }

    public UrgencyLevel getUrgency() {
        return urgency;
    }

    public void setUrgency(UrgencyLevel urgency) {
        this.urgency = urgency;
    }

    public double getPriorityScore() {
        return priorityScore;
    }

    public void setPriorityScore(double priorityScore) {
        this.priorityScore = priorityScore;
    }

    public PriorityLevel getPriority() {
        return priority;
    }

    public void setPriority(PriorityLevel priority) {
        this.priority = priority;
    }

    public IncidentStatus getStatus() {
        return status;
    }

    public void setStatus(IncidentStatus status) {
        this.status = status;
    }

    public Technician getAssignedTechnician() {
        return assignedTechnician;
    }

    public void setAssignedTechnician(Technician assignedTechnician) {
        this.assignedTechnician = assignedTechnician;
    }

    public SlaPolicy getAppliedSla() {
        return appliedSla;
    }

    public void setAppliedSla(SlaPolicy appliedSla) {
        this.appliedSla = appliedSla;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getAcknowledgedAt() {
        return acknowledgedAt;
    }

    public void setAcknowledgedAt(LocalDateTime acknowledgedAt) {
        this.acknowledgedAt = acknowledgedAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
    }

    public LocalDateTime getResponseDeadline() {
        return responseDeadline;
    }

    public void setResponseDeadline(LocalDateTime responseDeadline) {
        this.responseDeadline = responseDeadline;
    }

    public LocalDateTime getResolutionDeadline() {
        return resolutionDeadline;
    }

    public void setResolutionDeadline(LocalDateTime resolutionDeadline) {
        this.resolutionDeadline = resolutionDeadline;
    }

    public SlaStatus getSlaStatus() {
        return slaStatus;
    }

    public void setSlaStatus(SlaStatus slaStatus) {
        this.slaStatus = slaStatus;
    }

    public boolean isSlaViolated() {
        return slaViolated;
    }

    public void setSlaViolated(boolean slaViolated) {
        this.slaViolated = slaViolated;
    }

    public String getInvestigationNotes() {
        return investigationNotes;
    }

    public void setInvestigationNotes(String investigationNotes) {
        this.investigationNotes = investigationNotes;
    }

    public String getResolutionDetails() {
        return resolutionDetails;
    }

    public void setResolutionDetails(String resolutionDetails) {
        this.resolutionDetails = resolutionDetails;
    }

    public String getCustomerFeedback() {
        return customerFeedback;
    }

    public void setCustomerFeedback(String customerFeedback) {
        this.customerFeedback = customerFeedback;
    }

    public Integer getFeedbackRating() {
        return feedbackRating;
    }

    public void setFeedbackRating(Integer feedbackRating) {
        this.feedbackRating = feedbackRating;
    }

    public String getDuplicateOfIncidentCode() {
        return duplicateOfIncidentCode;
    }

    public void setDuplicateOfIncidentCode(String duplicateOfIncidentCode) {
        this.duplicateOfIncidentCode = duplicateOfIncidentCode;
    }

    public boolean isEscalated() {
        return escalated;
    }

    public void setEscalated(boolean escalated) {
        this.escalated = escalated;
    }

    public List<AuditLog> getAuditLogs() {
        return auditLogs;
    }

    public void setAuditLogs(List<AuditLog> auditLogs) {
        this.auditLogs = auditLogs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Incident incident = (Incident) o;
        return Objects.equals(incidentCode, incident.incidentCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(incidentCode);
    }

    @Override
    public String toString() {
        return "Incident{" +
                "id=" + id +
                ", incidentCode='" + incidentCode + '\'' +
                ", title='" + title + '\'' +
                ", priority=" + priority +
                ", status=" + status +
                ", slaStatus=" + slaStatus +
                ", slaViolated=" + slaViolated +
                ", escalated=" + escalated +
                '}';
    }
}
