package org.angularjsupgrader.service.upgrader;

import org.angularjsupgrader.model.angularjs.*;
import org.angularjsupgrader.model.typescript.*;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by Philip Harris on 9/03/2020
 */
public class AngularInjectableUpgraderImpl {

    private final AngularLocatorImpl angularLocator;
    private final StringServiceImpl stringService;

    public AngularInjectableUpgraderImpl(
            AngularLocatorImpl angularLocator,
            StringServiceImpl stringService) {
        this.angularLocator = angularLocator;
        this.stringService = stringService;
    }

    public TsService upgradeJsService(JsInjectable jsService, JsFile parentJsFile, TsModule parentTsModule) {
        TsService tsService = new TsService();
        tsService.name = stringService.camelToKebab(jsService.injectableName.replace("Service", ""));
        return upgradeJsInjectable(jsService, parentJsFile, tsService, parentTsModule);
    }

    public TsComponent upgradeJsController(JsInjectable jsController, JsFile parentJsFile, TsModule parentTsModule) {
        TsComponent tsComponent = new TsComponent();
        tsComponent.controllerSourcedFrom = jsController;
        tsComponent.name = stringService.camelToKebab(jsController.injectableName.replace("Controller", ""));
        return upgradeJsInjectable(jsController, parentJsFile, tsComponent, parentTsModule);
    }


    public <TS extends AbstractTsClass> TS upgradeJsInjectable(JsInjectable jsInjectable, JsFile parentJsFile, TS tsClass, TsModule parentTsModule) {
        JsFunction jsFunction = angularLocator.getJsFunction(parentJsFile, jsInjectable.functionName);
        if (jsFunction != null) {
            for (JsFunction childJsFunction : jsFunction.childFunctions) {
                tsClass.functions.add(upgradeJsFunction(childJsFunction));
            }
            tsClass.initialization.addAll(jsFunction.statements.stream().map(this::upgradeJsStatement).collect(Collectors.toList()));
        } else {
            addError("Could not find 'function " + jsInjectable.functionName + "() {...}' in " + parentJsFile.filename, tsClass);
        }
        tsClass.dependencies.addAll(jsInjectable.injections);
        tsClass.dependencies.addAll(getInjects(jsInjectable.functionName, parentJsFile));
        tsClass.parent = parentTsModule;
        return tsClass;
    }

    public TsStatement upgradeJsStatement(JsStatementBranch jsStatement) {
        TsStatement tsStatement = new TsStatement();
        tsStatement.text = jsStatement.toString(); // TODO: switch out old dependencies for new dependencies
        return tsStatement;
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

    private List<String> getInjects(String functionName, JsFile parentJsFile) {
        if (functionName == null) return new LinkedList<>();

        Optional<JsInjectStatement> jsInjectStatement = parentJsFile.injectStatements.stream()
                .filter((statement) -> functionName.equals(statement.functionName))
                .findFirst();
        if (jsInjectStatement.isPresent()) return jsInjectStatement.get().injects;
        return new LinkedList<>();
    }

    private void addError(final String error, final AbstractTsClass tsClass) {
        System.err.println(error);
        tsClass.upgradeErrors.add(error);
    }
}
