package toycc;

import java.util.ArrayList;
import java.util.List;

/**
 * All AST node types for the ToyC language.
 */
public class AST {

    // ---- Enums ----

    public enum Type {
        INT, VOID;

        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }

    // ---- Base classes ----

    public abstract static class Node {
        public int line;

        public Node() {
            this.line = 0;
        }
    }

    public abstract static class DeclNode extends Node {
    }

    public abstract static class StmtNode extends Node {
    }

    public abstract static class ExprNode extends Node {
        public Type type = Type.INT;
        public boolean isConst = false;
        public int constVal = 0; // valid only if isConst
    }

    // ---- Program ----

    public static class Program extends Node {
        public List<DeclNode> decls = new ArrayList<>();
    }

    // ---- Declarations ----

    public static class ConstDecl extends DeclNode {
        public String name;
        public ExprNode init;
        public boolean isGlobal;
        public int value; // compile-time evaluated constant value

        public ConstDecl(String name, ExprNode init, boolean isGlobal) {
            this.name = name;
            this.init = init;
            this.isGlobal = isGlobal;
        }
    }

    public static class VarDecl extends DeclNode {
        public String name;
        public ExprNode init;
        public boolean isGlobal;

        public VarDecl(String name, ExprNode init, boolean isGlobal) {
            this.name = name;
            this.init = init;
            this.isGlobal = isGlobal;
        }
    }

    // ---- Function ----

    public static class Param {
        public String name;

        public Param(String name) {
            this.name = name;
        }
    }

    public static class FuncDef extends DeclNode {
        public String name;
        public Type retType;
        public List<Param> params = new ArrayList<>();
        public Block body;

        public FuncDef(String name, Type retType, Block body) {
            this.name = name;
            this.retType = retType;
            this.body = body;
        }
    }

    // ---- Statements ----

    public static class Block extends StmtNode {
        public List<StmtNode> stmts = new ArrayList<>();
    }

    public static class DeclStmt extends StmtNode {
        public DeclNode decl;

        public DeclStmt(DeclNode decl) {
            this.decl = decl;
        }
    }

    public static class EmptyStmt extends StmtNode {
    }

    public static class ExprStmt extends StmtNode {
        public ExprNode expr;

        public ExprStmt(ExprNode expr) {
            this.expr = expr;
        }
    }

    public static class AssignStmt extends StmtNode {
        public String name;
        public ExprNode value;

        public AssignStmt(String name, ExprNode value) {
            this.name = name;
            this.value = value;
        }
    }

    public static class IfStmt extends StmtNode {
        public ExprNode cond;
        public StmtNode thenBody;
        public StmtNode elseBody; // nullable

        public IfStmt(ExprNode cond, StmtNode thenBody, StmtNode elseBody) {
            this.cond = cond;
            this.thenBody = thenBody;
            this.elseBody = elseBody;
        }
    }

    public static class WhileStmt extends StmtNode {
        public ExprNode cond;
        public StmtNode body;

        public WhileStmt(ExprNode cond, StmtNode body) {
            this.cond = cond;
            this.body = body;
        }
    }

    public static class BreakStmt extends StmtNode {
    }

    public static class ContinueStmt extends StmtNode {
    }

    public static class ReturnStmt extends StmtNode {
        public ExprNode value; // nullable for void return

        public ReturnStmt(ExprNode value) {
            this.value = value;
        }
    }

    // ---- Expressions ----

    public static class BinaryExpr extends ExprNode {
        public ExprNode left;
        public ExprNode right;
        public String op;

        public BinaryExpr(ExprNode left, ExprNode right, String op) {
            this.left = left;
            this.right = right;
            this.op = op;
        }
    }

    public static class UnaryExpr extends ExprNode {
        public ExprNode operand;
        public String op;

        public UnaryExpr(ExprNode operand, String op) {
            this.operand = operand;
            this.op = op;
        }
    }

    public static class NumberExpr extends ExprNode {
        public int value;

        public NumberExpr(int value) {
            this.value = value;
            this.isConst = true;
            this.constVal = value;
        }
    }

    public static class IdExpr extends ExprNode {
        public String name;
        // Resolved symbol info (set during semantic analysis)
        public boolean symIsGlobal = false;
        public int symOffset = 0;       // stack offset for locals
        public SymbolTable.Kind symKind = null;

        public IdExpr(String name) {
            this.name = name;
        }
    }

    public static class CallExpr extends ExprNode {
        public String name;
        public List<ExprNode> args = new ArrayList<>();

        public CallExpr(String name) {
            this.name = name;
        }
    }
}
