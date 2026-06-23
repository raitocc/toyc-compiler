package com.toyc.compiler;

import com.toyc.compiler.ast.AST.*;
import org.antlr.v4.runtime.tree.ParseTree;
import toyc.ToyCBaseVisitor;
import toyc.ToyCParser;

import java.util.ArrayList;
import java.util.List;

public class AstBuilder extends ToyCBaseVisitor<Node> {

    /**
     * {@code compUnit : (decl | funcDef)+ EOF ;}
     * @see CompUnit
     */
    @Override
    public Node visitCompUnit(ToyCParser.CompUnitContext ctx) {
        List<Node> elements = new ArrayList<>();
        if (ctx.children == null) {
            return new CompUnit(elements);
        }
        for (ParseTree child : ctx.children) {
            if (child instanceof ToyCParser.DeclContext || child instanceof ToyCParser.FuncDefContext) {
                Node astNode = visit(child);
                elements.add(astNode);
            }
        }
        return new CompUnit(elements);
    }

    /**
     * {@code constDecl : 'const' 'int' ID '=' expr ';' ;}
     * @see ConstDecl
     */
    @Override
    public Node visitConstDecl(ToyCParser.ConstDeclContext ctx) {
        String constantName = ctx.ID().getText();
        Expr initExpr = (Expr) visit(ctx.expr());
        return new ConstDecl(constantName, initExpr);
    }

    /**
     * {@code varDecl : 'int' ID '=' expr ';' ;}
     * @see VarDecl
     */
    @Override
    public Node visitVarDecl(ToyCParser.VarDeclContext ctx) {
        String name = ctx.ID().getText();
        Expr initExpr = (Expr) visit(ctx.expr());
        return new VarDecl(name, initExpr);
    }

    /**
     * {@code stmt : block    # StmtBlock}
     * @see #visitBlock(ToyCParser.BlockContext) visitBlock
     */
    @Override
    public Node visitStmtBlock(ToyCParser.StmtBlockContext ctx) {
        return visit(ctx.block()); // 引入到 visitBlock()
    }

    /**
     * {@code      stmt | ';'                                             # StmtEmpty}
     * @see EmptyStmt
     */
    @Override
    public Node visitStmtEmpty(ToyCParser.StmtEmptyContext ctx) {
        return new EmptyStmt();
    }

    /**
     * {@code      stmt | expr ';'                                        # StmtExpr}
     * @see ExprStmt
     */
    @Override
    public Node visitStmtExpr(ToyCParser.StmtExprContext ctx) {
        Expr innerExpression = (Expr) visit(ctx.expr());
        return new ExprStmt(innerExpression);
    }

    /**
     * {@code     stmt | ID '=' expr ';'                                 # StmtAssign}
     * @see AssignStmt
     */
    @Override
    public Node visitStmtAssign(ToyCParser.StmtAssignContext ctx) {
        String name = ctx.ID().getText();
        Expr expr = (Expr) visit(ctx.expr());
        return new AssignStmt(name, expr);
    }

    /**
     * {@code     stmt | decl                                            # StmtDecl}
     * @see #visitConstDecl(ToyCParser.ConstDeclContext)
     * @see #visitVarDecl(ToyCParser.VarDeclContext)
     */
    @Override
    public Node visitStmtDecl(ToyCParser.StmtDeclContext ctx) {
        return visit(ctx.decl()); // 引入到 visitDecl 后再引入到 visit Const/Var Decl
    }

    /**
     * {@code     stmt | 'if' '(' expr ')' stmt ('else' stmt)?           # StmtIf}
     * @see IfStmt
     */
    @Override
    public Node visitStmtIf(ToyCParser.StmtIfContext ctx) {
        Expr cond = (Expr) visit(ctx.expr());
        Stmt thenStmt = (Stmt) visit(ctx.stmt(0));
        Stmt elseStmt = ctx.stmt().size() > 1 ? (Stmt) visit(ctx.stmt(1)) : null;
        return new IfStmt(cond, thenStmt, elseStmt);
    }

    /**
     * {@code   stmt   | 'while' '(' expr ')' stmt                       # StmtWhile}
     * @see WhileStmt
     */
    @Override
    public Node visitStmtWhile(ToyCParser.StmtWhileContext ctx) {
        Expr cond = (Expr) visit(ctx.expr());
        Stmt body = (Stmt) visit(ctx.stmt());
        return new WhileStmt(cond, body);
    }

    /**
     * {@code    stmt  | 'break' ';'                                     # StmtBreak}
     * @see BreakStmt
     */
    @Override
    public Node visitStmtBreak(ToyCParser.StmtBreakContext ctx) {
        return new BreakStmt();
    }

    /**
     * {@code     stmt | 'continue' ';'                                  # StmtContinue}
     * @see ContinueStmt
     */
    @Override
    public Node visitStmtContinue(ToyCParser.StmtContinueContext ctx) {
        return new ContinueStmt();
    }

    /**
     * {@code     stmt | 'return' expr? ';'                              # StmtReturn}
     * @see ReturnStmt
     */
    @Override
    public Node visitStmtReturn(ToyCParser.StmtReturnContext ctx) {
        Expr expr = ctx.expr() != null ? (Expr) visit(ctx.expr()) : null;
        return new ReturnStmt(expr);
    }

    /**
     * {@code block : '{' stmt* '}' ;}
     * @see BlockStmt
     */
    @Override
    public Node visitBlock(ToyCParser.BlockContext ctx) {
        List<Stmt> stmts = new ArrayList<>();
        for (ToyCParser.StmtContext stmtCtx : ctx.stmt()) {
            stmts.add((Stmt) visit(stmtCtx));
        }
        return new BlockStmt(stmts);
    }

    /**
     * {@code funcDef : ('int' | 'void') ID '(' (param (',' param)*)? ')' block ;}
     * @see FuncDef
     */
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

    /**
     * {@code param : 'int' ID ;}
     * @see Param
     */
    @Override
    public Node visitParam(ToyCParser.ParamContext ctx) {
        return new Param(ctx.ID().getText());
    }

    // ---- Expression Alternatives ----

    /**
     * {@code expr : primaryExpr        # ExprPrimary}
     * @see #visitPrimaryId(ToyCParser.PrimaryIdContext)
     * @see #visitPrimaryNumber(ToyCParser.PrimaryNumberContext)
     * @see #visitPrimaryParen(ToyCParser.PrimaryParenContext)
     * @see #visitPrimaryCall(ToyCParser.PrimaryCallContext)
     */
    @Override
    public Node visitExprPrimary(ToyCParser.ExprPrimaryContext ctx) {
        return visit(ctx.primaryExpr()); // 引入到 primaryExpr 右边四选一
    }

    /**
     * {@code expr      | ('+' | '-' | '!') expr                                          # ExprUnary}
     * @see UnaryExpr
     */
    @Override
    public Node visitExprUnary(ToyCParser.ExprUnaryContext ctx) {
        String op = ctx.getChild(0).getText();
        Expr expr = (Expr) visit(ctx.expr());
        return new UnaryExpr(op, expr);
    }

    /**
     * {@code expr      | expr op=('*' | '/' | '%') expr                                  # ExprMul}
     * @see BinaryExpr
     */
    @Override
    public Node visitExprMul(ToyCParser.ExprMulContext ctx) {
        Expr left = (Expr) visit(ctx.expr(0));
        String op = ctx.op.getText();
        Expr right = (Expr) visit(ctx.expr(1));
        return new BinaryExpr(left, op, right);
    }

    /**
     * {@code expr          | expr op=('+' | '-') expr                                        # ExprAdd}
     * @see BinaryExpr
     */
    @Override
    public Node visitExprAdd(ToyCParser.ExprAddContext ctx) {
        Expr left = (Expr) visit(ctx.expr(0));
        String op = ctx.op.getText();
        Expr right = (Expr) visit(ctx.expr(1));
        return new BinaryExpr(left, op, right);
    }

    /**
     * {@code expr      | expr op=('<' | '>' | '<=' | '>=' | '==' | '!=') expr            # ExprRel}
     * @see BinaryExpr
     */
    @Override
    public Node visitExprRel(ToyCParser.ExprRelContext ctx) {
        Expr left = (Expr) visit(ctx.expr(0));
        String op = ctx.op.getText();
        Expr right = (Expr) visit(ctx.expr(1));
        return new BinaryExpr(left, op, right);
    }

    /**
     * {@code expr      | expr '&&' expr                                                  # ExprLAnd}
     * @see BinaryExpr
     */
    @Override
    public Node visitExprLAnd(ToyCParser.ExprLAndContext ctx) {
        Expr left = (Expr) visit(ctx.expr(0));
        Expr right = (Expr) visit(ctx.expr(1));
        return new BinaryExpr(left, "&&", right);
    }

    /**
     * {@code expr      | expr '||' expr                                                  # ExprLOr}
     * @see BinaryExpr
     */
    @Override
    public Node visitExprLOr(ToyCParser.ExprLOrContext ctx) {
        Expr left = (Expr) visit(ctx.expr(0));
        Expr right = (Expr) visit(ctx.expr(1));
        return new BinaryExpr(left, "||", right);
    }

    // ---- PrimaryExpr Alternatives ----

    /**
     * {@code primaryExpr : ID                                                       # PrimaryId}
     * @see IdExpr
     */
    @Override
    public Node visitPrimaryId(ToyCParser.PrimaryIdContext ctx) {
        return new IdExpr(ctx.ID().getText());
    }

    /**
     * {@code primaryExpr             | NUMBER                                                   # PrimaryNumber}
     * @see NumberExpr
     */
    @Override
    public Node visitPrimaryNumber(ToyCParser.PrimaryNumberContext ctx) {
        return new NumberExpr(Integer.parseInt(ctx.NUMBER().getText()));
    }

    /**
     * {@code primaryExpr             | '(' expr ')'                                             # PrimaryParen}
     * @see #visitExprAdd(ToyCParser.ExprAddContext) 
     */
    @Override
    public Node visitPrimaryParen(ToyCParser.PrimaryParenContext ctx) {
        return visit(ctx.expr());
    }

    /**
     * {@code primaryExpr             | ID '(' (expr (',' expr)*)? ')'                           # PrimaryCall}
     * @see CallExpr
     */
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
