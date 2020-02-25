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
            if (ctx.getChild(i) instanceof JavaScriptParser.NgComponentWithInjectionsDeclarationContext) { // TODO: parse this one
                visitNgComponentWithInjectionsDeclaration((JavaScriptParser.NgComponentWithInjectionsDeclarationContext) ctx.getChild(i), module);
            }
            // TODO: add in the other types
        }

        return ctx;
    }

    private JsStatementBranch visitAndCreateStatement(RuleNode ruleNode) {
        JsStatementBranch newBranch = new JsStatementBranch();
        newBranch.type = ruleNode.getClass();
        for (int i = 0; i < ruleNode.getChildCount(); i++) {
            if (ruleNode.getChild(i) instanceof TerminalNode) {
                newBranch.subParts.add(visitLeaf((TerminalNode) ruleNode.getChild(i)));
            } else if (ruleNode.getChild(i) instanceof RuleNode) {
                // If we are a named function, then let's not add the function declaration as a statement
                RuleNode possibleFunction = getFirstDecendantWithMoreThan1Child(ruleNode);

                if (!isNamedFunction(possibleFunction)) { // TODO: And is not a variable statement where all children are named functions
                    newBranch.subParts.add(visitAndCreateStatement((RuleNode) ruleNode.getChild(i)));
                }
            } else {
                System.out.println(ruleNode.getChild(i) + " : " + ruleNode.getChild(i).getText());
            }
        }
        return newBranch;
    }

    private boolean isNamedFunction(RuleNode ruleNode) {
        return ruleNode instanceof JavaScriptParser.FunctionDeclarationContext ||
                ((ruleNode instanceof JavaScriptParser.AssignmentExpressionContext ||
                        ruleNode instanceof JavaScriptParser.VariableDeclarationContext) &&
                        ruleNode.getChild(ruleNode.getChildCount() - 1) instanceof JavaScriptParser.FunctionExpressionContext);
    }

    private JsStatementLeaf visitLeaf(TerminalNode terminalNode) {
        JsStatementLeaf newLeaf = new JsStatementLeaf();
        newLeaf.text = terminalNode.getText();
        newLeaf.type = terminalNode.getClass();
        return newLeaf;
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
        for (int i = 0; i < arrayElementsList.getChildCount(); i++) {
            ParseTree arrayElement = arrayElementsList.getChild(i);
            if (arrayElement instanceof JavaScriptParser.ArrayElementContext &&
                    (arrayElement.getChild(0) instanceof JavaScriptParser.LiteralExpressionContext)) {
                injectable.injections.add(trimQuotes(arrayElementsList.getChild(i).getText()));
            }
        }
        module.injectables.add(injectable);

        ParseTree lastListElement = arrayElementsList.getChild(arrayElementsList.getChildCount() - 1).getChild(0);
        if (lastListElement instanceof JavaScriptParser.FunctionExpressionContext) {
            injectable.functionName = injectable.injectableName;
            visitAndCreateFunction(injectable.functionName, lastListElement.getChild(0));
        } else {
            injectable.functionName = trimQuotes(lastListElement.getText());
        }
    }

    private void visitNgComponentWithInjectionsDeclaration(JavaScriptParser.NgComponentWithInjectionsDeclarationContext ctx, JsModule module) {
        if (ctx.getChild(1).getText().equals("config")) {
            JsInjectable injectable = new JsInjectable();
            injectable.type = InjectableType.CONFIG;
            injectable.functionName = module.name + " config"; // This is kinda hacky, but we use it to resolve our function definition later

            ParseTree arrayElementsList = ctx.getChild(3).getChild(1);
            for (int i = 0; i < arrayElementsList.getChildCount(); i++) {
                ParseTree arrayElement = arrayElementsList.getChild(i);
                if (arrayElement instanceof JavaScriptParser.ArrayElementContext && !
                        (i == arrayElementsList.getChildCount() - 1 && arrayElement.getChild(0) instanceof JavaScriptParser.FunctionExpressionContext)) {
                    injectable.injections.add(trimQuotes(arrayElementsList.getChild(i).getText()));
                }
            }
            module.injectables.add(injectable);
            if (arrayElementsList.getChild(arrayElementsList.getChildCount() - 1).getChild(0) instanceof JavaScriptParser.FunctionExpressionContext) {
                visitAndCreateFunction(injectable.functionName, arrayElementsList.getChild(arrayElementsList.getChildCount() - 1).getChild(0).getChild(0));
            }
        } else {
            System.err.println("The statement is not config for " + ctx.getText());
        }
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
        // Try to see if we have `functionName = function(){ ...}` syntax
        if (ctx.parent != null && isNamedFunction(ctx.parent.parent)) {
            ParseTree identifiable = ctx.parent.parent.getChild(0);
            while (identifiable.getChildCount() > 0) { // Loop through to get the last leaf on the tree
                identifiable = identifiable.getChild(identifiable.getChildCount() - 1);
            }
            return visitAndCreateFunction(identifiable.getText(), ctx);
        }
        return super.visitAnoymousFunctionDecl(ctx);
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

        // Add the function to it's parent list
        if (currentFunction != null) {
            currentFunction.childFunctions.add(function);
        } else {
            currentFile.childFunctions.add(function);
        }
        function.parent = currentFunction;
        currentFunction = function;

        for (int i = 0; i < ctx.getChildCount(); i++) {
            ParseTree child = ctx.getChild(i);
            if (child instanceof JavaScriptParser.FormalParameterListContext) {
                for (int j = 0; j < child.getChildCount(); j += 2) { // += 2 to skip commas
                    function.arguments.add(child.getChild(j).getText());
                }
            }
            if (child instanceof JavaScriptParser.FunctionBodyContext && child.getChildCount() == 1) {

                ParseTree sourceElements = child.getChild(0);
                for (int j = 0; j < sourceElements.getChildCount(); j++) {
                    currentFunction.statements.add(visitAndCreateStatement((RuleNode) sourceElements.getChild(j)));
                }
            }
        }

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
