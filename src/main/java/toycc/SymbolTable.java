package toycc;

import java.util.*;

/**
 * Symbol table for the ToyC compiler with nested scopes.
 */
public class SymbolTable {

    public enum Kind {
        VAR,       // mutable variable
        CONST,     // compile-time constant
        PARAM,     // function parameter
        FUNC       // function
    }

    public static class Symbol {
        public String name;
        public Kind kind;
        public AST.Type type;         // INT or VOID (VOID only for functions)
        public int offset;            // stack offset for locals/params
        public boolean isGlobal;
        public int constValue;        // for CONST kind
        public List<AST.Type> paramTypes; // for FUNC kind
        public int paramCount;
        public int line;              // source line of declaration

        public Symbol(String name, Kind kind, AST.Type type) {
            this.name = name;
            this.kind = kind;
            this.type = type;
            this.paramTypes = new ArrayList<>();
        }
    }

    private static class Scope {
        final Map<String, Symbol> symbols = new LinkedHashMap<>();
        final Scope parent;
        int nextLocalOffset = 8;   // 0=ra, 4=fp, first local at 8

        Scope(Scope parent) {
            this.parent = parent;
        }
    }

    private Scope currentScope;
    private final Map<String, Symbol> globals = new LinkedHashMap<>();
    private final List<Symbol> orderedGlobals = new ArrayList<>();

    // Persistent store of all resolved symbols (for code generation)
    // Keyed by name; for shadowed names, the innermost scope wins
    private final Map<String, Symbol> resolvedSymbols = new HashMap<>();
    // Stack of overridden values: each pushScope() starts a new list of (name, oldSymbol) pairs
    private final Deque<List<Map.Entry<String, Symbol>>> overrideStack = new ArrayDeque<>();

    // Permanent store of function-local symbols (survives scope pop)
    // Keyed by function name -> map of local symbol name -> symbol
    private final Map<String, Map<String, Symbol>> functionLocals = new HashMap<>();

    // Per-function data
    private Symbol currentFunction;
    private int paramCount;
    // After all locals are counted, we store the final frame sizes per function
    private final Map<String, Integer> functionFrameSize = new HashMap<>();

    public SymbolTable() {
        currentScope = new Scope(null); // global scope
    }

    // ---- Scope management ----

    public void pushScope() {
        currentScope = new Scope(currentScope);
        overrideStack.push(new ArrayList<>());
    }

    public void popScope() {
        if (currentScope.parent != null) {
            // Propagate local offset to parent scope
            if (currentScope.nextLocalOffset > currentScope.parent.nextLocalOffset) {
                currentScope.parent.nextLocalOffset = currentScope.nextLocalOffset;
            }
            // Restore overridden symbols
            List<Map.Entry<String, Symbol>> overrides = overrideStack.pop();
            for (int i = overrides.size() - 1; i >= 0; i--) {
                Map.Entry<String, Symbol> entry = overrides.get(i);
                if (entry.getValue() == null) {
                    resolvedSymbols.remove(entry.getKey());
                } else {
                    resolvedSymbols.put(entry.getKey(), entry.getValue());
                }
            }
            currentScope = currentScope.parent;
        }
    }

    // ---- Symbol insertion ----

    private void addToResolved(String name, Symbol sym) {
        Symbol old = resolvedSymbols.get(name);
        if (!overrideStack.isEmpty()) {
            overrideStack.peek().add(new AbstractMap.SimpleEntry<>(name, old));
        }
        resolvedSymbols.put(name, sym);
    }

    public Symbol addGlobalVar(String name, AST.ExprNode init) {
        Symbol sym = new Symbol(name, Kind.VAR, AST.Type.INT);
        sym.isGlobal = true;
        globals.put(name, sym);
        orderedGlobals.add(sym);
        currentScope.symbols.put(name, sym);
        addToResolved(name, sym);
        return sym;
    }

    public Symbol addGlobalConst(String name, int value) {
        Symbol sym = new Symbol(name, Kind.CONST, AST.Type.INT);
        sym.isGlobal = true;
        sym.constValue = value;
        globals.put(name, sym);
        orderedGlobals.add(sym);
        currentScope.symbols.put(name, sym);
        addToResolved(name, sym);
        return sym;
    }

    public Symbol addLocalVar(String name) {
        Symbol sym = new Symbol(name, Kind.VAR, AST.Type.INT);
        sym.isGlobal = false;
        sym.offset = currentScope.nextLocalOffset;
        currentScope.nextLocalOffset += 4;
        currentScope.symbols.put(name, sym);
        addToResolved(name, sym);
        // Also save to function locals for code generation
        saveToFunctionLocals(name, sym);
        return sym;
    }

    public Symbol addLocalConst(String name, int value) {
        Symbol sym = new Symbol(name, Kind.CONST, AST.Type.INT);
        sym.isGlobal = false;
        sym.constValue = value;
        sym.offset = currentScope.nextLocalOffset;
        currentScope.nextLocalOffset += 4;
        currentScope.symbols.put(name, sym);
        addToResolved(name, sym);
        saveToFunctionLocals(name, sym);
        return sym;
    }

    public Symbol addParam(String name) {
        Symbol sym = new Symbol(name, Kind.PARAM, AST.Type.INT);
        sym.isGlobal = false;
        paramCount++;
        currentScope.symbols.put(name, sym);
        addToResolved(name, sym);
        saveToFunctionLocals(name, sym);
        return sym;
    }

    private void saveToFunctionLocals(String name, Symbol sym) {
        if (currentFunction != null) {
            Map<String, Symbol> locals = functionLocals.computeIfAbsent(
                    currentFunction.name, k -> new HashMap<>());
            locals.put(name, sym);
        }
    }

    public void addFunction(Symbol func) {
        globals.put(func.name, func);
        currentScope.symbols.put(func.name, func);
    }

    /**
     * Look up a resolved symbol (for code generation).
     * Returns the most recently resolved symbol with this name.
     */
    public Symbol lookupResolved(String name) {
        return resolvedSymbols.get(name);
    }

    // ---- Lookup ----

    public Symbol lookup(String name) {
        Scope s = currentScope;
        while (s != null) {
            Symbol sym = s.symbols.get(name);
            if (sym != null) return sym;
            s = s.parent;
        }
        return null;
    }

    public Symbol lookupInCurrentScope(String name) {
        return currentScope.symbols.get(name);
    }

    // ---- Current function management ----

    public void setCurrentFunction(Symbol func) {
        this.currentFunction = func;
        this.paramCount = 0;
    }

    public Symbol getCurrentFunction() {
        return currentFunction;
    }

    // ---- Frame size ----

    /**
     * Called after processing a function body to record its frame size.
     * Frame layout: [ra(4)][fp(4)][local0(4)]...[localN(4)][param0(4)]...[paramN(4)]
     */
    public void finalizeFunctionFrame(String funcName) {
        int frameSize = currentScope.nextLocalOffset + paramCount * 4;
        // Align to 16 bytes
        frameSize = (frameSize + 15) & ~15;
        functionFrameSize.put(funcName, frameSize);

        // Assign param offsets (after locals)
        int paramBase = currentScope.nextLocalOffset;
        int pIdx = 0;
        for (Symbol sym : currentScope.symbols.values()) {
            if (sym.kind == Kind.PARAM) {
                sym.offset = paramBase + pIdx * 4;
                pIdx++;
            }
        }

        // Merge function-scope symbols into the existing functionLocals map
        // (functionLocals already has block-scoped symbols from saveToFunctionLocals)
        Map<String, Symbol> locals = functionLocals.computeIfAbsent(funcName, k -> new HashMap<>());
        for (Symbol sym : currentScope.symbols.values()) {
            if (sym.kind == Kind.VAR || sym.kind == Kind.CONST || sym.kind == Kind.PARAM) {
                locals.putIfAbsent(sym.name, sym);
            }
        }
    }

    public int getFrameSize(String funcName) {
        return functionFrameSize.getOrDefault(funcName, 16);
    }

    /**
     * Look up a symbol from a function's local scope.
     * Used by the code generator.
     */
    public Symbol lookupFunctionLocal(String funcName, String symName) {
        Map<String, Symbol> locals = functionLocals.get(funcName);
        if (locals != null) {
            return locals.get(symName);
        }
        return null;
    }

    // ---- Globals ----

    public Collection<Symbol> getGlobals() {
        return orderedGlobals;
    }

    public boolean isGlobal(String name) {
        return globals.containsKey(name);
    }

    // ---- Reset for a new function ----

    public void resetFunctionState() {
        currentFunction = null;
        paramCount = 0;
    }
}
