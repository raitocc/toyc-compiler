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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BenchmarkTest {
    private static final String BENCH_START_STUB = """
            .text
        .globl _start
        _start:
        read_cycle_start:
            rdcycleh s4
            rdcycle s3
            rdcycleh t0
            bne s4, t0, read_cycle_start
            li s1, 3
            li s2, 0
        bench_loop:
            call main
            add s2, s2, a0
            addi s1, s1, -1
            bne s1, zero, bench_loop
        read_cycle_end:
            rdcycleh t1
            rdcycle t0
            rdcycleh t2
            bne t1, t2, read_cycle_end
            sltu t3, t0, s3
            sub t0, t0, s3
            sub t1, t1, s4
            sub t1, t1, t3
            la t2, bench_cycles
            sw t0, 0(t2)
            sw t1, 4(t2)
            andi s5, s2, 255
            li a0, 1
            la a1, bench_cycles
            li a2, 8
            li a7, 64
            ecall
            mv a0, s5
            li a7, 93
            ecall
            .bss
            .align 2
        bench_cycles:
            .space 8
            .text
        """;

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
        System.out.printf("%-25s | %-4s | %-10s | %-10s | %-10s | %-8s | %-8s%n",
            "Test Case", "Ret", "GCC cyc", "NoOpt cyc", "IR-Opt cyc", "Opt/No", "GCC/Opt");
        System.out.println("------------------------------------------------------------------------------------------");

        try (Stream<Path> paths = Files.walk(perfDir)) {
            List<Path> cases = paths.filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".tc"))
                .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                .toList();
            for (Path p : cases) {
                runSingleBenchmark(p);
            }
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
        String toycAsmNoOpt = new RiscvGenerator(false).generate(irGen1.program);

        // 2. 编译生成 IR-Opt 汇编代码
        ToyCLexer lexer2 = new ToyCLexer(CharStreams.fromString(sourceCode));
        ToyCParser parser2 = new ToyCParser(new CommonTokenStream(lexer2));
        AST.Node ast2 = new AstBuilder().visit(parser2.compUnit());
        new SemanticAnalyzer().analyze(ast2);
        IrGenerator irGen2 = new IrGenerator();
        ast2.accept(irGen2);
        IR.Program optProgram = new com.toyc.compiler.ir.IrOptimizer().optimize(irGen2.program);
        String toycAsmOpt = new RiscvGenerator(true).generate(optProgram);

        // 3. 构造在 VM 内运行的自动化评测 Bash 脚本
        String bashScript = "cat > /tmp/perf_" + baseName + ".c <<'EOF'\n" +
                sourceCode + "\nEOF\n" +
                "cat > /tmp/perf_start_" + baseName + ".s <<'EOF'\n" +
                BENCH_START_STUB + "EOF\n" +
                "cat > /tmp/perf_noopt_" + baseName + ".s <<'EOF'\n" +
                BENCH_START_STUB +
                toycAsmNoOpt + "\nEOF\n" +
                "cat > /tmp/perf_opt_" + baseName + ".s <<'EOF'\n" +
                BENCH_START_STUB +
                toycAsmOpt + "\nEOF\n" +
                "set -e\n" +
                "riscv64-unknown-elf-gcc -O2 -ffreestanding -nostdlib -mno-relax -march=rv32im -mabi=ilp32 /tmp/perf_start_" + baseName + ".s /tmp/perf_" + baseName + ".c -o /tmp/perf_gcc_" + baseName + ".out\n" +
                "riscv64-unknown-elf-gcc -march=rv32im -mabi=ilp32 -O0 -nostdlib -mno-relax /tmp/perf_noopt_" + baseName + ".s -o /tmp/perf_toyc_noopt_" + baseName + ".out\n" +
                "riscv64-unknown-elf-gcc -march=rv32im -mabi=ilp32 -O0 -nostdlib -mno-relax /tmp/perf_opt_" + baseName + ".s -o /tmp/perf_toyc_opt_" + baseName + ".out\n" +
                "run_case() {\n" +
                "  label=\"$1\"\n" +
                "  exe=\"$2\"\n" +
                "  set +e\n" +
                "  start=$(date +%s%N)\n" +
                "  timeout 30 qemu-riscv32 \"$exe\" >/tmp/out_${label}.bin 2>/tmp/err_${label}.txt\n" +
                "  code=$?\n" +
                "  end=$(date +%s%N)\n" +
                "  set -e\n" +
                "  real=$(awk -v s=\"$start\" -v e=\"$end\" 'BEGIN { printf \"%.3f\", (e - s) / 1000000000 }')\n" +
                "  cycles=0\n" +
                "  if [ -s /tmp/out_${label}.bin ]; then\n" +
                "    cycles=$(od -An -tu4 -N8 /tmp/out_${label}.bin | awk '{ printf \"%.0f\", $2 * 4294967296 + $1 }')\n" +
                "  fi\n" +
                "  if [ \"$code\" = \"124\" ]; then\n" +
                "    echo \"RESULT $label TIMEOUT ${real:-0} ${cycles:-0}\"\n" +
                "  else\n" +
                "    echo \"RESULT $label $code ${real:-0} ${cycles:-0}\"\n" +
                "  fi\n" +
                "}\n" +
                "run_case gcc /tmp/perf_gcc_" + baseName + ".out\n" +
                "run_case noopt /tmp/perf_toyc_noopt_" + baseName + ".out\n" +
                "run_case opt /tmp/perf_toyc_opt_" + baseName + ".out\n";

        // 将 bashScript 写入本地临时文件，防止直接命令行传参导致的转义问题
        File scriptFile = File.createTempFile("benchmark_script", ".sh");
        Files.writeString(scriptFile.toPath(), bashScript);

        // 3. 通过 SSH 传送给 VM 并在 bash 下执行
        ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "ssh vm \"bash -s\" < " + scriptFile.getAbsolutePath());
        pb.redirectErrorStream(true);
        Process process = pb.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }
        int exitStatus = process.waitFor();
        scriptFile.delete();
        assertEquals(0, exitStatus, "Benchmark VM script failed for " + baseName + ":\n" + output);

        // 4. 解析输出结果
        Map<String, BenchResult> results = parseResults(output.toString());
        BenchResult gcc = results.get("gcc");
        BenchResult noopt = results.get("noopt");
        BenchResult opt = results.get("opt");

        assertTrue(gcc != null && noopt != null && opt != null, "Benchmark output is incomplete for " + baseName + ":\n" + output);
        assertFalse(gcc.timeout, "GCC execution timed out for " + baseName);
        assertFalse(noopt.timeout, "ToyC NoOpt execution timed out for " + baseName);
        assertFalse(opt.timeout, "ToyC IR-Opt execution timed out for " + baseName);
        assertEquals(gcc.exitCode, noopt.exitCode, "ToyC NoOpt exit code differs from GCC for " + baseName);
        assertEquals(gcc.exitCode, opt.exitCode, "ToyC IR-Opt exit code differs from GCC for " + baseName);

        double optVsNoOpt = opt.cycles > 0 ? (double) noopt.cycles / opt.cycles : 0;
        double optVsGcc = opt.cycles > 0 ? (double) gcc.cycles / opt.cycles : 0;
        System.out.printf("%-25s | %-4d | %-10s | %-10s | %-10s | %-8.2f | %-8.2f%n",
            baseName, gcc.exitCode, formatCycles(gcc.cycles), formatCycles(noopt.cycles),
            formatCycles(opt.cycles), optVsNoOpt, optVsGcc);
    }

    private String formatCycles(long cycles) {
        if (cycles >= 1_000_000) {
            return String.format("%.2fM", cycles / 1_000_000.0);
        }
        if (cycles >= 1_000) {
            return String.format("%.1fK", cycles / 1_000.0);
        }
        return String.valueOf(cycles);
    }

    private Map<String, BenchResult> parseResults(String output) {
        Pattern pattern = Pattern.compile("RESULT\\s+(\\S+)\\s+(TIMEOUT|\\d+)\\s+([0-9.]+)\\s+(\\d+)");
        Matcher matcher = pattern.matcher(output);
        Map<String, BenchResult> results = new java.util.HashMap<>();
        while (matcher.find()) {
            String label = matcher.group(1);
            boolean timeout = "TIMEOUT".equals(matcher.group(2));
            int exitCode = timeout ? -1 : Integer.parseInt(matcher.group(2));
            double real = Double.parseDouble(matcher.group(3));
            long cycles = Long.parseLong(matcher.group(4));
            results.put(label, new BenchResult(exitCode, timeout, real, cycles));
        }
        return results;
    }

    private record BenchResult(int exitCode, boolean timeout, double realSeconds, long cycles) {
    }
}
