package com.experimentos.backend.ai.infrastructure;

import com.experimentos.backend.ai.domain.AiMessage;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiMessageRepository extends JpaRepository<AiMessage, Long> {
    List<AiMessage> findByConversationIdOrderByIdAsc(Long conversationId);

    List<AiMessage> findByConversationIdOrderByIdDesc(Long conversationId, Pageable pageable);

    void deleteByConversationId(Long conversationId);
}
