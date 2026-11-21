package com.itsm.system.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;

/**
 * Entity representing an incident classification category.
 * Can be dynamically configured at runtime by Administrators.
 */
@Entity
@Table(name = "incident_categories")
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class IncidentCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", unique = true, nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "default_keywords", length = 1000)
    private String defaultKeywords;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    // Default constructor for JPA
    public IncidentCategory() {
    }

    // Parameterized constructor
    public IncidentCategory(String name, String description, String defaultKeywords, boolean active) {
        this.name = name;
        this.description = description;
        this.defaultKeywords = defaultKeywords;
        this.active = active;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDefaultKeywords() {
        return defaultKeywords;
    }

    public void setDefaultKeywords(String defaultKeywords) {
        this.defaultKeywords = defaultKeywords;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IncidentCategory that = (IncidentCategory) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "IncidentCategory{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", defaultKeywords='" + defaultKeywords + '\'' +
                ", active=" + active +
                '}';
    }
}
