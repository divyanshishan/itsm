package com.itsm.system.controller;

import com.itsm.system.dto.ApiResponse;
import com.itsm.system.exception.InvalidUserInputException;
import com.itsm.system.model.entity.NotificationRecord;
import com.itsm.system.repository.NotificationRecordRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST Controller for In-App Dashboard notifications and Simulated Email audit records.
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationRestController {

    private final NotificationRecordRepository notificationRecordRepository;

    public NotificationRestController(NotificationRecordRepository notificationRecordRepository) {
        this.notificationRecordRepository = notificationRecordRepository;
    }

    /**
     * Retrieves all in-app DASHBOARD notifications for a specific user, sorted by newest first.
     */
    @GetMapping("/user/{userCode}")
    public ApiResponse<List<NotificationRecord>> getUserNotifications(@PathVariable String userCode) {
        if (userCode == null || userCode.trim().isEmpty()) {
            throw new InvalidUserInputException("User code cannot be null or empty.");
        }

        List<NotificationRecord> records = notificationRecordRepository
                .findByRecipientUserCodeAndChannelTypeOrderByCreatedAtDesc(userCode.trim(), "DASHBOARD");

        return ApiResponse.ok("User dashboard notifications retrieved successfully", records);
    }

    /**
     * Retrieves all SIMULATED_EMAIL records across the system, sorted by newest first.
     */
    @GetMapping("/emails")
    public ApiResponse<List<NotificationRecord>> getSimulatedEmails() {
        List<NotificationRecord> emails = notificationRecordRepository
                .findByChannelTypeOrderByCreatedAtDesc("SIMULATED_EMAIL");

        return ApiResponse.ok("Simulated email logs retrieved successfully", emails);
    }

    /**
     * Marks an in-app dashboard notification as read.
     */
    @PostMapping("/{id}/read")
    public ApiResponse<NotificationRecord> markAsRead(@PathVariable Long id) {
        NotificationRecord record = notificationRecordRepository.findById(id)
                .orElseThrow(() -> new InvalidUserInputException("Notification record not found with id: " + id));

        record.setReadStatus(true);
        NotificationRecord saved = notificationRecordRepository.save(record);
        return ApiResponse.ok("Notification marked as read successfully", saved);
    }
}
