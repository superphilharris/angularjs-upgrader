package org.angularjsupgrader.service.generation;

import org.angularjsupgrader.exception.UpgraderException;
import org.angularjsupgrader.model.AbstractComponent;
import org.angularjsupgrader.model.UpgradeProperties;
import org.angularjsupgrader.model.typescript.*;
import org.angularjsupgrader.service.FileListerServiceImpl;
import org.angularjsupgrader.service.UpgradePathServiceImpl;
import org.angularjsupgrader.service.upgrader.StringServiceImpl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Philip Harris on 18/01/2020
 */
public class TypeScriptGenerationServiceImpl {

    private final UpgradePathServiceImpl upgradePathService;
    private final HtmlGenerationServiceImpl htmlGenerationService;

    public TypeScriptGenerationServiceImpl(UpgradeProperties upgradeProperties,
                                           StringServiceImpl stringService,
                                           FileListerServiceImpl fileListerService) {
        this.upgradePathService = new UpgradePathServiceImpl();
        this.htmlGenerationService = new HtmlGenerationServiceImpl(upgradeProperties, stringService, fileListerService);
    }


    public void generateProgram(TsProgram program) throws UpgraderException {
        String outputDir = "upgradedAngular/";
        for (TsModule module : program.childModules) {
            generateModule(module, outputDir);
        }
    }

    private void generateModule(TsModule module, String parentDir) throws UpgraderException {
        String directory = parentDir + module.name + "/";
        createDirectory(directory);

        for (TsComponent component : module.components) {
            generateComponent(component, directory);
        }
        for (TsService service : module.services) {
            generateService(service, directory);
        }
        for (TsRouting routing : module.routings) {
            generateRouting(routing, directory);
        }

        // Imports
        List<String> moduleLines = new LinkedList<>();
        moduleLines.add("import {NgModule} from '@angular/core';");
        for (TsComponent component : module.components) {
            moduleLines.add("import {" + kebabToCamelUpperFirst(component.name) + "Component} from './" + component.name + "/" + component.name + ".component';");
        }

        moduleLines.add("\n// angularjs-upgrader generated from " + module.sourcedFrom);
        moduleLines.add("@NgModule({");
        moduleLines.add("\timports: [],");

        // Declarations
        List<String> componentDeclarations = module.components.stream()
                .map(component -> kebabToCamelUpperFirst(component.name) + "Component")
                .collect(Collectors.toList());
        moduleLines.add("\tdeclarations: [\n\t\t" + String.join(",\n\t\t", componentDeclarations) + "\n\t]");

        moduleLines.add(
                "})\n" +
                        "export class " + kebabToCamelUpperFirst(module.name) + "Module { }"
        );
        writeFile(moduleLines, directory + module.name + ".module.ts");

        for (TsModule childModule : module.childModules) {
            generateModule(childModule, directory);
        }
    }

    private void generateComponent(TsComponent component, String parentDirectory) throws UpgraderException {
        String directory = parentDirectory + component.name + "/";
        createDirectory(directory);
        String className = kebabToCamelUpperFirst(component.name) + "Component";

        // Template
        List<String> templateLines = new LinkedList<>();
        if (component.template != null) {
            templateLines.add(htmlGenerationService.upgradeInlineTemplate(component.template));
        } else if (component.templateUrl != null) {
            templateLines.add(htmlGenerationService.upgradeTemplateUrl(component.templateUrl));
        } else {
            if (component.controllerSourcedFrom != null) {
                templateLines.add("<!-- UPGRADE ERROR: Could not find the template for controller function:'" + component.controllerSourcedFrom.functionName + "'\nIs it embedded inside html with the syntax: '<div ng-controller=\"" + component.controllerSourcedFrom.injectableName + "\"...'? \n-->");
            } else {
                templateLines.add("<p>" + component.name + " works!</p>");
            }
        }
        writeFile(templateLines, directory + component.name + ".component.html");

        // Controller
        final List<String> controllerLines = new LinkedList<>();
        controllerLines.add("import {Component, OnInit} from '@angular/core';");
        controllerLines.addAll(getServiceImports(component));
        controllerLines.add(
                "\n" +
                        "@Component({\n" +
                        "\tselector: 'app-" + component.name + "',\n" +
                        "\ttemplateUrl: './" + component.name + ".component.html',\n" +
                        "\tstyleUrls: ['./" + component.name + ".component.scss']\n" +
                        "})\n" +
                        "export class " + className + " implements OnInit {\n"
        );
        // TODO: add in @Input() and @Output
        controllerLines.addAll(getConstructor(component));
        controllerLines.add("\n\tngOnInit() {");
        controllerLines.addAll(
                getStatementLines(component.initialization).stream()
                        .map(line -> formatIndentsForCurlyBraces(line, 1))
                        .collect(Collectors.toList()));
        controllerLines.add("\t}\n");
        controllerLines.addAll(getClassFunctionLines(component));

        controllerLines.add("}");
        writeFile(controllerLines, directory + component.name + ".component.ts");

        // SCSS
        writeFile(Collections.singletonList(""), directory + component.name + ".component.scss");

        // Spec
        List<String> testLines = new LinkedList<>();
        testLines.add("import {async, ComponentFixture, TestBed} from '@angular/core/testing';\n" +
                "import {MockComponent, MockModule, MockPipe} from 'ng-mocks';\n" +
                "import {Mock} from 'ts-mockery';\n" +
                "import {EMPTY} from 'rxjs';\n" +
                "import {" + className + "} from './" + component.name + ".component';\n" +
                "\n" +
                "describe('" + className + "', () => {\n" +
                "\tlet component: " + className + ";\n" +
                "\tlet fixture: ComponentFixture<" + className + ">;\n" +
                "\n" +
                "\tbeforeEach(async(() => {\n" +
                "\t\tTestBed.configureTestingModule({\n" +
                "\t\t\timports: [\n" +
                "\t\t\t],\n" +
                "\t\t\tproviders: [\n" +
                "\t\t\t],\n" +
                "\t\t\tdeclarations: [\n" +
                "\t\t\t\t" + className + "\n" +
                "\t\t\t]\n" +
                "\t\t});\n" +
                "\t}));\n" +
                "\n" +
                "\tbeforeEach(() => {\n" +
                "\t\tfixture = TestBed.createComponent(" + className + ");\n" +
                "\t\tcomponent = fixture.componentInstance;\n" +
                "\t\tfixture.detectChanges();\n" +
                "\t});\n" +
                "\n" +
                "\tit('should create', () => {\n" +
                "\t\texpect(component).toBeInstanceOf(" + className + ");\n" +
                "\t});\n" +
                "});");
        writeFile(testLines, directory + component.name + ".component.spec.ts");
    }

    private String formatIndentsForCurlyBraces(String rawText, int numberOfTabs) {
        StringBuilder tabs = new StringBuilder();
        for (int i = 0; i < numberOfTabs; i++) tabs.append("\t");

        return tabs.toString() + rawText;
    }

    private void generateService(TsService service, String parentDirectory) throws UpgraderException {
        String className = kebabToCamelUpperFirst(service.name) + "Service";
        List<String> serviceLines = new LinkedList<>();
        serviceLines.add("import {Injectable} from '@angular/core';\n" +
                "import {Observable} from 'rxjs';");
        serviceLines.addAll(getServiceImports(service));
        serviceLines.add("\n\n@Injectable({\n" +
                "\tprovidedIn: 'root'\n" +
                "})\n" +
                "export class " + className + " {");
        serviceLines.addAll(getConstructor(service));
        if (service.initialization.size() > 0) {
            serviceLines.add("\tinit() {");
            serviceLines.addAll(
                    getStatementLines(service.initialization).stream()
                            .map(line -> formatIndentsForCurlyBraces(line, 1))
                            .collect(Collectors.toList()));
            serviceLines.add("\t}");
        }
        serviceLines.addAll(getClassFunctionLines(service));
        serviceLines.add("}");
        writeFile(serviceLines, parentDirectory + service.name + ".service.ts");

        List<String> testLines = new LinkedList<>();
        testLines.add("import { HttpClientTestingModule } from '@angular/common/http/testing';\n" +
                "import {TestBed} from '@angular/core/testing';\n" +
                "\n" +
                "import {" + className + "} from './" + service.name + ".service';\n" +
                "\n" +
                "describe('" + className + "', () => {\n" +
                "  let service: " + className + ";\n" +
                "\n" +
                "  beforeEach(() => {\n" +
                "    TestBed.configureTestingModule({\n" +
                "      imports: [\n" +
                "        HttpClientTestingModule\n" +
                "      ]\n" +
                "    });\n" +
                "  });\n" +
                "\n" +
                "  beforeEach(() => {\n" +
                "    service = TestBed.get(" + className + ");\n" +
                "  });\n" +
                "\n" +
                "  it('should be created', () => {\n" +
                "    expect(service).toBeInstanceOf(" + className + ");\n" +
                "  });\n" +
                "});");
        writeFile(testLines, parentDirectory + service.name + ".service.spec.ts");
    }

    private void generateRouting(TsRouting routing, String parentDirectory) throws UpgraderException {
        if (routing.pathToComponent.isEmpty() && routing.initialization.isEmpty()) {
            return;
        }

        List<String> classLines = new LinkedList<>();
        classLines.add("import {NgModule} from '@angular/core';\n" +
                "import {RouterModule, Routes} from '@angular/router';");
        classLines.addAll(
                routing.pathToComponent.values().stream().map(component -> {
                    TsDependency dependency = upgradePathService.getComponentDependency(component.name, routing);
                    return "import {" + kebabToCamelUpperFirst(component.name) + "Component} from '" + dependency.packagePath + ';';
                }).collect(Collectors.toList()));
        classLines.add("\nconst routes: Routes = [");
        classLines.add(routing.pathToComponent.entrySet().stream()
                .map(pathToComponent -> "  {\n" +
                        "    path: " + pathToComponent.getKey() + ",\n" +
                        "    component: " + kebabToCamelUpperFirst(pathToComponent.getValue().name) + "Component,\n" +
                        "  }").collect(Collectors.joining(",\n")));
        classLines.add(
                "];\n\n" +
                        "@NgModule({\n" +
                        "  imports: [\n" +
                        "    RouterModule.forChild(routes)\n" +
                        "  ],\n" +
                        "  exports: [\n" +
                        "    RouterModule\n" +
                        "  ]\n" +
                        "})\n" +
                        "export class " + kebabToCamelUpperFirst(routing.name) + "RoutingModule {\n" +
                        "\n" +
                "}");

        writeFile(classLines, parentDirectory + routing.name + "-routing.module.ts");
    }

    private List<String> getClassFunctionLines(AbstractTsClass tsClass) {
        List<String> classFunctionLines = new LinkedList<>();
        for (TsFunction function : tsClass.functions) {
            classFunctionLines.add("");
            classFunctionLines.addAll(
                    getFunctionLines(function).stream()
                            .map(line -> formatIndentsForCurlyBraces(line, 1))
                            .collect(Collectors.toList()));
        }
        return classFunctionLines;
    }

    private List<String> getFunctionLines(TsFunction function) {
        if (function == null) return Collections.singletonList("");

        List<String> functionLines = new LinkedList<>();
        functionLines.add(function.name + "(" + String.join(", ", function.arguments) + ") {");
        for (TsFunction childFunction : function.childFunctions) {
            List<String> childFunctionLines = getFunctionLines(childFunction);
            functionLines.add("\tfunction " + childFunctionLines.get(0)); // Only inner children have the `function` prefix
            functionLines.addAll(
                    childFunctionLines.subList(1, childFunctionLines.size()).stream()
                            .map(line -> formatIndentsForCurlyBraces(line, 1)).collect(Collectors.toList()));
            functionLines.add("");
        }
        functionLines.addAll(getStatementLines(function.statements));
        functionLines.add("}");
        return functionLines;
    }

    private List<String> getStatementLines(List<TsStatement> statements) {
        return statements.stream()
                .map(tsStatement -> formatIndentsForCurlyBraces(tsStatement.text, 1))
                .collect(Collectors.toList());
    }

    private List<String> getConstructor(AbstractTsClass tsClass) {
        if (tsClass.dependencies.size() == 0) return Collections.singletonList("\tconstructor() { }");

        List<String> constructorLines = new LinkedList<>();
        constructorLines.add("\tconstructor(");
        constructorLines.add(tsClass.dependencies.stream().map((dependency) -> {
            TsDependency tsDependency = upgradePathService.getServiceDependency(dependency, tsClass);
            return "\t\tprivate " + lowerFirst(tsDependency.name) + ": " + tsDependency.name;
        }).collect(Collectors.joining(",\n")));
        constructorLines.add("\t) { }");
        return constructorLines;
    }

    private List<String> getServiceImports(AbstractTsClass tsClass) {
        return tsClass.dependencies.stream().map((dependency) -> {
            TsDependency tsDependency = upgradePathService.getServiceDependency(dependency, tsClass);
            return "import {" + tsDependency.name + "} from '" + tsDependency.packagePath + "';";
        }).collect(Collectors.toList());
    }

    private void writeFile(List<String> fileLines, String filepath) throws UpgraderException {
        try {
            Path path = Paths.get(filepath);
            Files.write(path, fileLines);
        } catch (IOException e) {
            System.err.println("Failed to write to " + filepath + ": " + e.getMessage());
            throw new UpgraderException(e);
        }
    }

    private void createDirectory(String directory) throws UpgraderException {
        try {
            Files.createDirectories(Paths.get(directory));
        } catch (IOException e) {
            throw new UpgraderException(e);
        }
    }

    private String kebabToCamelUpperFirst(String kebabCase) {
        String[] parts = kebabCase.split("-");
        String camelCase = "";
        for (String part : parts) {
            if (part.length() <= 1) {
                camelCase += part.toUpperCase();
            } else {
                camelCase += part.substring(0, 1).toUpperCase() + part.substring(1);
            }
        }
        return camelCase;
    }

    private String lowerFirst(String upperFirst) {
        if (upperFirst == null || upperFirst.length() == 0) return upperFirst;
        return upperFirst.substring(0, 1).toLowerCase() + upperFirst.substring(1);
    }

    private void addErrorsToOutput(final List<String> output, AbstractComponent component) {
        // TODO: add errors from component.originallySourcedFrom
        //output.addAll(component.upgradeErrors.stream())
    }
}
