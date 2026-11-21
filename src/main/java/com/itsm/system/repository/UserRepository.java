package com.itsm.system.repository;

import com.itsm.system.model.entity.User;
import com.itsm.system.model.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for base User entities.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUserIdCode(String userIdCode);

    Optional<User> findByUserIdCodeIgnoreCase(String userIdCode);

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailIgnoreCase(String email);

    List<User> findByRole(Role role);

    boolean existsByUserIdCode(String userIdCode);

    boolean existsByEmail(String email);
}
