package org.angularjsupgrader.service.generation;

import org.angularjsupgrader.model.UpgraderProperties;
import org.angularjsupgrader.service.upgrader.StringServiceImpl;

import java.util.Collections;
import java.util.List;

/**
 * Created by Philip Harris on 13/10/2020
 */
public class HtmlGenerationServiceImpl {

    private final UpgraderProperties upgraderProperties;
    private final StringServiceImpl stringService;

    public HtmlGenerationServiceImpl(UpgraderProperties upgraderProperties,
                                     StringServiceImpl stringService) {
        this.upgraderProperties = upgraderProperties;
        this.stringService = stringService;
    }

    public List<String> upgradeTemplateUrl(final String templateUrl) {
        final String parsedTemplateUrl = stringService.trimQuotes(removeRootVariable(templateUrl));
        return Collections.singletonList("<ng-container *ngTemplateOutlet=\"" + parsedTemplateUrl + "\"></ng-container>");
    }

    private String removeRootVariable(final String templateUrl) {
        final String templateRootVariable = upgraderProperties.getTemplateRootVariable();
        if (templateRootVariable != null) {
            return templateUrl.replace(templateRootVariable + " + ", "");
        }
        return templateUrl;
    }
}
