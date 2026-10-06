package com.experimentos.backend.validation;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.experimentos.backend.iam.domain.User;
import com.experimentos.backend.shared.infrastructure.firebase.repositories.AbstractFirestoreRepository;
import com.experimentos.backend.shared.security.Role;
import com.google.api.core.ApiFutures;
import com.google.cloud.firestore.*;
import java.time.*;
import java.util.*;

class FirestoreValidationTest extends ScenarioContract {
    static class Entity {
        String id = "existing";
        String passwordHash = "synthetic-root-hash";
        User user = ScenarioContract.user(1, Role.EMPLOYEE);
        Entity child;
        Role role = Role.EMPLOYEE;
        LocalDate date = DATE;
        byte[] data = "%PDF-".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        Long count = 1L;
        boolean enabled = true;
        Instant createdAt;

        Entity() {}
    }

    static class Repository extends AbstractFirestoreRepository<Entity, String> {
        Repository(Firestore store) {
            super(store, Entity.class, "synthetic");
        }

        Map<String, Object> encode(Entity entity) {
            return toDocument(entity);
        }

        Entity decode(Map<String, Object> data) {
            DocumentSnapshot s = mock(DocumentSnapshot.class);
            when(s.getData()).thenReturn(data);
            return fromDocument(s);
        }
    }

    static class Fixture {
        final Firestore firestore = mock(Firestore.class);
        final CollectionReference collection = mock(CollectionReference.class);
        final DocumentReference document = mock(DocumentReference.class);
        final DocumentSnapshot snapshot = mock(DocumentSnapshot.class);
        final Repository repository = new Repository(firestore);
        final Entity entity = new Entity();

        void bind() {
            when(firestore.collection("synthetic")).thenReturn(collection);
            when(collection.document("existing")).thenReturn(document);
            when(document.set(anyMap())).thenReturn(ApiFutures.immediateFuture(null));
            when(document.get()).thenReturn(ApiFutures.immediateFuture(snapshot));
        }

        void data(Map<String, Object> data) {
            when(snapshot.exists()).thenReturn(true);
            when(snapshot.getData()).thenReturn(data);
        }
    }

    @Override
    protected List<Scenario> scenarios() {
        return List.of(
                unit(
                        "null id save denied before database access",
                        () -> {
                            var f = new Fixture();
                            f.entity.id = null;
                            assertThatThrownBy(() -> f.repository.save(f.entity))
                                    .isInstanceOf(IllegalArgumentException.class)
                                    .hasMessageContaining("document id");
                            verifyNoInteractions(f.firestore);
                        }),
                unit(
                        "blank id save denied before database access",
                        () -> {
                            var f = new Fixture();
                            f.entity.id = " ";
                            assertThatThrownBy(() -> f.repository.save(f.entity))
                                    .isInstanceOf(IllegalArgumentException.class);
                            verifyNoInteractions(f.firestore);
                        }),
                unit(
                        "root credential hash retained only in root",
                        () -> {
                            var f = new Fixture();
                            assertThat(f.repository.encode(f.entity))
                                    .containsEntry("passwordHash", "synthetic-root-hash");
                        }),
                unit(
                        "related user snapshot strips credential hash",
                        () -> {
                            var f = new Fixture();
                            assertThat(
                                            ((Map<?, ?>) f.repository.encode(f.entity).get("user"))
                                                    .containsKey("passwordHash"))
                                    .isFalse();
                        }),
                unit(
                        "related nested snapshot strips credential hash",
                        () -> {
                            var f = new Fixture();
                            f.entity.child = new Entity();
                            f.entity.child.passwordHash = "synthetic-child-hash";
                            assertThat(
                                            ((Map<?, ?>) f.repository.encode(f.entity).get("child"))
                                                    .containsKey("passwordHash"))
                                    .isFalse();
                        }),
                unit(
                        "unknown enum rejected on hydration",
                        () -> {
                            var f = new Fixture();
                            assertThatThrownBy(() -> f.repository.decode(Map.of("role", "OWNER")))
                                    .isInstanceOf(IllegalArgumentException.class)
                                    .hasMessageContaining("Unknown Role");
                        }),
                unit(
                        "invalid date rejected on hydration",
                        () -> {
                            var f = new Fixture();
                            assertThatThrownBy(
                                            () -> f.repository.decode(Map.of("date", "2026-02-30")))
                                    .isInstanceOf(java.time.format.DateTimeParseException.class);
                        }),
                unit(
                        "invalid instant rejected on hydration",
                        () -> {
                            var f = new Fixture();
                            assertThatThrownBy(
                                            () ->
                                                    f.repository.decode(
                                                            Map.of("createdAt", "not-an-instant")))
                                    .isInstanceOf(java.time.format.DateTimeParseException.class);
                        }),
                unit(
                        "invalid base64 rejected on hydration",
                        () -> {
                            var f = new Fixture();
                            assertThatThrownBy(() -> f.repository.decode(Map.of("data", "%%")))
                                    .isInstanceOf(IllegalArgumentException.class);
                        }),
                unit(
                        "numeric field rejects string mismatch",
                        () -> {
                            var f = new Fixture();
                            assertThatThrownBy(
                                            () ->
                                                    f.repository.decode(
                                                            Map.of("count", "not-a-number")))
                                    .isInstanceOf(ClassCastException.class);
                        }),
                unit(
                        "primitive null never overwritten",
                        () -> {
                            var f = new Fixture();
                            var data = new HashMap<String, Object>();
                            data.put("enabled", null);
                            assertThat(f.repository.decode(data).enabled).isTrue();
                        }),
                unit(
                        "cyclic snapshots stop at finite depth",
                        () -> {
                            var f = new Fixture();
                            f.entity.child = f.entity;
                            assertThatCode(() -> f.repository.encode(f.entity))
                                    .doesNotThrowAnyException();
                        }),
                integration(
                        "actual write validates id and lifecycle",
                        () -> {
                            var f = new Fixture();
                            f.bind();
                            f.repository.save(f.entity);
                            verify(f.document)
                                    .set(
                                            argThat(
                                                    m ->
                                                            m.get("id").equals("existing")
                                                                    && m.get("createdAt") != null));
                            assertThat(f.entity.createdAt).isNotNull();
                        }),
                integration(
                        "actual read decodes scalar contract",
                        () -> {
                            var f = new Fixture();
                            f.bind();
                            f.data(
                                    Map.of(
                                            "id",
                                            "existing",
                                            "role",
                                            "EMPLOYEE",
                                            "date",
                                            "2026-01-31",
                                            "data",
                                            "JVBERi0=",
                                            "enabled",
                                            false));
                            var result = f.repository.findById("existing").orElseThrow();
                            assertThat(result.role).isEqualTo(Role.EMPLOYEE);
                            assertThat(result.date).isEqualTo(java.time.LocalDate.of(2026, 1, 31));
                            assertThat(result.enabled).isFalse();
                        }),
                integration(
                        "nonexistent document does not hydrate",
                        () -> {
                            var f = new Fixture();
                            f.bind();
                            when(f.snapshot.exists()).thenReturn(false);
                            assertThat(f.repository.findById("existing")).isEmpty();
                            verify(f.snapshot, never()).getData();
                        }),
                integration(
                        "null lookup performs no database access",
                        () -> {
                            var f = new Fixture();
                            assertThat(f.repository.findById(null)).isEmpty();
                            assertThat(f.repository.existsById(null)).isFalse();
                            f.repository.deleteById(null);
                            verifyNoInteractions(f.firestore);
                        }),
                integration(
                        "unknown stored enum fails read",
                        () -> {
                            var f = new Fixture();
                            f.bind();
                            f.data(Map.of("role", "OWNER"));
                            assertThatThrownBy(() -> f.repository.findById("existing"))
                                    .isInstanceOf(IllegalArgumentException.class);
                        }),
                integration(
                        "read execution failure translated",
                        () -> {
                            var f = new Fixture();
                            f.bind();
                            when(f.document.get())
                                    .thenReturn(
                                            ApiFutures.immediateFailedFuture(
                                                    new java.io.IOException("synthetic")));
                            assertThatThrownBy(() -> f.repository.findById("existing"))
                                    .isInstanceOf(IllegalStateException.class)
                                    .hasMessage("Firestore read failed");
                        }),
                integration(
                        "interrupted read restores interrupt flag",
                        () -> {
                            var f = new Fixture();
                            f.bind();
                            com.google.api.core.ApiFuture<DocumentSnapshot> future = mock();
                            when(future.get()).thenThrow(new InterruptedException("synthetic"));
                            when(f.document.get()).thenReturn(future);
                            try {
                                assertThatThrownBy(() -> f.repository.findById("existing"))
                                        .isInstanceOf(IllegalStateException.class);
                                assertThat(Thread.currentThread().isInterrupted()).isTrue();
                            } finally {
                                Thread.interrupted();
                            }
                        }),
                integration(
                        "write execution failure translated",
                        () -> {
                            var f = new Fixture();
                            f.bind();
                            when(f.document.set(anyMap()))
                                    .thenReturn(
                                            ApiFutures.immediateFailedFuture(
                                                    new java.io.IOException("synthetic")));
                            assertThatThrownBy(() -> f.repository.save(f.entity))
                                    .isInstanceOf(IllegalStateException.class)
                                    .hasMessage("Firestore write failed");
                        }));
    }
}
