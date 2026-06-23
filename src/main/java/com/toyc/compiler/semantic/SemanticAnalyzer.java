package com.toyc.compiler.semantic;

import com.toyc.compiler.ast.AST;
import com.toyc.compiler.ast.AST.*;

import java.util.Objects;

public class SemanticAnalyzer implements AST.Visitor<Void> {
    private final SymbolTable symTable = new SymbolTable();
    private int loopDepth = 0;
    private String currentFuncReturnType = null; // 用于辅助校验 return 语句与函数声明类型是否匹配

    public void analyze(Node root) {
        root.accept(this);
    }

    private void semError(Node node, String message) {
        if (node != null && node.line != -1) {
            throw new RuntimeException("Semantic Error at line " + node.line + ":" + node.column + " - " + message);
        }
        throw new RuntimeException("Semantic Error: " + message);
    }

    @Override
    public Void visit(CompUnit node) {
        // 1. 检查全局元素类型 (全局变量 VarDecl, 全局常量 ConstDecl, 全局函数 FuncDef)
        for (Node element : node.elements) {
            if (!(element instanceof VarDecl
                    || element instanceof ConstDecl
                    || element instanceof FuncDef)) {
                semError(element, "Only variables, constants, and functions are allowed at global scope");
            }
            element.accept(this);
        }

        // 2. 检查 main 函数的存在性与签名规则
        SymbolTable.Symbol mainSym = symTable.lookup("main");
        if (mainSym == null) {
            semError(node, "Function 'main' must be defined");
            return null;
        }
        if (!mainSym.isFunc) {
            Node mainNode = node.elements.stream()
                    .filter(e -> (e instanceof VarDecl v && v.name.equals("main"))
                            || (e instanceof ConstDecl c && c.name.equals("main")))
                    .findFirst().orElse(node);
            semError(mainNode, "'main' is not defined as a function");
        }
        if (mainSym.returnType == null || !mainSym.returnType.equals("int")) {
            Node mainNode = node.elements.stream()
                    .filter(e -> e instanceof FuncDef f && f.name.equals("main"))
                    .findFirst().orElse(node);
            semError(mainNode, "Function 'main' must return type int");
        }
        if (mainSym.paramCount != 0) {
            Node mainNode = node.elements.stream()
                    .filter(e -> e instanceof FuncDef f && f.name.equals("main"))
                    .findFirst().orElse(node);
            semError(mainNode, "Function 'main' must have empty parameter list");
        }
        return null;
    }

    @Override
    public Void visit(ConstDecl node) {
        // 无论是全局常量还是局部常量，其值都必须在编译期确定
        int val = evalConst(node.initExpr);

        // 注册到符号表，标记为常量，并绑定求得的常量值
        if (symTable.hasInCurrentScope(node.name)) {
            semError(node, "Duplicate declaration of '" + node.name + "'");
        }
        SymbolTable.Symbol constSym = new SymbolTable.Symbol(node.name, val);
        symTable.define(node.name, constSym);
        return null;
    }

    @Override
    public Void visit(VarDecl node) {
        if (symTable.isGlobalScope()) {
            // 全局变量：生命周期在程序运行前，其初值必须能在编译期确定（即必须是常量表达式）
            evalConst(node.initExpr);
        } else {
            // 局部变量：初值可以是运行期的任何合法表达式，因此只需常规的 AST 语义检查
            node.initExpr.accept(this);
            if (isVoidExpression(node.initExpr)) {
                semError(node.initExpr, "Variable initialization expression has no return value");
            }
        }

        // 注册到符号表，标记为普通变量
        if (symTable.hasInCurrentScope(node.name)) {
            semError(node, "Duplicate declaration of '" + node.name + "'");
        }
        SymbolTable.Symbol varSym = new SymbolTable.Symbol(node.name);
        symTable.define(node.name, varSym);
        return null;
    }

    @Override
    public Void visit(BlockStmt node) {
        symTable.enterScope();
        for (Stmt st : node.stmts) {
            st.accept(this);
        }
        symTable.exitScope();
        return null;
    }

    @Override
    public Void visit(EmptyStmt node) {
        return null;
    }

    @Override
    public Void visit(ExprStmt node) {
        node.expr.accept(this);
        return null;
    }

    @Override
    public Void visit(AssignStmt node) {
        node.expr.accept(this);
        SymbolTable.Symbol leftSym = symTable.lookup(node.name);
        if (leftSym == null) {
            semError(node, "Identifier '" + node.name + "' is undefined");
            return null;
        }
        if (leftSym.isConst) {
            semError(node, "Cannot assign to constant variable '" + node.name + "'");
        }
        if (leftSym.isFunc) {
            semError(node, "Cannot assign to function '" + node.name + "'");
        }
        if (isVoidExpression(node.expr)) {
            semError(node.expr, "Assignment right-hand side expression has no return value");
        }
        return null;
    }

    @Override
    public Void visit(IfStmt node) {
        node.cond.accept(this);
        if (isVoidExpression(node.cond)) {
            semError(node.cond, "Condition expression in 'if' statement has no return value");
        }
        node.thenStmt.accept(this);
        if (node.elseStmt != null) {
            node.elseStmt.accept(this);
        }
        return null;
    }

    @Override
    public Void visit(WhileStmt node) {
        node.cond.accept(this);
        if (isVoidExpression(node.cond)) {
            semError(node.cond, "Condition expression in 'while' statement has no return value");
        }
        loopDepth++;
        try {
            node.body.accept(this);
        } finally {
            loopDepth--;
        }
        return null;
    }

    @Override
    public Void visit(BreakStmt node) {
        if (loopDepth == 0) {
            semError(node, "'break' statement outside of loop");
        }
        return null;
    }

    @Override
    public Void visit(ContinueStmt node) {
        if (loopDepth == 0) {
            semError(node, "'continue' statement outside of loop");
        }
        return null;
    }

    @Override
    public Void visit(ReturnStmt node) {
        if (node.expr != null) {
            // 1. 如果有返回表达式，但当前函数返回类型是 void，报错
            if (currentFuncReturnType != null && currentFuncReturnType.equals("void")) {
                semError(node, "Void function cannot return a value");
            }
            // 2. 递归检查表达式自身合法性
            node.expr.accept(this);
            // 3. 返回表达式的推导类型不能是 void
            if (isVoidExpression(node.expr)) {
                semError(node.expr, "Return expression has no return value");
            }
        } else {
            // 4. 如果没有返回表达式，但当前函数要求返回 int，报错
            if (currentFuncReturnType != null && currentFuncReturnType.equals("int")) {
                semError(node, "Function returning int must return a value");
            }
        }
        return null;
    }

    @Override
    public Void visit(FuncDef node) {
        // 1. 检查重名冲突，无冲突则将该函数及其签名定义在全局符号表中
        if (symTable.hasInCurrentScope(node.name)) {
            semError(node, "Duplicate declaration of '" + node.name + "'");
        }
        SymbolTable.Symbol funcSym = new SymbolTable.Symbol(node.name, node.returnType, node.params.size());
        symTable.define(node.name, funcSym);

        // 2. 将当前函数的返回类型设置为该函数的返回类型
        currentFuncReturnType = node.returnType;

        // 3. 进入函数局部作用域
        symTable.enterScope();

        // 4. 将形参定义在局部作用域符号表中
        for (Param param : node.params) {
            if (symTable.hasInCurrentScope(param.name)) {
                semError(param, "Duplicate declaration of '" + param.name + "'");
            }
            SymbolTable.Symbol paramSym = new SymbolTable.Symbol(param.name);
            symTable.define(param.name, paramSym);
        }

        // 5. 递归检查其 body 块
        node.body.accept(this);

        // 6. 退出作用域，恢复全局的 currentFuncReturnType 状态为 null
        symTable.exitScope();
        currentFuncReturnType = null;

        // 7. 如果函数返回 int，检查其所有可能执行路径是否都确保 return 了一个值
        if (node.returnType.equals("int")) {
            if (!checkAllPathsReturn(node.body)) {
                semError(node, "Function '" + node.name + "' returning int must return a value on all execution paths");
            }
        }
        return null;
    }

    @Override
    public Void visit(Param node) {
        return null;
    }

    @Override
    public Void visit(NumberExpr node) {
        return null;
    }

    @Override
    public Void visit(IdExpr node) {
        SymbolTable.Symbol symbol = symTable.lookup(node.name);
        if (symbol == null) {
            semError(node, "Identifier '" + node.name + "' is undefined");
            return null;
        }
        if (symbol.isFunc) {
            semError(node, "Function '" + node.name + "' cannot be used as a value");
        }
        return null;
    }

    @Override
    public Void visit(CallExpr node) {
        SymbolTable.Symbol symbol = symTable.lookup(node.funcName);
        if (symbol == null) {
            semError(node, "Identifier '" + node.funcName + "' is undefined");
            return null;
        }
        if (!symbol.isFunc) {
            semError(node, "Identifier '" + node.funcName + "' is not a function");
        }
        int argN = node.args.size();
        if (argN != symbol.paramCount) {
            semError(node, "Expected " + symbol.paramCount + " arguments, but got " + argN);
        }
        for (Expr expr : node.args) {
            if (isVoidExpression(expr)) {
                semError(expr, "Argument cannot be of type 'void'");
            }
            expr.accept(this);
        }
        return null;
    }

    @Override
    public Void visit(UnaryExpr node) {
        Expr expr = node.expr;
        expr.accept(this);
        if (isVoidExpression(expr)) {
            semError(node.expr, "Operator '" + node.op + "' cannot be applied to 'void'");
        }
        return null;
    }

    @Override
    public Void visit(BinaryExpr node) {
        Expr left = node.left;
        Expr right = node.right;
        left.accept(this);
        right.accept(this);
        if (isVoidExpression(left)) {
            semError(node.left, "The left operand of '" + node.op + "' cannot be 'void'");
        }
        if (isVoidExpression(right)) {
            semError(node.right, "The right operand of '" + node.op + "' cannot be 'void'");
        }
        return null;
    }

    /**
     * 编译期常量求值（在 ConstDecl 以及全局变量 VarDecl 初始化中使用）
     */
    private int evalConst(Expr expr) {
        if (expr instanceof NumberExpr numExpr) {
            return numExpr.value;
        }
        if (expr instanceof IdExpr idExpr) {
            SymbolTable.Symbol id = symTable.lookup(idExpr.name);
            if (id == null) {
                semError(idExpr, "Identifier '" + idExpr.name + "' is undefined");
                return -1;
            }
            if (!id.isConst) {
                semError(idExpr, "Identifier '" + idExpr.name + "' is not a constant");
            }
            return id.constValue;
        }
        if (expr instanceof UnaryExpr unaryExpr) {
            int origin = evalConst(unaryExpr.expr);
            switch (unaryExpr.op) {
                case "+":
                    return origin;
                case "-":
                    return -origin;
                case "!":
                    return origin == 0 ? 1 : 0;
            }
        }
        if (expr instanceof BinaryExpr binaryExpr) {
            int left = evalConst(binaryExpr.left);
            int right = evalConst(binaryExpr.right);
            switch (binaryExpr.op) {
                case "*":
                    return left * right;
                case "/":
                    if (right == 0) {
                        semError(binaryExpr.right, "Division by zero in constant expression");
                    }
                    return left / right;
                case "%":
                    if (right == 0) {
                        semError(binaryExpr.right, "Modulo by zero in constant expression");
                    }
                    return left % right;
                case "+":
                    return left + right;
                case "-":
                    return left - right;
                case "<":
                    return left < right ? 1 : 0;
                case ">":
                    return left > right ? 1 : 0;
                case "<=":
                    return left <= right ? 1 : 0;
                case ">=":
                    return left >= right ? 1 : 0;
                case "==":
                    return left == right ? 1 : 0;
                case "!=":
                    return left != right ? 1 : 0;
                case "&&":
                    return left != 0 && right != 0 ? 1 : 0;
                case "||":
                    return left != 0 || right != 0 ? 1 : 0;
            }
        }
        // 常量计算时如果有函数调用直接报错
        semError(expr, "Expression is not a compile-time constant");
        return 0;
    }

    /**
     * 判定某个表达式的值是否为 void
     */
    private boolean isVoidExpression(Expr expr) {
        if (expr instanceof CallExpr callExpr) {
            SymbolTable.Symbol func = symTable.lookup(callExpr.funcName);
            return func != null && func.returnType != null && func.returnType.equals("void");
        }
        return false;
    }

    /**
     * 路径返回值分析（控制流判定）
     */
    private boolean checkAllPathsReturn(Stmt stmt) {
        switch (stmt) {
            case ReturnStmt ignored -> {
                return true;
            }
            case BlockStmt block -> {
                for (Stmt s : block.stmts) {
                    if (checkAllPathsReturn(s)) {
                        return true;
                    }
                }
                return false;
            }
            case IfStmt ifStmt -> {
                if (ifStmt.elseStmt == null) {
                    return false;
                }
                return checkAllPathsReturn(ifStmt.thenStmt) && checkAllPathsReturn(ifStmt.elseStmt);
            }
            case null, default -> {
                return false;
            }
        }
    }
}
