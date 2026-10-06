package com.experimentos.backend.validation;

import static org.mockito.Mockito.mock;

import com.experimentos.backend.survey.application.SurveyService;
import com.experimentos.backend.survey.interfaces.SurveyController;
import com.experimentos.backend.survey.interfaces.SurveyDtos;
import java.util.List;

class SurveyDtosValidationTest extends DtoContract {
    private final SurveyService service = mock(SurveyService.class);

    @Override
    protected Object controller() {
        return new SurveyController(service);
    }

    @Override
    protected List<Object> services() {
        return List.of(service);
    }

    @Override
    protected List<Case> cases() {
        return List.of(
                new Case(
                        "title null",
                        SurveyDtos.CreateSurveyRequest.class,
                        "POST",
                        "/api/v1/surveys",
                        "{\"title\":null,\"question\":\"How are you?\",\"type\":\"DAILY\",\"allowComments\":true}",
                        "title",
                        false),
                new Case(
                        "title whitespace",
                        SurveyDtos.CreateSurveyRequest.class,
                        "POST",
                        "/api/v1/surveys",
                        "{\"title\":\" \",\"question\":\"How are you?\",\"type\":\"DAILY\",\"allowComments\":true}",
                        "title",
                        false),
                new Case(
                        "title maximum",
                        SurveyDtos.CreateSurveyRequest.class,
                        "POST",
                        "/api/v1/surveys",
                        "{\"title\":\"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\",\"question\":\"How are you?\",\"type\":\"DAILY\",\"allowComments\":true}",
                        null,
                        false),
                new Case(
                        "title overflow",
                        SurveyDtos.CreateSurveyRequest.class,
                        "POST",
                        "/api/v1/surveys",
                        "{\"title\":\"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\",\"question\":\"How are you?\",\"type\":\"DAILY\",\"allowComments\":true}",
                        "title",
                        false),
                new Case(
                        "question null",
                        SurveyDtos.CreateSurveyRequest.class,
                        "POST",
                        "/api/v1/surveys",
                        "{\"title\":\"Wellbeing\",\"question\":null,\"type\":\"DAILY\",\"allowComments\":true}",
                        "question",
                        false),
                new Case(
                        "question blank",
                        SurveyDtos.CreateSurveyRequest.class,
                        "POST",
                        "/api/v1/surveys",
                        "{\"title\":\"Wellbeing\",\"question\":\"\\n\",\"type\":\"DAILY\",\"allowComments\":true}",
                        "question",
                        false),
                new Case(
                        "question maximum",
                        SurveyDtos.CreateSurveyRequest.class,
                        "POST",
                        "/api/v1/surveys",
                        "{\"title\":\"Wellbeing\",\"question\":\"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\",\"type\":\"DAILY\",\"allowComments\":true}",
                        null,
                        false),
                new Case(
                        "question overflow",
                        SurveyDtos.CreateSurveyRequest.class,
                        "POST",
                        "/api/v1/surveys",
                        "{\"title\":\"Wellbeing\",\"question\":\"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\",\"type\":\"DAILY\",\"allowComments\":true}",
                        "question",
                        false),
                new Case(
                        "type null",
                        SurveyDtos.CreateSurveyRequest.class,
                        "POST",
                        "/api/v1/surveys",
                        "{\"title\":\"Wellbeing\",\"question\":\"How are you?\",\"type\":null,\"allowComments\":true}",
                        "type",
                        false),
                new Case(
                        "weekly valid",
                        SurveyDtos.CreateSurveyRequest.class,
                        "POST",
                        "/api/v1/surveys",
                        "{\"title\":\"Wellbeing\",\"question\":\"How are you?\",\"type\":\"WEEKLY\",\"allowComments\":true}",
                        null,
                        false),
                new Case(
                        "daily create HTTP",
                        SurveyDtos.CreateSurveyRequest.class,
                        "POST",
                        "/api/v1/surveys",
                        "{\"title\":\"Wellbeing\",\"question\":\"How are you?\",\"type\":\"DAILY\",\"allowComments\":true}",
                        null,
                        true),
                new Case(
                        "weekly comments disabled HTTP",
                        SurveyDtos.CreateSurveyRequest.class,
                        "POST",
                        "/api/v1/surveys",
                        "{\"title\":\"Wellbeing\",\"question\":\"How are you?\",\"type\":\"WEEKLY\",\"allowComments\":false}",
                        null,
                        true),
                new Case(
                        "unknown type",
                        SurveyDtos.CreateSurveyRequest.class,
                        "POST",
                        "/api/v1/surveys",
                        "{\"title\":\"Wellbeing\",\"question\":\"How are you?\",\"type\":\"MONTHLY\",\"allowComments\":true}",
                        "binding",
                        true),
                new Case(
                        "type object",
                        SurveyDtos.CreateSurveyRequest.class,
                        "POST",
                        "/api/v1/surveys",
                        "{\"title\":\"Wellbeing\",\"question\":\"How are you?\",\"type\":{},\"allowComments\":true}",
                        "binding",
                        true),
                new Case(
                        "question array",
                        SurveyDtos.CreateSurveyRequest.class,
                        "POST",
                        "/api/v1/surveys",
                        "{\"title\":\"Wellbeing\",\"question\":[],\"type\":\"DAILY\",\"allowComments\":true}",
                        "binding",
                        true),
                new Case(
                        "answer valid",
                        SurveyDtos.AnswerRequest.class,
                        "POST",
                        "/api/v1/surveys/1/answers",
                        "{\"answerText\":\"Fine\"}",
                        null,
                        true),
                new Case(
                        "answer null",
                        SurveyDtos.AnswerRequest.class,
                        "POST",
                        "/api/v1/surveys/1/answers",
                        "{\"answerText\":null}",
                        "answerText",
                        true),
                new Case(
                        "answer blank",
                        SurveyDtos.AnswerRequest.class,
                        "POST",
                        "/api/v1/surveys/1/answers",
                        "{\"answerText\":\" \"}",
                        "answerText",
                        true),
                new Case(
                        "answer maximum",
                        SurveyDtos.AnswerRequest.class,
                        "POST",
                        "/api/v1/surveys/1/answers",
                        "{\"answerText\":\"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\"}",
                        null,
                        true),
                new Case(
                        "answer overflow",
                        SurveyDtos.AnswerRequest.class,
                        "POST",
                        "/api/v1/surveys/1/answers",
                        "{\"answerText\":\"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\"}",
                        "answerText",
                        true));
    }
}
