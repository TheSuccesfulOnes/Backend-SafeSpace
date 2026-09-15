package com.experimentos.backend.ai.infrastructure;

import com.experimentos.backend.ai.domain.AiMessage;
import com.experimentos.backend.shared.infrastructure.firebase.repositories.AbstractFirestoreRepository;
import com.google.cloud.firestore.Firestore;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public class AiMessageRepository extends AbstractFirestoreRepository<AiMessage, Long> {
    public AiMessageRepository(Firestore firestore) {
        super(firestore, AiMessage.class, "ai_messages");
    }

    public List<AiMessage> findByConversationIdOrderByIdAsc(Long conversationId) {
        return readAll().stream().filter(message -> belongsTo(message, conversationId)).toList();
    }

    public List<AiMessage> findByConversationIdOrderByIdDesc(
            Long conversationId, Pageable pageable) {
        return readAll().stream()
                .filter(message -> belongsTo(message, conversationId))
                .toList()
                .reversed()
                .stream()
                .limit(pageable.getPageSize())
                .toList();
    }

    public void deleteByConversationId(Long conversationId) {
        findByConversationIdOrderByIdAsc(conversationId).forEach(this::delete);
    }

    private boolean belongsTo(AiMessage message, Long conversationId) {
        Object conversation = readField(message, "conversation");
        return conversation != null && conversationId.equals(readField(conversation, "id"));
    }
}
