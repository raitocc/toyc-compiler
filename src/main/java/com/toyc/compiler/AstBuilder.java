package com.toyc.compiler;

import com.toyc.compiler.ast.AST.*;
import toyc.ToyCBaseVisitor;
import toyc.ToyCParser;

import java.util.ArrayList;
import java.util.List;

public class AstBuilder extends ToyCBaseVisitor<Node> {

    @Override
    public Node visitCompUnit(ToyCParser.CompUnitContext ctx) {
        List<Node> elements = new ArrayList<>();
        if (ctx.decl() != null) {
            for (ToyCParser.DeclContext declCtx : ctx.decl()) {
                elements.add(visit(declCtx));
            }
        }
        if (ctx.funcDef() != null) {
            for (ToyCParser.FuncDefContext funcCtx : ctx.funcDef()) {
                elements.add(visit(funcCtx));
            }
        }
        elements.clear();
        for (int i = 0; i < ctx.getChildCount(); i++) {
            if (ctx.getChild(i) instanceof ToyCParser.DeclContext || ctx.getChild(i) instanceof ToyCParser.FuncDefContext) {
                elements.add(visit(ctx.getChild(i)));
            }
        }
        return new CompUnit(elements);
    }

    @Override
    public Node visitConstDecl(ToyCParser.ConstDeclContext ctx) {
        String name = ctx.ID().getText();
        Expr initExpr = (Expr) visit(ctx.expr());
        return new ConstDecl(name, initExpr);
    }

    @Override
    public Node visitVarDecl(ToyCParser.VarDeclContext ctx) {
        String name = ctx.ID().getText();
        Expr initExpr = (Expr) visit(ctx.expr());
        return new VarDecl(name, initExpr);
    }

    @Override
    public Node visitStmtBlock(ToyCParser.StmtBlockContext ctx) {
        return visit(ctx.block());
    }

    @Override
    public Node visitStmtEmpty(ToyCParser.StmtEmptyContext ctx) {
        return new EmptyStmt();
    }

    @Override
    public Node visitStmtExpr(ToyCParser.StmtExprContext ctx) {
        return new ExprStmt((Expr) visit(ctx.expr()));
    }

    @Override
    public Node visitStmtAssign(ToyCParser.StmtAssignContext ctx) {
        String name = ctx.ID().getText();
        Expr expr = (Expr) visit(ctx.expr());
        return new AssignStmt(name, expr);
    }

    @Override
    public Node visitStmtDecl(ToyCParser.StmtDeclContext ctx) {
        return visit(ctx.decl());
    }

    @Override
    public Node visitStmtIf(ToyCParser.StmtIfContext ctx) {
        Expr cond = (Expr) visit(ctx.expr());
        Stmt thenStmt = (Stmt) visit(ctx.stmt(0));
        Stmt elseStmt = ctx.stmt().size() > 1 ? (Stmt) visit(ctx.stmt(1)) : null;
        return new IfStmt(cond, thenStmt, elseStmt);
    }

    @Override
    public Node visitStmtWhile(ToyCParser.StmtWhileContext ctx) {
        Expr cond = (Expr) visit(ctx.expr());
        Stmt body = (Stmt) visit(ctx.stmt());
        return new WhileStmt(cond, body);
    }

    @Override
    public Node visitStmtBreak(ToyCParser.StmtBreakContext ctx) {
        return new BreakStmt();
    }

    @Override
    public Node visitStmtContinue(ToyCParser.StmtContinueContext ctx) {
        return new ContinueStmt();
    }

    @Override
    public Node visitStmtReturn(ToyCParser.StmtReturnContext ctx) {
        Expr expr = ctx.expr() != null ? (Expr) visit(ctx.expr()) : null;
        return new ReturnStmt(expr);
    }

    @Override
    public Node visitBlock(ToyCParser.BlockContext ctx) {
        List<Stmt> stmts = new ArrayList<>();
        for (ToyCParser.StmtContext stmtCtx : ctx.stmt()) {
            stmts.add((Stmt) visit(stmtCtx));
        }
        return new BlockStmt(stmts);
    }

    @Override
    public Node visitFuncDef(ToyCParser.FuncDefContext ctx) {
        String returnType = ctx.getChild(0).getText(); // "int" or "void"
        String name = ctx.ID().getText();
        List<Param> params = new ArrayList<>();
        for (ToyCParser.ParamContext pCtx : ctx.param()) {
            params.add((Param) visit(pCtx));
        }
        BlockStmt body = (BlockStmt) visit(ctx.block());
        return new FuncDef(returnType, name, params, body);
    }

    @Override
    public Node visitParam(ToyCParser.ParamContext ctx) {
        return new Param(ctx.ID().getText());
    }

    // ---- Expression Alternatives ----

    @Override
    public Node visitExprPrimary(ToyCParser.ExprPrimaryContext ctx) {
        return visit(ctx.primaryExpr());
    }

    @Override
    public Node visitExprUnary(ToyCParser.ExprUnaryContext ctx) {
        String op = ctx.getChild(0).getText();
        Expr expr = (Expr) visit(ctx.expr());
        return new UnaryExpr(op, expr);
    }

    @Override
    public Node visitExprMul(ToyCParser.ExprMulContext ctx) {
        Expr left = (Expr) visit(ctx.expr(0));
        String op = ctx.op.getText();
        Expr right = (Expr) visit(ctx.expr(1));
        return new BinaryExpr(left, op, right);
    }

    @Override
    public Node visitExprAdd(ToyCParser.ExprAddContext ctx) {
        Expr left = (Expr) visit(ctx.expr(0));
        String op = ctx.op.getText();
        Expr right = (Expr) visit(ctx.expr(1));
        return new BinaryExpr(left, op, right);
    }

    @Override
    public Node visitExprRel(ToyCParser.ExprRelContext ctx) {
        Expr left = (Expr) visit(ctx.expr(0));
        String op = ctx.op.getText();
        Expr right = (Expr) visit(ctx.expr(1));
        return new BinaryExpr(left, op, right);
    }

    @Override
    public Node visitExprLAnd(ToyCParser.ExprLAndContext ctx) {
        Expr left = (Expr) visit(ctx.expr(0));
        Expr right = (Expr) visit(ctx.expr(1));
        return new BinaryExpr(left, "&&", right);
    }

    @Override
    public Node visitExprLOr(ToyCParser.ExprLOrContext ctx) {
        Expr left = (Expr) visit(ctx.expr(0));
        Expr right = (Expr) visit(ctx.expr(1));
        return new BinaryExpr(left, "||", right);
    }

    // ---- PrimaryExpr Alternatives ----

    @Override
    public Node visitPrimaryId(ToyCParser.PrimaryIdContext ctx) {
        return new IdExpr(ctx.ID().getText());
    }

    @Override
    public Node visitPrimaryNumber(ToyCParser.PrimaryNumberContext ctx) {
        return new NumberExpr(Integer.parseInt(ctx.NUMBER().getText()));
    }

    @Override
    public Node visitPrimaryParen(ToyCParser.PrimaryParenContext ctx) {
        return visit(ctx.expr());
    }

    @Override
    public Node visitPrimaryCall(ToyCParser.PrimaryCallContext ctx) {
        String funcName = ctx.ID().getText();
        List<Expr> args = new ArrayList<>();
        for (ToyCParser.ExprContext eCtx : ctx.expr()) {
            args.add((Expr) visit(eCtx));
        }
        return new CallExpr(funcName, args);
    }
}
