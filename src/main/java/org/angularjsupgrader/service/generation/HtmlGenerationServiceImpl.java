package org.angularjsupgrader.service.generation;

import org.angularjsupgrader.exception.UpgraderException;
import org.angularjsupgrader.model.UpgradeProperties;
import org.angularjsupgrader.service.FileListerServiceImpl;
import org.angularjsupgrader.service.upgrader.StringServiceImpl;

import java.util.Collections;
import java.util.List;

/**
 * Created by Philip Harris on 13/10/2020
 */
public class HtmlGenerationServiceImpl {

    private final UpgradeProperties upgradeProperties;
    private final StringServiceImpl stringService;
    private final FileListerServiceImpl fileListerService;

    public HtmlGenerationServiceImpl(UpgradeProperties upgradeProperties,
                                     StringServiceImpl stringService,
                                     FileListerServiceImpl fileListerService) {
        this.upgradeProperties = upgradeProperties;
        this.stringService = stringService;
        this.fileListerService = fileListerService;
    }

    public List<String> upgradeTemplateUrl(final String templateUrl) throws UpgraderException {
        final String parsedTemplateUrl = removeResourcesPrefix(stringService.trimQuotes(removeRootVariable(templateUrl)));
        final String templateContents = fileListerService.getFileMatchingPath(parsedTemplateUrl);
        return Collections.singletonList("<ng-container *ngTemplateOutlet=\"" + parsedTemplateUrl + "\"></ng-container>");
    }

    private String removeRootVariable(final String templateUrl) {
        final String templateRootVariable = upgradeProperties.getTemplateRootVariable();
        if (templateRootVariable != null) {
            return templateUrl.replace(templateRootVariable + " + ", "");
        }
        return templateUrl;
    }

    private String removeResourcesPrefix(final String templateUrl) {
        final String templatePublicUrlRoot = upgradeProperties.getTemplatePublicUrlRoot();
        if (templatePublicUrlRoot != null) {
            if (templateUrl.startsWith(templatePublicUrlRoot)) {
                return templateUrl.substring(templatePublicUrlRoot.length());
            } else if (templateUrl.startsWith("/" + templatePublicUrlRoot)) {
                return templateUrl.substring(templatePublicUrlRoot.length() + 1);
            }
        }
        return templateUrl;
    }
}
