package com.toyc.compiler;

import com.toyc.compiler.ast.*;
import toyc.ToyCBaseVisitor;
import toyc.ToyCParser;

import java.util.ArrayList;
import java.util.List;

public class AstBuilder extends ToyCBaseVisitor<ASTNode> {

    @Override
    public ASTNode visitCompUnit(ToyCParser.CompUnitContext ctx) {
        if (!ctx.funcDef().isEmpty()) {
            return visit(ctx.funcDef(0));
        }
        return null;
    }

    @Override
    public ASTNode visitFuncDef(ToyCParser.FuncDefContext ctx) {
        String returnType = ctx.type().getText();
        String name = ctx.ID().getText();
        List<Stmt> body = new ArrayList<>();
        
        if (ctx.block() != null) {
            for (ToyCParser.StmtContext stmtCtx : ctx.block().stmt()) {
                body.add((Stmt) visit(stmtCtx));
            }
        }
        
        return new FuncDef(returnType, name, body);
    }

    @Override
    public ASTNode visitStmt(ToyCParser.StmtContext ctx) {
        if (ctx.getText().startsWith("return")) {
            Expr expr = (Expr) visit(ctx.expr());
            return new ReturnStmt(expr);
        }
        return super.visitStmt(ctx);
    }

    @Override
    public ASTNode visitExpr(ToyCParser.ExprContext ctx) {
        if (ctx.NUMBER() != null) {
            return new NumberExpr(Integer.parseInt(ctx.NUMBER().getText()));
        }
        return super.visitExpr(ctx);
    }
}
