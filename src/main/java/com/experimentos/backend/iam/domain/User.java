package com.experimentos.backend.iam.domain;

import com.experimentos.backend.shared.security.Role;
import java.time.Instant;

public class User {
    private Long id;

    private String username;

    private String email;

    private String passwordHash;

    private String displayName;

    private Role role;

    private boolean enabled = true;

    private boolean systemOwner;

    private Instant createdAt;

    private Instant updatedAt;

    protected User() {}

    public User(String username, String email, String passwordHash, String displayName, Role role) {
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Role getRole() {
        return role;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isSystemOwner() {
        return systemOwner;
    }

    public void updateProfile(String username, String email, String displayName) {
        this.username = username;
        this.email = email;
        this.displayName = displayName;
    }

    public void changeRole(Role role) {
        this.role = role;
    }

    public void changePassword(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void disable() {
        enabled = false;
    }

    public void enable() {
        enabled = true;
    }

    public void markAsSystemOwner() {
        systemOwner = true;
    }
}
