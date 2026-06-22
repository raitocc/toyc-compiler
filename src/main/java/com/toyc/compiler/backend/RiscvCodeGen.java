package com.toyc.compiler.backend;

import com.toyc.compiler.ir.TAC;
import java.io.PrintStream;
import java.util.*;

public class RiscvCodeGen {
    private final PrintStream out;
    private final List<TAC> instructions;
    
    private final Set<String> globalVars = new HashSet<>();
    private final Map<String, Integer> stackOffsets = new HashMap<>();
    private int currentStackSize = 0;
    
    // For function arguments mapping
    private final List<String> currentParams = new ArrayList<>();
    private final List<String> callArgs = new ArrayList<>();

    public RiscvCodeGen(PrintStream out, List<TAC> instructions) {
        this.out = out;
        this.instructions = instructions;
    }

    public void generate() {
        out.println("\t.data");
        
        // Find globals
        for (TAC inst : instructions) {
            if (inst.op == TAC.Op.GLOBAL_VAR) {
                globalVars.add(inst.result);
                out.println("\t.globl " + inst.result);
                out.println("\t.align 2");
                out.println(inst.result + ":");
                out.println("\t.word " + (inst.arg1 != null ? inst.arg1 : "0"));
            }
        }

        out.println("\n\t.text");
        
        boolean inFunc = false;
        String currentFunc = null;

        for (int i = 0; i < instructions.size(); i++) {
            TAC inst = instructions.get(i);
            if (inst.op == TAC.Op.GLOBAL_VAR) continue;

            if (inst.op == TAC.Op.FUNC_BEGIN) {
                inFunc = true;
                currentFunc = inst.result;
                out.println("\t.globl " + currentFunc);
                out.println(currentFunc + ":");
                
                // Pre-scan variables for stack allocation
                stackOffsets.clear();
                currentParams.clear();
                int varCount = 0;
                for (int j = i + 1; j < instructions.size(); j++) {
                    TAC inner = instructions.get(j);
                    if (inner.op == TAC.Op.FUNC_END) break;
                    
                    if (inner.op == TAC.Op.LABEL && inner.result.startsWith("PARAM_DEF_")) {
                        String paramName = inner.arg1;
                        currentParams.add(paramName);
                        stackOffsets.put(paramName, (varCount + 1) * 4);
                        varCount++;
                        continue;
                    }

                    if (inner.result != null && !isLabelOp(inner.op) && !globalVars.contains(inner.result) && !stackOffsets.containsKey(inner.result)) {
                        varCount++;
                        stackOffsets.put(inner.result, varCount * 4);
                    }
                }
                
                currentStackSize = (varCount * 4 + 15) & ~15; // 16-byte aligned
                
                // Prologue
                int frameSize = currentStackSize + 16;
                out.println("\taddi sp, sp, -" + frameSize);
                out.println("\tsw ra, " + (frameSize - 4) + "(sp)");
                out.println("\tsw s0, " + (frameSize - 8) + "(sp)");
                out.println("\taddi s0, sp, " + frameSize);
                
                // Load params from a0-a7 to stack
                for (int p = 0; p < currentParams.size() && p < 8; p++) {
                    String param = currentParams.get(p);
                    int offset = -stackOffsets.get(param);
                    out.println("\tsw a" + p + ", " + offset + "(s0)");
                }
                // (More than 8 args not supported in this simple ABI for now)
                
                continue;
            }

            if (inst.op == TAC.Op.FUNC_END) {
                inFunc = false;
                // Epilogue is handled in RET, but we put a safety epilogue here
                emitEpilogue();
                continue;
            }

            if (inFunc) {
                emitInstruction(inst);
            }
        }
    }
    
    private void emitEpilogue() {
        int frameSize = currentStackSize + 16;
        out.println("\tlw ra, " + (frameSize - 4) + "(sp)");
        out.println("\tlw s0, " + (frameSize - 8) + "(sp)");
        out.println("\taddi sp, sp, " + frameSize);
        out.println("\tret");
    }

    private boolean isLabelOp(TAC.Op op) {
        return op == TAC.Op.LABEL || op == TAC.Op.FUNC_BEGIN || op == TAC.Op.FUNC_END;
    }

    private void loadArg(String arg, String reg) {
        if (arg == null) return;
        if (arg.matches("-?\\d+")) {
            out.println("\tli " + reg + ", " + arg);
        } else if (globalVars.contains(arg)) {
            out.println("\tla t6, " + arg);
            out.println("\tlw " + reg + ", 0(t6)");
        } else if (stackOffsets.containsKey(arg)) {
            int offset = -stackOffsets.get(arg);
            out.println("\tlw " + reg + ", " + offset + "(s0)");
        }
    }

    private void storeRes(String reg, String res) {
        if (res == null) return;
        if (globalVars.contains(res)) {
            out.println("\tla t6, " + res);
            out.println("\tsw " + reg + ", 0(t6)");
        } else if (stackOffsets.containsKey(res)) {
            int offset = -stackOffsets.get(res);
            out.println("\tsw " + reg + ", " + offset + "(s0)");
        }
    }

    private void emitInstruction(TAC inst) {
        switch (inst.op) {
            case LABEL:
                if (!inst.result.startsWith("PARAM_DEF_")) {
                    out.println(inst.result + ":");
                }
                break;
            case LI:
                out.println("\tli t0, " + inst.arg1);
                storeRes("t0", inst.result);
                break;
            case ASSIGN:
                loadArg(inst.arg1, "t0");
                storeRes("t0", inst.result);
                break;
            case ADD:
                loadArg(inst.arg1, "t1");
                loadArg(inst.arg2, "t2");
                out.println("\tadd t0, t1, t2");
                storeRes("t0", inst.result);
                break;
            case SUB:
                loadArg(inst.arg1, "t1");
                loadArg(inst.arg2, "t2");
                out.println("\tsub t0, t1, t2");
                storeRes("t0", inst.result);
                break;
            case MUL:
                loadArg(inst.arg1, "t1");
                loadArg(inst.arg2, "t2");
                out.println("\tmul t0, t1, t2");
                storeRes("t0", inst.result);
                break;
            case DIV:
                loadArg(inst.arg1, "t1");
                loadArg(inst.arg2, "t2");
                out.println("\tdiv t0, t1, t2");
                storeRes("t0", inst.result);
                break;
            case MOD:
                loadArg(inst.arg1, "t1");
                loadArg(inst.arg2, "t2");
                out.println("\trem t0, t1, t2");
                storeRes("t0", inst.result);
                break;
            case SEQ:
                loadArg(inst.arg1, "t1");
                loadArg(inst.arg2, "t2");
                out.println("\tsub t0, t1, t2");
                out.println("\tseqz t0, t0");
                storeRes("t0", inst.result);
                break;
            case SNE:
                loadArg(inst.arg1, "t1");
                loadArg(inst.arg2, "t2");
                out.println("\tsub t0, t1, t2");
                out.println("\tsnez t0, t0");
                storeRes("t0", inst.result);
                break;
            case SLT:
                loadArg(inst.arg1, "t1");
                loadArg(inst.arg2, "t2");
                out.println("\tslt t0, t1, t2");
                storeRes("t0", inst.result);
                break;
            case SLE: // t1 <= t2  =>  !(t2 < t1)
                loadArg(inst.arg1, "t1");
                loadArg(inst.arg2, "t2");
                out.println("\tslt t0, t2, t1");
                out.println("\txori t0, t0, 1");
                storeRes("t0", inst.result);
                break;
            case SGT: // t1 > t2   =>  t2 < t1
                loadArg(inst.arg1, "t1");
                loadArg(inst.arg2, "t2");
                out.println("\tslt t0, t2, t1");
                storeRes("t0", inst.result);
                break;
            case SGE: // t1 >= t2  =>  !(t1 < t2)
                loadArg(inst.arg1, "t1");
                loadArg(inst.arg2, "t2");
                out.println("\tslt t0, t1, t2");
                out.println("\txori t0, t0, 1");
                storeRes("t0", inst.result);
                break;
            case JMP:
                out.println("\tj " + inst.result);
                break;
            case BEQZ:
                loadArg(inst.arg1, "t1");
                out.println("\tbeqz t1, " + inst.result);
                break;
            case BNEZ:
                loadArg(inst.arg1, "t1");
                out.println("\tbnez t1, " + inst.result);
                break;
            case PARAM:
                callArgs.add(inst.arg1);
                break;
            case CALL:
                for (int p = 0; p < callArgs.size() && p < 8; p++) {
                    loadArg(callArgs.get(p), "a" + p);
                }
                callArgs.clear();
                out.println("\tcall " + inst.arg1);
                if (inst.result != null) {
                    storeRes("a0", inst.result);
                }
                break;
            case RET:
                if (inst.arg1 != null) {
                    loadArg(inst.arg1, "a0");
                }
                emitEpilogue();
                break;
        }
    }
}
