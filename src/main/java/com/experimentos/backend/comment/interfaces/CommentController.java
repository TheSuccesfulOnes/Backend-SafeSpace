package com.experimentos.backend.comment.interfaces;

import com.experimentos.backend.comment.application.CommentService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/surveys/{surveyId}/comments")
public class CommentController {
    private final CommentService service;

    public CommentController(CommentService service) {
        this.service = service;
    }

    @GetMapping
    public List<CommentDtos.CommentResponse> list(@PathVariable Long surveyId) {
        return service.list(surveyId);
    }

    @PostMapping
    public CommentDtos.CommentResponse create(
            @PathVariable Long surveyId,
            @Valid @RequestBody CommentDtos.CreateCommentRequest request) {
        return service.create(surveyId, request);
    }

    @PostMapping("/{commentId}/like")
    public void like(@PathVariable Long commentId) {
        service.like(commentId);
    }
}
