package com.toyc.compiler.semantic;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class SymbolTable {
    public static class Symbol {
        public String name;
        public boolean isConst;
        public boolean isGlobal;
        public int constValue; // Valid only if isConst == true
    }

    public static class FuncSymbol {
        public String name;
        public String returnType;
        public int paramCount;
    }

    private final Stack<Map<String, Symbol>> scopes = new Stack<>();
    private final Map<String, FuncSymbol> functions = new HashMap<>();

    public SymbolTable() {
        scopes.push(new HashMap<>()); // Global scope
    }

    public void enterScope() {
        scopes.push(new HashMap<>());
    }

    public void exitScope() {
        scopes.pop();
    }

    public boolean isGlobalScope() {
        return scopes.size() == 1;
    }

    public void defineVar(String name, boolean isConst, int constValue) {
        if (scopes.peek().containsKey(name)) {
            throw new RuntimeException("Semantic Error: Duplicate definition of variable '" + name + "'");
        }
        Symbol sym = new Symbol();
        sym.name = name;
        sym.isConst = isConst;
        sym.constValue = constValue;
        sym.isGlobal = isGlobalScope();
        scopes.peek().put(name, sym);
    }

    public Symbol resolveVar(String name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            if (scopes.get(i).containsKey(name)) {
                return scopes.get(i).get(name);
            }
        }
        throw new RuntimeException("Semantic Error: Undefined variable '" + name + "'");
    }

    public void defineFunc(String name, String returnType, int paramCount) {
        if (functions.containsKey(name) || scopes.get(0).containsKey(name)) {
            throw new RuntimeException("Semantic Error: Duplicate definition of function '" + name + "'");
        }
        FuncSymbol func = new FuncSymbol();
        func.name = name;
        func.returnType = returnType;
        func.paramCount = paramCount;
        functions.put(name, func);
    }

    public FuncSymbol resolveFunc(String name) {
        if (!functions.containsKey(name)) {
            throw new RuntimeException("Semantic Error: Undefined function '" + name + "'");
        }
        return functions.get(name);
    }
}
