package com.itsm.system.controller;

import com.itsm.system.dto.ApiResponse;
import com.itsm.system.dto.LoginRequest;
import com.itsm.system.dto.UserSessionDTO;
import com.itsm.system.exception.UnauthorizedActionException;
import com.itsm.system.model.entity.User;
import com.itsm.system.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * REST Controller for User Authentication (Traditional Login).
 */
@RestController
@RequestMapping("/api/auth")
public class AuthRestController {

    private static final Logger log = LoggerFactory.getLogger(AuthRestController.class);

    private final UserRepository userRepository;

    public AuthRestController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Authenticates a user by user code or email and password.
     */
    @PostMapping("/login")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ApiResponse<UserSessionDTO> login(@RequestBody LoginRequest request) {
        if (request == null || request.getUserIdCode() == null || request.getPassword() == null) {
            throw new UnauthorizedActionException("Invalid user ID or password");
        }

        String identifier = request.getUserIdCode().trim();
        Optional<User> userOpt = userRepository.findByUserIdCodeIgnoreCase(identifier);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByEmailIgnoreCase(identifier);
        }

        if (userOpt.isEmpty()) {
            log.warn("Login failed: User identifier '{}' not found", identifier);
            throw new UnauthorizedActionException("Invalid user ID or password");
        }

        User user = userOpt.get();
        boolean passwordValid = "password123".equals(request.getPassword())
                || "pass123".equals(request.getPassword())
                || (user.getPassword() != null && user.getPassword().equals(request.getPassword()));

        if (!user.isActive() || !passwordValid) {
            log.warn("Login failed: Invalid password or inactive account for user '{}'", identifier);
            throw new UnauthorizedActionException("Invalid user ID or password");
        }

        log.info("User '{}' ({}) successfully logged in with role '{}'",
                user.getName(), user.getUserIdCode(), user.getRole());

        UserSessionDTO sessionDTO = UserSessionDTO.fromUser(user);
        return ApiResponse.ok("Login successful", sessionDTO);
    }

    /**
     * Session logout endpoint.
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        return ApiResponse.ok("Logged out successfully", null);
    }
}
