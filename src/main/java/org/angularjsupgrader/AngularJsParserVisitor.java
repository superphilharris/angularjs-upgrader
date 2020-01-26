package org.angularjsupgrader;

import org.angularjsupgrader.model.angularjs.*;
import org.angularjsupgrader.parser.JavaScriptParser;
import org.angularjsupgrader.parser.JavaScriptParserBaseVisitor;
import org.angularjsupgrader.parser.JavaScriptParserVisitor;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.RuleNode;


/**
 * Created by Philip Harris on 15/01/2020
 */
public class AngularJsParserVisitor
        extends JavaScriptParserBaseVisitor
        implements JavaScriptParserVisitor {

    private final JsProgram program;
    private JsFile currentFile;
    private JsFunction currentFunction = null;

    public AngularJsParserVisitor() {
        this.program = new JsProgram();
    }

    public JsProgram getNgProgram() {
        return program;
    }

    public Object visitParsedFile(String filename, JavaScriptParser.ProgramContext programContext) {
        this.currentFile = new JsFile();
        this.currentFile.filename = filename;
        this.program.files.add(this.currentFile);
        return super.visitProgram(programContext);
    }

    @Override
    public Object visitNgModuleDeclaration(JavaScriptParser.NgModuleDeclarationContext ctx) {
        String moduleName = trimQuotes(ctx.getChild(2).getText());
        JsModule module = getOrCreateModule(moduleName);

        for (int i = 0; i < ctx.children.size(); i++) {
            if (ctx.getChild(i) instanceof JavaScriptParser.NgNamedComponentDeclarationContext) {
                visitNgNamedComponentDeclaration((JavaScriptParser.NgNamedComponentDeclarationContext) ctx.getChild(i), module);
            }
            if (ctx.getChild(i) instanceof JavaScriptParser.NgComponentInjectableDeclarationContext) {
                visitNgComponentInjectableDeclaration((JavaScriptParser.NgComponentInjectableDeclarationContext) ctx.getChild(i), module);
            }
            if (ctx.getChild(i) instanceof JavaScriptParser.NgInlineComponentDeclarationContext) {
                visitNgInlineComponentDeclaration((JavaScriptParser.NgInlineComponentDeclarationContext) ctx.getChild(i), module);
            }
            // TODO: add in the other types
        }

        return ctx;
    }

    private void visitNgComponentInjectableDeclaration(JavaScriptParser.NgComponentInjectableDeclarationContext ctx, JsModule module) {
        String ngType = ctx.getChild(1).getText();
        String assignable = ctx.getChild(3).getText();

        final JsInjectable injectable = new JsInjectable();
        injectable.type = InjectableType.getByIdentifier(ngType);
        injectable.functionName = assignable;
        module.injectables.add(injectable);

        if (injectable.type != InjectableType.CONFIG)
            System.err.println("Invalid definition " + injectable + " when visiting NgComponentInjectableDeclaration");
    }

    private void visitNgNamedComponentDeclaration(JavaScriptParser.NgNamedComponentDeclarationContext ctx, JsModule module) {
        String ngType = ctx.getChild(1).getText();
        String stringLiteral = ctx.getChild(3).getText();
        String assignable = ctx.getChild(5).getText();

        final JsInjectable injectable = new JsInjectable();
        injectable.type = InjectableType.getByIdentifier(ngType);
        injectable.functionName = assignable;
        injectable.injectableName = trimQuotes(stringLiteral);
        module.injectables.add(injectable);

        if (injectable.type == null)
            System.err.println("Could not determine type of '" + ngType + "' for " + injectable);
    }

    private void visitNgInlineComponentDeclaration(JavaScriptParser.NgInlineComponentDeclarationContext ctx, JsModule module) {
        String ngType = ctx.getChild(1).getText();
        String stringLiteral = ctx.getChild(3).getText();
        ParseTree arrayElementsList = ctx.getChild(5).getChild(1); // arrayLiteral > elementsList

        final JsInjectable injectable = new JsInjectable();
        injectable.type = InjectableType.getByIdentifier(ngType);
        injectable.injectableName = trimQuotes(stringLiteral);
        injectable.functionName = injectable.injectableName;
        for (int i = 0; i < arrayElementsList.getChildCount() - 1; i++) { // The last one should be the function definition
            if (arrayElementsList.getChild(i) instanceof JavaScriptParser.ArrayElementContext) {
                injectable.injections.add(trimQuotes(arrayElementsList.getChild(i).getText()));
            }
        }
        module.injectables.add(injectable);
        System.err.println("Create and assign the funtion declaration below for " + injectable.injectableName);
        visitAndCreateFunction(injectable.injectableName, arrayElementsList.getChild(arrayElementsList.getChildCount() - 1));
    }

    @Override
    public Object visitStatement(JavaScriptParser.StatementContext ctx) {
        JsStatement statement = new JsStatement();
        statement.type = JavaScriptParser.RULE_statement;
        statement.originalText = ctx.getText();
//        currentFile.statements.add(statement);
        return super.visitStatement(ctx);
    }

    @Override
    public Object visitFunctionDeclaration(JavaScriptParser.FunctionDeclarationContext ctx) {
        String functionName = "";
        if (ctx.children.size() > 1) {
            functionName = ctx.getChild(1).getText();
        } else System.err.println("Could not determine function name for: " + ctx.getText());
        return visitAndCreateFunction(functionName, ctx);
    }

    @Override
    public Object visitAnoymousFunctionDecl(JavaScriptParser.AnoymousFunctionDeclContext ctx) {
        return visitAndCreateFunction(null, ctx);
    }


    private Object visitAndCreateFunction(String functionName, ParseTree ctx) {
        JsFunction function = new JsFunction();
        function.functionName = functionName;

        for (int i = 0; i < ctx.getChildCount(); i++) {
            ParseTree child = ctx.getChild(i);
            if (child instanceof JavaScriptParser.FormalParameterListContext) {
                for (int j = 0; j < child.getChildCount(); j += 2) { // += 2 to skip commas
                    function.arguments.add(child.getChild(j).getText());
                }
                break;
            }
        }

        // Add the function to it's parent list
        if (currentFunction != null) {
            currentFunction.childFunctions.add(function);
        } else {
            currentFile.childFunctions.add(function);
        }
        function.parent = currentFunction;
        currentFunction = function;
        Object result = super.visitChildren((RuleNode) ctx);
        currentFunction = function.parent;
        return result;
    }

    private JsModule getOrCreateModule(String moduleName) {
        if (currentFile.modules.containsKey(moduleName)) {
            return currentFile.modules.get(moduleName);
        }
        JsModule module = new JsModule();
        module.name = moduleName;
        module.sourcedFrom = currentFile.filename + " angular.module('" + moduleName + "')";
        currentFile.modules.put(moduleName, module);
        return module;
    }

    private String trimQuotes(String untrimmed) {
        return untrimmed.replace("\"", "").replace("'", "");
    }
}
