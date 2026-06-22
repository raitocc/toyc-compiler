package com.toyc.compiler;

import com.toyc.compiler.ast.AST;
import com.toyc.compiler.semantic.SemanticAnalyzer;
import com.toyc.compiler.ir.TAC;
import com.toyc.compiler.ir.IRBuilder;
import com.toyc.compiler.optimize.Optimizer;
import com.toyc.compiler.backend.RiscvCodeGen;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import toyc.ToyCLexer;
import toyc.ToyCParser;

import java.io.IOException;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {
        boolean optimize = false;
        for (String arg : args) {
            if ("-opt".equals(arg)) {
                optimize = true;
            }
        }

        // 1. Read from standard input
        CharStream input = CharStreams.fromStream(System.in);
        
        // 2. Lexical and Syntax analysis
        ToyCLexer lexer = new ToyCLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ToyCParser parser = new ToyCParser(tokens);
        ParseTree tree = parser.compUnit();
        
        // 3. Build AST
        AstBuilder builder = new AstBuilder();
        AST.Node ast = builder.visit(tree);
        
        // 4. Semantic Analysis & Constant Folding
        SemanticAnalyzer semanticAnalyzer = new SemanticAnalyzer();
        AST.Node semanticAst = semanticAnalyzer.analyze(ast);
        
        // 5. IR Generation (Three-Address Code)
        IRBuilder irBuilder = new IRBuilder();
        List<TAC> instructions = irBuilder.build(semanticAst);
        
        // 6. Optimization (if -opt is passed)
        if (optimize) {
            Optimizer optimizer = new Optimizer();
            instructions = optimizer.optimize(instructions);
        }
        
        // 7. Backend: RISC-V Code Generation
        RiscvCodeGen codeGen = new RiscvCodeGen(System.out, instructions);
        codeGen.generate();
    }
}