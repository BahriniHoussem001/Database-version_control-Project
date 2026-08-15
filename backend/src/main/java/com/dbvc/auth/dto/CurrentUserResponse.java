package com.dbvc.auth.dto;

public class CurrentUserResponse {

    private String username;
    private String role;

    public CurrentUserResponse(String username, String role) {
        this.username = username;
        this.role = role;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }
}