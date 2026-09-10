package com.experimentos.backend.report.infrastructure;

import com.experimentos.backend.report.domain.Report;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {
    List<Report> findByUserIdOrderByIdDesc(Long userId);

    List<Report> findAllByOrderByIdDesc();
}
