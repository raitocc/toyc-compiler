package com.toyc.compiler;

import com.toyc.compiler.ast.*;
import java.io.PrintStream;

public class CodeGen implements ASTVisitor {
    private PrintStream out;

    public CodeGen(PrintStream out) {
        this.out = out;
    }

    public void generate(ASTNode root) {
        root.accept(this);
    }

    @Override
    public void visit(FuncDef node) {
        out.println("\t.text");
        out.println("\t.globl " + node.name);
        out.println(node.name + ":");
        for (Stmt stmt : node.body) {
            stmt.accept(this);
        }
    }

    @Override
    public void visit(ReturnStmt node) {
        node.expr.accept(this);
        out.println("\tret");
    }

    @Override
    public void visit(NumberExpr node) {
        out.println("\tli a0, " + node.value);
    }
}
