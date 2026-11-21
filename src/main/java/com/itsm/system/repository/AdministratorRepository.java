package com.itsm.system.repository;

import com.itsm.system.model.entity.Administrator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA Repository for Administrator entities.
 */
@Repository
public interface AdministratorRepository extends JpaRepository<Administrator, Long> {

    Optional<Administrator> findByUserIdCode(String userIdCode);

    Optional<Administrator> findByEmail(String email);
}
