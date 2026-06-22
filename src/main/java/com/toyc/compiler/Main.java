package com.toyc.compiler;

import com.toyc.compiler.ast.ASTNode;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import toyc.ToyCLexer;
import toyc.ToyCParser;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        // Read from standard input as required by the task
        CharStream input = CharStreams.fromStream(System.in);
        
        // Lexical analysis
        ToyCLexer lexer = new ToyCLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        
        // Syntax analysis
        ToyCParser parser = new ToyCParser(tokens);
        ParseTree tree = parser.compUnit();
        
        // Build AST
        AstBuilder builder = new AstBuilder();
        ASTNode ast = builder.visit(tree);
        
        // Generate Code to standard output
        CodeGen codeGen = new CodeGen(System.out);
        codeGen.generate(ast);
    }
}