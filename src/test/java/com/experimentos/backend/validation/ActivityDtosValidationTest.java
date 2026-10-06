package com.experimentos.backend.validation;

import static org.mockito.Mockito.mock;

import com.experimentos.backend.activity.application.ActivityService;
import com.experimentos.backend.activity.interfaces.ActivityController;
import com.experimentos.backend.activity.interfaces.ActivityDtos;
import java.util.List;

class ActivityDtosValidationTest extends DtoContract {
    private final ActivityService service = mock(ActivityService.class);

    @Override
    protected Object controller() {
        return new ActivityController(service);
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
                        ActivityDtos.CreateActivityRequest.class,
                        "POST",
                        "/api/v1/activities",
                        "{\"title\":null,\"description\":\"Outdoor\",\"options\":[\"Walk\",\"Yoga\"]}",
                        "title",
                        false),
                new Case(
                        "title whitespace",
                        ActivityDtos.CreateActivityRequest.class,
                        "POST",
                        "/api/v1/activities",
                        "{\"title\":\" \\t\",\"description\":\"Outdoor\",\"options\":[\"Walk\",\"Yoga\"]}",
                        "title",
                        false),
                new Case(
                        "title maximum",
                        ActivityDtos.CreateActivityRequest.class,
                        "POST",
                        "/api/v1/activities",
                        "{\"title\":\"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\",\"description\":\"Outdoor\",\"options\":[\"Walk\",\"Yoga\"]}",
                        null,
                        false),
                new Case(
                        "title overflow",
                        ActivityDtos.CreateActivityRequest.class,
                        "POST",
                        "/api/v1/activities",
                        "{\"title\":\"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\",\"description\":\"Outdoor\",\"options\":[\"Walk\",\"Yoga\"]}",
                        "title",
                        false),
                new Case(
                        "optional description absent",
                        ActivityDtos.CreateActivityRequest.class,
                        "POST",
                        "/api/v1/activities",
                        "{\"title\":\"Weekly walk\",\"description\":null,\"options\":[\"Walk\",\"Yoga\"]}",
                        null,
                        false),
                new Case(
                        "description maximum",
                        ActivityDtos.CreateActivityRequest.class,
                        "POST",
                        "/api/v1/activities",
                        "{\"title\":\"Weekly walk\",\"description\":\"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\",\"options\":[\"Walk\",\"Yoga\"]}",
                        null,
                        false),
                new Case(
                        "description overflow",
                        ActivityDtos.CreateActivityRequest.class,
                        "POST",
                        "/api/v1/activities",
                        "{\"title\":\"Weekly walk\",\"description\":\"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\",\"options\":[\"Walk\",\"Yoga\"]}",
                        "description",
                        false),
                new Case(
                        "options null",
                        ActivityDtos.CreateActivityRequest.class,
                        "POST",
                        "/api/v1/activities",
                        "{\"title\":\"Weekly walk\",\"description\":\"Outdoor\",\"options\":null}",
                        "options",
                        false),
                new Case(
                        "options empty",
                        ActivityDtos.CreateActivityRequest.class,
                        "POST",
                        "/api/v1/activities",
                        "{\"title\":\"Weekly walk\",\"description\":\"Outdoor\",\"options\":[]}",
                        "options",
                        false),
                new Case(
                        "blank nested option",
                        ActivityDtos.CreateActivityRequest.class,
                        "POST",
                        "/api/v1/activities",
                        "{\"title\":\"Weekly walk\",\"description\":\"Outdoor\",\"options\":[\"Walk\",\" \"]}",
                        "options",
                        false),
                new Case(
                        "maximum option length through HTTP",
                        ActivityDtos.CreateActivityRequest.class,
                        "POST",
                        "/api/v1/activities",
                        "{\"title\":\"Weekly walk\",\"description\":\"Outdoor\",\"options\":[\"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\"]}",
                        null,
                        true),
                new Case(
                        "oversized nested option blocked",
                        ActivityDtos.CreateActivityRequest.class,
                        "POST",
                        "/api/v1/activities",
                        "{\"title\":\"Weekly walk\",\"description\":\"Outdoor\",\"options\":[\"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\"]}",
                        "options",
                        true),
                new Case(
                        "twenty options accepted",
                        ActivityDtos.CreateActivityRequest.class,
                        "POST",
                        "/api/v1/activities",
                        "{\"title\":\"Weekly walk\",\"description\":\"Outdoor\",\"options\":[\"Walk\",\"Walk\",\"Walk\",\"Walk\",\"Walk\",\"Walk\",\"Walk\",\"Walk\",\"Walk\",\"Walk\",\"Walk\",\"Walk\",\"Walk\",\"Walk\",\"Walk\",\"Walk\",\"Walk\",\"Walk\",\"Walk\",\"Walk\"]}",
                        null,
                        true),
                new Case(
                        "twenty one options blocked",
                        ActivityDtos.CreateActivityRequest.class,
                        "POST",
                        "/api/v1/activities",
                        "{\"title\":\"Weekly walk\",\"description\":\"Outdoor\",\"options\":[\"Walk\",\"Walk\",\"Walk\",\"Walk\",\"Walk\",\"Walk\",\"Walk\",\"Walk\",\"Walk\",\"Walk\",\"Walk\",\"Walk\",\"Walk\",\"Walk\",\"Walk\",\"Walk\",\"Walk\",\"Walk\",\"Walk\",\"Walk\",\"Walk\"]}",
                        "options",
                        true),
                new Case(
                        "null nested option blocked",
                        ActivityDtos.CreateActivityRequest.class,
                        "POST",
                        "/api/v1/activities",
                        "{\"title\":\"Weekly walk\",\"description\":\"Outdoor\",\"options\":[\"Walk\",null]}",
                        "options",
                        true),
                new Case(
                        "options object rejected",
                        ActivityDtos.CreateActivityRequest.class,
                        "POST",
                        "/api/v1/activities",
                        "{\"title\":\"Weekly walk\",\"description\":\"Outdoor\",\"options\":{\"label\":\"Walk\"}}",
                        "binding",
                        true),
                new Case(
                        "valid create request",
                        ActivityDtos.CreateActivityRequest.class,
                        "POST",
                        "/api/v1/activities",
                        "{\"title\":\"Weekly walk\",\"description\":\"Outdoor\",\"options\":[\"Walk\",\"Yoga\"]}",
                        null,
                        true),
                new Case(
                        "JSON array instead of request",
                        ActivityDtos.CreateActivityRequest.class,
                        "POST",
                        "/api/v1/activities",
                        "[]",
                        "binding",
                        true),
                new Case(
                        "vote missing option rejected",
                        ActivityDtos.VoteRequest.class,
                        "POST",
                        "/api/v1/activities/1/votes",
                        "{}",
                        "optionId",
                        true),
                new Case(
                        "vote numeric option accepted",
                        ActivityDtos.VoteRequest.class,
                        "POST",
                        "/api/v1/activities/1/votes",
                        "{\"optionId\":1}",
                        null,
                        true));
    }
}
