package com.experimentos.backend.validation;

import static org.mockito.Mockito.mock;

import com.experimentos.backend.activity.application.AdminActivityService;
import com.experimentos.backend.activity.interfaces.ActivityAdminDtos;
import com.experimentos.backend.activity.interfaces.AdminActivityController;
import java.util.List;

class ActivityAdminDtosValidationTest extends DtoContract {
    private final AdminActivityService service = mock(AdminActivityService.class);

    @Override
    protected Object controller() {
        return new AdminActivityController(service);
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
                        ActivityAdminDtos.ActivityRequest.class,
                        "POST",
                        "/api/v1/admin/activities",
                        "{\"title\":null,\"description\":\"Outdoor\",\"options\":[\"Walk\",\"Yoga\"]}",
                        "title",
                        false),
                new Case(
                        "title whitespace",
                        ActivityAdminDtos.ActivityRequest.class,
                        "POST",
                        "/api/v1/admin/activities",
                        "{\"title\":\" \\t\",\"description\":\"Outdoor\",\"options\":[\"Walk\",\"Yoga\"]}",
                        "title",
                        false),
                new Case(
                        "title maximum",
                        ActivityAdminDtos.ActivityRequest.class,
                        "POST",
                        "/api/v1/admin/activities",
                        "{\"title\":\"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\",\"description\":\"Outdoor\",\"options\":[\"Walk\",\"Yoga\"]}",
                        null,
                        false),
                new Case(
                        "title overflow",
                        ActivityAdminDtos.ActivityRequest.class,
                        "POST",
                        "/api/v1/admin/activities",
                        "{\"title\":\"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\",\"description\":\"Outdoor\",\"options\":[\"Walk\",\"Yoga\"]}",
                        "title",
                        false),
                new Case(
                        "optional description absent",
                        ActivityAdminDtos.ActivityRequest.class,
                        "POST",
                        "/api/v1/admin/activities",
                        "{\"title\":\"Weekly walk\",\"description\":null,\"options\":[\"Walk\",\"Yoga\"]}",
                        null,
                        false),
                new Case(
                        "description maximum",
                        ActivityAdminDtos.ActivityRequest.class,
                        "POST",
                        "/api/v1/admin/activities",
                        "{\"title\":\"Weekly walk\",\"description\":\"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\",\"options\":[\"Walk\",\"Yoga\"]}",
                        null,
                        false),
                new Case(
                        "description overflow",
                        ActivityAdminDtos.ActivityRequest.class,
                        "POST",
                        "/api/v1/admin/activities",
                        "{\"title\":\"Weekly walk\",\"description\":\"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\",\"options\":[\"Walk\",\"Yoga\"]}",
                        "description",
                        false),
                new Case(
                        "options null",
                        ActivityAdminDtos.ActivityRequest.class,
                        "POST",
                        "/api/v1/admin/activities",
                        "{\"title\":\"Weekly walk\",\"description\":\"Outdoor\",\"options\":null}",
                        "options",
                        false),
                new Case(
                        "options empty",
                        ActivityAdminDtos.ActivityRequest.class,
                        "POST",
                        "/api/v1/admin/activities",
                        "{\"title\":\"Weekly walk\",\"description\":\"Outdoor\",\"options\":[]}",
                        "options",
                        false),
                new Case(
                        "blank nested option",
                        ActivityAdminDtos.ActivityRequest.class,
                        "POST",
                        "/api/v1/admin/activities",
                        "{\"title\":\"Weekly walk\",\"description\":\"Outdoor\",\"options\":[\"Walk\",\" \"]}",
                        "options",
                        false),
                new Case(
                        "maximum option length through HTTP",
                        ActivityAdminDtos.ActivityRequest.class,
                        "POST",
                        "/api/v1/admin/activities",
                        "{\"title\":\"Weekly walk\",\"description\":\"Outdoor\",\"options\":[\"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\"]}",
                        null,
                        true),
                new Case(
                        "oversized nested option blocked",
                        ActivityAdminDtos.ActivityRequest.class,
                        "POST",
                        "/api/v1/admin/activities",
                        "{\"title\":\"Weekly walk\",\"description\":\"Outdoor\",\"options\":[\"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\"]}",
                        "options",
                        true),
                new Case(
                        "twenty options accepted",
                        ActivityAdminDtos.ActivityRequest.class,
                        "POST",
                        "/api/v1/admin/activities",
                        "{\"title\":\"Weekly walk\",\"description\":\"Outdoor\",\"options\":[\"Walk\",\"Walk\",\"Walk\",\"Walk\",\"Walk\",\"Walk\",\"Walk\",\"Walk\",\"Walk\",\"Walk\",\"Walk\",\"Walk\",\"Walk\",\"Walk\",\"Walk\",\"Walk\",\"Walk\",\"Walk\",\"Walk\",\"Walk\"]}",
                        null,
                        true),
                new Case(
                        "twenty one options blocked",
                        ActivityAdminDtos.ActivityRequest.class,
                        "POST",
                        "/api/v1/admin/activities",
                        "{\"title\":\"Weekly walk\",\"description\":\"Outdoor\",\"options\":[\"Walk\",\"Walk\",\"Walk\",\"Walk\",\"Walk\",\"Walk\",\"Walk\",\"Walk\",\"Walk\",\"Walk\",\"Walk\",\"Walk\",\"Walk\",\"Walk\",\"Walk\",\"Walk\",\"Walk\",\"Walk\",\"Walk\",\"Walk\",\"Walk\"]}",
                        "options",
                        true),
                new Case(
                        "null nested option blocked",
                        ActivityAdminDtos.ActivityRequest.class,
                        "POST",
                        "/api/v1/admin/activities",
                        "{\"title\":\"Weekly walk\",\"description\":\"Outdoor\",\"options\":[\"Walk\",null]}",
                        "options",
                        true),
                new Case(
                        "options object rejected",
                        ActivityAdminDtos.ActivityRequest.class,
                        "POST",
                        "/api/v1/admin/activities",
                        "{\"title\":\"Weekly walk\",\"description\":\"Outdoor\",\"options\":{\"label\":\"Walk\"}}",
                        "binding",
                        true),
                new Case(
                        "valid create request",
                        ActivityAdminDtos.ActivityRequest.class,
                        "POST",
                        "/api/v1/admin/activities",
                        "{\"title\":\"Weekly walk\",\"description\":\"Outdoor\",\"options\":[\"Walk\",\"Yoga\"]}",
                        null,
                        true),
                new Case(
                        "JSON array instead of request",
                        ActivityAdminDtos.ActivityRequest.class,
                        "POST",
                        "/api/v1/admin/activities",
                        "[]",
                        "binding",
                        true),
                new Case(
                        "empty body rejected",
                        ActivityAdminDtos.ActivityRequest.class,
                        "POST",
                        "/api/v1/admin/activities",
                        "",
                        "binding",
                        true),
                new Case(
                        "missing title rejected",
                        ActivityAdminDtos.ActivityRequest.class,
                        "POST",
                        "/api/v1/admin/activities",
                        "{\"options\":[\"Walk\"]}",
                        "title",
                        true));
    }
}
