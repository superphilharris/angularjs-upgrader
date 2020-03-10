package org.angularjsupgrader.service.upgrader;

import org.angularjsupgrader.model.AbstractComponent;
import org.angularjsupgrader.model.JsConfig;
import org.angularjsupgrader.model.JsDirective;
import org.angularjsupgrader.model.JsRoutePage;
import org.angularjsupgrader.model.angularjs.*;
import org.angularjsupgrader.model.typescript.TsComponent;
import org.angularjsupgrader.model.typescript.TsModule;
import org.angularjsupgrader.parser.JavaScriptParser;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Philip Harris on 9/03/2020
 */
public class ComponentExtractorImpl {
    private final AngularInjectableUpgraderImpl injectableUpgrader;
    private final AngularLocatorImpl angularLocator;
    private final StringServiceImpl stringService;

    public ComponentExtractorImpl(
            AngularInjectableUpgraderImpl angularInjectableUpgrader,
            AngularLocatorImpl angularLocator,
            StringServiceImpl stringService) {
        this.injectableUpgrader = angularInjectableUpgrader;
        this.angularLocator = angularLocator;
        this.stringService = stringService;
    }


    public JsDirective extractJsDirective(JsInjectable jsInjectable, JsFile parentJsFile, TsModule parentTsModule) {
        JsFunction directiveFunction = angularLocator.getJsFunction(parentJsFile, jsInjectable.functionName);
        JsDirective directive = new JsDirective();
        directive.originalInjectable = jsInjectable;
        if (directiveFunction == null) {
            System.err.println("Could not find the directive for " + jsInjectable.functionName);
            return directive;
        }
        if (directiveFunction.statements.size() != 1) {
            System.err.println("Can only upgrade directives with 1 statement -> " + jsInjectable.functionName);
            return directive;
        }
        JsStatementBranch returnStatement = angularLocator.getFirstDescendantBranchWithMoreThan1Child(directiveFunction.statements.get(0));
        if (!returnStatement.type.equals(JavaScriptParser.ReturnStatementContext.class)) {
            System.err.println(jsInjectable.functionName + "'s statement must be a return statement");
            return directive;
        }
        JsStatementBranch returnedObject = angularLocator.getFirstDescendantBranchWithMoreThan1Child((JsStatementBranch) returnStatement.subParts.get(1));
        if (!returnedObject.type.equals(JavaScriptParser.ObjectLiteralContext.class)) {
            System.err.println(jsInjectable.functionName + " must return an object");
            return directive;
        }

        Map<String, JsStatementBranch> keyValues = extractKeyValuesFromObjectLiteral(returnedObject);

        for (Map.Entry<String, JsStatementBranch> keyValue : keyValues.entrySet()) {
            if (didAssignKeyValue(keyValue, directive, parentJsFile, parentTsModule)) continue;

            JsStatementBranch propertyValue = keyValue.getValue();
            switch (keyValue.getKey()) {
                case "restrict":
                    directive.restrictType = RestrictType.getByCode(propertyValue.toString());
                    break;
                case "scope":
                    directive.inputOutpus = extractScope(propertyValue);
                    break;
                case "bindToController":
                    directive.bindToController = Boolean.parseBoolean(propertyValue.toString());
                    break;
                case "link":
                    directive.linkFunction = propertyValue;
                    break;
                case "replace":
                    System.err.println("Angular2 does not support directives with 'replace': true. Please upgrade " + parentJsFile.filename + ">" + jsInjectable.functionName + " manually");
                    break;
                case "transclude":
                    directive.transclude = Boolean.parseBoolean(stringService.trimQuotes(propertyValue.toString()));
                    break;
                default:
                    System.err.println("Unsupported return key of '" + keyValue.getKey() + "' for " + parentJsFile.filename + ">" + jsInjectable.functionName);
            }
        }
        return directive;
    }


    public JsConfig extractPageComponents(JsInjectable jsInjectable, JsFile parentJsFile, TsModule parentTsModule) {
        JsFunction jsFunction = angularLocator.getJsFunction(parentJsFile, jsInjectable.functionName);
        JsConfig jsConfig = new JsConfig();
        jsConfig.originalInjectable = jsInjectable;
        for (JsStatementBranch statementBranch : jsFunction.statements) {
            JsStatementBranch possibleRouteProviderWhenRoute = angularLocator.getFirstDescendantBranchWithMoreThan1Child(statementBranch);

            if (isRouteProviderWhenRouteStatement(possibleRouteProviderWhenRoute)) {
                for (int i = 1; i < possibleRouteProviderWhenRoute.subParts.size(); i++) {
                    if (possibleRouteProviderWhenRoute.subParts.get(i).type.equals(JavaScriptParser.ArgumentsContext.class)) {
                        jsConfig.pages.add(extractPageComponent((JsStatementBranch) possibleRouteProviderWhenRoute.subParts.get(i), parentJsFile, parentTsModule));
                    }
                }
            }
        }
        return jsConfig;
    }

    private JsRoutePage extractPageComponent(JsStatementBranch argumentsContext, JsFile parentJsFile, TsModule parentTsModule) {
        JsRoutePage component = new JsRoutePage();
        component.path = argumentsContext.subParts.get(1).toString();
        JsStatementBranch routeProperties = angularLocator.getFirstDescendantBranchWithMoreThan1Child((JsStatementBranch) argumentsContext.subParts.get(3));
        Map<String, JsStatementBranch> keyValues = extractKeyValuesFromObjectLiteral(routeProperties);

        for (Map.Entry<String, JsStatementBranch> keyValue : keyValues.entrySet()) {
            if (didAssignKeyValue(keyValue, component, parentJsFile, parentTsModule)) continue;

            switch (keyValue.getKey()) {
                case "title":
                    component.title = stringService.trimQuotes(keyValue.getValue().toString());
                    break;
                case "reloadOnSearch":
                    component.reloadOnSearch = Boolean.parseBoolean(keyValue.getValue().toString());
                    break;
                default:
                    System.err.println("Unknown key: " + keyValue.getKey() + " for route " + parentJsFile.filename + ">" + component.path);
            }
        }
        return component;
    }


    private boolean didAssignKeyValue(Map.Entry<String, JsStatementBranch> keyValue, AbstractComponent component, JsFile parentJsFile, TsModule parentTsModule) {
        JsStatementBranch propertyValue = keyValue.getValue();
        switch (keyValue.getKey()) {
            case "templateUrl":
                component.templateUrl = propertyValue.toString();
                return true;
            case "template":
                component.template = propertyValue.toString();
                return true;
            case "controllerAs":
                component.controllerAs = stringService.trimQuotes(propertyValue.toString());
                return true;
            case "controller":
                String controllerName = propertyValue.toString();
                if (controllerName.contains(" as ")) {
                    String[] controllerAsParts = controllerName.split(" as ");
                    controllerName = controllerAsParts[0];
                    component.controllerAs = stringService.trimQuotes(controllerAsParts[1]);
                }
                if (controllerName.contains("'") || controllerName.contains("\"")) {
                    component.controllerInjectedName = stringService.trimQuotes(controllerName);
                } else {
                    component.controllerFunctionName = controllerName;
                    component.upgradedController = upgradeJsControllerFromFunctionName(parentJsFile, controllerName, parentTsModule);
                }
                return true;
            case "resolve":
                component.resolve = extractKeyValuesFromObjectLiteral(propertyValue);
                return true;
        }
        return false;
    }

    private boolean isRouteProviderWhenRouteStatement(JsStatementBranch statementBranch) {
        // TODO: we are relying on the assumption that the injected name '$routeProvider' string is the same name as the instantiated variable
        if (statementBranch.subParts.size() == 0) return false;
        if (!(statementBranch.subParts.get(0) instanceof JsStatementBranch)) return false;
        JsStatementBranch memberDotExpression = (JsStatementBranch) statementBranch.subParts.get(0);
        return (
                memberDotExpression.subParts.size() > 2 &&
                        memberDotExpression.subParts.get(0).toString().equals("$routeProvider") &&
                        memberDotExpression.subParts.get(2).toString().equals("when")
        );
    }

    private TsComponent upgradeJsControllerFromFunctionName(JsFile parentJsFile, String controllerName, TsModule parentTsModule) {
        TsComponent tsComponent = new TsComponent();
        tsComponent.name = stringService.camelToKebab(controllerName.replace("Controller", ""));
        JsInjectable mockJsInjectable = new JsInjectable();
        mockJsInjectable.functionName = controllerName;
        return injectableUpgrader.upgradeJsInjectable(mockJsInjectable, parentJsFile, tsComponent, parentTsModule);
    }

    private Map<String, JsStatementBranch> extractKeyValuesFromObjectLiteral(JsStatementBranch objectLiteral) {
        Map<String, JsStatementBranch> keyValues = new HashMap<>();
        for (AbstractJsStatementPart part : objectLiteral.subParts) {
            if (part.type.equals(JavaScriptParser.PropertyExpressionAssignmentContext.class)) {
                JsStatementBranch propertyAssignement = (JsStatementBranch) part;
                keyValues.put(
                        stringService.trimQuotes(propertyAssignement.subParts.get(0).toString()),
                        angularLocator.getFirstDescendantBranchWithMoreThan1Child((JsStatementBranch) propertyAssignement.subParts.get(2))
                );
            }
        }
        return keyValues;
    }

    private Map<String, ScopeType> extractScope(JsStatementBranch statement) {
        Map<String, ScopeType> scope = new HashMap<>();
        for (AbstractJsStatementPart objectLiteralPart : statement.subParts) {
            if (objectLiteralPart instanceof JsStatementBranch) {
                String inputOutputName = ((JsStatementBranch) objectLiteralPart).subParts.get(0).toString();
                String inputOutputType = ((JsStatementBranch) objectLiteralPart).subParts.get(2).toString();
                scope.put(inputOutputName, ScopeType.getByCode(stringService.trimQuotes(inputOutputType)));
            }
        }
        return scope;
    }
}
