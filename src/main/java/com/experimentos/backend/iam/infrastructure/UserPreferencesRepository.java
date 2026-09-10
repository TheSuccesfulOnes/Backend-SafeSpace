package com.experimentos.backend.iam.infrastructure;

import com.experimentos.backend.iam.domain.UserPreferences;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPreferencesRepository extends JpaRepository<UserPreferences, Long> {}
