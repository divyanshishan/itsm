package com.itsm.system.repository;

import com.itsm.system.model.entity.NotificationRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA Repository for NotificationRecord entities.
 */
@Repository
public interface NotificationRecordRepository extends JpaRepository<NotificationRecord, Long> {

    List<NotificationRecord> findByRecipientUserCodeOrderByCreatedAtDesc(String recipientUserCode);

    List<NotificationRecord> findByRecipientUserCodeAndChannelTypeOrderByCreatedAtDesc(String recipientUserCode, String channelType);

    List<NotificationRecord> findByChannelTypeOrderByCreatedAtDesc(String channelType);

    List<NotificationRecord> findByIncidentCodeOrderByCreatedAtDesc(String incidentCode);

    long countByChannelType(String channelType);
}
