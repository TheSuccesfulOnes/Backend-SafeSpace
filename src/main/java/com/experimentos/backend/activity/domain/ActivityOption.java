package com.experimentos.backend.activity.domain;

public class ActivityOption {
    private Long id;

    private WeeklyActivity activity;

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
