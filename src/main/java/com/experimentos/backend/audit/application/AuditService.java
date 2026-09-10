package com.experimentos.backend.audit.application;

import com.experimentos.backend.audit.domain.AuditLog;
import com.experimentos.backend.audit.infrastructure.AuditLogRepository;
import com.experimentos.backend.iam.domain.User;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private final AuditLogRepository logs;

    public AuditService(AuditLogRepository logs) {
        this.logs = logs;
    }

    public void record(User actor, String action, String resourceType, String resourceId) {
        logs.save(new AuditLog(actor, action, resourceType, resourceId));
    }
}
