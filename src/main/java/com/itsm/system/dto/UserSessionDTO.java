package com.itsm.system.dto;

import com.itsm.system.model.entity.Administrator;
import com.itsm.system.model.entity.Employee;
import com.itsm.system.model.entity.Technician;
import com.itsm.system.model.entity.User;
import com.itsm.system.model.enums.Role;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * DTO representing an authenticated user session with role-specific attributes.
 */
public class UserSessionDTO {

    private Long id;
    private String userIdCode;
    private String name;
    private String email;
    private Role role;
    private String department;
    private Set<String> skills = new HashSet<>();
    private Integer currentWorkload;
    private Integer maxWorkload;
    private Integer experienceYears;
    private String permissions;

    public UserSessionDTO() {
    }

    public UserSessionDTO(Long id, String userIdCode, String name, String email, Role role,
                          String department, Set<String> skills, Integer currentWorkload,
                          Integer maxWorkload, Integer experienceYears) {
        this.id = id;
        this.userIdCode = userIdCode;
        this.name = name;
        this.email = email;
        this.role = role;
        this.department = department;
        this.skills = skills != null ? skills : Collections.emptySet();
        this.currentWorkload = currentWorkload;
        this.maxWorkload = maxWorkload;
        this.experienceYears = experienceYears;
    }

    /**
     * Factory method mapping a polymorphic User entity into a UserSessionDTO.
     */
    public static UserSessionDTO fromUser(User user) {
        if (user == null) {
            return null;
        }

        UserSessionDTO dto = new UserSessionDTO();
        dto.setId(user.getId());
        dto.setUserIdCode(user.getUserIdCode());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole());
        dto.setPermissions(user.getDashboardPermissions());

        if (user instanceof Employee emp) {
            dto.setDepartment(emp.getDepartment());
        } else if (user instanceof Technician tech) {
            Set<String> skills = Collections.emptySet();
            try {
                if (tech.getSkills() != null) {
                    skills = new HashSet<>(tech.getSkills());
                }
            } catch (Exception ignored) {
                // Defensive fallback against LazyInitializationException when detached
            }
            dto.setSkills(skills);
            dto.setCurrentWorkload(tech.getCurrentWorkload());
            dto.setMaxWorkload(tech.getMaxWorkload());
            dto.setExperienceYears(tech.getExperienceYears());
        } else if (user instanceof Administrator admin) {
            dto.setDepartment(admin.getAdminLevel());
        }

        return dto;
    }

    // Explicit Getters and Setters (Encapsulation - No Lombok)
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserIdCode() {
        return userIdCode;
    }

    public void setUserIdCode(String userIdCode) {
        this.userIdCode = userIdCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public Set<String> getSkills() {
        return skills;
    }

    public void setSkills(Set<String> skills) {
        this.skills = skills;
    }

    public Integer getCurrentWorkload() {
        return currentWorkload;
    }

    public void setCurrentWorkload(Integer currentWorkload) {
        this.currentWorkload = currentWorkload;
    }

    public Integer getMaxWorkload() {
        return maxWorkload;
    }

    public void setMaxWorkload(Integer maxWorkload) {
        this.maxWorkload = maxWorkload;
    }

    public Integer getExperienceYears() {
        return experienceYears;
    }

    public void setExperienceYears(Integer experienceYears) {
        this.experienceYears = experienceYears;
    }

    public String getPermissions() {
        return permissions;
    }

    public void setPermissions(String permissions) {
        this.permissions = permissions;
    }
}
