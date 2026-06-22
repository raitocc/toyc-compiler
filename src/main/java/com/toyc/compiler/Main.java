package com.toyc.compiler;

import com.toyc.compiler.ast.AST;
import com.toyc.compiler.semantic.SemanticAnalyzer;
import com.toyc.compiler.ir.TAC;
import com.toyc.compiler.ir.IRBuilder;
import com.toyc.compiler.optimize.Optimizer;
import com.toyc.compiler.backend.RiscvCodeGen;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.*;
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

        try {
            CharStream input = CharStreams.fromStream(System.in);
            
            ToyCLexer lexer = new ToyCLexer(input);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            ToyCParser parser = new ToyCParser(tokens);
            
            // Add custom error listener
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
            AST.Node semanticAst = semanticAnalyzer.analyze(ast);
            
            IRBuilder irBuilder = new IRBuilder();
            List<TAC> instructions = irBuilder.build(semanticAst);
            
            if (optimize) {
                Optimizer optimizer = new Optimizer();
                instructions = optimizer.optimize(instructions);
            }
            
            RiscvCodeGen codeGen = new RiscvCodeGen(System.out, instructions);
            codeGen.generate();
            
        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }
}