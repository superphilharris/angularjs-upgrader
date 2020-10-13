package org.angularjsupgrader.service.generation;

import org.angularjsupgrader.model.UpgraderProperties;
import org.angularjsupgrader.service.FileListerServiceImpl;
import org.angularjsupgrader.service.upgrader.StringServiceImpl;

import java.util.Collections;
import java.util.List;

/**
 * Created by Philip Harris on 13/10/2020
 */
public class HtmlGenerationServiceImpl {

    private final UpgraderProperties upgraderProperties;
    private final StringServiceImpl stringService;
    private final FileListerServiceImpl fileListerService;

    public HtmlGenerationServiceImpl(UpgraderProperties upgraderProperties,
                                     StringServiceImpl stringService,
                                     FileListerServiceImpl fileListerService) {
        this.upgraderProperties = upgraderProperties;
        this.stringService = stringService;
        this.fileListerService = fileListerService;
    }

    public List<String> upgradeTemplateUrl(final String templateUrl) {
        final String parsedTemplateUrl = removeResourcesPrefix(stringService.trimQuotes(removeRootVariable(templateUrl)));
        final String templateContents = fileListerService.getFileMatchingPath(parsedTemplateUrl);
        return Collections.singletonList("<ng-container *ngTemplateOutlet=\"" + parsedTemplateUrl + "\"></ng-container>");
    }

    private String removeRootVariable(final String templateUrl) {
        final String templateRootVariable = upgraderProperties.getTemplateRootVariable();
        if (templateRootVariable != null) {
            return templateUrl.replace(templateRootVariable + " + ", "");
        }
        return templateUrl;
    }

    private String removeResourcesPrefix(final String templateUrl) {
        final String templatePublicUrlRoot = upgraderProperties.getTemplatePublicUrlRoot();
        if (templatePublicUrlRoot != null) {
        }
        return templateUrl;
    }
}
