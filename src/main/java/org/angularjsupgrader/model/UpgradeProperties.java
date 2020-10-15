package org.angularjsupgrader.model;

import java.util.Properties;

/**
 * Created by Philip Harris on 13/10/2020
 */
public class UpgradeProperties {

    public static final String TEMPLATE_PUBLIC_URL_ROOT_PROPERTY = "template.public-url-root";

    private final Properties properties;

    public UpgradeProperties(Properties properties) {
        this.properties = properties;
    }

    public String getTemplateRootVariable() {
        return this.properties.getProperty("template.root-variable");
    }

    public String getInputFolder() {
        return getPropertyOrDefault("input.folder", "angularJsToUpgrade/");
    }

    public String getTemplatePublicUrlRoot() {
        return this.properties.getProperty(TEMPLATE_PUBLIC_URL_ROOT_PROPERTY);
    }

    private String getPropertyOrDefault(String propertyName, String defaultValue) {
        final String propertyValue = this.properties.getProperty(propertyName);
        if (propertyValue == null) {
            return defaultValue;
        }
        return propertyValue;
    }
}
