package com.toyc.compiler;

import com.toyc.compiler.ast.AST;
import com.toyc.compiler.ast.AstPrinter;
import com.toyc.compiler.semantic.SemanticAnalyzer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import toyc.ToyCLexer;
import toyc.ToyCParser;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        boolean enableOpt = false;
        for (String arg : args) {
            if ("-opt".equals(arg)) {
                enableOpt = true;
                break;
            }
        }

        try {
            CharStream input = CharStreams.fromStream(System.in);
            
            ToyCLexer lexer = new ToyCLexer(input);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            ToyCParser parser = new ToyCParser(tokens);
            
            // 添加自定义错误监听器
            parser.removeErrorListeners();
            parser.addErrorListener(new BaseErrorListener() {
                @Override
                public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
                    throw new RuntimeException("Syntax Error at line " + line + ":" + charPositionInLine + " - " + msg);
                }
            });
            
            ParseTree tree = parser.compUnit();
            
            // 1. 构建 AST
            AstBuilder builder = new AstBuilder();
            AST.Node ast = builder.visit(tree);
            
            // 2. 语义分析
            SemanticAnalyzer analyzer = new SemanticAnalyzer();
            analyzer.analyze(ast);
            
            // 3. 中端 IR 生成
            com.toyc.compiler.ir.IrGenerator irGen = new com.toyc.compiler.ir.IrGenerator();
            ast.accept(irGen);
            
            // 如果开启了优化参数
            if (enableOpt) {
                System.err.println("Optimization (-opt) enabled: running IrOptimizer and register allocation...");
                irGen.program = new com.toyc.compiler.ir.IrOptimizer().optimize(irGen.program);
            }

            // 4. 后端 RISC-V 生成：-opt 开启时才启用寄存器分配
            com.toyc.compiler.backend.RiscvGenerator riscvGen = new com.toyc.compiler.backend.RiscvGenerator(enableOpt);
            
            String asm = riscvGen.generate(irGen.program);
            
            // 直接输出汇编代码到标准输出
            System.out.println(asm);
            
        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }
}
