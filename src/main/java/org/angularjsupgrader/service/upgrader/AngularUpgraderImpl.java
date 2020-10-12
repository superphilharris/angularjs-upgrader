package org.angularjsupgrader.service.upgrader;

import org.angularjsupgrader.model.AbstractComponent;
import org.angularjsupgrader.model.JsConfig;
import org.angularjsupgrader.model.JsDirective;
import org.angularjsupgrader.model.JsRoutePage;
import org.angularjsupgrader.model.angularjs.JsFile;
import org.angularjsupgrader.model.angularjs.JsProgram;
import org.angularjsupgrader.model.typescript.TsComponent;
import org.angularjsupgrader.model.typescript.TsModule;
import org.angularjsupgrader.model.typescript.TsProgram;
import org.angularjsupgrader.model.typescript.TsRouting;

/**
 * Created by Philip Harris on 18/01/2020
 */
public class AngularUpgraderImpl {
    private final AngularModuleUpgraderImpl angularModuleUpgrader;
    private final ComponentExtractorImpl componentExtractor;
    private final AngularInjectableUpgraderImpl injectableUpgrader;
    private final StringServiceImpl stringService;
    private final AngularLocatorImpl angularLocator;

    public AngularUpgraderImpl(StringServiceImpl stringService) {
        this.stringService = stringService;
        this.angularLocator = new AngularLocatorImpl();
        this.injectableUpgrader = new AngularInjectableUpgraderImpl(angularLocator, stringService);
        this.componentExtractor = new ComponentExtractorImpl(injectableUpgrader, angularLocator, stringService);
        this.angularModuleUpgrader = new AngularModuleUpgraderImpl(injectableUpgrader, componentExtractor);
    }

    public TsProgram upgradeAngularJsProgram(JsProgram jsProgram) {
        TsProgram tsProgram = new TsProgram();

        for (JsFile jsFile : jsProgram.files) {
            this.angularModuleUpgrader.createModuleFromFile(jsFile, tsProgram);
        }

        for (TsModule tsModule : tsProgram.childModules) {
            upgradeModuleConfigAndRoute(tsModule, tsProgram);
        }

        return tsProgram;
    }

    private void upgradeModuleConfigAndRoute(TsModule tsModule, TsProgram tsProgram) {
        // TODO: here phil we have our controllers upgraded
        // so we can upgrade our route items and directives
        for (int i = 0; i < tsModule.needToUpgradeJs.configs.size(); i++) {
            tsModule.routings.add(upgradeJsConfig(tsModule.needToUpgradeJs.configs.get(i), tsModule.needToUpgradeJs.sourcedFrom, tsModule, tsProgram, i));
        }
        for (JsDirective jsDirective : tsModule.needToUpgradeJs.directives) {
            TsComponent component = upgradeJsDirective(jsDirective, tsModule, tsProgram);
        }
        for (TsModule childModule : tsModule.childModules) {
            upgradeModuleConfigAndRoute(childModule, tsProgram);
        }
    }


    private TsRouting upgradeJsConfig(JsConfig jsConfig, JsFile parentJsFile, TsModule tsModule, TsProgram tsProgram, int sequenceOfConfig) {
        TsRouting tsRouting = new TsRouting();
        tsRouting.name = tsModule.name + (sequenceOfConfig == 0 ? "" : 1 + sequenceOfConfig);
        tsRouting.sourcedFrom = jsConfig.originalInjectable.functionName + " in " + parentJsFile.filename;

        for (JsRoutePage jsRoutePage : jsConfig.pages) {
            TsComponent tsComponent = upgradeComponent(jsRoutePage, tsModule, tsProgram, getNameFromPath(jsRoutePage.path));
            tsRouting.pathToComponent.put(jsRoutePage.path, tsComponent);
        }

        return injectableUpgrader.upgradeJsInjectable(jsConfig.originalInjectable, parentJsFile, tsRouting, tsModule);
    }

    private String getNameFromPath(String path) {
        String result = stringService.trimQuotes(path.replace("/", "-"))
                .replaceAll("-$", "").replaceAll("^-", "");
        if (result.indexOf(":") > 0) {
            result = result.substring(0, result.indexOf(":"));
        }
        return result;
    }

    private TsComponent upgradeJsDirective(JsDirective jsDirective, TsModule tsModule, TsProgram tsProgram) {
        TsComponent tsComponent = upgradeComponent(jsDirective, tsModule, tsProgram, jsDirective.originalInjectable.injectableName);
        // TODO: upgrade our jsDirective-specific stuff here
        return tsComponent;
    }

    private TsComponent upgradeComponent(AbstractComponent jsComponent, TsModule tsModule, TsProgram tsProgram, String componentName) {
        // TODO: we should probably use the 'componentName' as our component name, rather than our controller name
        TsComponent tsComponent = null;
        if (jsComponent.controllerInjectedName != null) {
            tsComponent = angularLocator.findControllerComponentInSubModules(tsProgram, jsComponent.controllerInjectedName);
        }
        if (jsComponent.controllerFunctionName != null) {
            tsComponent = angularLocator.findComponentByFunctionName(tsModule, jsComponent.controllerFunctionName);
        }
        if (tsComponent == null) {
            if (jsComponent.upgradedController != null) {
                tsComponent = jsComponent.upgradedController;
            } else { // There was no controller, so lets generate one from the template name
                tsComponent = new TsComponent();
                tsComponent.name = stringService.camelToKebab(componentName);
            }
            tsModule.components.add(tsComponent);
        }

        tsComponent.template = jsComponent.template;
        tsComponent.templateUrl = jsComponent.templateUrl;
        return tsComponent;
    }


}
