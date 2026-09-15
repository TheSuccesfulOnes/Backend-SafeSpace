package com.experimentos.backend.mood.domain;

import com.experimentos.backend.iam.domain.User;
import java.time.Instant;
import java.time.LocalDate;

public class MoodEntry {
    private Long id;

    private User user;

    private Mood mood;

    private LocalDate moodDate;

    private Instant createdAt;

    protected MoodEntry() {}

    public MoodEntry(User user, Mood mood, LocalDate moodDate) {
        this.user = user;
        this.mood = mood;
        this.moodDate = moodDate;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Mood getMood() {
        return mood;
    }

    public LocalDate getMoodDate() {
        return moodDate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
