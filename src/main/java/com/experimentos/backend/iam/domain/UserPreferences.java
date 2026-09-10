package com.experimentos.backend.iam.domain;

import com.experimentos.backend.shared.domain.Theme;
import jakarta.persistence.*;

@Entity
@Table(name = "user_preferences")
public class UserPreferences {
    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 10)
    private String language = "es";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Theme theme = Theme.LIGHT;

    protected UserPreferences() {}

    public UserPreferences(User user) {
        this.user = user;
    }

    public Long getUserId() {
        return userId;
    }

    public String getLanguage() {
        return language;
    }

    public Theme getTheme() {
        return theme;
    }

    public void update(String language, Theme theme) {
        this.language = language;
        this.theme = theme;
    }
}
