package org.angularjsupgrader;

import org.angularjsupgrader.model.angularjs.*;
import org.angularjsupgrader.parser.JavaScriptParser;
import org.angularjsupgrader.parser.JavaScriptParserBaseVisitor;
import org.angularjsupgrader.parser.JavaScriptParserVisitor;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNode;


/**
 * Created by Philip Harris on 15/01/2020
 */
public class AngularJsParserVisitor
        extends JavaScriptParserBaseVisitor
        implements JavaScriptParserVisitor {

    private final JsProgram program;
    private JsFile currentFile;
    private JsFunction currentFunction = null;
    private JsStatementBranch currentStatementBranch = null;

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

    private void visitAndCreateStatement(RuleNode ruleNode) {
        JsStatementBranch newBranch = new JsStatementBranch();
        currentStatementBranch.subParts.add(newBranch);
        newBranch.parent = currentStatementBranch;
        currentStatementBranch = newBranch;
        for (int i = 0; i < ruleNode.getChildCount(); i++) {
            if (ruleNode.getChild(i) instanceof TerminalNode) {
                visitLeaf((TerminalNode) ruleNode.getChild(i));
            } else if (ruleNode.getChild(i) instanceof RuleNode) {
                visitAndCreateStatement((RuleNode) ruleNode.getChild(i));
            } else {
                System.out.println(ruleNode.getChild(i) + " : " + ruleNode.getChild(i).getText());
            }
        }
        currentStatementBranch = newBranch.parent;
    }

    private void visitLeaf(TerminalNode terminalNode) {
        JsStatementLeaf newLeaf = new JsStatementLeaf();
        currentStatementBranch.subParts.add(newLeaf);
        newLeaf.text = terminalNode.getText();
        newLeaf.parent = currentStatementBranch;
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
        visitAndCreateFunction(injectable.injectableName, arrayElementsList.getChild(arrayElementsList.getChildCount() - 1));
    }

    // TODO: rather than calling super.visitStatement(), we should really be calling a private method
    @Override
    public Object visitStatement(JavaScriptParser.StatementContext ctx) {
        if (currentFunction == null) {
            return super.visitStatement(ctx);
        }
        // If we are a function, then let's not add the function declaration as a statement
        RuleNode newCtx = getFirstDecendantWithMoreThan1Child(ctx); // We need this as statements are recursive
        if (newCtx instanceof JavaScriptParser.FunctionDeclarationContext ||
                newCtx instanceof JavaScriptParser.AssignmentExpressionContext && newCtx.getChild(newCtx.getChildCount() - 1) instanceof JavaScriptParser.FunctionExpressionContext) {
            return super.visitStatement(ctx);
        }


        JsStatement statement = new JsStatement();
        statement.type = JavaScriptParser.RULE_statement;
        statement.originalText = newCtx.getText();
        currentFunction.statements.add(statement);
        currentStatementBranch = statement;
        visitAndCreateStatement(ctx);
        Object result = super.visitStatement(ctx);
        currentStatementBranch = null;
        return result;
    }

    private RuleNode getFirstDecendantWithMoreThan1Child(RuleNode ctx) {
        if ((ctx.getChildCount() != 1 &&
                !(ctx.getChildCount() == 2 && ctx.getChild(1) instanceof JavaScriptParser.EosContext)) ||
                !(ctx.getChild(0) instanceof RuleNode)) {
            return ctx;
        }
        return getFirstDecendantWithMoreThan1Child((RuleNode) ctx.getChild(0));
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
        String functionName = null;
        // Try to see if we have `functionName = function(){ ...}` syntax
        if (ctx.parent != null && ctx.parent.parent instanceof JavaScriptParser.AssignmentExpressionContext) {
            ParseTree identifiable = ctx.parent.parent.getChild(0);
            while (identifiable.getChildCount() > 0) { // Loop through to get the last leaf on the tree
                identifiable = identifiable.getChild(identifiable.getChildCount() - 1);
            }
            functionName = identifiable.getText();
        }
        return visitAndCreateFunction(functionName, ctx);
    }

    @Override
    public Object visitMemberDotExpression(JavaScriptParser.MemberDotExpressionContext ctx) {
        if ("$inject".equals(ctx.getChild(2).getText()) && ctx.parent.getChildCount() >= 3) {
            JsInjectStatement injectStatement = new JsInjectStatement();
            injectStatement.functionName = ctx.getChild(0).getText();

            if (ctx.parent.getChild(2).getChild(0).getChildCount() >= 3) { // parent for assign, getChild(2) for arrayLiteralExpression, getChild(0) for arrayLiteral
                ParseTree arrayElements = ctx.parent.getChild(2).getChild(0).getChild(1);
                for (int i = 0; i < arrayElements.getChildCount(); i += 2) { // +=2 for skipping commas in array
                    injectStatement.injects.add(trimQuotes(arrayElements.getChild(i).getText()));
                }
            }
            currentFile.injectStatements.add(injectStatement);
            return ctx;
        }
        return super.visitMemberDotExpression(ctx);
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
