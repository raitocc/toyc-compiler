package com.toyc.compiler.ast;

public interface ASTVisitor {
    void visit(FuncDef node);
    void visit(ReturnStmt node);
    void visit(NumberExpr node);
}
