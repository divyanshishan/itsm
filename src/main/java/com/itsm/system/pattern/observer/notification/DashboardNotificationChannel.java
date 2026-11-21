package com.itsm.system.pattern.observer.notification;

import com.itsm.system.model.entity.Incident;
import com.itsm.system.model.entity.NotificationRecord;
import com.itsm.system.model.entity.User;
import com.itsm.system.model.enums.NotificationEventType;
import com.itsm.system.repository.NotificationRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * In-App Dashboard notification channel.
 * Persists notification records with readStatus = false for real-time user dashboard viewing.
 */
@Component
public class DashboardNotificationChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(DashboardNotificationChannel.class);
    private static final String CHANNEL_NAME = "DASHBOARD";

    private final NotificationRecordRepository notificationRecordRepository;

    public DashboardNotificationChannel(NotificationRecordRepository notificationRecordRepository) {
        this.notificationRecordRepository = notificationRecordRepository;
    }

    @Override
    public String getChannelName() {
        return CHANNEL_NAME;
    }

    @Override
    public void sendNotification(User recipient, Incident incident, NotificationEventType eventType, String subject, String message) {
        String recipientCode = (recipient != null) ? recipient.getUserIdCode() : "SYSTEM";
        String recipientEmail = (recipient != null) ? recipient.getEmail() : null;
        String incidentCode = (incident != null) ? incident.getIncidentCode() : null;

        NotificationRecord record = new NotificationRecord(
                recipientCode,
                recipientEmail,
                incidentCode,
                eventType,
                CHANNEL_NAME,
                subject,
                message
        );
        record.setReadStatus(false);

        notificationRecordRepository.save(record);

        System.out.printf("[DASHBOARD NOTIFICATION] To User: %-6s | Event: %-24s | Msg: %s%n",
                recipientCode, eventType, subject);
        log.info("[DASHBOARD NOTIFICATION] Dispatched to user {} for incident {}: {}",
                recipientCode, incidentCode, subject);
    }
}
