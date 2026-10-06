package com.experimentos.backend.validation;

class MoodControllerSecurityTest extends ControllerSecurityContract {
    @Override
    protected String path() {
        return "/api/v1/mood/summary";
    }

    @Override
    protected Object service() {
        return moods;
    }

    @Override
    protected boolean employeeOnly() {
        return false;
    }
}
