package com.itsm.system.model.entity;

import com.itsm.system.model.enums.NotificationEventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entity representing a persistent notification record dispatched through a notification channel.
 * Stored for in-app dashboard views, audit trails, and simulated email inspection.
 */
@Entity
@Table(name = "notifications")
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class NotificationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recipient_user_code", nullable = false, length = 50)
    private String recipientUserCode;

    @Column(name = "recipient_email", length = 120)
    private String recipientEmail;

    @Column(name = "incident_code", length = 50)
    private String incidentCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private NotificationEventType eventType;

    @Column(name = "channel_type", nullable = false, length = 50)
    private String channelType;

    @Column(name = "subject", nullable = false, length = 255)
    private String subject;

    @Column(name = "message", nullable = false, length = 2000)
    private String message;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "read_status", nullable = false)
    private boolean readStatus = false;

    // Default constructor for JPA
    public NotificationRecord() {
        this.createdAt = LocalDateTime.now();
        this.readStatus = false;
    }

    // Parameterized constructor
    public NotificationRecord(String recipientUserCode,
                              String recipientEmail,
                              String incidentCode,
                              NotificationEventType eventType,
                              String channelType,
                              String subject,
                              String message) {
        this();
        this.recipientUserCode = recipientUserCode;
        this.recipientEmail = recipientEmail;
        this.incidentCode = incidentCode;
        this.eventType = eventType;
        this.channelType = channelType;
        this.subject = subject;
        this.message = message;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRecipientUserCode() {
        return recipientUserCode;
    }

    public void setRecipientUserCode(String recipientUserCode) {
        this.recipientUserCode = recipientUserCode;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public void setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }

    public String getIncidentCode() {
        return incidentCode;
    }

    public void setIncidentCode(String incidentCode) {
        this.incidentCode = incidentCode;
    }

    public NotificationEventType getEventType() {
        return eventType;
    }

    public void setEventType(NotificationEventType eventType) {
        this.eventType = eventType;
    }

    public String getChannelType() {
        return channelType;
    }

    public void setChannelType(String channelType) {
        this.channelType = channelType;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isReadStatus() {
        return readStatus;
    }

    public void setReadStatus(boolean readStatus) {
        this.readStatus = readStatus;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NotificationRecord that = (NotificationRecord) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "NotificationRecord{" +
                "id=" + id +
                ", recipientUserCode='" + recipientUserCode + '\'' +
                ", recipientEmail='" + recipientEmail + '\'' +
                ", incidentCode='" + incidentCode + '\'' +
                ", eventType=" + eventType +
                ", channelType='" + channelType + '\'' +
                ", subject='" + subject + '\'' +
                ", createdAt=" + createdAt +
                ", readStatus=" + readStatus +
                '}';
    }
}
