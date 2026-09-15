package com.experimentos.backend.payment.domain;

import com.experimentos.backend.iam.domain.User;
import java.time.Instant;
import java.time.LocalDate;

public class Payment {
    private Long id;

    private User user;

    private String beneficiaryName;

    private User recordedBy;

    private String recordedByUsername;

    private PaymentPlan plan;

    private String voucherFilename;

    private String voucherContentType;

    private long voucherSize;

    private byte[] voucherData;

    private LocalDate nextPaymentDate;

    private Instant createdAt;

    protected Payment() {}

    public Payment(
            User user,
            String beneficiaryName,
            User recordedBy,
            String recordedByUsername,
            PaymentPlan plan,
            String voucherFilename,
            String voucherContentType,
            long voucherSize,
            byte[] voucherData,
            LocalDate nextPaymentDate) {
        this.user = user;
        this.beneficiaryName = beneficiaryName;
        this.recordedBy = recordedBy;
        this.recordedByUsername = recordedByUsername;
        this.plan = plan;
        this.voucherFilename = voucherFilename;
        this.voucherContentType = voucherContentType;
        this.voucherSize = voucherSize;
        this.voucherData = voucherData;
        this.nextPaymentDate = nextPaymentDate;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getBeneficiaryName() {
        return beneficiaryName;
    }

    public PaymentPlan getPlan() {
        return plan;
    }

    public String getVoucherFilename() {
        return voucherFilename;
    }

    public long getVoucherSize() {
        return voucherSize;
    }

    public LocalDate getNextPaymentDate() {
        return nextPaymentDate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
