package com.itsm.system.pattern.observer.notification;

import com.itsm.system.model.entity.Incident;
import com.itsm.system.model.entity.Technician;
import com.itsm.system.model.entity.User;
import com.itsm.system.model.enums.NotificationEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Publisher / Subject service in the Notification Observer Pattern.
 * Injects all registered NotificationChannel beans (e.g. Dashboard, Simulated Email, and future channels)
 * automatically via Spring's constructor injection, satisfying the Open/Closed Principle (OCP).
 * Allows dynamic registration and unregistration of channels at runtime.
 */
@Service
public class NotificationPublisherService {

    private static final Logger log = LoggerFactory.getLogger(NotificationPublisherService.class);

    private final List<NotificationChannel> channels;

    public NotificationPublisherService(List<NotificationChannel> channels) {
        // CopyOnWriteArrayList enables thread-safe iteration and runtime registration
        this.channels = new CopyOnWriteArrayList<>(channels != null ? channels : Collections.emptyList());
        log.info("Initialized NotificationPublisherService with {} notification channel(s): {}",
                this.channels.size(),
                this.channels.stream().map(NotificationChannel::getChannelName).toList());
    }

    /**
     * Dynamically registers an additional notification channel at runtime.
     */
    public void registerChannel(NotificationChannel channel) {
        if (channel != null && !channels.contains(channel)) {
            channels.add(channel);
            log.info("Registered new notification channel: {}", channel.getChannelName());
        }
    }

    /**
     * Dynamically unregisters a notification channel at runtime.
     */
    public void unregisterChannel(NotificationChannel channel) {
        if (channel != null && channels.remove(channel)) {
            log.info("Unregistered notification channel: {}", channel.getChannelName());
        }
    }

    /**
     * Returns an unmodifiable list of currently registered notification channels.
     */
    public List<NotificationChannel> getChannels() {
        return Collections.unmodifiableList(channels);
    }

    /**
     * Core observer dispatch method: broadcasts a notification across all registered channels.
     *
     * @param recipient the target recipient User
     * @param incident  the relevant Incident context
     * @param eventType the lifecycle event category
     * @param subject   the headline / subject line
     * @param message   the full message body
     */
    public void notifyUser(User recipient, Incident incident, NotificationEventType eventType, String subject, String message) {
        if (recipient == null) {
            log.warn("Cannot dispatch notification: recipient is null. Event: {}, Incident: {}",
                    eventType, incident != null ? incident.getIncidentCode() : "N/A");
            return;
        }

        for (NotificationChannel channel : channels) {
            try {
                channel.sendNotification(recipient, incident, eventType, subject, message);
            } catch (Exception ex) {
                log.error("Failed to send notification via channel '{}' to recipient '{}': {}",
                        channel.getChannelName(), recipient.getUserIdCode(), ex.getMessage(), ex);
            }
        }
    }

    /**
     * Notifies the reporting employee that an incident has been reported, classified, and prioritized.
     */
    public void notifyIncidentReported(Incident incident) {
        if (incident == null || incident.getReporter() == null) {
            return;
        }

        String subject = "Incident Logged: " + incident.getTitle();
        String message = String.format(
                "Your incident has been logged successfully with ticket code %s. Priority assigned: %s. " +
                        "A technician will be assigned shortly to begin investigation.",
                incident.getIncidentCode(),
                incident.getPriority()
        );

        notifyUser(incident.getReporter(), incident, NotificationEventType.INCIDENT_REPORTED, subject, message);
    }

    /**
     * Notifies both the assigned technician (new work assigned) and the reporting employee
     * (technician assigned & investigation underway).
     */
    public void notifyTechnicianAssigned(Incident incident) {
        if (incident == null) {
            return;
        }

        Technician technician = incident.getAssignedTechnician();
        if (technician != null) {
            String techSubject = "New Incident Assigned: " + incident.getIncidentCode() + " - " + incident.getTitle();
            String techMessage = String.format(
                    "You have been assigned to incident %s ('%s') with priority %s. " +
                            "Target resolution deadline: %s. Please review investigation notes and proceed.",
                    incident.getIncidentCode(),
                    incident.getTitle(),
                    incident.getPriority(),
                    incident.getResolutionDeadline() != null ? incident.getResolutionDeadline().toString() : "N/A"
            );
            notifyUser(technician, incident, NotificationEventType.TECHNICIAN_ASSIGNED, techSubject, techMessage);
        }

        if (incident.getReporter() != null) {
            String reporterSubject = "Technician Assigned to Incident: " + incident.getIncidentCode();
            String reporterMessage = String.format(
                    "Technician %s (%s) has been assigned to your incident %s ('%s'). Investigation is now in progress.",
                    technician != null ? technician.getName() : "A technician",
                    technician != null ? technician.getUserIdCode() : "N/A",
                    incident.getIncidentCode(),
                    incident.getTitle()
            );
            notifyUser(incident.getReporter(), incident, NotificationEventType.TECHNICIAN_ASSIGNED, reporterSubject, reporterMessage);
        }
    }

    /**
     * Notifies the reporting employee that their incident has been resolved and closed.
     */
    public void notifyIncidentResolvedAndClosed(Incident incident) {
        if (incident == null || incident.getReporter() == null) {
            return;
        }

        String resolution = incident.getResolutionDetails() != null ? incident.getResolutionDetails() : "Resolution completed.";
        String subject = "Incident Resolved and Closed: " + incident.getIncidentCode();
        String message = String.format(
                "Your incident %s ('%s') has been resolved and closed.\nResolution Summary: %s\n" +
                        "Please provide your customer feedback rating to help us maintain service quality.",
                incident.getIncidentCode(),
                incident.getTitle(),
                resolution
        );

        notifyUser(incident.getReporter(), incident, NotificationEventType.INCIDENT_RESOLVED_CLOSED, subject, message);
    }

    /**
     * Dispatches SLA warning or violation alerts to a designated stakeholder (technician or manager/admin).
     */
    public void notifySlaAlert(Incident incident, User stakeholder, boolean isViolated) {
        if (incident == null || stakeholder == null) {
            return;
        }

        NotificationEventType eventType = isViolated ? NotificationEventType.SLA_VIOLATED : NotificationEventType.SLA_APPROACHING;
        String subject = isViolated
                ? "SLA VIOLATION ALERT: Incident " + incident.getIncidentCode()
                : "SLA WARNING: Incident " + incident.getIncidentCode() + " Approaching Deadline";

        String message = isViolated
                ? String.format("CRITICAL: Incident %s ('%s') with priority %s has breached its SLA resolution deadline (%s).",
                incident.getIncidentCode(), incident.getTitle(), incident.getPriority(), incident.getResolutionDeadline())
                : String.format("WARNING: Incident %s ('%s') with priority %s is approaching its SLA target (%s). Action required.",
                incident.getIncidentCode(), incident.getTitle(), incident.getPriority(), incident.getResolutionDeadline());

        notifyUser(stakeholder, incident, eventType, subject, message);
    }
}
