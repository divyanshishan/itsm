package com.itsm.system.repository;

import com.itsm.system.model.entity.Technician;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for Technician entities.
 */
@Repository
public interface TechnicianRepository extends JpaRepository<Technician, Long> {

    Optional<Technician> findByUserIdCode(String userIdCode);

    List<Technician> findByAvailableTrue();

    List<Technician> findByAvailableTrueAndActiveTrue();

    @Query("SELECT t FROM Technician t JOIN t.skills s WHERE UPPER(s) = UPPER(:skill)")
    List<Technician> findBySkill(@Param("skill") String skill);

    @Query("SELECT t FROM Technician t JOIN t.skills s WHERE UPPER(s) = UPPER(:skill) AND t.available = true AND t.active = true AND t.currentWorkload < t.maxWorkload ORDER BY t.currentWorkload ASC")
    List<Technician> findAvailableTechniciansBySkillOrderedByWorkload(@Param("skill") String skill);
}
