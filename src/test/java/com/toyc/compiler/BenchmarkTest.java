package com.toyc.compiler;

import com.toyc.compiler.ast.AST;
import com.toyc.compiler.backend.RiscvGenerator;
import com.toyc.compiler.ir.IrGenerator;
import com.toyc.compiler.ir.IR;
import com.toyc.compiler.semantic.SemanticAnalyzer;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.Test;
import toyc.ToyCLexer;
import toyc.ToyCParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class BenchmarkTest {

    @Test
    public void runAllBenchmarks() throws Exception {
        Path perfDir = Paths.get("src/test/resources/testcases/perf");
        if (!Files.exists(perfDir)) {
            System.out.println("No perf directory found.");
            return;
        }

        System.out.println("======================================================");
        System.out.println("                 TOYC BENCHMARK RUNNER                ");
        System.out.println("======================================================");
        System.out.printf("%-15s | %-12s | %-15s | %-15s%n", "Test Case", "GCC -O2 (s)", "ToyC NoOpt (s)", "ToyC IR-Opt (s)");
        System.out.println("-------------------------------------------------------------------------");

        try (Stream<Path> paths = Files.walk(perfDir)) {
            paths.filter(Files::isRegularFile)
                 .filter(p -> p.toString().endsWith(".tc"))
                 .forEach(p -> {
                     try {
                         runSingleBenchmark(p);
                     } catch (Exception e) {
                         System.err.println("Failed to run benchmark for " + p.getFileName() + ": " + e.getMessage());
                     }
                 });
        }
        System.out.println("======================================================");
    }

    private void runSingleBenchmark(Path tcFile) throws Exception {
        String baseName = tcFile.getFileName().toString().replace(".tc", "");
        String sourceCode = Files.readString(tcFile);

        // 1. 编译生成 NoOpt 汇编代码
        ToyCLexer lexer1 = new ToyCLexer(CharStreams.fromString(sourceCode));
        ToyCParser parser1 = new ToyCParser(new CommonTokenStream(lexer1));
        AST.Node ast1 = new AstBuilder().visit(parser1.compUnit());
        new SemanticAnalyzer().analyze(ast1);
        IrGenerator irGen1 = new IrGenerator();
        ast1.accept(irGen1);
        String toycAsmNoOpt = new RiscvGenerator().generate(irGen1.program);

        // 2. 编译生成 IR-Opt 汇编代码
        ToyCLexer lexer2 = new ToyCLexer(CharStreams.fromString(sourceCode));
        ToyCParser parser2 = new ToyCParser(new CommonTokenStream(lexer2));
        AST.Node ast2 = new AstBuilder().visit(parser2.compUnit());
        new SemanticAnalyzer().analyze(ast2);
        IrGenerator irGen2 = new IrGenerator();
        ast2.accept(irGen2);
        IR.Program optProgram = new com.toyc.compiler.ir.IrOptimizer().optimize(irGen2.program);
        String toycAsmOpt = new RiscvGenerator().generate(optProgram);

        // 3. 构造在 VM 内运行的自动化评测 Bash 脚本
        String bashScript = "cat > /tmp/perf_" + baseName + ".c <<'EOF'\n" +
                sourceCode + "\nEOF\n" +
                "cat > /tmp/perf_noopt_" + baseName + ".s <<'EOF'\n" +
                ".globl _start\n_start:\n    call main\n    li a7, 93\n    ecall\n" +
                toycAsmNoOpt + "\nEOF\n" +
                "cat > /tmp/perf_opt_" + baseName + ".s <<'EOF'\n" +
                ".globl _start\n_start:\n    call main\n    li a7, 93\n    ecall\n" +
                toycAsmOpt + "\nEOF\n" +
                "riscv64-unknown-elf-gcc -O2 -fno-ipa-cp -march=rv32im -mabi=ilp32 /tmp/perf_" + baseName + ".c -o /tmp/perf_gcc_" + baseName + ".out\n" +
                "riscv64-unknown-elf-gcc -march=rv32im -mabi=ilp32 -O0 -nostdlib -mno-relax /tmp/perf_noopt_" + baseName + ".s -o /tmp/perf_toyc_noopt_" + baseName + ".out\n" +
                "riscv64-unknown-elf-gcc -march=rv32im -mabi=ilp32 -O0 -nostdlib -mno-relax /tmp/perf_opt_" + baseName + ".s -o /tmp/perf_toyc_opt_" + baseName + ".out\n" +
                "/usr/bin/time -p qemu-riscv32 /tmp/perf_gcc_" + baseName + ".out 2> /tmp/time_gcc.txt\n" +
                "/usr/bin/time -p qemu-riscv32 /tmp/perf_toyc_noopt_" + baseName + ".out 2> /tmp/time_toyc_noopt.txt\n" +
                "/usr/bin/time -p qemu-riscv32 /tmp/perf_toyc_opt_" + baseName + ".out 2> /tmp/time_toyc_opt.txt\n" +
                "cat /tmp/time_gcc.txt\n" +
                "echo \"---\"\n" +
                "cat /tmp/time_toyc_noopt.txt\n" +
                "echo \"---\"\n" +
                "cat /tmp/time_toyc_opt.txt\n";

        // 将 bashScript 写入本地临时文件，防止直接命令行传参导致的转义问题
        File scriptFile = File.createTempFile("benchmark_script", ".sh");
        Files.writeString(scriptFile.toPath(), bashScript);

        // 3. 通过 SSH 传送给 VM 并在 bash 下执行
        ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "ssh vm \"bash -s\" < " + scriptFile.getAbsolutePath());
        Process process = pb.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }
        process.waitFor();
        scriptFile.delete();

        // 4. 解析输出结果
        double gccTime = parseUserTime(output.toString(), 0);
        double toycNoOptTime = parseUserTime(output.toString(), 1);
        double toycOptTime = parseUserTime(output.toString(), 2);

        System.out.printf("%-15s | %-12.3f | %-15.3f | %-15.3f%n", baseName, gccTime, toycNoOptTime, toycOptTime);
    }

    private double parseUserTime(String output, int index) {
        // time -p outputs lines like "user 0.07"
        Pattern pattern = Pattern.compile("user\\s+([0-9.]+)");
        Matcher matcher = pattern.matcher(output);
        int count = 0;
        while (matcher.find()) {
            if (count == index) {
                return Double.parseDouble(matcher.group(1));
            }
            count++;
        }
        return -1.0;
    }
}
