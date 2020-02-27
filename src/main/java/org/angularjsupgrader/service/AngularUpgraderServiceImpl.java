package org.angularjsupgrader.service;

import com.google.common.base.CaseFormat;
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

        List<JsDirective> jsDirectives = getType(jsModule, InjectableType.DIRECTIVE).stream()
                .map(jsInjectable -> extractJsDirective(jsInjectable.functionName, parentJsFile))
                .collect(Collectors.toList());

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

    private JsDirective extractJsDirective(String directiveFunctionName, JsFile parentJsFile) {
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

        JsDirective jsDirective = new JsDirective();
        for (AbstractJsStatementPart part : returnedObject.subParts) {
            if (part.type.equals(JavaScriptParser.PropertyExpressionAssignmentContext.class)) {
                JsStatementBranch propertyAssignement = (JsStatementBranch) part;
                String propertyKey = trimQuotes(propertyAssignement.subParts.get(0).toString());
                JsStatementBranch propertyValue = getFirstDescendantBranchWithMoreThan1Child((JsStatementBranch) propertyAssignement.subParts.get(2));
                switch (propertyKey) {
                    case "templateUrl":
                        jsDirective.templateUrl = propertyValue.toString();
                        break;
                    case "template":
                        jsDirective.template = propertyValue.toString();
                        break;
                    case "controllerAs":
                        jsDirective.controllerAs = trimQuotes(propertyValue.toString());
                        break;
                    case "controller":
                        String controllerName = propertyValue.toString();
                        if (controllerName.contains(" as ")) {
                            String[] controllerAsParts = controllerName.split(" as ");
                            controllerName = controllerAsParts[0];
                            jsDirective.controllerAs = controllerAsParts[1];
                        }
                        jsDirective.controller = getJsFunction(parentJsFile, controllerName);
                        break;
                    case "restrict":
                        jsDirective.restrictType = RestrictType.getByCode(propertyValue.toString());
                        break;
                    case "scope":
                        jsDirective.inputOutpus = extractScope(propertyValue);
                        break;
                    case "bindToController":
                        jsDirective.bindToController = Boolean.parseBoolean(propertyValue.toString());
                        break;
                    case "link":
                        jsDirective.linkFunction = propertyValue;
                        break;
                    case "replace":
                        System.err.println("Angular2 does not support directives with 'replace': true. Please upgrade " + parentJsFile.filename + ">" + directiveFunctionName + " manually");
                        break;
                    case "transclude":
                        jsDirective.transclude = Boolean.parseBoolean(trimQuotes(propertyValue.toString()));
                        break;
                    default:
                        System.err.println("Unsupported return key of '" + propertyKey + "' for " + parentJsFile.filename + ">" + directiveFunctionName);
                }
            }
        }
        return jsDirective;
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
        if (currentBranch.subParts.size() == 1 && currentBranch.subParts.get(0) instanceof JsStatementBranch) {
            return getFirstDescendantBranchWithMoreThan1Child((JsStatementBranch) currentBranch.subParts.get(0));
        }
        return currentBranch;
    }

    private String trimQuotes(String untrimmed) {
        return untrimmed.replace("\"", "").replace("'", "");
    }
}
