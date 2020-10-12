package org.angularjsupgrader.service.generation;

import org.angularjsupgrader.model.UpgraderProperties;

import java.util.Collections;
import java.util.List;

/**
 * Created by Philip Harris on 13/10/2020
 */
public class HtmlGenerationServiceImpl {

    private final UpgraderProperties upgraderProperties;

    public HtmlGenerationServiceImpl(UpgraderProperties upgraderProperties) {
        this.upgraderProperties = upgraderProperties;
    }

    public List<String> upgradeTemplateUrl(final String templateUrl) {
        return Collections.singletonList("<ng-container *ngTemplateOutlet=\"" + removeRootVariable(templateUrl) + "\"></ng-container>");
    }

    private String removeRootVariable(final String templateUrl) {
        final String templateRootVariable = upgraderProperties.getTemplateRootVariable();
        if (templateRootVariable != null) {
            return templateUrl.replace(templateRootVariable, "");
        }
        return templateUrl;
    }
}
