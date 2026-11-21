package com.itsm.system.repository;

import com.itsm.system.model.entity.ItService;
import com.itsm.system.model.enums.BusinessCriticality;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for ItService entities.
 */
@Repository
public interface ItServiceRepository extends JpaRepository<ItService, Long> {

    Optional<ItService> findByServiceCode(String serviceCode);

    List<ItService> findByCriticality(BusinessCriticality criticality);

    List<ItService> findByOperationalStatus(String operationalStatus);

    boolean existsByServiceCode(String serviceCode);
}
