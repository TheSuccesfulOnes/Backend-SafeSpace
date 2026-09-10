package com.experimentos.backend.ai.interfaces;

import com.experimentos.backend.ai.application.AiChatService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai")
@PreAuthorize("hasRole('EMPLOYEE')")
public class AiChatController {
    private final AiChatService service;

    public AiChatController(AiChatService service) {
        this.service = service;
    }

    @PostMapping("/conversations")
    public AiDtos.ConversationResponse createConversation() {
        return service.createConversation();
    }

    @GetMapping("/conversations")
    public List<AiDtos.ConversationResponse> conversations() {
        return service.conversations();
    }

    @PatchMapping("/conversations/{id}")
    public AiDtos.ConversationResponse renameConversation(
            @PathVariable Long id, @Valid @RequestBody AiDtos.UpdateConversationRequest request) {
        return service.renameConversation(id, request);
    }

    @DeleteMapping("/conversations/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteConversation(@PathVariable Long id) {
        service.deleteConversation(id);
    }

    @GetMapping("/conversations/{id}/messages")
    public List<AiDtos.MessageResponse> history(@PathVariable Long id) {
        return service.history(id);
    }

    @PostMapping("/conversations/{id}/messages")
    public List<AiDtos.MessageResponse> messages(
            @PathVariable Long id, @Valid @RequestBody AiDtos.SendMessageRequest request) {
        return service.messages(id, request);
    }
}
