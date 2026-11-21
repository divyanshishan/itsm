package com.itsm.system.pattern.observer.notification;

import com.itsm.system.model.entity.Incident;
import com.itsm.system.model.entity.User;
import com.itsm.system.model.enums.NotificationEventType;

/**
 * Observer / Channel interface for dispatching notifications to stakeholders.
 * Implementing classes represent discrete delivery channels (e.g. Dashboard, Email, SMS, Push).
 * Adding new notification channels requires zero modification to existing business services (Open/Closed Principle).
 */
public interface NotificationChannel {

    /**
     * Unique identifier for this notification channel (e.g., "DASHBOARD", "SIMULATED_EMAIL").
     */
    String getChannelName();

    /**
     * Dispatches a notification payload to a specified recipient regarding an incident event.
     *
     * @param recipient the target User receiving the notification
     * @param incident  the associated Incident context
     * @param eventType the category of lifecycle event
     * @param subject   the headline / subject line
     * @param message   the comprehensive notification body
     */
    void sendNotification(User recipient, Incident incident, NotificationEventType eventType, String subject, String message);
}
