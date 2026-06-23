package com.toyc.compiler;

import com.toyc.compiler.ast.AST;
import com.toyc.compiler.ast.AstPrinter;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import toyc.ToyCLexer;
import toyc.ToyCParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class AstTest {

    private static final Path TESTCASES_DIR = Paths.get("src", "test", "resources", "testcases");

    @TestFactory
    public Collection<DynamicTest> dynamicTests() throws IOException {
        List<DynamicTest> tests = new ArrayList<>();
        if (!Files.exists(TESTCASES_DIR)) {
            return tests;
        }

        try (Stream<Path> paths = Files.walk(TESTCASES_DIR)) {
            paths.filter(Files::isRegularFile)
                 .filter(p -> p.toString().endsWith(".tc"))
                 .forEach(path -> {
                     String relativePath = TESTCASES_DIR.relativize(path).toString();
                     boolean isSyntaxError = relativePath.contains("syntax_error") || path.getFileName().toString().contains("syntax_error");
                     tests.add(DynamicTest.dynamicTest(relativePath, () -> {
                         if (isSyntaxError) {
                             assertThrows(Exception.class, () -> runCompiler(path), 
                                 "Expected syntax error for testcase: " + relativePath);
                         } else {
                             String actualAst = runCompiler(path);
                             Path astPath = path.resolveSibling(path.getFileName().toString().replace(".tc", ".ast"));
                             boolean regen = Boolean.parseBoolean(System.getProperty("regen", "false"));
                             if (regen) {
                                 Files.writeString(astPath, actualAst, StandardCharsets.UTF_8);
                             } else {
                                 assertTrue(Files.exists(astPath), "Expected AST file does not exist: " + astPath);
                                 String expectedAst = Files.readString(astPath, StandardCharsets.UTF_8);
                                 // Normalise line endings for cross-platform matching
                                 assertEquals(expectedAst.replace("\r\n", "\n").trim(), actualAst.replace("\r\n", "\n").trim());
                             }
                         }
                     }));
                 });
        }
        return tests;
    }

    private String runCompiler(Path sourcePath) throws IOException {
        String content = Files.readString(sourcePath, StandardCharsets.UTF_8);
        CharStream input = CharStreams.fromString(content);
        ToyCLexer lexer = new ToyCLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ToyCParser parser = new ToyCParser(tokens);

        parser.removeErrorListeners();
        parser.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
                throw new RuntimeException("Syntax Error at line " + line + ":" + charPositionInLine + " - " + msg);
            }
        });

        ParseTree tree = parser.compUnit();
        AstBuilder builder = new AstBuilder();
        AST.Node ast = builder.visit(tree);

        AstPrinter printer = new AstPrinter();
        return ast.accept(printer);
    }
}
