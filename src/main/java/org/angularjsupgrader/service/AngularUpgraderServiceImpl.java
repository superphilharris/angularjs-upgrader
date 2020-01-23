package org.angularjsupgrader.service;

import com.google.common.base.CaseFormat;
import org.angularjsupgrader.model.angularjs.*;
import org.angularjsupgrader.model.typescript.*;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by Philip Harris on 18/01/2020
 */
public class AngularUpgraderServiceImpl {

    public TsProgram upgradeAngularJsProgram(JsProgram jsProgram) {
        TsProgram tsProgram = new TsProgram();

        for (JsFile jsFile : jsProgram.files) {
            List<String> modulePath = getModulePathFromFilename(removeDotJsFromEnd(jsFile.filename));
            TsModule tsModule = getOrCreateModuleFromPath(modulePath, tsProgram);

            for (JsModule jsModule : jsFile.modules.values()) {
                tsModule.childModules.add(upgradeJsModule(jsModule, jsFile));
            }
        }

        System.out.println("Upgraded: " + tsProgram);
        return tsProgram;
    }

    private TsModule getOrCreateModuleFromPath(List<String> modulePath, AbstractTsModule currentModuleNode) {
        if (modulePath.size() == 0) return (TsModule) currentModuleNode;
        String nextModuleName = modulePath.get(0);
        modulePath.remove(0);

        // Get the child if they exist
        for (TsModule childTsModule : currentModuleNode.childModules) {
            if (childTsModule.name.equals(nextModuleName)) {
                return getOrCreateModuleFromPath(modulePath, childTsModule);
            }
        }

        // Create new module
        TsModule newModule = new TsModule();
        newModule.name = nextModuleName;
        currentModuleNode.childModules.add(newModule);
        return getOrCreateModuleFromPath(modulePath, newModule);
    }

    private TsModule upgradeJsModule(JsModule jsModule, JsFile parentJsFile) {
        TsModule tsModule = new TsModule();
        tsModule.name = camelToKebab(jsModule.name);
        tsModule.sourcedFrom = jsModule.sourcedFrom;

        for (JsInjectable jsController : getType(jsModule, InjectableType.CONTROLLER)) {
            tsModule.components.add(upgradeJsController(jsController, parentJsFile));
        }

        for (JsInjectable jsService : getType(jsModule, InjectableType.SERVICE)) {
            tsModule.services.add(upgradeJsService(jsService, parentJsFile));
        }

        for (JsInjectable jsFactory : getType(jsModule, InjectableType.FACTORY)) {
            tsModule.services.add(upgradeJsService(jsFactory, parentJsFile));
        }

        return tsModule;
    }


    private TsComponent upgradeJsController(JsInjectable jsController, JsFile parentJsFile) {
        TsComponent tsComponent = new TsComponent();
        tsComponent.name = camelToKebab(jsController.injectableName.replace("Controller", ""));
        return upgradeJsInjectable(jsController, parentJsFile, tsComponent);
    }

    private TsFunction upgradeJsFunction(JsFunction jsFunction) {
        TsFunction tsFunction = new TsFunction();
        tsFunction.name = jsFunction.functionName;
        tsFunction.arguments = jsFunction.arguments;

        for (JsFunction childJsFunction : jsFunction.childFunctions) {
            tsFunction.childFunctions.add(upgradeJsFunction(childJsFunction));
        }

        return tsFunction;
    }

    private <TS extends AbstractTsClass> TS upgradeJsInjectable(JsInjectable jsInjectable, JsFile parentJsFile, TS tsClass) {
        JsFunction jsFunction = getJsFunction(parentJsFile, jsInjectable.functionName);

        if (jsFunction != null) {
            for (JsFunction childJsFunction : jsFunction.childFunctions) {
                tsClass.functions.add(upgradeJsFunction(childJsFunction));
            }
        } else {
            System.err.println("Could not find 'function " + jsInjectable.functionName + "() {...}' in " + parentJsFile.filename + " for " + jsInjectable);
        }
        return tsClass;
    }

    private JsFunction getJsFunction(AbstractJsFunctionWrapper parentJsFunctionWrapper, String functionName) {
        Optional<JsFunction> jsFunction = parentJsFunctionWrapper.childFunctions.stream()
                .filter(childFunction -> functionName.equals(childFunction.functionName)).findFirst();
        if (jsFunction.isPresent()) return jsFunction.get();

        // Search anonomous functions
        List<JsFunction> anonomousFunctions = parentJsFunctionWrapper.childFunctions.stream()
                .filter(childFunction -> childFunction.functionName == null)
                .collect(Collectors.toList());
        for (JsFunction anonomousFunction : anonomousFunctions) {
            JsFunction foundFromAnonomous = getJsFunction(anonomousFunction, functionName);
            if (foundFromAnonomous != null) return foundFromAnonomous;
        }
        return null;
    }

    private TsService upgradeJsService(JsInjectable jsService, JsFile parentJsFile) {
        TsService tsService = new TsService();
        tsService.name = camelToKebab(jsService.injectableName.replace("Service", ""));
        return upgradeJsInjectable(jsService, parentJsFile, tsService);
    }

    private List<String> getModulePathFromFilename(String filename) {
        String[] filePathParts = filename.split("/");

        List<String> modulePath = new LinkedList<>();
        for (String filePathPart : filePathParts) {
            modulePath.add(convertToKebabCase(filePathPart));
        }
        return modulePath;
    }

    private String convertToKebabCase(String camelCase) {
        return camelCase; // TODO: this looks wrong
    }

    private String removeDotJsFromEnd(String filenameWithDotJs) {
        if (filenameWithDotJs.length() > 3 && filenameWithDotJs.substring(filenameWithDotJs.length() - 3).equals(".js")) {
            return filenameWithDotJs.substring(0, filenameWithDotJs.length() - 3);
        }
        return filenameWithDotJs;
    }

    private List<JsInjectable> getType(JsModule jsModule, InjectableType type) {
        return jsModule.injectables.stream()
                .filter((jsInjectable -> type.equals(jsInjectable.type)))
                .collect(Collectors.toList());
    }


    private String camelToKebab(String camelCase) {
        return CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, camelCase);
    }
}
