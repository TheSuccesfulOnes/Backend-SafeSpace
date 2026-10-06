package com.experimentos.backend.validation;

import static org.mockito.Mockito.mock;

import com.experimentos.backend.mood.application.MoodService;
import com.experimentos.backend.mood.interfaces.MoodController;
import com.experimentos.backend.mood.interfaces.MoodDtos;
import java.util.List;

class MoodDtosValidationTest extends DtoContract {
    private final MoodService service = mock(MoodService.class);

    @Override
    protected Object controller() {
        return new MoodController(service);
    }

    @Override
    protected List<Object> services() {
        return List.of(service);
    }

    @Override
    protected List<Case> cases() {
        return List.of(
                new Case(
                        "VERY_BAD accepted",
                        MoodDtos.SubmitMoodRequest.class,
                        "POST",
                        "/api/v1/mood/today",
                        "{\"mood\":\"VERY_BAD\"}",
                        null,
                        false),
                new Case(
                        "BAD accepted",
                        MoodDtos.SubmitMoodRequest.class,
                        "POST",
                        "/api/v1/mood/today",
                        "{\"mood\":\"BAD\"}",
                        null,
                        false),
                new Case(
                        "GOOD accepted",
                        MoodDtos.SubmitMoodRequest.class,
                        "POST",
                        "/api/v1/mood/today",
                        "{\"mood\":\"GOOD\"}",
                        null,
                        false),
                new Case(
                        "VERY_GOOD accepted",
                        MoodDtos.SubmitMoodRequest.class,
                        "POST",
                        "/api/v1/mood/today",
                        "{\"mood\":\"VERY_GOOD\"}",
                        null,
                        false),
                new Case(
                        "null mood invalid",
                        MoodDtos.SubmitMoodRequest.class,
                        "POST",
                        "/api/v1/mood/today",
                        "{\"mood\":null}",
                        "mood",
                        false),
                new Case(
                        "missing mood invalid",
                        MoodDtos.SubmitMoodRequest.class,
                        "POST",
                        "/api/v1/mood/today",
                        "{}",
                        "mood",
                        false),
                new Case(
                        "VERY_BAD through HTTP",
                        MoodDtos.SubmitMoodRequest.class,
                        "POST",
                        "/api/v1/mood/today",
                        "{\"mood\":\"VERY_BAD\"}",
                        null,
                        true),
                new Case(
                        "BAD through HTTP",
                        MoodDtos.SubmitMoodRequest.class,
                        "POST",
                        "/api/v1/mood/today",
                        "{\"mood\":\"BAD\"}",
                        null,
                        true),
                new Case(
                        "GOOD through HTTP",
                        MoodDtos.SubmitMoodRequest.class,
                        "POST",
                        "/api/v1/mood/today",
                        "{\"mood\":\"GOOD\"}",
                        null,
                        true),
                new Case(
                        "VERY_GOOD through HTTP",
                        MoodDtos.SubmitMoodRequest.class,
                        "POST",
                        "/api/v1/mood/today",
                        "{\"mood\":\"VERY_GOOD\"}",
                        null,
                        true),
                new Case(
                        "unknown mood rejected",
                        MoodDtos.SubmitMoodRequest.class,
                        "POST",
                        "/api/v1/mood/today",
                        "{\"mood\":\"HAPPY\"}",
                        "binding",
                        true),
                new Case(
                        "lowercase mood rejected",
                        MoodDtos.SubmitMoodRequest.class,
                        "POST",
                        "/api/v1/mood/today",
                        "{\"mood\":\"good\"}",
                        "binding",
                        true),
                new Case(
                        "empty mood rejected",
                        MoodDtos.SubmitMoodRequest.class,
                        "POST",
                        "/api/v1/mood/today",
                        "{\"mood\":\"\"}",
                        "binding",
                        true),
                new Case(
                        "embedded whitespace mood rejected",
                        MoodDtos.SubmitMoodRequest.class,
                        "POST",
                        "/api/v1/mood/today",
                        "{\"mood\":\"GO OD\"}",
                        "binding",
                        true),
                new Case(
                        "object mood rejected",
                        MoodDtos.SubmitMoodRequest.class,
                        "POST",
                        "/api/v1/mood/today",
                        "{\"mood\":{}}",
                        "binding",
                        true),
                new Case(
                        "array mood rejected",
                        MoodDtos.SubmitMoodRequest.class,
                        "POST",
                        "/api/v1/mood/today",
                        "{\"mood\":[\"GOOD\"]}",
                        "binding",
                        true),
                new Case(
                        "ordinal above enum mood rejected",
                        MoodDtos.SubmitMoodRequest.class,
                        "POST",
                        "/api/v1/mood/today",
                        "{\"mood\":4}",
                        "binding",
                        true),
                new Case(
                        "negative ordinal mood rejected",
                        MoodDtos.SubmitMoodRequest.class,
                        "POST",
                        "/api/v1/mood/today",
                        "{\"mood\":-1}",
                        "binding",
                        true),
                new Case(
                        "boolean mood rejected",
                        MoodDtos.SubmitMoodRequest.class,
                        "POST",
                        "/api/v1/mood/today",
                        "{\"mood\":true}",
                        "binding",
                        true),
                new Case(
                        "empty HTTP body rejected",
                        MoodDtos.SubmitMoodRequest.class,
                        "POST",
                        "/api/v1/mood/today",
                        "",
                        "binding",
                        true));
    }
}
