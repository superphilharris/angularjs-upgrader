package org.angularjsupgrader.service.upgrader;

import com.google.common.base.CaseFormat;
import org.angularjsupgrader.model.angularjs.InjectableType;
import org.angularjsupgrader.model.angularjs.JsFile;
import org.angularjsupgrader.model.angularjs.JsInjectable;
import org.angularjsupgrader.model.angularjs.JsModule;
import org.angularjsupgrader.model.typescript.AbstractTsModule;
import org.angularjsupgrader.model.typescript.TsModule;
import org.angularjsupgrader.model.typescript.TsProgram;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Philip Harris on 9/03/2020
 */
public class AngularModuleUpgraderImpl {
    private final AngularInjectableUpgraderImpl injectableUpgrader;
    private final ComponentExtractorImpl componentExtractor;

    public AngularModuleUpgraderImpl(
            AngularInjectableUpgraderImpl angularInjectableUpgrader,
            ComponentExtractorImpl componentExtractor) {
        this.injectableUpgrader = angularInjectableUpgrader;
        this.componentExtractor = componentExtractor;
    }

    public void createModuleFromFile(JsFile jsFile, TsProgram tsProgram) {
        int position = 0;
        List<String> modulePath = getModulePathFromFilename(removeDotJsFromEnd(jsFile.filename));
        TsModule tsModule = getOrCreateModuleFromPath(modulePath, tsProgram, position);

        for (JsModule jsModule : jsFile.modules.values()) {
            tsModule.childModules.add(upgradeJsModule(jsModule, jsFile, tsModule));
        }
    }

    private TsModule getOrCreateModuleFromPath(List<String> modulePath, AbstractTsModule currentModuleNode, int position) {
        if (position >= modulePath.size()) return (TsModule) currentModuleNode;
        String nextModuleName = modulePath.get(position);

        // Get the child if they exist
        for (TsModule childTsModule : currentModuleNode.childModules) {
            if (childTsModule.name.equals(nextModuleName)) {
                return getOrCreateModuleFromPath(modulePath, childTsModule, position + 1);
            }
        }

        // Create new module
        TsModule newModule = new TsModule();
        newModule.name = nextModuleName;
        newModule.sourcedFrom = "the filepath " + String.join("/", modulePath.subList(0, position + 1));
        newModule.parent = currentModuleNode;
        currentModuleNode.childModules.add(newModule);
        return getOrCreateModuleFromPath(modulePath, newModule, position + 1);
    }

    private List<String> getModulePathFromFilename(String filename) {
        String[] filePathParts = filename.split("/");

        List<String> modulePath = new LinkedList<>();
        for (String filePathPart : filePathParts) {
            modulePath.add(camelToKebab(filePathPart));
        }
        return modulePath;
    }

    private TsModule upgradeJsModule(JsModule jsModule, JsFile parentJsFile, TsModule parentTsModule) {
        TsModule tsModule = new TsModule();
        tsModule.name = camelToKebab(jsModule.name.replace(".", "-").replace("/", "-"));
        tsModule.sourcedFrom = jsModule.sourcedFrom;
        tsModule.parent = parentTsModule;

        for (JsInjectable jsController : getType(jsModule, InjectableType.CONTROLLER)) {
            tsModule.components.add(injectableUpgrader.upgradeJsController(jsController, parentJsFile, tsModule));
        }
        for (JsInjectable jsService : getType(jsModule, InjectableType.SERVICE)) {
            tsModule.services.add(injectableUpgrader.upgradeJsService(jsService, parentJsFile, tsModule));
        }
        for (JsInjectable jsFactory : getType(jsModule, InjectableType.FACTORY)) {
            tsModule.services.add(injectableUpgrader.upgradeJsService(jsFactory, parentJsFile, tsModule));
        }

        // Stash this js until we have upgraded all the controllers / services first as these are referenced by our routes and directives
        tsModule.needToUpgradeJs.sourcedFrom = parentJsFile;
        tsModule.needToUpgradeJs.directives = getType(jsModule, InjectableType.DIRECTIVE).stream()
                .map(jsInjectable -> componentExtractor.extractJsDirective(jsInjectable, parentJsFile, tsModule))
                .collect(Collectors.toList());
        tsModule.needToUpgradeJs.configs = getType(jsModule, InjectableType.CONFIG).stream()
                .map(jsConfig -> componentExtractor.extractPageComponents(jsConfig, parentJsFile, tsModule))
                .collect(Collectors.toList());

        return tsModule;
    }

    private List<JsInjectable> getType(JsModule jsModule, InjectableType type) {
        return jsModule.injectables.stream()
                .filter((jsInjectable -> type.equals(jsInjectable.type)))
                .collect(Collectors.toList());
    }

    private String removeDotJsFromEnd(String filenameWithDotJs) {
        if (filenameWithDotJs.length() > 3 && filenameWithDotJs.substring(filenameWithDotJs.length() - 3).equals(".js")) {
            return filenameWithDotJs.substring(0, filenameWithDotJs.length() - 3);
        }
        return filenameWithDotJs;
    }

    private String camelToKebab(String camelCase) {
        return CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, camelCase);
    }
}
