package com.toyc.compiler;

import com.toyc.compiler.ast.AST;
import com.toyc.compiler.semantic.SemanticAnalyzer;
import com.toyc.compiler.ir.IrGenerator;
import com.toyc.compiler.ir.IR;
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

public class IrTest {

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
                     // 判断是否属于非法用例目录（无论语法错还是语义错，均存放在 invalid 目录下）
                     boolean isInvalid = relativePath.contains("invalid") || path.toString().contains("invalid");
                     tests.add(DynamicTest.dynamicTest(relativePath, () -> {
                         if (isInvalid) {
                             assertThrows(Exception.class, () -> runCompiler(path), 
                                 "Expected compilation error (syntax or semantic) for testcase: " + relativePath);
                         } else {
                             String actualIr = runCompiler(path);
                             Path irPath = path.resolveSibling(path.getFileName().toString().replace(".tc", ".ir"));
                             boolean regen = Boolean.parseBoolean(System.getProperty("regen", "false"));
                             if (regen) {
                                 Files.writeString(irPath, actualIr, StandardCharsets.UTF_8);
                             } else {
                                 assertTrue(Files.exists(irPath), "Expected IR file does not exist: " + irPath);
                                 String expectedIr = Files.readString(irPath, StandardCharsets.UTF_8);
                                 // Normalise line endings for cross-platform matching
                                 assertEquals(expectedIr.replace("\r\n", "\n").trim(), actualIr.replace("\r\n", "\n").trim());
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

        // 运行语义分析器校验 AST 是否语义合法
        SemanticAnalyzer semanticAnalyzer = new SemanticAnalyzer();
        semanticAnalyzer.analyze(ast);

        // 运行 IR 生成器
        IrGenerator irGen = new IrGenerator();
        ast.accept(irGen);

        StringBuilder sb = new StringBuilder();
        for (IR.FuncDef func : irGen.program.functions) {
            sb.append("FuncDef: ").append(func.name).append("(").append(String.join(", ", func.params)).append(")\n");
            for (IR.BasicBlock block : func.blocks) {
                sb.append(block.name).append(":\n");
                for (IR.IrInstr instr : block.instructions) {
                    sb.append("    ").append(instr.toString()).append("\n");
                }
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}
