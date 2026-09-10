package com.experimentos.backend.activity.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "activity_options")
public class ActivityOption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "activity_id", nullable = false)
    private WeeklyActivity activity;

    @Column(nullable = false, length = 160)
    private String label;

    protected ActivityOption() {}

    ActivityOption(WeeklyActivity activity, String label) {
        this.activity = activity;
        this.label = label;
    }

    public Long getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public WeeklyActivity getActivity() {
        return activity;
    }
}
