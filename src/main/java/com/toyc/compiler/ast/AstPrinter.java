package com.toyc.compiler.ast;

import java.util.List;

public class AstPrinter implements AST.Visitor<String> {
    private int indent = 0;

    private String getIndent() {
        return "  ".repeat(indent);
    }

    private String visitChild(AST.Node node) {
        if (node == null) {
            return getIndent() + "null\n";
        }
        indent++;
        String res = node.accept(this);
        indent--;
        return res;
    }

    private String visitChildList(List<? extends AST.Node> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return getIndent() + "[]\n";
        }
        StringBuilder sb = new StringBuilder();
        indent++;
        for (AST.Node node : nodes) {
            sb.append(node.accept(this));
        }
        indent--;
        return sb.toString();
    }

    @Override
    public String visit(AST.CompUnit node) {
        StringBuilder sb = new StringBuilder();
        sb.append(getIndent()).append("CompUnit\n");
        for (AST.Node element : node.elements) {
            sb.append(visitChild(element));
        }
        return sb.toString();
    }

    @Override
    public String visit(AST.ConstDecl node) {
        StringBuilder sb = new StringBuilder();
        sb.append(getIndent()).append("ConstDecl (name: ").append(node.name).append(")\n");
        sb.append(visitChild(node.initExpr));
        return sb.toString();
    }

    @Override
    public String visit(AST.VarDecl node) {
        StringBuilder sb = new StringBuilder();
        sb.append(getIndent()).append("VarDecl (name: ").append(node.name).append(")\n");
        sb.append(visitChild(node.initExpr));
        return sb.toString();
    }

    @Override
    public String visit(AST.BlockStmt node) {
        StringBuilder sb = new StringBuilder();
        sb.append(getIndent()).append("BlockStmt\n");
        for (AST.Stmt stmt : node.stmts) {
            sb.append(visitChild(stmt));
        }
        return sb.toString();
    }

    @Override
    public String visit(AST.EmptyStmt node) {
        return getIndent() + "EmptyStmt\n";
    }

    @Override
    public String visit(AST.ExprStmt node) {
        StringBuilder sb = new StringBuilder();
        sb.append(getIndent()).append("ExprStmt\n");
        sb.append(visitChild(node.expr));
        return sb.toString();
    }

    @Override
    public String visit(AST.AssignStmt node) {
        StringBuilder sb = new StringBuilder();
        sb.append(getIndent()).append("AssignStmt (name: ").append(node.name).append(")\n");
        sb.append(visitChild(node.expr));
        return sb.toString();
    }

    @Override
    public String visit(AST.IfStmt node) {
        StringBuilder sb = new StringBuilder();
        sb.append(getIndent()).append("IfStmt\n");
        sb.append(getIndent()).append("  cond:\n");
        sb.append(visitChild(node.cond));
        sb.append(getIndent()).append("  then:\n");
        sb.append(visitChild(node.thenStmt));
        if (node.elseStmt != null) {
            sb.append(getIndent()).append("  else:\n");
            sb.append(visitChild(node.elseStmt));
        }
        return sb.toString();
    }

    @Override
    public String visit(AST.WhileStmt node) {
        StringBuilder sb = new StringBuilder();
        sb.append(getIndent()).append("WhileStmt\n");
        sb.append(getIndent()).append("  cond:\n");
        sb.append(visitChild(node.cond));
        sb.append(getIndent()).append("  body:\n");
        sb.append(visitChild(node.body));
        return sb.toString();
    }

    @Override
    public String visit(AST.BreakStmt node) {
        return getIndent() + "BreakStmt\n";
    }

    @Override
    public String visit(AST.ContinueStmt node) {
        return getIndent() + "ContinueStmt\n";
    }

    @Override
    public String visit(AST.ReturnStmt node) {
        StringBuilder sb = new StringBuilder();
        sb.append(getIndent()).append("ReturnStmt\n");
        if (node.expr != null) {
            sb.append(visitChild(node.expr));
        } else {
            sb.append(getIndent()).append("  null\n");
        }
        return sb.toString();
    }

    @Override
    public String visit(AST.FuncDef node) {
        StringBuilder sb = new StringBuilder();
        sb.append(getIndent()).append("FuncDef (returnType: ").append(node.returnType)
          .append(", name: ").append(node.name).append(")\n");
        sb.append(getIndent()).append("  params:\n");
        sb.append(visitChildList(node.params));
        sb.append(getIndent()).append("  body:\n");
        sb.append(visitChild(node.body));
        return sb.toString();
    }

    @Override
    public String visit(AST.Param node) {
        return getIndent() + "Param (name: " + node.name + ")\n";
    }

    @Override
    public String visit(AST.NumberExpr node) {
        return getIndent() + "NumberExpr (value: " + node.value + ")\n";
    }

    @Override
    public String visit(AST.IdExpr node) {
        return getIndent() + "IdExpr (name: " + node.name + ")\n";
    }

    @Override
    public String visit(AST.CallExpr node) {
        StringBuilder sb = new StringBuilder();
        sb.append(getIndent()).append("CallExpr (funcName: ").append(node.funcName).append(")\n");
        sb.append(getIndent()).append("  args:\n");
        sb.append(visitChildList(node.args));
        return sb.toString();
    }

    @Override
    public String visit(AST.UnaryExpr node) {
        StringBuilder sb = new StringBuilder();
        sb.append(getIndent()).append("UnaryExpr (op: ").append(node.op).append(")\n");
        sb.append(visitChild(node.expr));
        return sb.toString();
    }

    @Override
    public String visit(AST.BinaryExpr node) {
        StringBuilder sb = new StringBuilder();
        sb.append(getIndent()).append("BinaryExpr (op: ").append(node.op).append(")\n");
        sb.append(visitChild(node.left));
        sb.append(visitChild(node.right));
        return sb.toString();
    }
}
