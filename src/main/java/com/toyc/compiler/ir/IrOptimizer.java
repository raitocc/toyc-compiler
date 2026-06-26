package com.toyc.compiler.ir;

import java.util.*;

public class IrOptimizer {

    public IR.Program optimize(IR.Program program) {
        for (IR.FuncDef func : program.functions) {
            optimizeFunc(func);
        }
        return program;
    }

    private void optimizeFunc(IR.FuncDef func) {
        boolean changed;
        do {
            changed = false;
            // 1. 本地常量折叠、常量传播、复写传播
            for (IR.BasicBlock block : func.blocks) {
                changed |= optimizeBlock(block);
            }
            // 2. 全局死代码消除 (Dead Code Elimination)
            changed |= eliminateDeadCode(func);
        } while (changed);
    }

    private boolean optimizeBlock(IR.BasicBlock block) {
        boolean changed = false;
        Map<String, IR.Value> valueEnv = new HashMap<>(); // TempVar name -> IR.Value (ConstValue or TempVar)

        for (int i = 0; i < block.instructions.size(); i++) {
            IR.IrInstr instr = block.instructions.get(i);

            // 1. 常量传播与复写传播：尝试替换读取的操作数
            if (instr.arg1 instanceof IR.TempVar) {
                String name = instr.arg1.toPrintString();
                if (valueEnv.containsKey(name)) {
                    IR.Value replacement = valueEnv.get(name);
                    if (!sameValue(instr.arg1, replacement)) {
                        instr.arg1 = replacement;
                        changed = true;
                    }
                }
            }
            if (instr.arg2 instanceof IR.TempVar) {
                String name = instr.arg2.toPrintString();
                if (valueEnv.containsKey(name)) {
                    IR.Value replacement = valueEnv.get(name);
                    if (!sameValue(instr.arg2, replacement)) {
                        instr.arg2 = replacement;
                        changed = true;
                    }
                }
            }
            // RET now uses arg1, so it is handled above!

            // 2. 常量折叠 (Constant Folding) - 二元运算
            if (isBinaryArithmetic(instr.op) && instr.arg1 instanceof IR.ConstValue && instr.arg2 instanceof IR.ConstValue) {
                int v1 = ((IR.ConstValue) instr.arg1).val;
                int v2 = ((IR.ConstValue) instr.arg2).val;
                
                // 防止除0异常崩溃
                if ((instr.op == IR.OpCode.DIV || instr.op == IR.OpCode.MOD) && v2 == 0) {
                    // 保留原样，让运行时报错
                } else {
                    int res = foldBinary(instr.op, v1, v2);
                    instr.op = IR.OpCode.ASSIGN;
                    instr.arg1 = new IR.ConstValue(res);
                    instr.arg2 = null;
                    changed = true;
                }
            }

            // 3. 常量折叠 (Constant Folding) - 一元运算
            if (isUnaryArithmetic(instr.op) && instr.arg1 instanceof IR.ConstValue) {
                int v1 = ((IR.ConstValue) instr.arg1).val;
                int res = foldUnary(instr.op, v1);
                instr.op = IR.OpCode.ASSIGN;
                instr.arg1 = new IR.ConstValue(res);
                instr.arg2 = null;
                changed = true;
            }

            // 4. 代数化简 (Algebraic Simplification)
            changed |= simplifyAlgebraic(instr);

            // 5. 记录 ASSIGN 指令，用于后续的常量/复写传播
            if (instr.op == IR.OpCode.ASSIGN && instr.result instanceof IR.TempVar) {
                killValueEnv(valueEnv, instr.result.toPrintString());
                if (instr.arg1 instanceof IR.ConstValue || instr.arg1 instanceof IR.TempVar) {
                    if (sameValue(instr.result, instr.arg1)) {
                        valueEnv.remove(instr.result.toPrintString());
                    } else {
                        valueEnv.put(instr.result.toPrintString(), instr.arg1);
                    }
                }
            } else if (instr.result instanceof IR.TempVar) {
                // 如果局部变量被重新定义，依赖它的旧复写关系都必须失效。
                killValueEnv(valueEnv, instr.result.toPrintString());
            }
        }
        return changed;
    }

    private void killValueEnv(Map<String, IR.Value> valueEnv, String definedName) {
        valueEnv.entrySet().removeIf(entry ->
            entry.getKey().equals(definedName)
                || entry.getValue() instanceof IR.TempVar
                && entry.getValue().toPrintString().equals(definedName));
    }

    private boolean sameValue(IR.Value left, IR.Value right) {
        if (left == right) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return left.getClass() == right.getClass()
            && left.toPrintString().equals(right.toPrintString());
    }

    private boolean eliminateDeadCode(IR.FuncDef func) {
        boolean changed = false;
        // 1. 统计所有用到的 TempVar 集合
        Set<String> usedVars = new HashSet<>();
        
        // 我们不能消除函数参数对应的 TempVar，因为即使没被读取，也不能当做死指令来删 (虽然参数绑定没有显式 ASSIGN，但还是保留为好)
        for (String paramName : func.params) {
            usedVars.add(paramName);
        }

        for (IR.BasicBlock block : func.blocks) {
            for (IR.IrInstr instr : block.instructions) {
                if (instr.arg1 instanceof IR.TempVar) usedVars.add(instr.arg1.toPrintString());
                if (instr.arg2 instanceof IR.TempVar) usedVars.add(instr.arg2.toPrintString());
                // RET uses arg1, which is checked above!
            }
        }

        // 2. 扫描并删除未使用的 TempVar 的赋值指令
        for (IR.BasicBlock block : func.blocks) {
            Iterator<IR.IrInstr> it = block.instructions.iterator();
            while (it.hasNext()) {
                IR.IrInstr instr = it.next();
                if (instr.result instanceof IR.TempVar) {
                    String resName = instr.result.toPrintString();
                    // 如果这个 TempVar 从未被使用，且该操作是纯赋值或算术运算（无副作用）
                    if (!usedVars.contains(resName) && !hasSideEffect(instr.op)) {
                        it.remove();
                        changed = true;
                    }
                }
            }
        }
        return changed;
    }

    private boolean isBinaryArithmetic(IR.OpCode op) {
        return op == IR.OpCode.ADD || op == IR.OpCode.SUB || op == IR.OpCode.MUL || op == IR.OpCode.DIV || op == IR.OpCode.MOD
            || op == IR.OpCode.SEQ || op == IR.OpCode.SNE || op == IR.OpCode.SLT || op == IR.OpCode.SGT || op == IR.OpCode.SLE || op == IR.OpCode.SGE
            || op == IR.OpCode.AND || op == IR.OpCode.OR;
    }

    private boolean isUnaryArithmetic(IR.OpCode op) {
        return op == IR.OpCode.NEG || op == IR.OpCode.NOT;
    }

    private boolean hasSideEffect(IR.OpCode op) {
        // ASSIGN, 算术运算均无副作用；但 CALL 函数调用可能有副作用
        // 注意：NameValue (全局变量) 的 ASSIGN 不能删除，但因为我们只剔除 TempVar 类型的 result，所以没问题
        return op == IR.OpCode.CALL || op == IR.OpCode.LOAD || op == IR.OpCode.STORE;
    }

    private int foldBinary(IR.OpCode op, int v1, int v2) {
        return switch (op) {
            case ADD -> v1 + v2;
            case SUB -> v1 - v2;
            case MUL -> v1 * v2;
            case DIV -> v1 / v2; 
            case MOD -> v1 % v2;
            case SEQ -> v1 == v2 ? 1 : 0;
            case SNE -> v1 != v2 ? 1 : 0;
            case SLT -> v1 < v2 ? 1 : 0;
            case SGT -> v1 > v2 ? 1 : 0;
            case SLE -> v1 <= v2 ? 1 : 0;
            case SGE -> v1 >= v2 ? 1 : 0;
            case AND -> (v1 != 0 && v2 != 0) ? 1 : 0;
            case OR -> (v1 != 0 || v2 != 0) ? 1 : 0;
            default -> throw new RuntimeException("Unknown fold bin op: " + op);
        };
    }

    private int foldUnary(IR.OpCode op, int v1) {
        return switch (op) {
            case NEG -> -v1;
            case NOT -> v1 == 0 ? 1 : 0;
            default -> throw new RuntimeException("Unknown fold unary op: " + op);
        };
    }

    private boolean simplifyAlgebraic(IR.IrInstr instr) {
        boolean changed = false;
        if (instr.op == IR.OpCode.ADD) {
            if (isConst(instr.arg1, 0)) { // 0 + x -> x
                instr.op = IR.OpCode.ASSIGN; instr.arg1 = instr.arg2; instr.arg2 = null; changed = true;
            } else if (isConst(instr.arg2, 0)) { // x + 0 -> x
                instr.op = IR.OpCode.ASSIGN; instr.arg2 = null; changed = true;
            }
        } else if (instr.op == IR.OpCode.SUB) {
            if (isConst(instr.arg2, 0)) { // x - 0 -> x
                instr.op = IR.OpCode.ASSIGN; instr.arg2 = null; changed = true;
            } else if (isConst(instr.arg1, 0)) { // 0 - x -> NEG x
                instr.op = IR.OpCode.NEG; instr.arg1 = instr.arg2; instr.arg2 = null; changed = true;
            }
        } else if (instr.op == IR.OpCode.MUL) {
            if (isConst(instr.arg1, 1)) { // 1 * x -> x
                instr.op = IR.OpCode.ASSIGN; instr.arg1 = instr.arg2; instr.arg2 = null; changed = true;
            } else if (isConst(instr.arg2, 1)) { // x * 1 -> x
                instr.op = IR.OpCode.ASSIGN; instr.arg2 = null; changed = true;
            } else if (isConst(instr.arg1, 0) || isConst(instr.arg2, 0)) { // x * 0 -> 0
                instr.op = IR.OpCode.ASSIGN; instr.arg1 = new IR.ConstValue(0); instr.arg2 = null; changed = true;
            }
        } else if (instr.op == IR.OpCode.DIV) {
            if (isConst(instr.arg2, 1)) { // x / 1 -> x
                instr.op = IR.OpCode.ASSIGN; instr.arg2 = null; changed = true;
            } else if (isConst(instr.arg1, 0)) { // 0 / x -> 0 (assume x!=0)
                instr.op = IR.OpCode.ASSIGN; instr.arg1 = new IR.ConstValue(0); instr.arg2 = null; changed = true;
            }
        }
        return changed;
    }

    private boolean isConst(IR.Value val, int num) {
        return val instanceof IR.ConstValue && ((IR.ConstValue) val).val == num;
    }
}
