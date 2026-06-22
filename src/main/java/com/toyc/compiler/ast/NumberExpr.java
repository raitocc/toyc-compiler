package com.toyc.compiler.ast;

public class NumberExpr extends Expr {
    public int value;

    public NumberExpr(int value) {
        this.value = value;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
