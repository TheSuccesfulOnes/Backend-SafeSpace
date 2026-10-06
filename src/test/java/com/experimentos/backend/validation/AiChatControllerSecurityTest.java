package com.experimentos.backend.validation;

class AiChatControllerSecurityTest extends ControllerSecurityContract {
    @Override
    protected String path() {
        return "/api/v1/ai/conversations";
    }

    @Override
    protected Object service() {
        return ai;
    }

    @Override
    protected boolean employeeOnly() {
        return true;
    }
}
