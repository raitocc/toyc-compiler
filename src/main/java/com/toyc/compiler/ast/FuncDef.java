package com.toyc.compiler.ast;

import java.util.List;

public class FuncDef extends ASTNode {
    public String returnType;
    public String name;
    public List<Stmt> body;

    public FuncDef(String returnType, String name, List<Stmt> body) {
        this.returnType = returnType;
        this.name = name;
        this.body = body;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
