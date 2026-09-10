package com.experimentos.backend.audit.infrastructure;

import com.experimentos.backend.audit.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {}
