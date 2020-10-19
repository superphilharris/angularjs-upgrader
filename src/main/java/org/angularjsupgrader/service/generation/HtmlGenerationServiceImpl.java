package org.angularjsupgrader.service.generation;

import org.angularjsupgrader.exception.UpgraderException;
import org.angularjsupgrader.model.UpgradeProperties;
import org.angularjsupgrader.service.FileListerServiceImpl;
import org.angularjsupgrader.service.upgrader.StringServiceImpl;

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

    public String upgradeTemplateUrl(final String templateUrl) throws UpgraderException {
        final String parsedTemplateUrl = upgradeProperties.getInputFolder() + removeResourcesPrefix(stringService.trimQuotes(removeRootVariable(templateUrl)));
        final String templateContents = fileListerService.getFileMatchingPath(parsedTemplateUrl);

        if (templateContents == null) {
            System.err.println("'" + parsedTemplateUrl + "' does not exist for template");
            return "<ng-container *ngTemplateOutlet=\"" + parsedTemplateUrl + "\"></ng-container>";
        } else {
            return replaceAngularJsWithAngular(templateContents);
        }
    }

    private String replaceAngularJsWithAngular(final String templateContent) {
        return templateContent
                .replace(" ng-if=", " *ngIf=")
                .replace(" ng-click=", " (click)=")
                .replace(" ng-class=", " [ngClass]=")
                .replace(" ng-disabled=", " [disabled]=")
                .replace(" ng-model=", " [(ngModel)]=")
                .replace(" ng-href=", " [href]=")
                .replace(" ng-bind-html=", " [innerHtml]=") // Both ng-bind-html and innerHtml sanitize the html by stripping out any inline styles
                .replace(" ng-mouseenter=", " (mouseenter)=")
                .replace(" ng-mouseleave=", " (mouseleave)=")
                .replace(" ng-mouseover=", " (mouseover)=")
                .replace(" ng-switch=", " [ngSwitch]=")
                .replace(" ng-switch-when=", " *ngSwitchCase=")
                .replace(" ng-switch-default", " *ngSwitchDefault")
                .replace(" ng-change=", " (change)=")
                .replace(" ng-bind=", " [textContent]=")
                .replaceAll(" ([a-zA-Z]*)=\"\\{\\{([^}]*)}}\"", " [$1]=\"$2\"");
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
