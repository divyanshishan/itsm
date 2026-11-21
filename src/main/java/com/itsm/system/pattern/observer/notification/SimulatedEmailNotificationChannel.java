package com.itsm.system.pattern.observer.notification;

import com.itsm.system.model.entity.Incident;
import com.itsm.system.model.entity.NotificationRecord;
import com.itsm.system.model.entity.User;
import com.itsm.system.model.enums.NotificationEventType;
import com.itsm.system.repository.NotificationRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Simulated Email notification channel.
 * Formats standard RFC-style email headers/body, persists records for auditing/inspection,
 * and outputs formatted email blocks to application logs.
 */
@Component
public class SimulatedEmailNotificationChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(SimulatedEmailNotificationChannel.class);
    private static final String CHANNEL_NAME = "SIMULATED_EMAIL";
    private static final String SENDER_EMAIL = "noreply@i-itsm.org";

    private final NotificationRecordRepository notificationRecordRepository;

    public SimulatedEmailNotificationChannel(NotificationRecordRepository notificationRecordRepository) {
        this.notificationRecordRepository = notificationRecordRepository;
    }

    @Override
    public String getChannelName() {
        return CHANNEL_NAME;
    }

    @Override
    public void sendNotification(User recipient, Incident incident, NotificationEventType eventType, String subject, String message) {
        String recipientCode = (recipient != null) ? recipient.getUserIdCode() : "SYSTEM";
        String recipientEmail = (recipient != null && recipient.getEmail() != null) ? recipient.getEmail() : "user@i-itsm.org";
        String recipientName = (recipient != null) ? recipient.getName() : "ITSM User";
        String incidentCode = (incident != null) ? incident.getIncidentCode() : "N/A";
        String priority = (incident != null && incident.getPriority() != null) ? incident.getPriority().name() : "N/A";

        String formattedSubject = String.format("[I-ITSM %s] %s", incidentCode, subject);

        // Build comprehensive email text
        StringBuilder emailBody = new StringBuilder();
        emailBody.append("Dear ").append(recipientName).append(",\n\n");
        emailBody.append(message).append("\n\n");
        emailBody.append("Incident Details:\n");
        emailBody.append(" - Incident Code : ").append(incidentCode).append("\n");
        emailBody.append(" - Priority      : ").append(priority).append("\n");
        if (incident != null && incident.getAffectedService() != null) {
            emailBody.append(" - Service       : ").append(incident.getAffectedService().getServiceName()).append("\n");
        }
        if (incident != null && incident.getResponseDeadline() != null) {
            emailBody.append(" - Response By   : ").append(incident.getResponseDeadline()).append("\n");
        }
        if (incident != null && incident.getResolutionDeadline() != null) {
            emailBody.append(" - Resolution By : ").append(incident.getResolutionDeadline()).append("\n");
        }
        emailBody.append("\nRegards,\nIntelligent ITSM Automated Service Desk");

        // Persist NotificationRecord
        NotificationRecord record = new NotificationRecord(
                recipientCode,
                recipientEmail,
                incidentCode,
                eventType,
                CHANNEL_NAME,
                formattedSubject,
                emailBody.toString()
        );
        record.setReadStatus(false);
        notificationRecordRepository.save(record);

        // Print formatted simulated email block
        System.out.println("\n----------------------------- [SIMULATED EMAIL] -----------------------------");
        System.out.println("From    : " + SENDER_EMAIL);
        System.out.println("To      : " + recipientEmail + " (" + recipientName + " [" + recipientCode + "])");
        System.out.println("Subject : " + formattedSubject);
        System.out.println("Event   : " + eventType);
        System.out.println("Priority: " + priority + " | Timestamp: " + LocalDateTime.now());
        System.out.println("Body    :");
        for (String line : emailBody.toString().split("\n")) {
            System.out.println("  " + line);
        }
        System.out.println("-----------------------------------------------------------------------------\n");

        log.info("[SIMULATED EMAIL SENT] To: {} | Subject: {}", recipientEmail, formattedSubject);
    }
}
