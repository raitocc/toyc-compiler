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
            
            AstBuilder builder = new AstBuilder();
            AST.Node ast = builder.visit(tree);
            
            SemanticAnalyzer analyzer = new SemanticAnalyzer();
            analyzer.analyze(ast);
            
            AstPrinter printer = new AstPrinter();
            System.out.print(ast.accept(printer));
            
        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }
}