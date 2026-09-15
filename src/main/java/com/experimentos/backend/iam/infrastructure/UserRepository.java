package com.experimentos.backend.iam.infrastructure;

import com.experimentos.backend.iam.domain.User;
import com.experimentos.backend.shared.infrastructure.firebase.repositories.AbstractFirestoreRepository;
import com.experimentos.backend.shared.security.Role;
import com.google.cloud.firestore.Firestore;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository extends AbstractFirestoreRepository<User, Long> {
    public UserRepository(Firestore firestore) {
        super(firestore, User.class, "users");
    }

    public Optional<User> findByUsernameIgnoreCaseOrEmailIgnoreCase(String username, String email) {
        String normalizedUsername = normalize(username);
        String normalizedEmail = normalize(email);
        return readAll().stream()
                .filter(
                        user ->
                                normalize(user.getUsername()).equals(normalizedUsername)
                                        || (normalizedEmail != null
                                                && Objects.equals(normalize(user.getEmail()), normalizedEmail)))
                .findFirst();
    }

    public Optional<User> findByEmailIgnoreCase(String email) {
        String normalizedEmail = normalize(email);
        return readAll().stream()
                .filter(user -> normalizedEmail != null && Objects.equals(normalize(user.getEmail()), normalizedEmail))
                .findFirst();
    }

    public boolean existsByUsernameIgnoreCase(String username) {
        return readAll().stream().anyMatch(user -> normalize(user.getUsername()).equals(normalize(username)));
    }

    public boolean existsByEmailIgnoreCase(String email) {
        String normalized = normalize(email);
        return normalized != null && readAll().stream().anyMatch(user -> normalized.equals(normalize(user.getEmail())));
    }

    public boolean existsByUsernameIgnoreCaseAndIdNot(String username, Long id) {
        return readAll().stream()
                .anyMatch(user -> !id.equals(user.getId()) && normalize(user.getUsername()).equals(normalize(username)));
    }

    public boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id) {
        String normalized = normalize(email);
        return normalized != null
                && readAll().stream()
                        .anyMatch(user -> !id.equals(user.getId()) && normalized.equals(normalize(user.getEmail())));
    }

    public long countByRole(Role role) {
        return readAll().stream().filter(user -> user.getRole() == role).count();
    }

    public long countByRoleAndEnabledTrue(Role role) {
        return readAll().stream().filter(user -> user.getRole() == role && user.isEnabled()).count();
    }

    public Optional<User> findBySystemOwnerTrue() {
        return readAll().stream().filter(User::isSystemOwner).findFirst();
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }
}
