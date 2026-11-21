package com.itsm.system.model.entity;

import com.itsm.system.model.enums.Role;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

import java.util.Objects;

/**
 * Abstract Base Entity representing a system user.
 * Demonstrates inheritance and polymorphism in domain modeling.
 */
@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "user_role", discriminatorType = DiscriminatorType.STRING)
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public abstract class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long id;

    @Column(name = "user_id_code", unique = true, nullable = false, length = 50)
    protected String userIdCode;

    @Column(name = "name", nullable = false, length = 100)
    protected String name;

    @Column(name = "email", unique = true, nullable = false, length = 120)
    protected String email;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @Column(name = "password", nullable = false, length = 255)
    protected String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 50)
    protected Role role;

    @Column(name = "active", nullable = false)
    protected boolean active = true;

    // Default constructor required by JPA
    protected User() {
    }

    // Parameterized constructor
    protected User(String userIdCode, String name, String email, String password, Role role, boolean active) {
        this.userIdCode = userIdCode;
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
        this.active = active;
    }

    /**
     * Polymorphic method implemented by each subclass to provide role-specific permission scopes.
     *
     * @return comma-separated string of granted permissions
     */
    public abstract String getDashboardPermissions();

    // Getters and Setters
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(userIdCode, user.userIdCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userIdCode);
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", userIdCode='" + userIdCode + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", role=" + role +
                ", active=" + active +
                '}';
    }
}
