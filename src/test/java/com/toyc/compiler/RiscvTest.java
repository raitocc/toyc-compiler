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
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class RiscvTest {

    private static final Path TESTCASES_DIR = Paths.get("src", "test", "resources", "testcases");
    private static final Pattern EXPECT_RETURN_PATTERN = Pattern.compile("//\\s*EXPECT_RETURN:\\s*(\\d+)");

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
                             
                             // 尝试解析 EXPECT_RETURN 注释进行 E2E QEMU 验证
                             String sourceCode = Files.readString(path, StandardCharsets.UTF_8);
                             Matcher matcher = EXPECT_RETURN_PATTERN.matcher(sourceCode);
                             if (matcher.find()) {
                                 int expectedExitCode = Integer.parseInt(matcher.group(1));
                                 runE2eTestOnVM(actualAsm, expectedExitCode, path.getFileName().toString());
                             }
                         }
                     }));
                 });
        }
        return tests;
    }

    private void runE2eTestOnVM(String asm, int expectedExitCode, String fileName) {
        String baseName = fileName.replace(".tc", "");
        // Windows cmd or powershell escaping
        String sshCmd = "cat > /tmp/toyc_" + baseName + ".s && " +
                        "riscv64-unknown-elf-gcc -march=rv32im -mabi=ilp32 -O0 -nostdlib -mno-relax /tmp/toyc_" + baseName + ".s -o /tmp/toyc_" + baseName + ".out && " +
                        "qemu-riscv32 /tmp/toyc_" + baseName + ".out; " +
                        "echo EXIT_CODE=$?";
                        
        // 添加 _start 作为入口点，因为使用了 -nostdlib
        String stub = ".globl _start\n_start:\n    call main\n    li a7, 93\n    ecall\n\n";
        String finalAsm = stub + asm;

        ProcessBuilder pb = new ProcessBuilder("ssh", "vm", sshCmd);
        pb.redirectErrorStream(true);
        try {
            Process process = pb.start();
            try (OutputStream out = process.getOutputStream()) {
                out.write(finalAsm.getBytes(StandardCharsets.UTF_8));
                out.flush();
            }
            
            try (InputStream in = process.getInputStream()) {
                String output = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                int exitStatus = process.waitFor();
                assertEquals(0, exitStatus, "SSH connection or execution failed. Output:\n" + output);
                
                Matcher exitCodeMatcher = Pattern.compile("EXIT_CODE=(\\d+)").matcher(output);
                assertTrue(exitCodeMatcher.find(), "Could not find EXIT_CODE in output: " + output);
                int actualExitCode = Integer.parseInt(exitCodeMatcher.group(1));
                assertEquals(expectedExitCode, actualExitCode, "E2E Execution failed. Output:\n" + output);
            }
        } catch (Exception e) {
            fail("Failed to run E2E test on VM: " + e.getMessage());
        }
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
