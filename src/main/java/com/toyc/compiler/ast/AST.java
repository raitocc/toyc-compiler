package com.toyc.compiler.ast;

import java.util.List;

public class AST {

    public interface Visitor<T> {
        T visit(CompUnit node);
        T visit(ConstDecl node);
        T visit(VarDecl node);
        T visit(BlockStmt node);
        T visit(EmptyStmt node);
        T visit(ExprStmt node);
        T visit(AssignStmt node);
        T visit(IfStmt node);
        T visit(WhileStmt node);
        T visit(BreakStmt node);
        T visit(ContinueStmt node);
        T visit(ReturnStmt node);
        T visit(FuncDef node);
        T visit(Param node);
        T visit(NumberExpr node);
        T visit(IdExpr node);
        T visit(CallExpr node);
        T visit(UnaryExpr node);
        T visit(BinaryExpr node);
    }

    public static abstract class Node {
        public int line = -1;
        public int column = -1;
        public abstract <T> T accept(Visitor<T> visitor);
    }

    /**<pre>{@code
    compUnit : (decl | funcDef)+ EOF ;
    decl : constDecl | varDecl ;}</pre>
     */
    public static class CompUnit extends Node {
        public final List<Node> elements; // ConstDecl, VarDecl, or FuncDef
        public CompUnit(List<Node> elements) { this.elements = elements; }
        @Override public <T> T accept(Visitor<T> v) { return v.visit(this); }
    }

    // ---- Declarations ----
    /**
     <pre>{@code
     constDecl : 'const' 'int' ID '=' expr ';' ;
     varDecl : 'int' ID '=' expr ';' ;}</pre>
     @see FuncDef
     */
    public static abstract class Decl extends Stmt {}

    public static class ConstDecl extends Decl {
        public final String name;
        public final Expr initExpr;
        public ConstDecl(String name, Expr initExpr) { this.name = name; this.initExpr = initExpr; }
        @Override public <T> T accept(Visitor<T> v) { return v.visit(this); }
    }

    public static class VarDecl extends Decl {
        public final String name;
        public final Expr initExpr;
        public VarDecl(String name, Expr initExpr) { this.name = name; this.initExpr = initExpr; }
        @Override public <T> T accept(Visitor<T> v) { return v.visit(this); }
    }

    // ---- Statements ----
    /**
     * <pre>{@code
     * stmt : block
     * | ';'
     * | expr ';'
     * | ID '=' expr ';'
     * | decl
     * | 'if' '(' expr ')' stmt ('else' stmt)?
     * | 'while' '(' expr ')' stmt
     * | 'break' ';'
     * | 'continue' ';'
     * | 'return' expr? ';'
     * ;
     * }</pre>
     *
     * 对于 decl：{@code public static abstract class Decl extends Stmt {}} 继承 Stmt 来体现。
     * @see Decl
     */
    public static abstract class Stmt extends Node {}

    /**
     * {@code block : '{' stmt* '}' ;}
     */
    public static class BlockStmt extends Stmt {
        public final List<Stmt> stmts;
        public BlockStmt(List<Stmt> stmts) { this.stmts = stmts; }
        @Override public <T> T accept(Visitor<T> v) { return v.visit(this); }
    }

    public static class EmptyStmt extends Stmt {
        @Override public <T> T accept(Visitor<T> v) { return v.visit(this); }
    }

    public static class ExprStmt extends Stmt {
        public final Expr expr;
        public ExprStmt(Expr expr) { this.expr = expr; }
        @Override public <T> T accept(Visitor<T> v) { return v.visit(this); }
    }

    /**
     * {@code ID '=' expr ';'}
     */
    public static class AssignStmt extends Stmt {
        public final String name;
        public final Expr expr;
        public AssignStmt(String name, Expr expr) { this.name = name; this.expr = expr; }
        @Override public <T> T accept(Visitor<T> v) { return v.visit(this); }
    }

    public static class IfStmt extends Stmt {
        public final Expr cond;
        public final Stmt thenStmt;
        public final Stmt elseStmt; // can be null
        public IfStmt(Expr cond, Stmt thenStmt, Stmt elseStmt) { this.cond = cond; this.thenStmt = thenStmt; this.elseStmt = elseStmt; }
        @Override public <T> T accept(Visitor<T> v) { return v.visit(this); }
    }

    public static class WhileStmt extends Stmt {
        public final Expr cond;
        public final Stmt body;
        public WhileStmt(Expr cond, Stmt body) { this.cond = cond; this.body = body; }
        @Override public <T> T accept(Visitor<T> v) { return v.visit(this); }
    }

    public static class BreakStmt extends Stmt {
        @Override public <T> T accept(Visitor<T> v) { return v.visit(this); }
    }

    public static class ContinueStmt extends Stmt {
        @Override public <T> T accept(Visitor<T> v) { return v.visit(this); }
    }

    public static class ReturnStmt extends Stmt {
        public final Expr expr; // can be null
        public ReturnStmt(Expr expr) { this.expr = expr; }
        @Override public <T> T accept(Visitor<T> v) { return v.visit(this); }
    }

    // ---- Functions ----
    /**
     * {@code param : 'int' ID ;}
     */
    public static class Param extends Node {
        public final String name;
        public Param(String name) { this.name = name; }
        @Override public <T> T accept(Visitor<T> v) { return v.visit(this); }
    }

    /**
     * {@code funcDef : ('int' | 'void') ID '(' (param (',' param)*)? ')' block ;}
     */
    public static class FuncDef extends Node {
        public final String returnType; // "int" or "void"
        public final String name;
        public final List<Param> params;
        public final BlockStmt body;
        public FuncDef(String returnType, String name, List<Param> params, BlockStmt body) {
            this.returnType = returnType; this.name = name; this.params = params; this.body = body;
        }
        @Override public <T> T accept(Visitor<T> v) { return v.visit(this); }
    }

    // ---- Expressions ----
    /**
     * <pre>{@code
     * primaryExpr : ID
     *             | NUMBER
     *             | '(' expr ')'
     *             | ID '(' (expr (',' expr)*)? ')'
     *             ;
     *             }</pre>
     * 一旦树的结构建立起来，树的分支先后顺序就已经决定了执行顺序，不再需要括号<br>
     * 所以没有'(' expr ')'对应的括号节点
     */
    public static abstract class Expr extends Node {}

    public static class NumberExpr extends Expr {
        public final int value;
        public NumberExpr(int value) { this.value = value; }
        @Override public <T> T accept(Visitor<T> v) { return v.visit(this); }
    }

    public static class IdExpr extends Expr {
        public final String name;
        public IdExpr(String name) { this.name = name; }
        @Override public <T> T accept(Visitor<T> v) { return v.visit(this); }
    }

    public static class CallExpr extends Expr {
        public final String funcName;
        public final List<Expr> args;
        public CallExpr(String funcName, List<Expr> args) { this.funcName = funcName; this.args = args; }
        @Override public <T> T accept(Visitor<T> v) { return v.visit(this); }
    }

    /**
     * <pre>{@code
     * expr : primaryExpr
     *      | ('+' | '-' | '!') expr
     *      | expr op=('*' | '/' | '%') expr
     *      | expr op=('+' | '-') expr
     *      | expr op=('<' | '>' | '<=' | '>=' | '==' | '!=') expr
     *      | expr '&&' expr
     *      | expr '||' expr
     *      ;
     *      }</pre>
     */
    public static class UnaryExpr extends Expr {
        public final String op;
        public final Expr expr;
        public UnaryExpr(String op, Expr expr) { this.op = op; this.expr = expr; }
        @Override public <T> T accept(Visitor<T> v) { return v.visit(this); }
    }

    public static class BinaryExpr extends Expr {
        public final Expr left;
        public final String op;
        public final Expr right;
        public BinaryExpr(Expr left, String op, Expr right) { this.left = left; this.op = op; this.right = right; }
        @Override public <T> T accept(Visitor<T> v) { return v.visit(this); }
    }
}
