package com.experimentos.backend.audit.domain;

import com.experimentos.backend.iam.domain.User;
import java.time.Instant;

public class AuditLog {
    private Long id;

    private User actor;

    private String action;

    private String resourceType;

    private String resourceId;

    private Instant createdAt;

    protected AuditLog() {}

    public AuditLog(User actor, String action, String resourceType, String resourceId) {
        this.actor = actor;
        this.action = action;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }
}
