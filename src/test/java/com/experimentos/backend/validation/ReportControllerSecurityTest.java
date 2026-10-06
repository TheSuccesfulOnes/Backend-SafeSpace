package com.experimentos.backend.validation;

class ReportControllerSecurityTest extends ControllerSecurityContract {
    @Override
    protected String path() {
        return "/api/v1/reports";
    }

    @Override
    protected Object service() {
        return reports;
    }

    @Override
    protected boolean employeeOnly() {
        return false;
    }
}
