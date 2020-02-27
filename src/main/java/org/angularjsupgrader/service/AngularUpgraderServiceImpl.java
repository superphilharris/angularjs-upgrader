package org.angularjsupgrader.service;

import com.google.common.base.CaseFormat;
import org.angularjsupgrader.model.AbstractComponent;
import org.angularjsupgrader.model.DirectiveComponent;
import org.angularjsupgrader.model.PageComponent;
import org.angularjsupgrader.model.angularjs.*;
import org.angularjsupgrader.model.typescript.*;
import org.angularjsupgrader.parser.JavaScriptParser;

import java.util.*;
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

        List<DirectiveComponent> directives = getType(jsModule, InjectableType.DIRECTIVE).stream()
                .map(jsInjectable -> extractJsDirective(jsInjectable.functionName, parentJsFile))
                .collect(Collectors.toList());
        List<PageComponent> pageComponents = new LinkedList<>();
        for (JsInjectable jsConfig : getType(jsModule, InjectableType.CONFIG)) {
            pageComponents.addAll(extractPageComponents(jsConfig, parentJsFile));
        }

        for (JsInjectable jsController : getType(jsModule, InjectableType.CONTROLLER)) {
            tsModule.components.add(upgradeJsController(jsController, parentJsFile, tsModule));
        }

        /*
         * TODO:
         * - upgrade our jsDirectives after our controllers (using the name of our controller to override the files)
         *   - or some other way of marking that we have upgrader our controller???
         * - upgrade our routeProvider.when(path, directive) as a directive
         */

        for (JsInjectable jsService : getType(jsModule, InjectableType.SERVICE)) {
            tsModule.services.add(upgradeJsService(jsService, parentJsFile, tsModule));
        }
        for (JsInjectable jsFactory : getType(jsModule, InjectableType.FACTORY)) {
            tsModule.services.add(upgradeJsService(jsFactory, parentJsFile, tsModule));
        }
        List<JsInjectable> jsConfigs = getType(jsModule, InjectableType.CONFIG);
        for (int i = 0; i < jsConfigs.size(); i++) {
            tsModule.routings.add(upgradeJsConfig(jsConfigs.get(i), parentJsFile, tsModule, i));
        }

        return tsModule;
    }

    private DirectiveComponent extractJsDirective(String directiveFunctionName, JsFile parentJsFile) {
        JsFunction directiveFunction = getJsFunction(parentJsFile, directiveFunctionName);
        if (directiveFunction == null) {
            System.err.println("Could not find the directive for " + directiveFunctionName);
            return null;
        }
        if (directiveFunction.statements.size() != 1) {
            System.err.println("Can only upgrade directives with 1 statement -> " + directiveFunctionName);
            return null;
        }
        JsStatementBranch returnStatement = getFirstDescendantBranchWithMoreThan1Child(directiveFunction.statements.get(0));
        if (!returnStatement.type.equals(JavaScriptParser.ReturnStatementContext.class)) {
            System.err.println(directiveFunctionName + "'s statement must be a return statement");
            return null;
        }
        JsStatementBranch returnedObject = getFirstDescendantBranchWithMoreThan1Child((JsStatementBranch) returnStatement.subParts.get(1));
        if (!returnedObject.type.equals(JavaScriptParser.ObjectLiteralContext.class)) {
            System.err.println(directiveFunctionName + " must return an object");
            return null;
        }

        DirectiveComponent directive = new DirectiveComponent();
        Map<String, JsStatementBranch> keyValues = extractKeyValuesFromObjectLiteral(returnedObject);

        for (Map.Entry<String, JsStatementBranch> keyValue : keyValues.entrySet()) {
            if (didAssignKeyValue(keyValue, directive, parentJsFile)) continue;

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
                    System.err.println("Angular2 does not support directives with 'replace': true. Please upgrade " + parentJsFile.filename + ">" + directiveFunctionName + " manually");
                    break;
                case "transclude":
                    directive.transclude = Boolean.parseBoolean(trimQuotes(propertyValue.toString()));
                    break;
                default:
                    System.err.println("Unsupported return key of '" + keyValue.getKey() + "' for " + parentJsFile.filename + ">" + directiveFunctionName);
            }
        }
        return directive;
    }

    private boolean didAssignKeyValue(Map.Entry<String, JsStatementBranch> keyValue, AbstractComponent component, JsFile parentJsFile) {
        JsStatementBranch propertyValue = keyValue.getValue();
        switch (keyValue.getKey()) {
            case "templateUrl":
                component.templateUrl = propertyValue.toString();
                return true;
            case "template":
                component.template = propertyValue.toString();
                return true;
            case "controllerAs":
                component.controllerAs = trimQuotes(propertyValue.toString());
                return true;
            case "controller":
                String controllerName = propertyValue.toString();
                if (controllerName.contains(" as ")) {
                    String[] controllerAsParts = controllerName.split(" as ");
                    controllerName = controllerAsParts[0];
                    component.controllerAs = controllerAsParts[1];
                }
                component.controller = getJsFunction(parentJsFile, controllerName);
                return true;
        }
        return false;
    }

    private Map<String, JsStatementBranch> extractKeyValuesFromObjectLiteral(JsStatementBranch objectLiteral) {
        Map<String, JsStatementBranch> keyValues = new HashMap<>();
        for (AbstractJsStatementPart part : objectLiteral.subParts) {
            if (part.type.equals(JavaScriptParser.PropertyExpressionAssignmentContext.class)) {
                JsStatementBranch propertyAssignement = (JsStatementBranch) part;
                keyValues.put(
                        trimQuotes(propertyAssignement.subParts.get(0).toString()),
                        getFirstDescendantBranchWithMoreThan1Child((JsStatementBranch) propertyAssignement.subParts.get(2))
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
                scope.put(inputOutputName, ScopeType.getByCode(trimQuotes(inputOutputType)));
            }
        }
        return scope;
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

    private TsRouting upgradeJsConfig(JsInjectable jsConfig, JsFile parentJsFile, TsModule tsModule, int sequenceOfConfig) {
        // TODO: extract out our paths from our upgraded component
        TsRouting tsRouting = new TsRouting();
        tsRouting.name = tsModule.name + (sequenceOfConfig == 0 ? "" : 1 + sequenceOfConfig);
        tsRouting.sourcedFrom = jsConfig.functionName + " in " + parentJsFile.filename;
        return upgradeJsInjectable(jsConfig, parentJsFile, tsRouting, tsModule);
    }

    private List<PageComponent> extractPageComponents(JsInjectable jsConfig, JsFile parentJsFile) {
        JsFunction jsFunction = getJsFunction(parentJsFile, jsConfig.functionName);
        List<PageComponent> components = new LinkedList<>();
        for (JsStatementBranch statementBranch : jsFunction.statements) {
            JsStatementBranch possibleRouteProviderWhenRoute = getFirstDescendantBranchWithMoreThan1Child(statementBranch);

            if (isRouteProviderWhenRouteStatement(possibleRouteProviderWhenRoute)) {
                for (int i = 1; i < possibleRouteProviderWhenRoute.subParts.size(); i++) {
                    if (possibleRouteProviderWhenRoute.subParts.get(i).type.equals(JavaScriptParser.ArgumentsContext.class)) {
                        components.add(extractPageComponent((JsStatementBranch) possibleRouteProviderWhenRoute.subParts.get(i), parentJsFile));
                    }
                }
            }
        }
        return components;
    }

    private boolean isRouteProviderWhenRouteStatement(JsStatementBranch statementBranch) {
        // TODO: we are relying on the assumption that the injected name '$routeProvider' string is the same name as the instantiated variable
        if (!(statementBranch.subParts.get(0) instanceof JsStatementBranch)) return false;
        JsStatementBranch memberDotExpression = (JsStatementBranch) statementBranch.subParts.get(0);
        return (
                memberDotExpression.subParts.size() > 2 &&
                        memberDotExpression.subParts.get(0).toString().equals("$routeProvider") &&
                        memberDotExpression.subParts.get(2).toString().equals("when")
        );
    }

    private PageComponent extractPageComponent(JsStatementBranch argumentsContext, JsFile parentJsFile) {
        PageComponent component = new PageComponent();
        component.path = argumentsContext.subParts.get(1).toString();
        JsStatementBranch routeProperties = getFirstDescendantBranchWithMoreThan1Child((JsStatementBranch) argumentsContext.subParts.get(3));
        Map<String, JsStatementBranch> keyValues = extractKeyValuesFromObjectLiteral(routeProperties);

        for (Map.Entry<String, JsStatementBranch> keyValue : keyValues.entrySet()) {
            if (didAssignKeyValue(keyValue, component, parentJsFile)) continue;

            switch (keyValue.getKey()) {
                case "title":
                    component.title = trimQuotes(keyValue.getValue().toString());
                    break;
                default:
                    System.err.println("Unknown key: " + keyValue.getKey() + " for route " + parentJsFile.filename + ">" + component.path);
            }
        }
        return component;
    }

    private <TS extends AbstractTsClass> TS upgradeJsInjectable(JsInjectable jsInjectable, JsFile parentJsFile, TS tsClass, TsModule parentTsModule) {
        JsFunction jsFunction = getJsFunction(parentJsFile, jsInjectable.functionName);
        if (jsFunction != null) {
            for (JsFunction childJsFunction : jsFunction.childFunctions) {
                tsClass.functions.add(upgradeJsFunction(childJsFunction));
            }
            tsClass.initialization.addAll(jsFunction.statements.stream().map(this::upgradeJsStatement).collect(Collectors.toList()));
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

    private JsStatementBranch getFirstDescendantBranchWithMoreThan1Child(JsStatementBranch currentBranch) {
        if ((currentBranch.subParts.size() == 1 && currentBranch.subParts.get(0) instanceof JsStatementBranch) ||
                (currentBranch.subParts.size() == 2 && currentBranch.subParts.get(1).type.equals(JavaScriptParser.EosContext.class))) {
            return getFirstDescendantBranchWithMoreThan1Child((JsStatementBranch) currentBranch.subParts.get(0));
        }
        return currentBranch;
    }

    private String trimQuotes(String untrimmed) {
        return untrimmed.replace("\"", "").replace("'", "");
    }
}
