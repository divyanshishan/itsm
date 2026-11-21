package com.itsm.system.repository;

import com.itsm.system.model.entity.SlaPolicy;
import com.itsm.system.model.enums.PriorityLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA Repository for SlaPolicy entities.
 */
@Repository
public interface SlaPolicyRepository extends JpaRepository<SlaPolicy, Long> {

    Optional<SlaPolicy> findByPriorityLevel(PriorityLevel priorityLevel);

    boolean existsByPriorityLevel(PriorityLevel priorityLevel);
}
