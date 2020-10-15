package org.angularjsupgrader;

import org.angularjsupgrader.model.UpgraderProperties;
import org.angularjsupgrader.model.typescript.TsProgram;
import org.angularjsupgrader.parser.JavaScriptParser;
import org.angularjsupgrader.service.AngularModelBuilderServiceImpl;
import org.angularjsupgrader.service.FileListerServiceImpl;
import org.angularjsupgrader.service.ParserFacadeServiceImpl;
import org.angularjsupgrader.service.PropertiesLoaderImpl;
import org.angularjsupgrader.service.generation.TypeScriptGenerationServiceImpl;
import org.angularjsupgrader.service.upgrader.AngularUpgraderImpl;
import org.angularjsupgrader.service.upgrader.StringServiceImpl;

import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {
        final UpgraderProperties upgraderProperties = (new PropertiesLoaderImpl()).getProperties();
        final StringServiceImpl stringService = new StringServiceImpl();
        final FileListerServiceImpl fileListerService = new FileListerServiceImpl();
        final ParserFacadeServiceImpl sampleFileParserService = new ParserFacadeServiceImpl();
        final AngularModelBuilderServiceImpl angularModelBuilderService = new AngularModelBuilderServiceImpl();
        final TypeScriptGenerationServiceImpl typeScriptFileGenerationService = new TypeScriptGenerationServiceImpl(upgraderProperties, stringService, fileListerService);
        final AngularUpgraderImpl angularUpgraderService = new AngularUpgraderImpl(stringService);

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
