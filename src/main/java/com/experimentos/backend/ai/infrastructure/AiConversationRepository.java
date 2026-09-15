package com.experimentos.backend.ai.infrastructure;

import com.experimentos.backend.ai.domain.AiConversation;
import com.experimentos.backend.shared.infrastructure.firebase.repositories.AbstractFirestoreRepository;
import com.google.cloud.firestore.Firestore;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class AiConversationRepository extends AbstractFirestoreRepository<AiConversation, Long> {
    public AiConversationRepository(Firestore firestore) {
        super(firestore, AiConversation.class, "ai_conversations");
    }

    public List<AiConversation> findByUserIdOrderByIdDesc(Long userId) {
        return readAll().stream()
                .filter(
                        conversation -> {
                            Object user = readField(conversation, "user");
                            return user != null && userId.equals(readField(user, "id"));
                        })
                .toList()
                .reversed();
    }

    public Optional<AiConversation> findByIdAndUserId(Long id, Long userId) {
        return findById(id)
                .filter(
                        conversation -> {
                            Object user = readField(conversation, "user");
                            return user != null && userId.equals(readField(user, "id"));
                        });
    }
}
