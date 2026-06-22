package com.toyc.compiler.semantic;

import com.toyc.compiler.ast.AST.*;
import java.util.ArrayList;
import java.util.List;

public class SemanticAnalyzer implements Visitor<Node> {
    private final SymbolTable symTable = new SymbolTable();
    private String currentFuncReturnType = null;

    public Node analyze(Node root) {
        return root.accept(this);
    }

    private int evalConst(Expr expr) {
        if (expr instanceof NumberExpr) {
            return ((NumberExpr) expr).value;
        } else if (expr instanceof IdExpr) {
            String name = ((IdExpr) expr).name;
            SymbolTable.Symbol sym = symTable.resolveVar(name);
            if (!sym.isConst) {
                throw new RuntimeException("Semantic Error: Variable '" + name + "' is not a constant");
            }
            return sym.constValue;
        } else if (expr instanceof UnaryExpr) {
            UnaryExpr u = (UnaryExpr) expr;
            int val = evalConst(u.expr);
            switch (u.op) {
                case "+": return val;
                case "-": return -val;
                case "!": return val == 0 ? 1 : 0;
                default: throw new RuntimeException("Semantic Error: Unknown unary op " + u.op);
            }
        } else if (expr instanceof BinaryExpr) {
            BinaryExpr b = (BinaryExpr) expr;
            int left = evalConst(b.left);
            int right = evalConst(b.right);
            switch (b.op) {
                case "+": return left + right;
                case "-": return left - right;
                case "*": return left * right;
                case "/": 
                    if (right == 0) throw new RuntimeException("Semantic Error: Division by zero in const expression");
                    return left / right;
                case "%": 
                    if (right == 0) throw new RuntimeException("Semantic Error: Modulo by zero in const expression");
                    return left % right;
                case "==": return left == right ? 1 : 0;
                case "!=": return left != right ? 1 : 0;
                case "<": return left < right ? 1 : 0;
                case "<=": return left <= right ? 1 : 0;
                case ">": return left > right ? 1 : 0;
                case ">=": return left >= right ? 1 : 0;
                case "&&": return (left != 0 && right != 0) ? 1 : 0;
                case "||": return (left != 0 || right != 0) ? 1 : 0;
                default: throw new RuntimeException("Semantic Error: Unknown binary op " + b.op);
            }
        }
        throw new RuntimeException("Semantic Error: Expression is not a compile-time constant");
    }

    @Override
    public Node visit(CompUnit node) {
        List<Node> newElements = new ArrayList<>();
        for (Node e : node.elements) {
            newElements.add(e.accept(this));
        }
        return new CompUnit(newElements);
    }

    @Override
    public Node visit(ConstDecl node) {
        int val = evalConst(node.initExpr);
        symTable.defineVar(node.name, true, val);
        return new ConstDecl(node.name, new NumberExpr(val));
    }

    @Override
    public Node visit(VarDecl node) {
        Expr init = (Expr) node.initExpr.accept(this);
        if (symTable.isGlobalScope()) {
            int val = evalConst(init);
            init = new NumberExpr(val);
        }
        symTable.defineVar(node.name, false, 0);
        return new VarDecl(node.name, init);
    }

    @Override
    public Node visit(BlockStmt node) {
        symTable.enterScope();
        List<Stmt> newStmts = new ArrayList<>();
        for (Stmt s : node.stmts) {
            newStmts.add((Stmt) s.accept(this));
        }
        symTable.exitScope();
        return new BlockStmt(newStmts);
    }

    @Override
    public Node visit(EmptyStmt node) {
        return node;
    }

    @Override
    public Node visit(ExprStmt node) {
        return new ExprStmt((Expr) node.expr.accept(this));
    }

    @Override
    public Node visit(AssignStmt node) {
        SymbolTable.Symbol sym = symTable.resolveVar(node.name);
        if (sym.isConst) {
            throw new RuntimeException("Semantic Error: Cannot assign to constant '" + node.name + "'");
        }
        Expr expr = (Expr) node.expr.accept(this);
        return new AssignStmt(node.name, expr);
    }

    @Override
    public Node visit(IfStmt node) {
        Expr cond = (Expr) node.cond.accept(this);
        Stmt thenStmt = (Stmt) node.thenStmt.accept(this);
        Stmt elseStmt = node.elseStmt != null ? (Stmt) node.elseStmt.accept(this) : null;
        return new IfStmt(cond, thenStmt, elseStmt);
    }

    @Override
    public Node visit(WhileStmt node) {
        Expr cond = (Expr) node.cond.accept(this);
        Stmt body = (Stmt) node.body.accept(this);
        return new WhileStmt(cond, body);
    }

    @Override
    public Node visit(BreakStmt node) {
        // Here we could check if we are inside a while loop
        return node;
    }

    @Override
    public Node visit(ContinueStmt node) {
        return node;
    }

    @Override
    public Node visit(ReturnStmt node) {
        Expr expr = node.expr != null ? (Expr) node.expr.accept(this) : null;
        if (currentFuncReturnType != null) {
            if (currentFuncReturnType.equals("void") && expr != null) {
                throw new RuntimeException("Semantic Error: void function cannot return a value");
            }
            if (currentFuncReturnType.equals("int") && expr == null) {
                throw new RuntimeException("Semantic Error: int function must return a value");
            }
        }
        return new ReturnStmt(expr);
    }

    @Override
    public Node visit(FuncDef node) {
        symTable.defineFunc(node.name, node.returnType, node.params.size());
        symTable.enterScope();
        currentFuncReturnType = node.returnType;
        
        List<Param> newParams = new ArrayList<>();
        for (Param p : node.params) {
            symTable.defineVar(p.name, false, 0);
            newParams.add((Param) p.accept(this));
        }
        
        // Don't enter a new scope for the body block, use the func scope for params
        List<Stmt> newStmts = new ArrayList<>();
        for (Stmt s : node.body.stmts) {
            newStmts.add((Stmt) s.accept(this));
        }
        BlockStmt body = new BlockStmt(newStmts);
        
        currentFuncReturnType = null;
        symTable.exitScope();
        return new FuncDef(node.returnType, node.name, newParams, body);
    }

    @Override
    public Node visit(Param node) {
        return node;
    }

    @Override
    public Node visit(NumberExpr node) {
        return node;
    }

    @Override
    public Node visit(IdExpr node) {
        SymbolTable.Symbol sym = symTable.resolveVar(node.name);
        if (sym.isConst) {
            return new NumberExpr(sym.constValue);
        }
        return node;
    }

    @Override
    public Node visit(CallExpr node) {
        SymbolTable.FuncSymbol func = symTable.resolveFunc(node.funcName);
        if (func.paramCount != node.args.size()) {
            throw new RuntimeException("Semantic Error: Function '" + node.funcName + "' expects " + func.paramCount + " arguments but got " + node.args.size());
        }
        List<Expr> newArgs = new ArrayList<>();
        for (Expr arg : node.args) {
            newArgs.add((Expr) arg.accept(this));
        }
        return new CallExpr(node.funcName, newArgs);
    }

    @Override
    public Node visit(UnaryExpr node) {
        Expr expr = (Expr) node.expr.accept(this);
        if (expr instanceof NumberExpr) {
            return new NumberExpr(evalConst(new UnaryExpr(node.op, expr)));
        }
        return new UnaryExpr(node.op, expr);
    }

    @Override
    public Node visit(BinaryExpr node) {
        Expr left = (Expr) node.left.accept(this);
        Expr right = (Expr) node.right.accept(this);
        if (left instanceof NumberExpr && right instanceof NumberExpr) {
            return new NumberExpr(evalConst(new BinaryExpr(left, node.op, right)));
        }
        return new BinaryExpr(left, node.op, right);
    }
}
