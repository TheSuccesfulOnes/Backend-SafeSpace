package com.experimentos.backend.audit.infrastructure;

import com.experimentos.backend.audit.domain.AuditLog;
import com.experimentos.backend.shared.infrastructure.firebase.repositories.AbstractFirestoreRepository;
import com.google.cloud.firestore.Firestore;
import org.springframework.stereotype.Repository;

@Repository
public class AuditLogRepository extends AbstractFirestoreRepository<AuditLog, Long> {
    public AuditLogRepository(Firestore firestore) {
        super(firestore, AuditLog.class, "audit_logs");
    }
}
