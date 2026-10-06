package com.experimentos.backend.validation;

class ActivityControllerSecurityTest extends ControllerSecurityContract {
    @Override
    protected String path() {
        return "/api/v1/activities/managed";
    }

    @Override
    protected Object service() {
        return activities;
    }

    @Override
    protected boolean employeeOnly() {
        return false;
    }
}
