package org.angularjsupgrader.service.generation;

import org.angularjsupgrader.exception.UpgraderException;
import org.angularjsupgrader.model.UpgradeProperties;
import org.angularjsupgrader.service.FileListerServiceImpl;
import org.angularjsupgrader.service.upgrader.StringServiceImpl;

import java.util.HashMap;
import java.util.Map;

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
        final String templateUrlWithRootRemoved = stringService.trimQuotes(removeRootVariable(templateUrl));
        final String parsedTemplateUrl = upgradeProperties.getInputFolder() + removeResourcesPrefix(templateUrlWithRootRemoved);
        final String templateContents = fileListerService.getFileMatchingPath(parsedTemplateUrl);

        if (templateContents == null) {
            System.err.println("'" + parsedTemplateUrl + "' does not exist for template");
            return "<!-- UPGRADE ERROR: Could not find file for path:'" + templateUrlWithRootRemoved + "'.\nPossibly it is embedded inside another file with the syntax:\n <script type=\"text/ng-template\" id=\"" + templateUrlWithRootRemoved + "\"...\n-->";
        } else {
            return replaceAngularJsWithAngular(templateContents);
        }
    }

    private String replaceAngularJsWithAngular(String templateContent) {
        templateContent = replaceAttribute(templateContent, "ng\\-if", "*ngIf");
        templateContent = replaceAttribute(templateContent, "ng\\-show", "*ngIf");
        templateContent = replaceAttribute(templateContent, "ng\\-click", "(click)");
        templateContent = replaceAttribute(templateContent, "ng\\-class", "[ngClass]");
        templateContent = replaceAttribute(templateContent, "ng\\-style", "[ngStyle]");
        templateContent = replaceAttribute(templateContent, "ng\\-disabled", "[disabled]");
        templateContent = replaceAttribute(templateContent, "ng\\-model", "[(ngModel)]");

        return templateContent
                .replace(" ng-hide=\"!", " *ngIf=\"")
                .replace(" ng-hide=\"", " *ngIf=\"!")
                .replace(" ng-src=", " src=")
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
                .replaceAll(" ng-repeat=\"([a-zA-Z]*) in ", " *ngFor=\"let $1 of ")
                .replaceAll(" ([a-zA-Z]*)=\"\\{\\{([^}]*)}}\"", " [$1]=\"$2\"");
    }

    private String replaceAttribute(String template, final String oldAttribute, final String newAttribute) {
        return template.replaceAll("(\\s)" + oldAttribute + "=", "$1" + newAttribute + "=");
    }

    private Map<String, String> getOldToNewAttributeWarnings() {
        final Map<String, String> oldToWarnings = new HashMap<>();
        oldToWarnings.put("ng-options", "*ngFor with looping through option elements");
        //oldToWarnings.put("ng-show", "*ngIf except ngIf doesn't initialize embedded components, whereas ng-show does");
        //oldToWarnings.put("ng-hide", "*ngIf=\"!...\", except that ngIf doesn't initialize embedded components, whereas ng-hide does");
        oldToWarnings.put("ng-include", "need to create components for included files");
        oldToWarnings.put("ng-transclude", "use <ng-container></ng-container> or separate component with element (not attribute) selector");
        return oldToWarnings;
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
