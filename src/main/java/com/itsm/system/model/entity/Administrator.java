package com.itsm.system.model.entity;

import com.itsm.system.model.enums.Role;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * Entity representing an Administrator with global system management and override authority.
 */
@Entity
@DiscriminatorValue("ADMINISTRATOR")
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Administrator extends User {

    @Column(name = "admin_level", length = 100)
    private String adminLevel;

    // Default constructor for JPA
    public Administrator() {
        super();
        this.role = Role.ADMINISTRATOR;
    }

    // Parameterized constructor
    public Administrator(String userIdCode, String name, String email, String password, boolean active,
                         String adminLevel) {
        super(userIdCode, name, email, password, Role.ADMINISTRATOR, active);
        this.adminLevel = adminLevel;
    }

    @Override
    public String getDashboardPermissions() {
        return "ALL_PRIVILEGES,MANAGE_USERS,MANAGE_SERVICES,MANAGE_CATEGORIES,MANAGE_SLA,OVERRIDE_ASSIGNMENTS,VIEW_METRICS";
    }

    // Getters and Setters
    public String getAdminLevel() {
        return adminLevel;
    }

    public void setAdminLevel(String adminLevel) {
        this.adminLevel = adminLevel;
    }

    @Override
    public String toString() {
        return "Administrator{" +
                "id=" + id +
                ", userIdCode='" + userIdCode + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", adminLevel='" + adminLevel + '\'' +
                ", active=" + active +
                '}';
    }
}
