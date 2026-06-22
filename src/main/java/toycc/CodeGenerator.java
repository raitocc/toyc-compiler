package toycc;

import java.util.*;

/**
 * Generates RISC-V32 assembly code from the AST.
 * Uses a simple stack-based approach for expression evaluation.
 */
public class CodeGenerator {

    private final SymbolTable symtab;
    private final StringBuilder out = new StringBuilder();
    private int labelCounter = 0;

    // Loop context: stack of (startLabel, endLabel) pairs
    private final Deque<String> loopStartLabels = new ArrayDeque<>();
    private final Deque<String> loopEndLabels = new ArrayDeque<>();

    // Current function context
    private String currentFuncName;
    private int frameSize;

    public CodeGenerator(SymbolTable symtab) {
        this.symtab = symtab;
    }

    // Collect global variables needing runtime initialization
    private final List<AST.VarDecl> deferredGlobalInits = new ArrayList<>();

    public String generate(AST.Program program) {
        out.setLength(0);
        deferredGlobalInits.clear();

        // ---- Data Section (globals) ----
        Collection<SymbolTable.Symbol> globals = symtab.getGlobals();
        boolean hasGlobals = false;

        // Build a map from global variable name to its AST VarDecl
        Map<String, AST.VarDecl> globalVarDecls = new HashMap<>();
        for (AST.DeclNode decl : program.decls) {
            if (decl instanceof AST.VarDecl varDecl && varDecl.isGlobal) {
                globalVarDecls.put(varDecl.name, varDecl);
            }
        }

        for (SymbolTable.Symbol g : globals) {
            if (g.kind == SymbolTable.Kind.VAR || g.kind == SymbolTable.Kind.CONST) {
                if (!hasGlobals) {
                    emit(".data");
                    hasGlobals = true;
                }
                emit(".globl " + g.name);
                emit(g.name + ":");

                int initValue = 0;
                boolean hasConstInit = false;

                if (g.kind == SymbolTable.Kind.CONST) {
                    initValue = g.constValue;
                    hasConstInit = true;
                } else {
                    // Check if the VarDecl init is compile-time constant
                    AST.VarDecl varDecl = globalVarDecls.get(g.name);
                    if (varDecl != null && varDecl.init != null && varDecl.init.isConst) {
                        initValue = varDecl.init.constVal;
                        hasConstInit = true;
                    }
                }

                emit("  .word " + initValue);

                // Defer non-constant global var inits
                if (g.kind == SymbolTable.Kind.VAR && !hasConstInit) {
                    AST.VarDecl varDecl = globalVarDecls.get(g.name);
                    if (varDecl != null) {
                        deferredGlobalInits.add(varDecl);
                    }
                }
            }
        }

        // ---- Text Section ----
        emit(".text");

        // Generate code for each function
        for (AST.DeclNode decl : program.decls) {
            if (decl instanceof AST.FuncDef funcDef) {
                generateFunction(funcDef);
            }
        }

        return out.toString();
    }

    // ================================================================
    // Function Generation
    // ================================================================

    private void generateFunction(AST.FuncDef func) {
        currentFuncName = func.name;
        frameSize = symtab.getFrameSize(currentFuncName);

        emit("");
        emit(".globl " + currentFuncName);
        emit(currentFuncName + ":");

        // Prologue
        emit("  addi sp, sp, -" + frameSize);
        emit("  sw ra, 0(sp)");
        emit("  sw fp, 4(sp)");
        emit("  addi fp, sp, " + frameSize);

        // Save arguments to param slots
        for (int i = 0; i < func.params.size(); i++) {
            String paramName = func.params.get(i).name;
            SymbolTable.Symbol sym = symtab.lookupFunctionLocal(currentFuncName, paramName);
            if (sym != null) {
                emit("  sw a" + i + ", " + sym.offset + "(sp)");
            }
        }

        // Global variable initialization (only in main)
        if (currentFuncName.equals("main") && !deferredGlobalInits.isEmpty()) {
            for (AST.VarDecl varDecl : deferredGlobalInits) {
                generateExpr(varDecl.init);
                emit("  la t0, " + varDecl.name);
                emit("  sw a0, 0(t0)");
            }
        }

        // Generate body
        generateStmt(func.body);

        // Default return for void functions
        // Add epilogue only if the body doesn't end with a return
        if (func.retType == AST.Type.VOID && !endsWithReturn(func.body)) {
            emit("  lw fp, 4(sp)");
            emit("  lw ra, 0(sp)");
            emit("  addi sp, sp, " + frameSize);
            emit("  ret");
        } else if (func.retType == AST.Type.INT && !endsWithReturn(func.body)) {
            // Should not happen (semantic analyzer ensures this), but add safety
            emit("  li a0, 0");
            emit("  lw fp, 4(sp)");
            emit("  lw ra, 0(sp)");
            emit("  addi sp, sp, " + frameSize);
            emit("  ret");
        }

        // Reset function context
        currentFuncName = null;
        frameSize = 0;
    }

    // ================================================================
    // Statement Generation
    // ================================================================

    private void generateStmt(AST.StmtNode stmt) {
        if (stmt instanceof AST.Block block) {
            for (AST.StmtNode s : block.stmts) {
                generateStmt(s);
            }
        } else if (stmt instanceof AST.EmptyStmt) {
            // nothing
        } else if (stmt instanceof AST.ExprStmt exprStmt) {
            generateExpr(exprStmt.expr);
        } else if (stmt instanceof AST.AssignStmt assign) {
            generateAssign(assign);
        } else if (stmt instanceof AST.DeclStmt declStmt) {
            AST.DeclNode decl = declStmt.decl;
            if (decl instanceof AST.ConstDecl constDecl) {
                generateConstDecl(constDecl);
            } else if (decl instanceof AST.VarDecl varDecl) {
                generateVarDecl(varDecl);
            }
        } else if (stmt instanceof AST.IfStmt ifStmt) {
            generateIf(ifStmt);
        } else if (stmt instanceof AST.WhileStmt whileStmt) {
            generateWhile(whileStmt);
        } else if (stmt instanceof AST.BreakStmt) {
            String endLabel = loopEndLabels.peek();
            emit("  j " + endLabel);
        } else if (stmt instanceof AST.ContinueStmt) {
            String startLabel = loopStartLabels.peek();
            emit("  j " + startLabel);
        } else if (stmt instanceof AST.ReturnStmt ret) {
            generateReturn(ret);
        }
    }

    private void generateAssign(AST.AssignStmt assign) {
        // Evaluate RHS into a0
        generateExpr(assign.value);

        SymbolTable.Symbol sym = lookupSym(assign.name);
        if (sym == null) return;

        if (sym.isGlobal) {
            emit("  la t0, " + assign.name);
            emit("  sw a0, 0(t0)");
        } else {
            emit("  sw a0, " + sym.offset + "(sp)");
        }
    }

    private void generateConstDecl(AST.ConstDecl decl) {
        // Constants don't need runtime initialization (value is already folded)
        // But if it's non-global, we still store it on the stack
        if (!decl.isGlobal) {
            SymbolTable.Symbol sym = lookupSym(decl.name);
            if (sym != null) {
                emit("  li a0, " + decl.value);
                emit("  sw a0, " + sym.offset + "(sp)");
            }
        }
    }

    private void generateVarDecl(AST.VarDecl decl) {
        generateExpr(decl.init); // result in a0

        if (decl.isGlobal) {
            emit("  la t0, " + decl.name);
            emit("  sw a0, 0(t0)");
        } else {
            SymbolTable.Symbol sym = lookupSym(decl.name);
            if (sym != null) {
                emit("  sw a0, " + sym.offset + "(sp)");
            }
        }
    }

    private void generateIf(AST.IfStmt stmt) {
        String elseLabel = newLabel("else");
        String endLabel = newLabel("endif");

        // Short-circuit evaluation for condition
        generateCond(stmt.cond, elseLabel, false); // jump to else if false

        // Then block
        generateStmt(stmt.thenBody);
        if (stmt.elseBody != null) {
            emit("  j " + endLabel);
        }

        // Else block
        emit(elseLabel + ":");
        if (stmt.elseBody != null) {
            generateStmt(stmt.elseBody);
        }

        emit(endLabel + ":");
    }

    private void generateWhile(AST.WhileStmt stmt) {
        String startLabel = newLabel("while_start");
        String bodyLabel = newLabel("while_body");
        String endLabel = newLabel("while_end");

        loopStartLabels.push(startLabel);
        loopEndLabels.push(endLabel);

        // Jump to condition check
        emit("  j " + startLabel);

        // Loop body
        emit(bodyLabel + ":");
        generateStmt(stmt.body);

        // Condition check
        emit(startLabel + ":");
        generateCond(stmt.cond, bodyLabel, true); // jump to body if true
        // Fall through = exit loop
        emit(endLabel + ":");

        loopStartLabels.pop();
        loopEndLabels.pop();
    }

    private void generateReturn(AST.ReturnStmt stmt) {
        if (stmt.value != null) {
            generateExpr(stmt.value); // result in a0
        }
        // Epilogue
        emit("  lw fp, 4(sp)");
        emit("  lw ra, 0(sp)");
        emit("  addi sp, sp, " + frameSize);
        emit("  ret");
    }

    // ================================================================
    // Expression Generation (result always in a0)
    // ================================================================

    private void generateExpr(AST.ExprNode expr) {
        if (expr instanceof AST.NumberExpr num) {
            emit("  li a0, " + num.value);
        } else if (expr instanceof AST.IdExpr id) {
            generateLoad(id);
        } else if (expr instanceof AST.BinaryExpr bin) {
            generateBinary(bin);
        } else if (expr instanceof AST.UnaryExpr un) {
            generateUnary(un);
        } else if (expr instanceof AST.CallExpr call) {
            generateCall(call);
        }
    }

    private SymbolTable.Symbol lookupSym(String name) {
        // Check function locals first
        if (currentFuncName != null) {
            SymbolTable.Symbol sym = symtab.lookupFunctionLocal(currentFuncName, name);
            if (sym != null) return sym;
        }
        // Fall back to global/resolved
        return symtab.lookupResolved(name);
    }

    private void generateLoad(AST.IdExpr id) {
        SymbolTable.Symbol sym = lookupSym(id.name);
        if (sym == null) {
            emit("  li a0, 0"); // error recovery
            return;
        }

        if (sym.kind == SymbolTable.Kind.CONST) {
            // Compile-time constant - just load the value
            emit("  li a0, " + sym.constValue);
        } else if (sym.isGlobal) {
            emit("  la t0, " + id.name);
            emit("  lw a0, 0(t0)");
        } else {
            emit("  lw a0, " + sym.offset + "(sp)");
        }
    }

    private void generateBinary(AST.BinaryExpr expr) {
        String op = expr.op;

        // Short-circuit evaluation for logical operators
        if (op.equals("||")) {
            String trueLabel = newLabel("or_true");
            String endLabel = newLabel("or_end");
            generateExpr(expr.left);
            emit("  bnez a0, " + trueLabel);
            generateExpr(expr.right);
            emit("  beqz a0, " + endLabel);
            emit(trueLabel + ":");
            emit("  li a0, 1");
            emit(endLabel + ":");
            return;
        }

        if (op.equals("&&")) {
            String falseLabel = newLabel("and_false");
            String endLabel = newLabel("and_end");
            generateExpr(expr.left);
            emit("  beqz a0, " + falseLabel);
            generateExpr(expr.right);
            emit("  beqz a0, " + falseLabel);
            emit("  li a0, 1");
            emit("  j " + endLabel);
            emit(falseLabel + ":");
            emit("  li a0, 0");
            emit(endLabel + ":");
            return;
        }

        // Regular binary operators
        generateExpr(expr.left);
        emit("  addi sp, sp, -4");
        emit("  sw a0, 0(sp)");
        generateExpr(expr.right);
        emit("  lw t0, 0(sp)");
        emit("  addi sp, sp, 4");

        switch (op) {
            case "+":
                emit("  add a0, t0, a0");
                break;
            case "-":
                emit("  sub a0, t0, a0");
                break;
            case "*":
                emit("  mul a0, t0, a0");
                break;
            case "/":
                emit("  div a0, t0, a0");
                break;
            case "%":
                emit("  rem a0, t0, a0");
                break;
            case "<":
                emit("  slt a0, t0, a0");
                break;
            case ">":
                emit("  slt a0, a0, t0");
                break;
            case "<=":
                // a0 = (t0 <= a0) = !(a0 < t0) = (a0 < t0) ? 0 : 1
                emit("  slt a0, a0, t0");
                emit("  xori a0, a0, 1");
                break;
            case ">=":
                // a0 = (t0 >= a0) = !(t0 < a0) = (t0 < a0) ? 0 : 1
                emit("  slt a0, t0, a0");
                emit("  xori a0, a0, 1");
                break;
            case "==":
                emit("  sub a0, t0, a0");
                emit("  sltiu a0, a0, 1");
                break;
            case "!=":
                emit("  sub a0, t0, a0");
                emit("  sltu a0, zero, a0");
                break;
        }
    }

    private void generateUnary(AST.UnaryExpr expr) {
        generateExpr(expr.operand);
        switch (expr.op) {
            case "-":
                emit("  sub a0, zero, a0");
                break;
            case "!":
                emit("  sltiu a0, a0, 1");
                break;
            case "+":
                // nothing
                break;
        }
    }

    private void generateCall(AST.CallExpr call) {
        int argCount = call.args.size();

        // Evaluate arguments, pushing each to the stack
        for (int i = 0; i < argCount; i++) {
            generateExpr(call.args.get(i)); // result in a0
            emit("  addi sp, sp, -4");
            emit("  sw a0, 0(sp)");
        }

        // Load arguments from stack into a0-a7 registers
        // Stack has: [argN-1] ... [arg0] (top to bottom)
        // We need arg0→a0, arg1→a1, etc.
        if (argCount > 0) {
            // Args are at offsets 0, 4, 8, ... from sp
            // arg0 is at the bottom = sp + 4*(argCount-1)
            for (int i = 0; i < argCount && i < 8; i++) {
                int offset = (argCount - 1 - i) * 4;
                emit("  lw a" + i + ", " + offset + "(sp)");
            }
            // Restore sp
            emit("  addi sp, sp, " + (argCount * 4));
        }

        // Call the function
        emit("  jal ra, " + call.name);
    }

    // ================================================================
    // Condition Generation (for if/while)
    // ================================================================

    /**
     * Generate code that jumps to targetLabel if condition is true/false.
     * @param jumpIfTrue if true, jump when condition is true; if false, jump when false
     */
    private void generateCond(AST.ExprNode cond, String targetLabel, boolean jumpIfTrue) {
        if (cond instanceof AST.BinaryExpr bin) {
            String op = bin.op;
            if (op.equals("||")) {
                if (jumpIfTrue) {
                    // Jump if left || right is true
                    String skipLabel = newLabel("cond_skip");
                    generateExpr(bin.left);
                    emit("  bnez a0, " + targetLabel); // if left is true, jump
                    generateExpr(bin.right);
                    emit("  bnez a0, " + targetLabel); // if right is true, jump
                } else {
                    // Jump if left || right is false
                    // (left || right) is false iff left is false AND right is false
                    String checkRight = newLabel("cond_or_check");
                    generateExpr(bin.left);
                    emit("  bnez a0, " + checkRight); // left is true, skip jump
                    generateExpr(bin.right);
                    emit("  beqz a0, " + targetLabel); // both false, jump
                    emit(checkRight + ":");
                }
                return;
            }
            if (op.equals("&&")) {
                if (jumpIfTrue) {
                    // Jump if left && right is true
                    String checkRight = newLabel("cond_and_check");
                    generateExpr(bin.left);
                    emit("  beqz a0, " + checkRight); // left is false, skip
                    generateExpr(bin.right);
                    emit("  bnez a0, " + targetLabel); // both true, jump
                    emit(checkRight + ":");
                } else {
                    // Jump if left && right is false
                    generateExpr(bin.left);
                    emit("  beqz a0, " + targetLabel);
                    generateExpr(bin.right);
                    emit("  beqz a0, " + targetLabel);
                }
                return;
            }
        }

        // Default: evaluate and compare to zero
        generateExpr(cond);
        if (jumpIfTrue) {
            emit("  bnez a0, " + targetLabel);
        } else {
            emit("  beqz a0, " + targetLabel);
        }
    }

    // ================================================================
    // Helpers
    // ================================================================

    private void emit(String line) {
        out.append(line).append('\n');
    }

    private String newLabel(String prefix) {
        return ".L" + prefix + "_" + (labelCounter++);
    }

    private boolean endsWithReturn(AST.StmtNode stmt) {
        if (stmt instanceof AST.ReturnStmt) {
            return true;
        } else if (stmt instanceof AST.Block block) {
            if (block.stmts.isEmpty()) return false;
            return endsWithReturn(block.stmts.get(block.stmts.size() - 1));
        } else if (stmt instanceof AST.IfStmt ifStmt) {
            if (ifStmt.elseBody != null) {
                return endsWithReturn(ifStmt.thenBody) && endsWithReturn(ifStmt.elseBody);
            }
            return false;
        } else if (stmt instanceof AST.WhileStmt) {
            return false;
        }
        return false;
    }
}
