package com.toyc.compiler.ast;

public abstract class ASTNode {
    public abstract void accept(ASTVisitor visitor);
}
