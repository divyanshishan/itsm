package com.itsm.system.model.entity;

import com.itsm.system.model.enums.BusinessCriticality;
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
import jakarta.persistence.Table;

import java.util.Objects;

/**
 * Entity representing an organizational IT Service and its criticality baseline.
 */
@Entity
@Table(name = "it_services")
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ItService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "service_code", unique = true, nullable = false, length = 50)
    private String serviceCode;

    @Column(name = "service_name", nullable = false, length = 150)
    private String serviceName;

    @Column(name = "description", length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "criticality", nullable = false, length = 50)
    private BusinessCriticality criticality;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "default_category_id")
    private IncidentCategory defaultCategory;

    @Column(name = "operational_status", nullable = false, length = 50)
    private String operationalStatus = "OPERATIONAL";

    // Default constructor for JPA
    public ItService() {
    }

    // Parameterized constructor
    public ItService(String serviceCode, String serviceName, String description,
                     BusinessCriticality criticality, IncidentCategory defaultCategory,
                     String operationalStatus) {
        this.serviceCode = serviceCode;
        this.serviceName = serviceName;
        this.description = description;
        this.criticality = criticality;
        this.defaultCategory = defaultCategory;
        this.operationalStatus = operationalStatus != null ? operationalStatus : "OPERATIONAL";
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getServiceCode() {
        return serviceCode;
    }

    public void setServiceCode(String serviceCode) {
        this.serviceCode = serviceCode;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BusinessCriticality getCriticality() {
        return criticality;
    }

    public void setCriticality(BusinessCriticality criticality) {
        this.criticality = criticality;
    }

    public IncidentCategory getDefaultCategory() {
        return defaultCategory;
    }

    public void setDefaultCategory(IncidentCategory defaultCategory) {
        this.defaultCategory = defaultCategory;
    }

    public String getOperationalStatus() {
        return operationalStatus;
    }

    public void setOperationalStatus(String operationalStatus) {
        this.operationalStatus = operationalStatus;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ItService itService = (ItService) o;
        return Objects.equals(serviceCode, itService.serviceCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceCode);
    }

    @Override
    public String toString() {
        return "ItService{" +
                "id=" + id +
                ", serviceCode='" + serviceCode + '\'' +
                ", serviceName='" + serviceName + '\'' +
                ", criticality=" + criticality +
                ", operationalStatus='" + operationalStatus + '\'' +
                '}';
    }
}
