package com.itsm.system.dto;

/**
 * Request payload for user authentication.
 */
public class LoginRequest {

    private String userIdCode;
    private String password;

    public LoginRequest() {
    }

    public LoginRequest(String userIdCode, String password) {
        this.userIdCode = userIdCode;
        this.password = password;
    }

    // Explicit Getters and Setters (Encapsulation - No Lombok)
    public String getUserIdCode() {
        return userIdCode;
    }

    public void setUserIdCode(String userIdCode) {
        this.userIdCode = userIdCode;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
