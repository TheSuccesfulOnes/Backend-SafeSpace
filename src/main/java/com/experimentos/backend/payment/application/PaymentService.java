package com.experimentos.backend.payment.application;

import com.experimentos.backend.audit.application.AuditService;
import com.experimentos.backend.iam.domain.User;
import com.experimentos.backend.iam.infrastructure.UserRepository;
import com.experimentos.backend.payment.domain.Payment;
import com.experimentos.backend.payment.domain.PaymentPlan;
import com.experimentos.backend.payment.infrastructure.PaymentRepository;
import com.experimentos.backend.payment.interfaces.PaymentDtos;
import com.experimentos.backend.shared.security.CurrentUser;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** Handles administrator payment records and voucher validation. */
@Service
public class PaymentService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Lima");
    private static final String PDF_CONTENT_TYPE = "application/pdf";
    private static final String PDF_SIGNATURE = "%PDF-";

    private final PaymentRepository payments;
    private final UserRepository users;
    private final AuditService auditService;
    private final long maxVoucherBytes;

    public PaymentService(
            PaymentRepository payments,
            UserRepository users,
            AuditService auditService,
            @Value("${app.payment.max-voucher-bytes:10485760}") long maxVoucherBytes) {
        this.payments = payments;
        this.users = users;
        this.auditService = auditService;
        this.maxVoucherBytes = maxVoucherBytes;
    }

    @Transactional(readOnly = true)
    public List<PaymentDtos.PlanSummary> listPlans() {
        return List.of(PaymentPlan.MONTHLY, PaymentPlan.ANNUAL).stream()
                .map(PaymentDtos.PlanSummary::from)
                .toList();
    }

    @Transactional
    public PaymentDtos.PaymentResponse recordPayment(
            String beneficiaryName, PaymentPlan plan, MultipartFile voucher) {
        String normalizedBeneficiaryName = normalizeBeneficiaryName(beneficiaryName);
        if (plan == null) {
            throw new IllegalArgumentException("A payment plan is required");
        }

        User actor = currentAdmin();
        ValidatedVoucher validatedVoucher = validateVoucher(voucher);
        LocalDate nextPaymentDate =
                LocalDate.now(BUSINESS_ZONE).plusMonths(plan.getDurationMonths());

        Payment payment =
                payments.save(
                        new Payment(
                                null,
                                normalizedBeneficiaryName,
                                actor,
                                actor.getUsername(),
                                plan,
                                validatedVoucher.filename(),
                                PDF_CONTENT_TYPE,
                                validatedVoucher.data().length,
                                validatedVoucher.data(),
                                nextPaymentDate));
        auditService.record(actor, "RECORD_PAYMENT", "PAYMENT", payment.getId().toString());
        return PaymentDtos.PaymentResponse.from(payment);
    }

    private String normalizeBeneficiaryName(String beneficiaryName) {
        String normalized = beneficiaryName == null ? "" : beneficiaryName.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("A beneficiary name is required");
        }
        if (normalized.length() > 100) {
            throw new IllegalArgumentException("The beneficiary name is too long");
        }
        return normalized;
    }

    private ValidatedVoucher validateVoucher(MultipartFile voucher) {
        if (voucher == null || voucher.isEmpty()) {
            throw new IllegalArgumentException("A PDF voucher is required");
        }
        if (voucher.getSize() > maxVoucherBytes) {
            throw new IllegalArgumentException("The voucher exceeds the maximum allowed size");
        }

        String filename = safeFilename(voucher.getOriginalFilename());
        if (!filename.toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("The voucher must be a PDF file");
        }
        String contentType = voucher.getContentType();
        if (contentType != null
                && !contentType.equalsIgnoreCase(PDF_CONTENT_TYPE)
                && !contentType.equalsIgnoreCase("application/octet-stream")) {
            throw new IllegalArgumentException("The voucher must be a PDF file");
        }

        try (InputStream inputStream = voucher.getInputStream()) {
            byte[] data = inputStream.readAllBytes();
            if (data.length == 0 || data.length > maxVoucherBytes || !hasPdfSignature(data)) {
                throw new IllegalArgumentException("The voucher content is not a valid PDF");
            }
            return new ValidatedVoucher(filename, data);
        } catch (IOException exception) {
            throw new IllegalArgumentException("The voucher could not be read", exception);
        }
    }

    private boolean hasPdfSignature(byte[] data) {
        byte[] signature = PDF_SIGNATURE.getBytes(StandardCharsets.US_ASCII);
        if (data.length < signature.length) {
            return false;
        }
        for (int index = 0; index < signature.length; index++) {
            if (data[index] != signature[index]) {
                return false;
            }
        }
        return true;
    }

    private String safeFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new IllegalArgumentException("The voucher filename is required");
        }
        String normalized = originalFilename.replace('\\', '/');
        String filename = normalized.substring(normalized.lastIndexOf('/') + 1).trim();
        if (filename.isBlank() || filename.length() > 255 || filename.contains("..")) {
            throw new IllegalArgumentException("The voucher filename is not valid");
        }
        return filename;
    }

    private User currentAdmin() {
        return users.findByUsernameIgnoreCaseOrEmailIgnoreCase(
                        CurrentUser.username(), CurrentUser.username())
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "Authenticated administrator was not found"));
    }

    private record ValidatedVoucher(String filename, byte[] data) {}
}
