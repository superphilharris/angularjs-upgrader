package org.angularjsupgrader.service;

/**
 * Created by Philip Harris on 10/03/2020
 */
public enum TsClassType {
    COMPONENT(".component"),
    SERVICE(".service");

    private final String fileSuffix;

    TsClassType(String fileSuffix) {
        this.fileSuffix = fileSuffix;
    }

    public String getFileSuffix() {
        return this.fileSuffix;
    }
}
