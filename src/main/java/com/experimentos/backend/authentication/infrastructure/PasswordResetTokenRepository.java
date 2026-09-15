package com.experimentos.backend.authentication.infrastructure;

import com.experimentos.backend.authentication.domain.PasswordResetToken;
import com.experimentos.backend.shared.infrastructure.firebase.repositories.AbstractFirestoreRepository;
import com.google.cloud.firestore.Firestore;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class PasswordResetTokenRepository
        extends AbstractFirestoreRepository<PasswordResetToken, Long> {
    public PasswordResetTokenRepository(Firestore firestore) {
        super(firestore, PasswordResetToken.class, "password_reset_tokens");
    }

    public Optional<PasswordResetToken> findByTokenHash(String tokenHash) {
        return readAll().stream()
                .filter(token -> tokenHash.equals(readField(token, "tokenHash")))
                .findFirst();
    }

    public void deleteByUserId(Long userId) {
        readAll().stream()
                .filter(
                        token -> {
                            Object user = readField(token, "user");
                            return user != null && userId.equals(readField(user, "id"));
                        })
                .toList()
                .forEach(this::delete);
    }
}
