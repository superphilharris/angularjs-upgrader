package org.angularjsupgrader.service;

import org.angularjsupgrader.exception.UpgraderException;
import org.angularjsupgrader.model.typescript.*;

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
public class TypeScriptFileGenerationServiceImpl {

    public void generateProgram(TsProgram program) throws UpgraderException {
        String outputDir = "upgradedAngular/";
        for (TsModule module : program.childModules) {
            generateModule(module, outputDir);
        }
    }

    private void generateModule(TsModule module, String parentDir) throws UpgraderException {
        String directory = parentDir + module.name + "/";
        System.out.println("Generating " + module.name + " in " + directory);
        createDirectory(directory);

        for (TsComponent component : module.components) {
            generateComponent(component, directory);
        }
        for (TsService service : module.services) {
            generateService(service, directory);
        }

        // Imports
        List<String> moduleLines = new LinkedList<>();
        moduleLines.add("import {NgModule} from '@angular/core';");
        for (TsComponent component : module.components) {
            moduleLines.add("import {" + kebabToCamelUpperFirst(component.name) + "Component} from '" + component.name + "/" + component.name + ".component.ts';");
        }

        moduleLines.add("\n// angularjs-upgrader generated from " + module.sourcedFrom);
        moduleLines.add("@NgModule({");
        moduleLines.add("\timports: [],");

        // Declarations
        List<String> componentDeclarations = module.components.stream().map(component -> {
            return "\n\t\t" + kebabToCamelUpperFirst(component.name) + "Component";
        }).collect(Collectors.toList());
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
        templateLines.add("<p>" + component.name + " works!</p>");
        writeFile(templateLines, directory + component.name + ".component.html");

        // Controller
        List<String> controllerLines = new LinkedList<>();
        controllerLines.add("import {Component, OnInit} from '@angular/core';");
        // TODO: add other imports
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
        controllerLines.add("\tconstructor() { }\n"); // TODO: add in injected services
        controllerLines.add("\tngOnInit() {\n\n\t}\n");
        // TODO: add in upgraded body
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

    private List<String> getClassFunctionLines(AbstractTsClass tsClass) {
        List<String> classFunctionLines = new LinkedList<>();
        for (TsFunction function : tsClass.functions) {
            classFunctionLines.add("\n\t" + function.name + "(" + String.join(", ", function.arguments) + ") {");
            // TODO: add function body, and any embedded functions
            classFunctionLines.add("\t}");
        }
        return classFunctionLines;
    }

    private void generateService(TsService service, String parentDirectory) throws UpgraderException {
        String className = kebabToCamelUpperFirst(service.name) + "Service";
        List<String> serviceLines = new LinkedList<>();
        serviceLines.add("import {Injectable} from '@angular/core';\n" +
                "import {Observable} from 'rxjs';\n\n");
        serviceLines.add("@Injectable({\n" +
                "\tprovidedIn: 'root'\n" +
                "})\n" +
                "export class " + className + " {\n" +
                "\tconstructor() { }");
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

        }
    }

    private String kebabToCamelUpperFirst(String kebabCase) {
        String[] parts = kebabCase.split("-");
        String camelCase = "";
        for (String part : parts) {
            camelCase += part.substring(0, 1).toUpperCase() + part.substring(1);
        }
        return camelCase;
    }
}
