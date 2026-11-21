package com.itsm.system.model.entity;

import com.itsm.system.model.enums.Role;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * Entity representing an organizational employee who can report IT incidents and submit feedback.
 */
@Entity
@DiscriminatorValue("EMPLOYEE")
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Employee extends User {

    @Column(name = "department", length = 100)
    private String department;

    @Column(name = "location", length = 150)
    private String location;

    // Default constructor for JPA
    public Employee() {
        super();
        this.role = Role.EMPLOYEE;
    }

    // Parameterized constructor
    public Employee(String userIdCode, String name, String email, String password, boolean active,
                    String department, String location) {
        super(userIdCode, name, email, password, Role.EMPLOYEE, active);
        this.department = department;
        this.location = location;
    }

    @Override
    public String getDashboardPermissions() {
        return "CREATE_INCIDENT,VIEW_OWN_INCIDENTS,PROVIDE_FEEDBACK,VIEW_SERVICE_CATALOG";
    }

    // Getters and Setters
    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    @Override
    public String toString() {
        return "Employee{" +
                "id=" + id +
                ", userIdCode='" + userIdCode + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", department='" + department + '\'' +
                ", location='" + location + '\'' +
                ", active=" + active +
                '}';
    }
}
