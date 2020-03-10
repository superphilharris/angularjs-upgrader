package org.angularjsupgrader.service.upgrader;

import org.angularjsupgrader.model.angularjs.AbstractJsFunctionWrapper;
import org.angularjsupgrader.model.angularjs.JsFunction;
import org.angularjsupgrader.model.angularjs.JsStatementBranch;
import org.angularjsupgrader.model.typescript.AbstractTsModule;
import org.angularjsupgrader.model.typescript.TsComponent;
import org.angularjsupgrader.model.typescript.TsModule;
import org.angularjsupgrader.parser.JavaScriptParser;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by Philip Harris on 9/03/2020
 */
public class AngularLocatorImpl {


    public JsFunction getJsFunction(AbstractJsFunctionWrapper parentJsFunctionWrapper, String functionName) {
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


    public JsStatementBranch getFirstDescendantBranchWithMoreThan1Child(JsStatementBranch currentBranch) {
        if ((currentBranch.subParts.size() == 1 && currentBranch.subParts.get(0) instanceof JsStatementBranch) ||
                (currentBranch.subParts.size() == 2 && currentBranch.subParts.get(1).type.equals(JavaScriptParser.EosContext.class))) {
            return getFirstDescendantBranchWithMoreThan1Child((JsStatementBranch) currentBranch.subParts.get(0));
        }
        return currentBranch;
    }


    public TsComponent findControllerComponentInSubModules(AbstractTsModule tsModule, String controllerInjectableName) {
        for (TsModule childTsModule : tsModule.childModules) {
            TsComponent foundComponent = findControllerComponent(childTsModule, controllerInjectableName);
            if (foundComponent != null) {
                return foundComponent;
            }
        }
        return null;
    }

    private TsComponent findControllerComponent(TsModule tsModule, String controllerInjectableName) {
        for (TsComponent tsComponent : tsModule.components) {
            if (tsComponent.controllerSourcedFrom != null) {
                if (tsComponent.controllerSourcedFrom.injectableName.equals(controllerInjectableName)) {
                    return tsComponent;
                }
            }
        }
        return findControllerComponentInSubModules(tsModule, controllerInjectableName);
    }

    public TsComponent findComponentByFunctionName(TsModule tsModule, String controllerFunctionName) {
        for (TsComponent tsComponent : tsModule.components) {
            if (tsComponent.controllerSourcedFrom != null) {
                if (tsComponent.controllerSourcedFrom.functionName.equals(controllerFunctionName)) {
                    return tsComponent;
                }
            }
        }
        return null;
    }
}
