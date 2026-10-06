package com.experimentos.backend.validation;

import static org.mockito.Mockito.mock;

import com.experimentos.backend.survey.application.AdminSurveyService;
import com.experimentos.backend.survey.interfaces.AdminSurveyController;
import com.experimentos.backend.survey.interfaces.SurveyAdminDtos;
import java.util.List;

class SurveyAdminDtosValidationTest extends DtoContract {
    private final AdminSurveyService service = mock(AdminSurveyService.class);

    @Override
    protected Object controller() {
        return new AdminSurveyController(service);
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
                        SurveyAdminDtos.SurveyRequest.class,
                        "POST",
                        "/api/v1/admin/surveys",
                        "{\"title\":null,\"question\":\"How are you?\",\"type\":\"DAILY\",\"allowComments\":true}",
                        "title",
                        false),
                new Case(
                        "title whitespace",
                        SurveyAdminDtos.SurveyRequest.class,
                        "POST",
                        "/api/v1/admin/surveys",
                        "{\"title\":\" \",\"question\":\"How are you?\",\"type\":\"DAILY\",\"allowComments\":true}",
                        "title",
                        false),
                new Case(
                        "title maximum",
                        SurveyAdminDtos.SurveyRequest.class,
                        "POST",
                        "/api/v1/admin/surveys",
                        "{\"title\":\"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\",\"question\":\"How are you?\",\"type\":\"DAILY\",\"allowComments\":true}",
                        null,
                        false),
                new Case(
                        "title overflow",
                        SurveyAdminDtos.SurveyRequest.class,
                        "POST",
                        "/api/v1/admin/surveys",
                        "{\"title\":\"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\",\"question\":\"How are you?\",\"type\":\"DAILY\",\"allowComments\":true}",
                        "title",
                        false),
                new Case(
                        "question null",
                        SurveyAdminDtos.SurveyRequest.class,
                        "POST",
                        "/api/v1/admin/surveys",
                        "{\"title\":\"Wellbeing\",\"question\":null,\"type\":\"DAILY\",\"allowComments\":true}",
                        "question",
                        false),
                new Case(
                        "question blank",
                        SurveyAdminDtos.SurveyRequest.class,
                        "POST",
                        "/api/v1/admin/surveys",
                        "{\"title\":\"Wellbeing\",\"question\":\"\\n\",\"type\":\"DAILY\",\"allowComments\":true}",
                        "question",
                        false),
                new Case(
                        "question maximum",
                        SurveyAdminDtos.SurveyRequest.class,
                        "POST",
                        "/api/v1/admin/surveys",
                        "{\"title\":\"Wellbeing\",\"question\":\"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\",\"type\":\"DAILY\",\"allowComments\":true}",
                        null,
                        false),
                new Case(
                        "question overflow",
                        SurveyAdminDtos.SurveyRequest.class,
                        "POST",
                        "/api/v1/admin/surveys",
                        "{\"title\":\"Wellbeing\",\"question\":\"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\",\"type\":\"DAILY\",\"allowComments\":true}",
                        "question",
                        false),
                new Case(
                        "type null",
                        SurveyAdminDtos.SurveyRequest.class,
                        "POST",
                        "/api/v1/admin/surveys",
                        "{\"title\":\"Wellbeing\",\"question\":\"How are you?\",\"type\":null,\"allowComments\":true}",
                        "type",
                        false),
                new Case(
                        "weekly valid",
                        SurveyAdminDtos.SurveyRequest.class,
                        "POST",
                        "/api/v1/admin/surveys",
                        "{\"title\":\"Wellbeing\",\"question\":\"How are you?\",\"type\":\"WEEKLY\",\"allowComments\":true}",
                        null,
                        false),
                new Case(
                        "daily create HTTP",
                        SurveyAdminDtos.SurveyRequest.class,
                        "POST",
                        "/api/v1/admin/surveys",
                        "{\"title\":\"Wellbeing\",\"question\":\"How are you?\",\"type\":\"DAILY\",\"allowComments\":true}",
                        null,
                        true),
                new Case(
                        "weekly comments disabled HTTP",
                        SurveyAdminDtos.SurveyRequest.class,
                        "POST",
                        "/api/v1/admin/surveys",
                        "{\"title\":\"Wellbeing\",\"question\":\"How are you?\",\"type\":\"WEEKLY\",\"allowComments\":false}",
                        null,
                        true),
                new Case(
                        "unknown type",
                        SurveyAdminDtos.SurveyRequest.class,
                        "POST",
                        "/api/v1/admin/surveys",
                        "{\"title\":\"Wellbeing\",\"question\":\"How are you?\",\"type\":\"MONTHLY\",\"allowComments\":true}",
                        "binding",
                        true),
                new Case(
                        "type object",
                        SurveyAdminDtos.SurveyRequest.class,
                        "POST",
                        "/api/v1/admin/surveys",
                        "{\"title\":\"Wellbeing\",\"question\":\"How are you?\",\"type\":{},\"allowComments\":true}",
                        "binding",
                        true),
                new Case(
                        "question array",
                        SurveyAdminDtos.SurveyRequest.class,
                        "POST",
                        "/api/v1/admin/surveys",
                        "{\"title\":\"Wellbeing\",\"question\":[],\"type\":\"DAILY\",\"allowComments\":true}",
                        "binding",
                        true),
                new Case(
                        "missing title HTTP",
                        SurveyAdminDtos.SurveyRequest.class,
                        "POST",
                        "/api/v1/admin/surveys",
                        "{\"question\":\"How?\",\"type\":\"DAILY\"}",
                        "title",
                        true),
                new Case(
                        "missing question HTTP",
                        SurveyAdminDtos.SurveyRequest.class,
                        "POST",
                        "/api/v1/admin/surveys",
                        "{\"title\":\"Wellbeing\",\"type\":\"DAILY\"}",
                        "question",
                        true),
                new Case(
                        "missing type HTTP",
                        SurveyAdminDtos.SurveyRequest.class,
                        "POST",
                        "/api/v1/admin/surveys",
                        "{\"title\":\"Wellbeing\",\"question\":\"How?\"}",
                        "type",
                        true),
                new Case(
                        "request array HTTP",
                        SurveyAdminDtos.SurveyRequest.class,
                        "POST",
                        "/api/v1/admin/surveys",
                        "[]",
                        "binding",
                        true),
                new Case(
                        "request empty HTTP",
                        SurveyAdminDtos.SurveyRequest.class,
                        "POST",
                        "/api/v1/admin/surveys",
                        "",
                        "binding",
                        true));
    }
}
