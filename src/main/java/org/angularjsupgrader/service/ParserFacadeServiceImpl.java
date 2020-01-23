package org.angularjsupgrader.service;

import org.angularjsupgrader.exception.UpgraderException;
import org.angularjsupgrader.parser.JavaScriptLexer;
import org.angularjsupgrader.parser.JavaScriptParser;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * FUEL-4369 FuelArchitect
 * <p>
 * Created by Philip on 11/01/2020
 */
public class ParserFacadeServiceImpl {

    public JavaScriptParser parse(String filename) throws UpgraderException {
        String fullFilePath = getClass().getClassLoader().getResource(filename).getPath();

        try {
            InputStream inputStream = new FileInputStream(fullFilePath);
            CharStream charStream = CharStreams.fromStream(inputStream);
            JavaScriptLexer lexer = new JavaScriptLexer(charStream);
            System.out.println("FINISHED LEXING");
            return new JavaScriptParser(new CommonTokenStream(lexer));
        } catch (FileNotFoundException e) {
            System.out.println("Could not find " + filename + " to parse");
            throw new UpgraderException(e);
        } catch (IOException e) {
            System.out.println("Failed to open " + filename + " for parsing");
            throw new UpgraderException(e);
        }
    }
}
