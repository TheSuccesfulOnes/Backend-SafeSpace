package com.experimentos.backend.report.infrastructure;

import com.experimentos.backend.report.domain.Report;
import com.experimentos.backend.shared.infrastructure.firebase.repositories.AbstractFirestoreRepository;
import com.google.cloud.firestore.Firestore;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class ReportRepository extends AbstractFirestoreRepository<Report, Long> {
    public ReportRepository(Firestore firestore) {
        super(firestore, Report.class, "reports");
    }

    public List<Report> findByUserIdOrderByIdDesc(Long userId) {
        return readAll().stream()
                .filter(report -> reportUserId(report) != null && userId.equals(reportUserId(report)))
                .toList()
                .reversed();
    }

    public List<Report> findAllByOrderByIdDesc() {
        return readAll().reversed();
    }

    private Long reportUserId(Report report) {
        Object user = readField(report, "user");
        Object id = user == null ? null : readField(user, "id");
        return id instanceof Number number ? number.longValue() : id == null ? null : Long.valueOf(id.toString());
    }
}
