package com.experimentos.backend.payment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.experimentos.backend.audit.application.AuditService;
import com.experimentos.backend.iam.domain.User;
import com.experimentos.backend.iam.infrastructure.UserRepository;
import com.experimentos.backend.payment.domain.Payment;
import com.experimentos.backend.payment.domain.PaymentPlan;
import com.experimentos.backend.payment.infrastructure.PaymentRepository;
import com.experimentos.backend.payment.interfaces.PaymentDtos;
import com.experimentos.backend.shared.security.Role;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

/** Unit tests for payment validation and renewal rules. */
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {
    @Mock PaymentRepository payments;
    @Mock UserRepository users;
    @Mock AuditService auditService;

    private PaymentService service;
    private User admin;

    @BeforeEach
    void setUp() {
        service = new PaymentService(payments, users, auditService, 10 * 1024 * 1024L);
        admin = user(1L, "admin", Role.SYSTEM_ADMIN);
        SecurityContextHolder.getContext()
                .setAuthentication(
                        UsernamePasswordAuthenticationToken.authenticated(
                                "admin",
                                null,
                                java.util.List.of(
                                        new SimpleGrantedAuthority("ROLE_SYSTEM_ADMIN"))));
        lenient()
                .when(users.findByUsernameIgnoreCaseOrEmailIgnoreCase("admin", "admin"))
                .thenReturn(Optional.of(admin));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void recordsValidPdfAndCalculatesNextPaymentDate() {
        when(payments.save(any(Payment.class)))
                .thenAnswer(
                        invocation -> {
                            Payment payment = invocation.getArgument(0);
                            ReflectionTestUtils.setField(payment, "id", 10L);
                            return payment;
                        });

        PaymentDtos.PaymentResponse response =
                service.recordPayment(
                        "Maria Cárdenas",
                        PaymentPlan.ANNUAL,
                        new MockMultipartFile(
                                "voucher",
                                "receipt.pdf",
                                "application/pdf",
                                "%PDF-1.7\nvalid test voucher".getBytes()));

        assertThat(response.beneficiaryName()).isEqualTo("Maria Cárdenas");
        assertThat(response.plan()).isEqualTo(PaymentPlan.ANNUAL);
        assertThat(response.nextPaymentDate()).isAfter(java.time.LocalDate.now());
        verify(auditService).record(admin, "RECORD_PAYMENT", "PAYMENT", "10");
    }

    @Test
    void rejectsAFileWithAFalsePdfExtension() {
        assertThatThrownBy(
                        () ->
                                service.recordPayment(
                                        "Any person",
                                        PaymentPlan.MONTHLY,
                                        new MockMultipartFile(
                                                "voucher",
                                                "receipt.pdf",
                                                "application/pdf",
                                                "not a pdf".getBytes())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The voucher content is not a valid PDF");
        verify(payments, never()).save(any(Payment.class));
    }

    @Test
    void rejectsAnEmptyBeneficiaryName() {
        assertThatThrownBy(
                        () ->
                                service.recordPayment(
                                        "   ",
                                        PaymentPlan.MONTHLY,
                                        new MockMultipartFile(
                                                "voucher",
                                                "receipt.pdf",
                                                "application/pdf",
                                                "%PDF-1.7".getBytes())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A beneficiary name is required");
        verify(payments, never()).save(any(Payment.class));
    }

    private User user(Long id, String username, Role role) {
        User user = new User(username, username + "@example.com", "hash", username, role);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
