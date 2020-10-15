package org.angularjsupgrader.service;

import org.angularjsupgrader.exception.UpgraderException;
import org.angularjsupgrader.model.UpgradeProperties;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by Philip Harris on 13/10/2020
 */
public class PropertiesLoaderImpl {

    private UpgradeProperties properties = null;

    public PropertiesLoaderImpl() {

    }


    public UpgradeProperties getProperties() throws UpgraderException {
        if (this.properties != null) {
            return this.properties;
        }

        try (InputStream is = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            final Properties properties = new Properties();
            if (is != null) {
                properties.load(is);
            } else {
                System.out.println("WARN: no 'application.properties' file in the resources folder");
            }
            this.properties = new UpgradeProperties(properties);
            return this.properties;
        } catch (IOException e) {
            throw new UpgraderException(e);
        }
    }
}
