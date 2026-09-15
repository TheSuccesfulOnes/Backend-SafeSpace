package com.experimentos.backend.iam.domain;

import com.experimentos.backend.shared.domain.Theme;

public class UserPreferences {
    private Long userId;

    private User user;

    private String language = "es";

    private Theme theme = Theme.LIGHT;

    protected UserPreferences() {}

    public UserPreferences(User user) {
        this.user = user;
    }

    public Long getUserId() {
        return userId;
    }

    public User getUser() {
        return user;
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
