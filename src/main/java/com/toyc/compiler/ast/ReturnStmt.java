package com.toyc.compiler.ast;

public class ReturnStmt extends Stmt {
    public Expr expr;

    public ReturnStmt(Expr expr) {
        this.expr = expr;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
