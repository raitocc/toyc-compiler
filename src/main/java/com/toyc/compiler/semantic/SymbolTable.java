package com.toyc.compiler.semantic;

import java.util.*;

public class SymbolTable {
    
    public static class Symbol {
        public final String name;
        public final boolean isConst;     // 是否为常量
        public final int constValue;      // 常量值
        
        public final boolean isFunc;      // 是否为函数
        public final String returnType;   // 函数返回类型 ("int" 或 "void")
        public final int paramCount;      // 函数形参个数

        // 变量符号
        public Symbol(String name) {
            this.name = name;
            this.isConst = false;
            this.constValue = 0;
            this.isFunc = false;
            this.returnType = null;
            this.paramCount = 0;
        }

        // 常量符号
        public Symbol(String name, int constValue) {
            this.name = name;
            this.isConst = true;
            this.constValue = constValue;
            this.isFunc = false;
            this.returnType = null;
            this.paramCount = 0;
        }

        // 函数符号
        public Symbol(String name, String returnType, int paramCount) {
            this.name = name;
            this.isConst = false;
            this.constValue = 0;
            this.isFunc = true;
            this.returnType = returnType;
            this.paramCount = paramCount;
        }
    }

    /**
     *   一个存放 Map 的栈：<br>
     *   <li>全局作用域：处于栈底（ {@code scopes.get(0)} ）。</li>
     *   <li>进入新作用域（如 Block 或函数）：向栈顶  push  一个全新的空  HashMap 。</li>
     *   <li>退出作用域：从栈顶  pop  弹出一个 Map（它内部声明的所有局部变量会随着 Map 的销毁而自动失效）。</li>
     */
    private final Deque<Map<String, Symbol>> scopes = new ArrayDeque<>();

    public SymbolTable() {
        enterScope(); // 默认创建全局作用域
    }

    public void enterScope() {
        scopes.push(new HashMap<>());
    }

    public void exitScope() {
        if (scopes.size() <= 1) {
            throw new RuntimeException("System Error: Cannot exit global scope");
        }
        scopes.pop();
    }

    public boolean isGlobalScope() {
        return scopes.size() == 1;
    }

    public void define(String name, Symbol sym) {
        Map<String, Symbol> currentScope = scopes.peek();
        if (currentScope == null) {
            throw new RuntimeException("System Error: No active scope found for definition");
        }
        if (currentScope.containsKey(name)) {
            throw new RuntimeException("Semantic Error: Duplicate declaration of '" + name + "'");
        }
        currentScope.put(name, sym);
    }

    // 自底向上（从内向外）安全查找符号，找不到返回 null
    public Symbol lookup(String name) {
        for (Map<String, Symbol> scope : scopes) {
            if (scope.containsKey(name)) {
                return scope.get(name);
            }
        }
        return null;
    }

    // 自底向上（从内向外）解析符号，找不到则直接抛出语义异常
    public Symbol resolve(String name) {
        Symbol sym = lookup(name);
        if (sym == null) {
            throw new RuntimeException("Semantic Error: Undefined identifier '" + name + "'");
        }
        return sym;
    }
}
