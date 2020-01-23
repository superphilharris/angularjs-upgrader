package org.angularjsupgrader.service;

import org.angularjsupgrader.AngularJsParserVisitor;
import org.angularjsupgrader.exception.UpgraderException;
import org.angularjsupgrader.model.angularjs.JsProgram;
import org.angularjsupgrader.parser.JavaScriptParser;

/**
 * Created by Philip Harris on 15/01/2020
 */
public class AngularModelBuilderServiceImpl {

    private final AngularJsParserVisitor visitor;

    public AngularModelBuilderServiceImpl() {
        this.visitor = new AngularJsParserVisitor();
    }

    public void buildModelFromAngularJs(JavaScriptParser.ProgramContext parsedAngularJsFile, String filename) throws UpgraderException {
        visitor.visitParsedFile(filename, parsedAngularJsFile); // To parse another file within the same program, we need to just call visit() again on the same visitor
    }

    public JsProgram getJsProgram() {
        return this.visitor.getNgProgram();
    }
}

