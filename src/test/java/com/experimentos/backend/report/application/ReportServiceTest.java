package com.experimentos.backend.report.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.experimentos.backend.iam.domain.User;
import com.experimentos.backend.iam.infrastructure.UserRepository;
import com.experimentos.backend.report.domain.Report;
import com.experimentos.backend.report.domain.ReportPriority;
import com.experimentos.backend.report.infrastructure.ReportRepository;
import com.experimentos.backend.report.interfaces.ReportDtos;
import com.experimentos.backend.shared.security.Role;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

/** Regression tests for report ownership and anonymous presentation. */
@ExtendWith(MockitoExtension.class)
class ReportServiceTest {
    @Mock ReportRepository reports;
    @Mock UserRepository users;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void anonymousReportsRemainVisibleToTheirOwnerWithoutRevealingTheirIdentity() {
        User employee = user(2L, "maria");
        when(users.findByUsernameIgnoreCaseOrEmailIgnoreCase("maria", "maria"))
                .thenReturn(Optional.of(employee));
        when(reports.save(any(Report.class)))
                .thenAnswer(
                        invocation -> {
                            Report report = invocation.getArgument(0);
                            ReflectionTestUtils.setField(report, "id", 10L);
                            return report;
                        });
        authenticateAs("maria");

        ReportService service = new ReportService(reports, users);
        ReportDtos.ReportResponse created =
                service.create(
                        new ReportDtos.CreateReportRequest(
                                "TI",
                                "Access issue",
                                "The VPN is unavailable",
                                ReportPriority.NORMAL,
                                true));

        ArgumentCaptor<Report> reportCaptor = ArgumentCaptor.forClass(Report.class);
        verify(reports).save(reportCaptor.capture());
        Report saved = reportCaptor.getValue();
        assertThat(created.reporterDisplayName()).isNull();
        assertThat(saved).isNotNull();
        assertThat(ReflectionTestUtils.getField(saved, "user")).isSameAs(employee);
    }

    private User user(Long id, String username) {
        User user = new User(username, username + "@example.com", "hash", "Maria", Role.EMPLOYEE);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private void authenticateAs(String username) {
        SecurityContextHolder.getContext()
                .setAuthentication(
                        UsernamePasswordAuthenticationToken.authenticated(
                                username, null, List.of()));
    }
}
