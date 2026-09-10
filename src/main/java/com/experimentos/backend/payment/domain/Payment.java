package com.experimentos.backend.payment.domain;

import com.experimentos.backend.iam.domain.User;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "payment_records")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "beneficiary_name", nullable = false, length = 100)
    private String beneficiaryName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by_user_id")
    private User recordedBy;

    @Column(name = "recorded_by_username", nullable = false, length = 50)
    private String recordedByUsername;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentPlan plan;

    @Column(name = "voucher_filename", nullable = false, length = 255)
    private String voucherFilename;

    @Column(name = "voucher_content_type", nullable = false, length = 100)
    private String voucherContentType;

    @Column(name = "voucher_size", nullable = false)
    private long voucherSize;

    @Basic(fetch = FetchType.LAZY)
    @Column(name = "voucher_data", nullable = false, columnDefinition = "LONGBLOB")
    private byte[] voucherData;

    @Column(name = "next_payment_date", nullable = false)
    private LocalDate nextPaymentDate;

    @Column(name = "created_at", nullable = false, updatable = false)
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

    @jakarta.persistence.PrePersist
    void onCreate() {
        createdAt = Instant.now();
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
