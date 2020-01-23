package org.angularjsupgrader.model.angularjs;

/**
 * Created by Philip Harris on 18/01/2020
 */
public enum InjectableType {
    CONTROLLER("controller"),
    DIRECTIVE("directive"),
    SERVICE("service"),
    FACTORY("factory"),
    CONFIG("config");


    private final String identifier;

    InjectableType(String identifier) {
        this.identifier = identifier;
    }

    public static InjectableType getByIdentifier(String identifier) {
        for (InjectableType type : InjectableType.values()) {
            if (type.getIdentifier().equals(identifier)) return type;
        }
        return null;
    }

    public String getIdentifier() {
        return identifier;
    }
}
