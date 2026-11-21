package com.itsm.system.dto;

/**
 * Request payload for appending an investigation note to an incident.
 */
public class IncidentNoteRequest {

    private String actorUserCode;
    private String note;

    public IncidentNoteRequest() {
    }

    public IncidentNoteRequest(String actorUserCode, String note) {
        this.actorUserCode = actorUserCode;
        this.note = note;
    }

    // Explicit Getters and Setters (Encapsulation - No Lombok)
    public String getActorUserCode() {
        return actorUserCode;
    }

    public void setActorUserCode(String actorUserCode) {
        this.actorUserCode = actorUserCode;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
