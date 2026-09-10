package com.experimentos.backend.payment.interfaces;

import com.experimentos.backend.payment.domain.Payment;
import com.experimentos.backend.payment.domain.PaymentPlan;
import java.time.Instant;
import java.time.LocalDate;

public final class PaymentDtos {
    private PaymentDtos() {}

    public record PlanSummary(String code, int durationMonths) {
        public static PlanSummary from(PaymentPlan plan) {
            return new PlanSummary(plan.name(), plan.getDurationMonths());
        }
    }

    public record PaymentResponse(
            Long id,
            Long userId,
            String beneficiaryName,
            PaymentPlan plan,
            LocalDate nextPaymentDate,
            String voucherFilename,
            long voucherSize,
            Instant createdAt) {
        public static PaymentResponse from(Payment payment) {
            return new PaymentResponse(
                    payment.getId(),
                    payment.getUser() == null ? null : payment.getUser().getId(),
                    payment.getBeneficiaryName(),
                    payment.getPlan(),
                    payment.getNextPaymentDate(),
                    payment.getVoucherFilename(),
                    payment.getVoucherSize(),
                    payment.getCreatedAt());
        }
    }
}
