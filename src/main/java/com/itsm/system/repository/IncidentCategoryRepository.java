package com.itsm.system.repository;

import com.itsm.system.model.entity.IncidentCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for IncidentCategory entities.
 */
@Repository
public interface IncidentCategoryRepository extends JpaRepository<IncidentCategory, Long> {

    Optional<IncidentCategory> findByName(String name);

    Optional<IncidentCategory> findByNameIgnoreCase(String name);

    List<IncidentCategory> findByActiveTrue();

    boolean existsByNameIgnoreCase(String name);
}
