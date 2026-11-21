package com.itsm.system.dto;

/**
 * Request payload for submitting customer feedback on a closed incident.
 */
public class FeedbackRequest {

    private String reporterUserCode;
    private int rating;
    private String comments;

    public FeedbackRequest() {
    }

    public FeedbackRequest(String reporterUserCode, int rating, String comments) {
        this.reporterUserCode = reporterUserCode;
        this.rating = rating;
        this.comments = comments;
    }

    // Explicit Getters and Setters (Encapsulation - No Lombok)
    public String getReporterUserCode() {
        return reporterUserCode;
    }

    public void setReporterUserCode(String reporterUserCode) {
        this.reporterUserCode = reporterUserCode;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }
}
