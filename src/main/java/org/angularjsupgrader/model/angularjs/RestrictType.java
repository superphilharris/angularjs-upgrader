package org.angularjsupgrader.model.angularjs;

/**
 * Created by Philip Harris on 26/02/2020
 */
public enum RestrictType {
    ELEMENT("E"),
    ATTRIBUTE("A");

    private final String code;

    RestrictType(String code) {
        this.code = code;
    }

    public static RestrictType getByCode(String code) {
        for (RestrictType restrictType : RestrictType.values()) {
            if (restrictType.code.equals(code)) {
                return restrictType;
            }
        }
        return null;
    }

    public String getCode() {
        return code;
    }
}
