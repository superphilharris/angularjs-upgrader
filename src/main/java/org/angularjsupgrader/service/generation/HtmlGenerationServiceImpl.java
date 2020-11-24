package org.angularjsupgrader.service.generation;

import org.angularjsupgrader.exception.UpgraderException;
import org.angularjsupgrader.model.UpgradeProperties;
import org.angularjsupgrader.service.FileListerServiceImpl;
import org.angularjsupgrader.service.upgrader.StringServiceImpl;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
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
            return "<!-- UPGRADE ERROR: Could not find file for path:'" + templateUrlWithRootRemoved + "'.\nPossibly it is embedded inside another file with the syntax:\n <script type=\"text/ng-template\" id=\"" + templateUrlWithRootRemoved + "\"...\n-->";
        } else {
            return upgradeTemplate(templateContents);
        }
    }

    public String upgradeInlineTemplate(String inlineTemplate) {
        if (inlineTemplate.length() <= 2) {
            return "";
        }
        String templateWithQuotesRemoved = inlineTemplate.replace("' + '", "");
        // Remove starting and ending quotes
        return upgradeTemplate(
                templateWithQuotesRemoved.substring(1, templateWithQuotesRemoved.length() - 1)
        );
    }

    private String upgradeTemplate(String templateContents) {
        final String upgradedTemplate = replaceAngularJsWithAngular(templateContents);
        final List<String> errors = new LinkedList<>();
        for (Map.Entry<String, String> oldToNewEntry : getOldToNewAttributeWarnings().entrySet()) {
            if (upgradedTemplate.contains(oldToNewEntry.getKey())) {
                errors.add("<!-- UPGRADE ERROR: Found '" + oldToNewEntry.getKey() + "'. " + oldToNewEntry.getValue() + "-->\n");
            }
        }
        return String.join("", errors) + upgradedTemplate;
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
                .replace(" ng-value=", " [value]=")
                .replace(" ng-checked=", " [checked]=")
                .replace(" ng-blur=", " (blur)=")
                .replace(" ng-minlength=", " [minlength]=")
                .replace(" ng-bind=", " [textContent]=")
                .replace(" ng-focus=", " (focus)=")
                .replaceAll(" ng-repeat=\"([a-zA-Z]*) in ", " *ngFor=\"let $1 of ")
                .replaceAll(" ([a-zA-Z]*)=\"\\{\\{([^}]*)}}\"", " [$1]=\"$2\"");
    }

    private String replaceAttribute(String template, final String oldAttribute, final String newAttribute) {
        return template.replaceAll("(\\s)" + oldAttribute + "=", "$1" + newAttribute + "=");
    }

    private Map<String, String> getOldToNewAttributeWarnings() {
        final Map<String, String> oldToWarnings = new HashMap<>();
        oldToWarnings.put("ng-options", "Use *ngFor with looping through option elements");
        //oldToWarnings.put("ng-show", "*ngIf except ngIf doesn't initialize embedded components, whereas ng-show does");
        //oldToWarnings.put("ng-hide", "*ngIf=\"!...\", except that ngIf doesn't initialize embedded components, whereas ng-hide does");
        oldToWarnings.put("ng-include", "Please create components for included files");
        oldToWarnings.put("ng-transclude", "Use <ng-container></ng-container> or separate component with element (not attribute) selector");
        oldToWarnings.put("ng-controller", "Copy the embedded html into the controller's upgraded component and create inputs for any variables bound to the parent controller's vm");
        oldToWarnings.put("ng-repeat=\"(", "Use *ngFor=\"... | keyvalue\" syntax");
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
