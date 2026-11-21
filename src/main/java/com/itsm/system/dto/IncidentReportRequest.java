package com.itsm.system.dto;

import com.itsm.system.model.enums.ImpactLevel;
import com.itsm.system.model.enums.UrgencyLevel;

/**
 * Request payload for reporting an incident.
 * Impact and Urgency are optional and auto-classified by the backend if omitted.
 */
public class IncidentReportRequest {

    private String title;
    private String description;
    private Long reporterId;
    private String reporterIdCode;
    private Long serviceId;
    private String serviceCode;
    private Long categoryId;
    private String categoryCode;
    private ImpactLevel impact;
    private UrgencyLevel urgency;

    public IncidentReportRequest() {
    }

    public IncidentReportRequest(String title, String description, Long reporterId, Long serviceId,
                                 Long categoryId, ImpactLevel impact, UrgencyLevel urgency) {
        this.title = title;
        this.description = description;
        this.reporterId = reporterId;
        this.serviceId = serviceId;
        this.categoryId = categoryId;
        this.impact = impact;
        this.urgency = urgency;
    }

    public IncidentReportRequest(String title, String description, String reporterIdCode, String serviceCode,
                                 String categoryCode, ImpactLevel impact, UrgencyLevel urgency) {
        this.title = title;
        this.description = description;
        this.reporterIdCode = reporterIdCode;
        this.serviceCode = serviceCode;
        this.categoryCode = categoryCode;
        this.impact = impact;
        this.urgency = urgency;
    }

    // Explicit Getters and Setters (Encapsulation - No Lombok)
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

    public Long getReporterId() {
        return reporterId;
    }

    public void setReporterId(Long reporterId) {
        this.reporterId = reporterId;
    }

    public String getReporterIdCode() {
        return reporterIdCode;
    }

    public void setReporterIdCode(String reporterIdCode) {
        this.reporterIdCode = reporterIdCode;
    }

    public Long getServiceId() {
        return serviceId;
    }

    public void setServiceId(Long serviceId) {
        this.serviceId = serviceId;
    }

    public String getServiceCode() {
        return serviceCode;
    }

    public void setServiceCode(String serviceCode) {
        this.serviceCode = serviceCode;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryCode() {
        return categoryCode;
    }

    public void setCategoryCode(String categoryCode) {
        this.categoryCode = categoryCode;
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
}
