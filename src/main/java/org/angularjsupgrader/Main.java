package org.angularjsupgrader;

import org.angularjsupgrader.model.typescript.TsProgram;
import org.angularjsupgrader.parser.JavaScriptParser;
import org.angularjsupgrader.service.*;
import org.angularjsupgrader.service.upgrader.AngularUpgraderImpl;

import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {
        DirectoryFileListerServiceImpl directoryFileListerService = new DirectoryFileListerServiceImpl();
        ParserFacadeServiceImpl sampleFileParserService = new ParserFacadeServiceImpl();
        AngularModelBuilderServiceImpl angularModelBuilderService = new AngularModelBuilderServiceImpl();
        TypeScriptFileGenerationServiceImpl typeScriptFileGenerationService = new TypeScriptFileGenerationServiceImpl((new PropertiesLoaderServiceImpl()).getProperties());
        AngularUpgraderImpl angularUpgraderService = new AngularUpgraderImpl();

        List<String> files = directoryFileListerService.listJsFilesInDirectory("examples/");
        for (String filename : files) {
            System.out.println("PARSING: " + filename);
            JavaScriptParser parser = sampleFileParserService.parse(filename);
            JavaScriptParser.ProgramContext program = parser.program();
            System.out.println("BUILDING AngularJS MODEL: " + filename);
            angularModelBuilderService.buildModelFromAngularJs(program, filename);
        }
        TsProgram tsProgram = angularUpgraderService.upgradeAngularJsProgram(angularModelBuilderService.getJsProgram());
        typeScriptFileGenerationService.generateProgram(tsProgram);
    }
}
