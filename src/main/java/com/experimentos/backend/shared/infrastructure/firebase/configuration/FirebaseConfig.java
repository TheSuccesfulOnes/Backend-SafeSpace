package com.experimentos.backend.shared.infrastructure.firebase.configuration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import com.google.cloud.firestore.Firestore;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Configures the Firebase Admin SDK using Application Default Credentials. */
@Configuration
public class FirebaseConfig {

    @Bean
    FirebaseApp firebaseApp(@Value("${firebase.project-id}") String projectId) {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        try {
            FirebaseOptions options =
                    FirebaseOptions.builder()
                            .setCredentials(GoogleCredentials.getApplicationDefault())
                            .setProjectId(projectId)
                            .build();
            return FirebaseApp.initializeApp(options);
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException(
                    "Firebase credentials are not configured. Set GOOGLE_APPLICATION_CREDENTIALS "
                            + "or configure Application Default Credentials.",
                    exception);
        }
    }

    @Bean
    Firestore firestore(FirebaseApp firebaseApp) {
        return FirestoreClient.getFirestore(firebaseApp);
    }
}
