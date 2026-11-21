package com.itsm.system.model.entity;

import com.itsm.system.model.enums.PriorityLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;

/**
 * Entity representing Service Level Agreement (SLA) target thresholds per priority level.
 */
@Entity
@Table(name = "sla_policies")
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class SlaPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority_level", unique = true, nullable = false, length = 50)
    private PriorityLevel priorityLevel;

    @Column(name = "response_target_minutes", nullable = false)
    private int responseTargetMinutes;

    @Column(name = "resolution_target_minutes", nullable = false)
    private int resolutionTargetMinutes;

    @Column(name = "description", length = 500)
    private String description;

    // Default constructor for JPA
    public SlaPolicy() {
    }

    // Parameterized constructor
    public SlaPolicy(PriorityLevel priorityLevel, int responseTargetMinutes,
                     int resolutionTargetMinutes, String description) {
        this.priorityLevel = priorityLevel;
        this.responseTargetMinutes = responseTargetMinutes;
        this.resolutionTargetMinutes = resolutionTargetMinutes;
        this.description = description;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PriorityLevel getPriorityLevel() {
        return priorityLevel;
    }

    public void setPriorityLevel(PriorityLevel priorityLevel) {
        this.priorityLevel = priorityLevel;
    }

    public int getResponseTargetMinutes() {
        return responseTargetMinutes;
    }

    public void setResponseTargetMinutes(int responseTargetMinutes) {
        this.responseTargetMinutes = responseTargetMinutes;
    }

    public int getResolutionTargetMinutes() {
        return resolutionTargetMinutes;
    }

    public void setResolutionTargetMinutes(int resolutionTargetMinutes) {
        this.resolutionTargetMinutes = resolutionTargetMinutes;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SlaPolicy slaPolicy = (SlaPolicy) o;
        return priorityLevel == slaPolicy.priorityLevel;
    }

    @Override
    public int hashCode() {
        return Objects.hash(priorityLevel);
    }

    @Override
    public String toString() {
        return "SlaPolicy{" +
                "id=" + id +
                ", priorityLevel=" + priorityLevel +
                ", responseTargetMinutes=" + responseTargetMinutes +
                ", resolutionTargetMinutes=" + resolutionTargetMinutes +
                ", description='" + description + '\'' +
                '}';
    }
}
