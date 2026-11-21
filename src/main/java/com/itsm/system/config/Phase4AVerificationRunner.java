package com.itsm.system.config;

import com.itsm.system.model.entity.NotificationRecord;
import com.itsm.system.repository.NotificationRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Startup verification runner for Phase 4A:
 * Verifies that the Observer / Channel notification system correctly dispatched and persisted
 * notifications across both DASHBOARD and SIMULATED_EMAIL channels during incident lifecycle events.
 */
@Component
@Order(4)
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "itsm.verification.runners.enabled", havingValue = "true", matchIfMissing = false)
public class Phase4AVerificationRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(Phase4AVerificationRunner.class);

    private final NotificationRecordRepository notificationRecordRepository;

    public Phase4AVerificationRunner(NotificationRecordRepository notificationRecordRepository) {
        this.notificationRecordRepository = notificationRecordRepository;
    }

    @Override
    public void run(String... args) {
        System.out.println("\n=========================================================================================");
        System.out.println("                 PHASE 4A: NOTIFICATION SYSTEM VERIFICATION (OBSERVER PATTERN)");
        System.out.println("=========================================================================================");

        long dashboardCount = notificationRecordRepository.countByChannelType("DASHBOARD");
        long emailCount = notificationRecordRepository.countByChannelType("SIMULATED_EMAIL");
        long totalCount = notificationRecordRepository.count();

        System.out.printf("Total Notification Records Persisted : %d%n", totalCount);
        System.out.printf(" - DASHBOARD Channel Notifications   : %d%n", dashboardCount);
        System.out.printf(" - SIMULATED_EMAIL Notifications     : %d%n", emailCount);

        List<NotificationRecord> recentNotifications = notificationRecordRepository.findAll();
        System.out.println("\nPersisted Notifications Audit Sample:");
        System.out.println("-----------------------------------------------------------------------------------------");
        System.out.printf("%-4s | %-10s | %-16s | %-24s | %-8s | %s%n",
                "ID", "Recipient", "Channel", "Event Type", "Ticket", "Subject");
        System.out.println("-----------------------------------------------------------------------------------------");

        for (NotificationRecord rec : recentNotifications) {
            System.out.printf("%-4d | %-10s | %-16s | %-24s | %-8s | %s%n",
                    rec.getId(),
                    rec.getRecipientUserCode(),
                    rec.getChannelType(),
                    rec.getEventType(),
                    rec.getIncidentCode() != null ? rec.getIncidentCode() : "N/A",
                    rec.getSubject());
        }
        System.out.println("-----------------------------------------------------------------------------------------");

        if (dashboardCount > 0 && emailCount > 0) {
            System.out.println("\n[PHASE 4A TEST PASSED] Extensible Notification System verified across DASHBOARD and SIMULATED_EMAIL channels.");
            log.info("[PHASE 4A TEST PASSED] Extensible Notification System verified across DASHBOARD and SIMULATED_EMAIL channels.");
        } else {
            System.err.println("\n[PHASE 4A TEST FAILED] Expected notifications in both DASHBOARD and SIMULATED_EMAIL channels, but found "
                    + dashboardCount + " dashboard and " + emailCount + " email records.");
            log.error("[PHASE 4A TEST FAILED] Incomplete notifications: dashboard={}, email={}", dashboardCount, emailCount);
        }

        System.out.println("=========================================================================================\n");
    }
}
