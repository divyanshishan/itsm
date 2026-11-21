package com.itsm.system.dto;

import java.time.LocalDateTime;

/**
 * Data Transfer Object representing a suggested historical resolution (Feature G).
 * Contains the closed incident's details, resolution notes, and computed relevance score.
 */
public class SuggestedResolutionDTO {

    private String historicalIncidentCode;
    private String historicalTitle;
    private String serviceName;
    private String categoryName;
    private double similarityScore; // 0.0 to 100.0 percentage
    private String resolutionDetails;
    private String resolvedByTechnicianCode;
    private LocalDateTime closedAt;

    public SuggestedResolutionDTO() {
    }

    public SuggestedResolutionDTO(String historicalIncidentCode,
                                  String historicalTitle,
                                  String serviceName,
                                  String categoryName,
                                  double similarityScore,
                                  String resolutionDetails,
                                  String resolvedByTechnicianCode,
                                  LocalDateTime closedAt) {
        this.historicalIncidentCode = historicalIncidentCode;
        this.historicalTitle = historicalTitle;
        this.serviceName = serviceName;
        this.categoryName = categoryName;
        this.similarityScore = similarityScore;
        this.resolutionDetails = resolutionDetails;
        this.resolvedByTechnicianCode = resolvedByTechnicianCode;
        this.closedAt = closedAt;
    }

    public String getHistoricalIncidentCode() {
        return historicalIncidentCode;
    }

    public void setHistoricalIncidentCode(String historicalIncidentCode) {
        this.historicalIncidentCode = historicalIncidentCode;
    }

    public String getHistoricalTitle() {
        return historicalTitle;
    }

    public void setHistoricalTitle(String historicalTitle) {
        this.historicalTitle = historicalTitle;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public double getSimilarityScore() {
        return similarityScore;
    }

    public void setSimilarityScore(double similarityScore) {
        this.similarityScore = similarityScore;
    }

    public String getResolutionDetails() {
        return resolutionDetails;
    }

    public void setResolutionDetails(String resolutionDetails) {
        this.resolutionDetails = resolutionDetails;
    }

    public String getResolvedByTechnicianCode() {
        return resolvedByTechnicianCode;
    }

    public void setResolvedByTechnicianCode(String resolvedByTechnicianCode) {
        this.resolvedByTechnicianCode = resolvedByTechnicianCode;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
    }

    @Override
    public String toString() {
        return "SuggestedResolutionDTO{" +
                "historicalIncidentCode='" + historicalIncidentCode + '\'' +
                ", historicalTitle='" + historicalTitle + '\'' +
                ", serviceName='" + serviceName + '\'' +
                ", categoryName='" + categoryName + '\'' +
                ", similarityScore=" + String.format("%.1f%%", similarityScore) +
                ", resolutionDetails='" + resolutionDetails + '\'' +
                ", resolvedByTechnicianCode='" + resolvedByTechnicianCode + '\'' +
                ", closedAt=" + closedAt +
                '}';
    }
}
