package org.angularjsupgrader.model;

import java.util.Properties;

/**
 * Created by Philip Harris on 13/10/2020
 */
public class UpgraderProperties {

    private final Properties properties;

    public UpgraderProperties(Properties properties) {
        this.properties = properties;
    }

    public String getTemplateRootVariable() {
        return this.properties.getProperty("template.root-variable");
    }
}
