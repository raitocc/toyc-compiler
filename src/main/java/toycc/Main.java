package toycc;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * ToyC Compiler entry point.
 * Reads ToyC source from stdin, outputs RISC-V32 assembly to stdout.
 */
public class Main {

    public static void main(String[] args) throws Exception {
        boolean optimize = false;

        // Parse command-line arguments
        for (String arg : args) {
            if ("-opt".equals(arg)) {
                optimize = true;
            }
        }

        // Read entire stdin
        String source;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            source = sb.toString();
        }

        if (source.isBlank()) {
            System.err.println("Error: Empty input");
            System.exit(1);
        }

        // Lexical analysis
        CharStream input = CharStreams.fromString(source);
        ToyCLexer lexer = new ToyCLexer(input);
        lexer.removeErrorListeners();
        lexer.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                    int line, int charPositionInLine, String msg,
                                    RecognitionException e) {
                System.err.println("Lexer error at line " + line + ":" +
                        charPositionInLine + ": " + msg);
            }
        });

        CommonTokenStream tokens = new CommonTokenStream(lexer);

        // Syntax analysis
        ToyCParser parser = new ToyCParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                    int line, int charPositionInLine, String msg,
                                    RecognitionException e) {
                System.err.println("Parse error at line " + line + ":" +
                        charPositionInLine + ": " + msg);
            }
        });

        ParseTree tree = parser.compUnit();

        // Check for syntax errors
        if (parser.getNumberOfSyntaxErrors() > 0) {
            System.err.println("Compilation failed due to syntax errors.");
            System.exit(1);
        }

        // Semantic analysis (builds AST and symbol table)
        SymbolTable symtab = new SymbolTable();
        SemanticAnalyzer analyzer = new SemanticAnalyzer(symtab);
        AST.Program program = (AST.Program) analyzer.visit(tree);

        if (analyzer.hasError()) {
            System.err.println("Compilation failed due to semantic errors.");
            System.exit(1);
        }

        // Code generation
        CodeGenerator codegen = new CodeGenerator(symtab);
        String assembly = codegen.generate(program);

        // Output
        System.out.print(assembly);
    }
}
