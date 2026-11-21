package com.itsm.system.dto;

/**
 * Request payload for resolving and closing an incident.
 */
public class IncidentResolveRequest {

    private String actorUserCode;
    private String resolutionDetails;

    public IncidentResolveRequest() {
    }

    public IncidentResolveRequest(String actorUserCode, String resolutionDetails) {
        this.actorUserCode = actorUserCode;
        this.resolutionDetails = resolutionDetails;
    }

    // Explicit Getters and Setters (Encapsulation - No Lombok)
    public String getActorUserCode() {
        return actorUserCode;
    }

    public void setActorUserCode(String actorUserCode) {
        this.actorUserCode = actorUserCode;
    }

    public String getResolutionDetails() {
        return resolutionDetails;
    }

    public void setResolutionDetails(String resolutionDetails) {
        this.resolutionDetails = resolutionDetails;
    }
}
