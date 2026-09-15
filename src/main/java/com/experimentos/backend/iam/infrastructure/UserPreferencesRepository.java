package com.experimentos.backend.iam.infrastructure;

import com.experimentos.backend.iam.domain.UserPreferences;
import com.experimentos.backend.shared.infrastructure.firebase.repositories.AbstractFirestoreRepository;
import com.google.cloud.firestore.Firestore;
import org.springframework.stereotype.Repository;

@Repository
public class UserPreferencesRepository extends AbstractFirestoreRepository<UserPreferences, Long> {
    public UserPreferencesRepository(Firestore firestore) {
        super(firestore, UserPreferences.class, "user_preferences");
    }
}
