package com.experimentos.backend.ai.infrastructure;

import com.experimentos.backend.ai.domain.AiConversation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiConversationRepository extends JpaRepository<AiConversation, Long> {
    List<AiConversation> findByUserIdOrderByIdDesc(Long userId);

    Optional<AiConversation> findByIdAndUserId(Long id, Long userId);
}
