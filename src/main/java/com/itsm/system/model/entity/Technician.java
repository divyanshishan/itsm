package com.itsm.system.model.entity;

import com.itsm.system.exception.TechnicianWorkloadExceededException;
import com.itsm.system.model.enums.Role;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;

import java.util.HashSet;
import java.util.Set;

/**
 * Entity representing an IT Support Technician responsible for resolving incidents.
 * In SINGLE_TABLE inheritance, subclass-specific columns must be nullable in the shared table.
 */
@Entity
@DiscriminatorValue("TECHNICIAN")
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Technician extends User {

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "technician_skills", joinColumns = @JoinColumn(name = "technician_id"))
    @Column(name = "skill", nullable = false, length = 50)
    private Set<String> skills = new HashSet<>();

    @Column(name = "experience_years")
    private Integer experienceYears = 0;

    @Column(name = "available")
    private Boolean available = true;

    @Column(name = "max_workload")
    private Integer maxWorkload = 5;

    @Column(name = "current_workload")
    private Integer currentWorkload = 0;

    // Default constructor for JPA
    public Technician() {
        super();
        this.role = Role.TECHNICIAN;
    }

    // Parameterized constructor
    public Technician(String userIdCode, String name, String email, String password, boolean active,
                      int experienceYears, boolean available, int maxWorkload) {
        super(userIdCode, name, email, password, Role.TECHNICIAN, active);
        this.experienceYears = experienceYears;
        this.available = available;
        this.maxWorkload = maxWorkload;
        this.currentWorkload = 0;
    }

    @Override
    public String getDashboardPermissions() {
        return "VIEW_ASSIGNED_INCIDENTS,UPDATE_INCIDENT_STATUS,RESOLVE_INCIDENT,INVESTIGATE_INCIDENT,UPDATE_AVAILABILITY";
    }

    // Domain Business Methods
    public boolean canAcceptIncident() {
        return isActive() && isAvailable() && (getCurrentWorkload() < getMaxWorkload());
    }

    public void incrementWorkload() {
        int max = getMaxWorkload();
        int current = getCurrentWorkload();
        if (current >= max) {
            throw new TechnicianWorkloadExceededException(
                    "Technician " + getUserIdCode() + " has reached maximum workload limit of " + max
            );
        }
        this.currentWorkload = current + 1;
    }

    public void decrementWorkload() {
        int current = getCurrentWorkload();
        if (current > 0) {
            this.currentWorkload = current - 1;
        }
    }

    public boolean hasSkill(String categoryName) {
        if (categoryName == null || skills == null) {
            return false;
        }
        return skills.stream().anyMatch(s -> s.equalsIgnoreCase(categoryName.trim()));
    }

    public void addSkill(String skill) {
        if (skill != null && !skill.trim().isEmpty()) {
            this.skills.add(skill.trim().toUpperCase());
        }
    }

    public void removeSkill(String skill) {
        if (skill != null && this.skills != null) {
            this.skills.remove(skill.trim().toUpperCase());
        }
    }

    // Getters and Setters
    public Set<String> getSkills() {
        return skills;
    }

    public void setSkills(Set<String> skills) {
        this.skills = skills;
    }

    public int getExperienceYears() {
        return experienceYears != null ? experienceYears : 0;
    }

    public void setExperienceYears(int experienceYears) {
        this.experienceYears = experienceYears;
    }

    public boolean isAvailable() {
        return available != null ? available : true;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public int getMaxWorkload() {
        return maxWorkload != null ? maxWorkload : 5;
    }

    public void setMaxWorkload(int maxWorkload) {
        this.maxWorkload = maxWorkload;
    }

    public int getCurrentWorkload() {
        return currentWorkload != null ? currentWorkload : 0;
    }

    public void setCurrentWorkload(int currentWorkload) {
        this.currentWorkload = currentWorkload;
    }

    @Override
    public String toString() {
        return "Technician{" +
                "id=" + id +
                ", userIdCode='" + userIdCode + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", experienceYears=" + getExperienceYears() +
                ", available=" + isAvailable() +
                ", maxWorkload=" + getMaxWorkload() +
                ", currentWorkload=" + getCurrentWorkload() +
                ", skills=" + skills +
                ", active=" + active +
                '}';
    }
}
