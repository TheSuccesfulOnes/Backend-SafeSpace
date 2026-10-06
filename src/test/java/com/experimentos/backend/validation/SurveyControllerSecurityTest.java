package com.experimentos.backend.validation;

class SurveyControllerSecurityTest extends ControllerSecurityContract {
    @Override
    protected String path() {
        return "/api/v1/surveys/managed";
    }

    @Override
    protected Object service() {
        return surveys;
    }

    @Override
    protected boolean employeeOnly() {
        return false;
    }
}
