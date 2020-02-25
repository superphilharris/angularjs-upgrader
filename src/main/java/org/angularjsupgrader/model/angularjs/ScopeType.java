package org.angularjsupgrader.model.angularjs;

/**
 * Created by Philip Harris on 26/02/2020
 */
public enum ScopeType {
    INPUT("@"),
    INOUT("=");

    private final String code;

    ScopeType(String code) {
        this.code = code;
    }

    public static ScopeType getByCode(String code) {
        for (ScopeType scopeType : ScopeType.values()) {
            if (scopeType.code.equals(code)) {
                return scopeType;
            }
        }
        return null;
    }

    public String getCode() {
        return code;
    }
}
