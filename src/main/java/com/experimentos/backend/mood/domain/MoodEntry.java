package com.experimentos.backend.mood.domain;

import com.experimentos.backend.iam.domain.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(
        name = "mood_entries",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_mood_user_date",
                        columnNames = {"user_id", "mood_date"}))
public class MoodEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Mood mood;

    @Column(name = "mood_date", nullable = false)
    private LocalDate moodDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected MoodEntry() {}

    public MoodEntry(User user, Mood mood, LocalDate moodDate) {
        this.user = user;
        this.mood = mood;
        this.moodDate = moodDate;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
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
}
