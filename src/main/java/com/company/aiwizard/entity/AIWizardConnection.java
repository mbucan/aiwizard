package com.company.aiwizard.entity;

import io.jmix.core.metamodel.datatype.EnumClass;

import org.springframework.lang.Nullable;


public enum AIWizardConnection implements EnumClass<String> {

    GEMINI("GEMINI"),
    OPENAI("OPENAI");

    private final String id;

    AIWizardConnection(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @Nullable
    public static AIWizardConnection fromId(String id) {
        for (AIWizardConnection at : AIWizardConnection.values()) {
            if (at.getId().equals(id)) {
                return at;
            }
        }
        return null;
    }
}