package com.itsm.system.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Entity capturing immutable chronological audit events for an incident lifecycle.
 * Represents a composition relationship with Incident.
 */
@Entity
@Table(name = "audit_logs")
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "performed_by_user_code", nullable = false, length = 50)
    private String performedByUserCode;

    @Column(name = "action", nullable = false, length = 100)
    private String action;

    @Column(name = "previous_state", length = 50)
    private String previousState;

    @Column(name = "new_state", length = 50)
    private String newState;

    @Column(name = "description", length = 1000)
    private String description;

    // Default constructor for JPA
    public AuditLog() {
        this.timestamp = LocalDateTime.now();
    }

    // Parameterized constructor
    public AuditLog(Incident incident, LocalDateTime timestamp, String performedByUserCode,
                    String action, String previousState, String newState, String description) {
        this.incident = incident;
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
        this.performedByUserCode = performedByUserCode;
        this.action = action;
        this.previousState = previousState;
        this.newState = newState;
        this.description = description;
    }

    // Helper constructor without incident (when attaching to Incident via incident.addAuditLog)
    public AuditLog(String performedByUserCode, String action, String previousState,
                    String newState, String description) {
        this.timestamp = LocalDateTime.now();
        this.performedByUserCode = performedByUserCode;
        this.action = action;
        this.previousState = previousState;
        this.newState = newState;
        this.description = description;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Incident getIncident() {
        return incident;
    }

    public void setIncident(Incident incident) {
        this.incident = incident;
    }

    public String getIncidentCode() {
        return incident != null ? incident.getIncidentCode() : null;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getPerformedByUserCode() {
        return performedByUserCode;
    }

    public void setPerformedByUserCode(String performedByUserCode) {
        this.performedByUserCode = performedByUserCode;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getPreviousState() {
        return previousState;
    }

    public void setPreviousState(String previousState) {
        this.previousState = previousState;
    }

    public String getNewState() {
        return newState;
    }

    public void setNewState(String newState) {
        this.newState = newState;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "AuditLog{" +
                "id=" + id +
                ", timestamp=" + timestamp +
                ", performedByUserCode='" + performedByUserCode + '\'' +
                ", action='" + action + '\'' +
                ", previousState='" + previousState + '\'' +
                ", newState='" + newState + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
