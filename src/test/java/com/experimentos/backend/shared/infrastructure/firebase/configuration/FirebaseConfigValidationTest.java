package com.experimentos.backend.shared.infrastructure.firebase.configuration;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.*;
import com.google.firebase.cloud.FirestoreClient;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

/**
 * Every SDK/ADC entry point is intercepted; no credentials are loaded and no Firebase app is
 * initialized.
 */
class FirebaseConfigValidationTest {
    static class Fixture implements AutoCloseable {
        final MockedStatic<GoogleCredentials> adc = mockStatic(GoogleCredentials.class);
        final MockedStatic<FirebaseApp> apps = mockStatic(FirebaseApp.class);
        final MockedStatic<FirestoreClient> clients = mockStatic(FirestoreClient.class);
        final FirebaseConfig config = new FirebaseConfig();
        final GoogleCredentials credentials = mock(GoogleCredentials.class);
        final FirebaseApp app = mock(FirebaseApp.class);
        final Firestore firestore = mock(Firestore.class);

        Fixture() {
            // The SDK scopes ADC while building options; this is local, not token refresh.
            when(credentials.createScoped(anyCollection())).thenReturn(credentials);
            apps.when(FirebaseApp::getApps).thenReturn(List.of());
            adc.when(GoogleCredentials::getApplicationDefault).thenReturn(credentials);
            apps.when(() -> FirebaseApp.initializeApp(any(FirebaseOptions.class))).thenReturn(app);
            clients.when(() -> FirestoreClient.getFirestore(app)).thenReturn(firestore);
        }

        AnnotationConfigApplicationContext context() {
            var c = new AnnotationConfigApplicationContext();
            c.getEnvironment()
                    .getPropertySources()
                    .addFirst(
                            new MapPropertySource(
                                    "synthetic",
                                    Map.of("firebase.project-id", "synthetic-project")));
            c.register(FirebaseConfig.class);
            try {
                c.refresh();
                return c;
            } catch (RuntimeException e) {
                c.close();
                throw e;
            }
        }

        public void close() {
            clients.close();
            apps.close();
            adc.close();
        }
    }

    @FunctionalInterface
    interface Action {
        void run(Fixture fixture) throws Exception;
    }

    record Case(String name, Action action) {}

    static DynamicTest test(Case c) {
        return DynamicTest.dynamicTest(
                c.name(),
                () -> {
                    try (var f = new Fixture()) {
                        c.action().run(f);
                    }
                });
    }

    @TestFactory
    Stream<DynamicTest> unit() {
        return Stream.of(
                        new Case(
                                "existing default app reused before ADC",
                                f -> {
                                    f.apps.when(FirebaseApp::getApps).thenReturn(List.of(f.app));
                                    f.apps.when(FirebaseApp::getInstance).thenReturn(f.app);
                                    assertThat(f.config.firebaseApp("synthetic-project"))
                                            .isSameAs(f.app);
                                    f.adc.verifyNoInteractions();
                                }),
                        new Case(
                                "default app reused among multiple apps",
                                f -> {
                                    f.apps.when(FirebaseApp::getApps)
                                            .thenReturn(List.of(mock(FirebaseApp.class), f.app));
                                    f.apps.when(FirebaseApp::getInstance).thenReturn(f.app);
                                    assertThat(f.config.firebaseApp("other-project"))
                                            .isSameAs(f.app);
                                    f.adc.verifyNoInteractions();
                                }),
                        new Case(
                                "named apps without default cannot initialize duplicate default",
                                f -> {
                                    f.apps.when(FirebaseApp::getApps).thenReturn(List.of(f.app));
                                    f.apps.when(FirebaseApp::getInstance)
                                            .thenThrow(
                                                    new IllegalStateException("default missing"));
                                    assertThatThrownBy(
                                                    () -> f.config.firebaseApp("synthetic-project"))
                                            .isInstanceOf(IllegalStateException.class)
                                            .hasMessage("default missing");
                                    f.adc.verifyNoInteractions();
                                }),
                        new Case(
                                "app discovery failure stops before ADC",
                                f -> {
                                    f.apps.when(FirebaseApp::getApps)
                                            .thenThrow(
                                                    new IllegalStateException("discovery failed"));
                                    assertThatThrownBy(
                                                    () -> f.config.firebaseApp("synthetic-project"))
                                            .hasMessage("discovery failed");
                                    f.adc.verifyNoInteractions();
                                }),
                        new Case(
                                "ADC IOException translated",
                                f -> {
                                    f.adc.when(GoogleCredentials::getApplicationDefault)
                                            .thenThrow(new java.io.IOException("synthetic"));
                                    assertThatThrownBy(
                                                    () -> f.config.firebaseApp("synthetic-project"))
                                            .isInstanceOf(IllegalStateException.class)
                                            .hasMessageContaining(
                                                    "Firebase credentials are not configured")
                                            .hasCauseInstanceOf(java.io.IOException.class);
                                }),
                        new Case(
                                "ADC runtime failure translated",
                                f -> {
                                    f.adc.when(GoogleCredentials::getApplicationDefault)
                                            .thenThrow(new SecurityException("synthetic"));
                                    assertThatThrownBy(
                                                    () -> f.config.firebaseApp("synthetic-project"))
                                            .isInstanceOf(IllegalStateException.class)
                                            .hasCauseInstanceOf(SecurityException.class);
                                }),
                        new Case(
                                "SDK initialization failure translated",
                                f -> {
                                    f.apps.when(
                                                    () ->
                                                            FirebaseApp.initializeApp(
                                                                    any(FirebaseOptions.class)))
                                            .thenThrow(new IllegalArgumentException("synthetic"));
                                    assertThatThrownBy(
                                                    () -> f.config.firebaseApp("synthetic-project"))
                                            .isInstanceOf(IllegalStateException.class)
                                            .hasCauseInstanceOf(IllegalArgumentException.class);
                                }),
                        new Case(
                                "null ADC rejected before SDK initialization",
                                f -> {
                                    f.adc.when(GoogleCredentials::getApplicationDefault)
                                            .thenReturn(null);
                                    assertThatThrownBy(
                                                    () -> f.config.firebaseApp("synthetic-project"))
                                            .isInstanceOf(IllegalStateException.class);
                                    f.apps.verify(
                                            () ->
                                                    FirebaseApp.initializeApp(
                                                            any(FirebaseOptions.class)),
                                            never());
                                }),
                        new Case(
                                "Firestore bean preserves supplied app identity",
                                f -> {
                                    assertThat(f.config.firestore(f.app)).isSameAs(f.firestore);
                                    f.clients.verify(() -> FirestoreClient.getFirestore(f.app));
                                    f.adc.verifyNoInteractions();
                                }),
                        new Case(
                                "Firestore construction failure propagated",
                                f -> {
                                    f.clients
                                            .when(() -> FirestoreClient.getFirestore(f.app))
                                            .thenThrow(
                                                    new IllegalStateException(
                                                            "synthetic store failure"));
                                    assertThatThrownBy(() -> f.config.firestore(f.app))
                                            .hasMessage("synthetic store failure");
                                }),
                        new Case(
                                "configured project initialized using application default credentials",
                                f -> {
                                    f.apps.when(
                                                    () ->
                                                            FirebaseApp.initializeApp(
                                                                    any(FirebaseOptions.class)))
                                            .thenAnswer(
                                                    i -> {
                                                        FirebaseOptions options = i.getArgument(0);
                                                        assertThat(options.getProjectId())
                                                                .isEqualTo("synthetic-project");
                                                        return f.app;
                                                    });
                                    assertThat(f.config.firebaseApp("synthetic-project"))
                                            .isSameAs(f.app);
                                    f.adc.verify(
                                            GoogleCredentials::getApplicationDefault, times(1));
                                }),
                        new Case(
                                "configuration does not refresh tokens or fetch metadata",
                                f -> {
                                    f.config.firebaseApp("synthetic-project");
                                    verify(f.credentials, never()).refresh();
                                    verify(f.credentials, never()).refreshIfExpired();
                                    verify(f.credentials, never()).getRequestMetadata();
                                    verify(f.credentials, never()).getRequestMetadata(any());
                                    f.adc.verify(
                                            GoogleCredentials::getApplicationDefault, times(1));
                                }))
                .map(FirebaseConfigValidationTest::test);
    }

    @TestFactory
    Stream<DynamicTest> integration() {
        return Stream.of(
                        new Case(
                                "Spring creates mocked app and Firestore beans",
                                f -> {
                                    try (var c = f.context()) {
                                        assertThat(c.getBean(FirebaseApp.class)).isSameAs(f.app);
                                        assertThat(c.getBean(Firestore.class))
                                                .isSameAs(f.firestore);
                                        f.apps.verify(
                                                () ->
                                                        FirebaseApp.initializeApp(
                                                                any(FirebaseOptions.class)),
                                                times(1));
                                    }
                                }),
                        new Case(
                                "Spring reuses existing app without ADC",
                                f -> {
                                    f.apps.when(FirebaseApp::getApps).thenReturn(List.of(f.app));
                                    f.apps.when(FirebaseApp::getInstance).thenReturn(f.app);
                                    try (var c = f.context()) {
                                        assertThat(c.getBean(Firestore.class))
                                                .isSameAs(f.firestore);
                                        f.adc.verifyNoInteractions();
                                    }
                                }),
                        new Case(
                                "Spring ADC failure prevents Firestore access",
                                f -> {
                                    f.adc.when(GoogleCredentials::getApplicationDefault)
                                            .thenThrow(new java.io.IOException("synthetic"));
                                    assertThatThrownBy(f::context)
                                            .isInstanceOf(
                                                    org.springframework.beans.factory
                                                            .BeanCreationException.class);
                                    f.clients.verifyNoInteractions();
                                }),
                        new Case(
                                "Spring SDK app failure prevents Firestore access",
                                f -> {
                                    f.apps.when(
                                                    () ->
                                                            FirebaseApp.initializeApp(
                                                                    any(FirebaseOptions.class)))
                                            .thenThrow(new IllegalStateException("synthetic"));
                                    assertThatThrownBy(f::context)
                                            .isInstanceOf(
                                                    org.springframework.beans.factory
                                                            .BeanCreationException.class);
                                    f.clients.verifyNoInteractions();
                                }),
                        new Case(
                                "Spring Firestore failure cannot leave a usable context",
                                f -> {
                                    f.clients
                                            .when(() -> FirestoreClient.getFirestore(f.app))
                                            .thenThrow(new IllegalStateException("synthetic"));
                                    assertThatThrownBy(f::context)
                                            .isInstanceOf(
                                                    org.springframework.beans.factory
                                                            .BeanCreationException.class);
                                }),
                        new Case(
                                "Spring named only app fails default lookup",
                                f -> {
                                    f.apps.when(FirebaseApp::getApps).thenReturn(List.of(f.app));
                                    f.apps.when(FirebaseApp::getInstance)
                                            .thenThrow(
                                                    new IllegalStateException("default missing"));
                                    assertThatThrownBy(f::context)
                                            .isInstanceOf(
                                                    org.springframework.beans.factory
                                                            .BeanCreationException.class);
                                    f.adc.verifyNoInteractions();
                                }),
                        new Case(
                                "Spring app discovery failure stops all initialization",
                                f -> {
                                    f.apps.when(FirebaseApp::getApps)
                                            .thenThrow(new IllegalStateException("synthetic"));
                                    assertThatThrownBy(f::context)
                                            .isInstanceOf(
                                                    org.springframework.beans.factory
                                                            .BeanCreationException.class);
                                    f.adc.verifyNoInteractions();
                                    f.clients.verifyNoInteractions();
                                }),
                        new Case(
                                "two Spring contexts reuse one SDK initialization",
                                f -> {
                                    f.apps.when(FirebaseApp::getApps)
                                            .thenReturn(List.of(), List.of(f.app));
                                    f.apps.when(FirebaseApp::getInstance).thenReturn(f.app);
                                    try (var first = f.context()) {
                                        assertThat(first.getBean(Firestore.class))
                                                .isSameAs(f.firestore);
                                    }
                                    try (var second = f.context()) {
                                        assertThat(second.getBean(Firestore.class))
                                                .isSameAs(f.firestore);
                                    }
                                    f.apps.verify(
                                            () ->
                                                    FirebaseApp.initializeApp(
                                                            any(FirebaseOptions.class)),
                                            times(1));
                                    f.adc.verify(
                                            GoogleCredentials::getApplicationDefault, times(1));
                                }))
                .map(FirebaseConfigValidationTest::test);
    }
}
