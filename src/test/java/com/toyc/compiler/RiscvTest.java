package com.toyc.compiler;

import com.toyc.compiler.ast.AST;
import com.toyc.compiler.backend.RiscvGenerator;
import com.toyc.compiler.semantic.SemanticAnalyzer;
import com.toyc.compiler.ir.IrGenerator;
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

public class RiscvTest {

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
                     boolean isInvalid = relativePath.contains("invalid") || path.toString().contains("invalid");
                     tests.add(DynamicTest.dynamicTest(relativePath, () -> {
                         if (isInvalid) {
                             assertThrows(Exception.class, () -> runCompiler(path), 
                                 "Expected compilation error (syntax or semantic) for testcase: " + relativePath);
                         } else {
                             String actualAsm = runCompiler(path);
                             Path asmPath = path.resolveSibling(path.getFileName().toString().replace(".tc", ".s"));
                             boolean regen = Boolean.parseBoolean(System.getProperty("regen", "false"));
                             if (regen) {
                                 Files.writeString(asmPath, actualAsm, StandardCharsets.UTF_8);
                             } else {
                                 assertTrue(Files.exists(asmPath), "Expected ASM file does not exist: " + asmPath);
                                 String expectedAsm = Files.readString(asmPath, StandardCharsets.UTF_8);
                                 assertEquals(expectedAsm.replace("\r\n", "\n").trim(), actualAsm.replace("\r\n", "\n").trim());
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

        SemanticAnalyzer semanticAnalyzer = new SemanticAnalyzer();
        semanticAnalyzer.analyze(ast);

        IrGenerator irGen = new IrGenerator();
        ast.accept(irGen);

        RiscvGenerator backend = new RiscvGenerator();
        return backend.generate(irGen.program);
    }
}
