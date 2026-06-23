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

    @Override
    public Void visit(CompUnit node) {
        // 1. 检查全局元素类型 (全局变量 VarDecl, 全局常量 ConstDecl, 全局函数 FuncDef)
        for (Node element : node.elements) {
            if (!(element instanceof VarDecl
                    || element instanceof ConstDecl
                    || element instanceof FuncDef)) {
                throw new RuntimeException("Semantic Error: Only variables, constants, and functions are allowed at global scope");
            }
            element.accept(this);
        }
        
        // 2. 检查 main 函数的存在性与签名规则
        SymbolTable.Symbol mainSym = symTable.lookup("main");
        if (mainSym == null) {
            throw new RuntimeException("Semantic Error: Function 'main' must be defined");
        }
        if (!mainSym.isFunc) {
            throw new RuntimeException("Semantic Error: 'main' is not defined as a function");
        }
        if (mainSym.returnType == null || !mainSym.returnType.equals("int")) {
            throw new RuntimeException("Semantic Error: Function 'main' must return type int");
        }
        if (mainSym.paramCount != 0) {
            throw new RuntimeException("Semantic Error: Function 'main' must have empty parameter list");
        }
        return null;
    }

    @Override
    public Void visit(ConstDecl node) {
        // TODO: 1. 常量必须编译期能确定其值：int val = evalConst(node.initExpr);
        // TODO: 2. 在当前作用域的符号表中注册该常量符号 (标记为 const 并存储常量值)
        return null;
    }

    @Override
    public Void visit(VarDecl node) {
        // TODO: 1. 如果在全局作用域 (symTable.isGlobalScope())，initExpr 必须是编译期常量并求值
        // TODO: 2. 在当前作用域的符号表中注册该变量符号 (标记为普通变量)
        return null;
    }

    @Override
    public Void visit(BlockStmt node) {
        // TODO: 1. 进入新局部作用域 (symTable.enterScope())
        // TODO: 2. 遍历并检查其 stmts 语句列表
        // TODO: 3. 退出局部作用域 (symTable.exitScope())
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
        // TODO: 1. 递归检查右侧表达式 node.expr.accept(this)
        // TODO: 2. 检查左侧变量 node.name 是否已定义，并且不能是常量 (const)
        // TODO: 3. 检查右侧表达式的类型不能为 void (isVoidExpression(node.expr))
        return null;
    }

    @Override
    public Void visit(IfStmt node) {
        // TODO: 1. 检查条件表达式 cond，且 cond 的推导类型不能为 void
        // TODO: 2. 递归检查 thenStmt 和 elseStmt
        return null;
    }

    @Override
    public Void visit(WhileStmt node) {
        // TODO: 1. 检查条件表达式 cond，且 cond 的推导类型不能为 void
        // TODO: 2. 递增 loopDepth 状态，递归检查 body 循环体，最后还原 loopDepth
        return null;
    }

    @Override
    public Void visit(BreakStmt node) {
        // TODO: 检查 loopDepth，确认其在循环中，否则报错
        return null;
    }

    @Override
    public Void visit(ContinueStmt node) {
        // TODO: 检查 loopDepth，确认其在循环中，否则报错
        return null;
    }

    @Override
    public Void visit(ReturnStmt node) {
        // TODO: 1. 校验返回值匹配关系 (对比 currentFuncReturnType)
        // TODO:    - 若函数是 void，但 return 带表达式，报错
        // TODO:    - 若函数是 int，但 return 不带表达式，报错
        // TODO: 2. 若带表达式，递归检查 node.expr，且返回值表达式不能为 void
        return null;
    }

    @Override
    public Void visit(FuncDef node) {
        // TODO: 1. 检查重名冲突，无冲突则将该函数及其签名定义在全局符号表中
        // TODO: 2. 暂存原有的 currentFuncReturnType，将其设为 node.returnType
        // TODO: 3. 进入函数作用域，定义形参
        // TODO: 4. 递归检查其 body 块
        // TODO: 5. 退出作用域，恢复先前的 currentFuncReturnType 状态
        // TODO: 6. 如果函数返回 int，检查其所有可能执行路径是否都确保 return 了一个值 (checkAllPathsReturn(node.body))
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
        // TODO: 1. 查找变量，未定义报错
        // TODO: 2. 检查是否试图把函数名当成值来使用 (查出的符号如果是函数，且当前节点没有作为 CallExpr 被调用，报错)
        return null;
    }

    @Override
    public Void visit(CallExpr node) {
        // TODO: 1. 查找被调用的函数名是否存在，不存在报错
        // TODO: 2. 检查传入参数个数是否匹配
        // TODO: 3. 递归检查每个实参表达式，且实参表达式均不能为 void
        return null;
    }

    @Override
    public Void visit(UnaryExpr node) {
        // TODO: 1. 递归检查操作数 expr
        // TODO: 2. 检查操作数类型不能为 void
        return null;
    }

    @Override
    public Void visit(BinaryExpr node) {
        // TODO: 1. 递归检查 left 和 right
        // TODO: 2. 检查操作数类型均不能为 void
        return null;
    }

    // ==========================================
    // 推荐实现的辅助方法：
    // ==========================================

    /**
     * 编译期常量求值（在 ConstDecl 以及全局变量 VarDecl 初始化中使用）
     */
    private int evalConst(Expr expr) {
        // TODO: 递归计算常量表达式的值，对于 IdExpr 需去符号表查找其 const 属性。
        // TODO: 捕获除数为零或模零错误。
        throw new UnsupportedOperationException("evalConst not implemented");
    }

    /**
     * 判定某个表达式的值是否为 void
     */
    private boolean isVoidExpression(Expr expr) {
        // TODO: 仅当表达式是一个 CallExpr，且其目标函数的返回类型为 "void" 时，表达式才是 void。其他一律为 int。
        return false;
    }

    /**
     * 路径返回值分析（控制流判定）
     */
    private boolean checkAllPathsReturn(Stmt stmt) {
        // TODO: 递归分析 Stmt (BlockStmt, IfStmt, ReturnStmt) 是否保证最终一定有返回值。
        return false;
    }
}
