package com.experimentos.backend.payment.domain;

/** Supported renewal periods for an employee wellbeing plan. */
public enum PaymentPlan {
    MONTHLY(1),
    ANNUAL(12);

    private final int durationMonths;

    PaymentPlan(int durationMonths) {
        this.durationMonths = durationMonths;
    }

    public int getDurationMonths() {
        return durationMonths;
    }
}
