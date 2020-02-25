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
            int position = 0;
            List<String> modulePath = getModulePathFromFilename(removeDotJsFromEnd(jsFile.filename));
            TsModule tsModule = getOrCreateModuleFromPath(modulePath, tsProgram, position);

            for (JsModule jsModule : jsFile.modules.values()) {
                tsModule.childModules.add(upgradeJsModule(jsModule, jsFile, tsModule));
            }
        }

        return tsProgram;
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

    private TsModule upgradeJsModule(JsModule jsModule, JsFile parentJsFile, TsModule parentTsModule) {
        TsModule tsModule = new TsModule();
        tsModule.name = camelToKebab(jsModule.name.replace(".", "-"));
        tsModule.sourcedFrom = jsModule.sourcedFrom;
        tsModule.parent = parentTsModule;

        for (JsInjectable jsController : getType(jsModule, InjectableType.CONTROLLER)) {
            tsModule.components.add(upgradeJsController(jsController, parentJsFile, tsModule));
        }
        for (JsInjectable jsService : getType(jsModule, InjectableType.SERVICE)) {
            tsModule.services.add(upgradeJsService(jsService, parentJsFile, tsModule));
        }
        for (JsInjectable jsFactory : getType(jsModule, InjectableType.FACTORY)) {
            tsModule.services.add(upgradeJsService(jsFactory, parentJsFile, tsModule));
        }
        for (JsInjectable jsConfig : getType(jsModule, InjectableType.CONFIG)) {
            tsModule.routing = upgradeJsConfig(jsConfig, parentJsFile, tsModule);
        }

        return tsModule;
    }


    private TsComponent upgradeJsController(JsInjectable jsController, JsFile parentJsFile, TsModule parentTsModule) {
        TsComponent tsComponent = new TsComponent();
        tsComponent.name = camelToKebab(jsController.injectableName.replace("Controller", ""));
        return upgradeJsInjectable(jsController, parentJsFile, tsComponent, parentTsModule);
    }

    private TsFunction upgradeJsFunction(JsFunction jsFunction) {
        TsFunction tsFunction = new TsFunction();
        tsFunction.name = jsFunction.functionName;
        tsFunction.arguments = jsFunction.arguments;
        tsFunction.statements.addAll(jsFunction.statements.stream().map(this::upgradeJsStatement).collect(Collectors.toList()));

        for (JsFunction childJsFunction : jsFunction.childFunctions) {
            tsFunction.childFunctions.add(upgradeJsFunction(childJsFunction));
        }

        return tsFunction;
    }

    private TsStatement upgradeJsStatement(JsStatementBranch jsStatement) {
        TsStatement tsStatement = new TsStatement();
        tsStatement.text = jsStatement.toString(); // TODO: switch out old dependencies for new dependencies
        return tsStatement;
    }

    private TsRouting upgradeJsConfig(JsInjectable jsConfig, JsFile parentJsFile, TsModule tsModule) {
        // TODO: extract out our paths from our upgraded component
        return upgradeJsInjectable(jsConfig, parentJsFile, new TsRouting(), tsModule);
    }

    private <TS extends AbstractTsClass> TS upgradeJsInjectable(JsInjectable jsInjectable, JsFile parentJsFile, TS tsClass, TsModule parentTsModule) {
        JsFunction jsFunction = getJsFunction(parentJsFile, jsInjectable.functionName);
        if (jsFunction != null) {
            for (JsFunction childJsFunction : jsFunction.childFunctions) {
                tsClass.functions.add(upgradeJsFunction(childJsFunction));
            }
            tsClass.initialization = jsFunction.statements.stream().map(this::upgradeJsStatement).collect(Collectors.toList());
        } else {
            System.err.println("Could not find 'function " + jsInjectable.functionName + "() {...}' in " + parentJsFile.filename + " for " + jsInjectable);
        }
        tsClass.dependencies.addAll(jsInjectable.injections);
        tsClass.dependencies.addAll(getInjects(jsInjectable.functionName, parentJsFile));
        tsClass.parent = parentTsModule;
        return tsClass;
    }

    private JsFunction getJsFunction(AbstractJsFunctionWrapper parentJsFunctionWrapper, String functionName) {
        Optional<JsFunction> jsFunction = parentJsFunctionWrapper.childFunctions.stream()
                .filter(childFunction -> functionName.equals(childFunction.functionName)).findFirst();
        if (jsFunction.isPresent()) return jsFunction.get();

        // Search anonymous functions
        List<JsFunction> anonomousFunctions = parentJsFunctionWrapper.childFunctions.stream()
                .filter(childFunction -> childFunction.functionName == null)
                .collect(Collectors.toList());
        for (JsFunction anonomousFunction : anonomousFunctions) {
            JsFunction foundFromAnonomous = getJsFunction(anonomousFunction, functionName);
            if (foundFromAnonomous != null) return foundFromAnonomous;
        }
        return null;
    }

    private List<String> getInjects(String functionName, JsFile parentJsFile) {
        if (functionName == null) return new LinkedList<>();

        Optional<JsInjectStatement> jsInjectStatement = parentJsFile.injectStatements.stream()
                .filter((statement) -> functionName.equals(statement.functionName))
                .findFirst();
        if (jsInjectStatement.isPresent()) return jsInjectStatement.get().injects;
        return new LinkedList<>();
    }

    private TsService upgradeJsService(JsInjectable jsService, JsFile parentJsFile, TsModule parentTsModule) {
        TsService tsService = new TsService();
        tsService.name = camelToKebab(jsService.injectableName.replace("Service", ""));
        return upgradeJsInjectable(jsService, parentJsFile, tsService, parentTsModule);
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
        return CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, camelCase);
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
