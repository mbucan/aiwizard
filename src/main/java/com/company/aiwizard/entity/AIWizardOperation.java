package com.company.aiwizard.entity;

import io.jmix.core.metamodel.datatype.EnumClass;

import org.springframework.lang.Nullable;


public enum AIWizardOperation implements EnumClass<String> {

    CREATE("CREATE"),
    MODIFY("MODIFY");

    private final String id;

    AIWizardOperation(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @Nullable
    public static AIWizardOperation fromId(String id) {
        for (AIWizardOperation at : AIWizardOperation.values()) {
            if (at.getId().equals(id)) {
                return at;
            }
        }
        return null;
    }
}