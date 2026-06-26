package com.toyc.compiler.ir;

import java.util.*;

public class IrOptimizer {

    public IR.Program optimize(IR.Program program) {
        propagateReadOnlyGlobals(program);
        ConstantCallEvaluator constantCallEvaluator = new ConstantCallEvaluator(program);
        for (IR.FuncDef func : program.functions) {
            optimizeFunc(func, constantCallEvaluator);
        }
        return program;
    }

    private boolean propagateReadOnlyGlobals(IR.Program program) {
        Map<String, Integer> globalValues = new HashMap<>();
        for (IR.GlobalVar globalVar : program.globalVars) {
            globalValues.put(globalVar.name, globalVar.initValue);
        }
        if (globalValues.isEmpty()) {
            return false;
        }

        Set<String> writtenGlobals = new HashSet<>();
        for (IR.FuncDef func : program.functions) {
            for (IR.BasicBlock block : func.blocks) {
                for (IR.IrInstr instr : block.instructions) {
                    if (instr.result instanceof IR.NameValue) {
                        writtenGlobals.add(((IR.NameValue) instr.result).name);
                    }
                }
            }
        }
        for (String writtenGlobal : writtenGlobals) {
            globalValues.remove(writtenGlobal);
        }
        if (globalValues.isEmpty()) {
            return false;
        }

        boolean changed = false;
        for (IR.FuncDef func : program.functions) {
            for (IR.BasicBlock block : func.blocks) {
                for (IR.IrInstr instr : block.instructions) {
                    IR.Value newArg1 = replaceReadOnlyGlobal(instr.arg1, globalValues);
                    if (!sameValue(instr.arg1, newArg1)) {
                        instr.arg1 = newArg1;
                        changed = true;
                    }
                    IR.Value newArg2 = replaceReadOnlyGlobal(instr.arg2, globalValues);
                    if (!sameValue(instr.arg2, newArg2)) {
                        instr.arg2 = newArg2;
                        changed = true;
                    }
                }
            }
        }
        return changed;
    }

    private IR.Value replaceReadOnlyGlobal(IR.Value value, Map<String, Integer> globalValues) {
        if (value instanceof IR.NameValue) {
            String name = ((IR.NameValue) value).name;
            Integer constant = globalValues.get(name);
            if (constant != null) {
                return new IR.ConstValue(constant);
            }
        }
        return value;
    }

    private void optimizeFunc(IR.FuncDef func, ConstantCallEvaluator constantCallEvaluator) {
        boolean changed;
        do {
            changed = false;
            // 先清理明显不可达的控制流，避免后续数据流分析被脏块干扰。
            changed |= cleanupControlFlow(func);
            // 1. 本地常量折叠、常量传播、复写传播
            for (IR.BasicBlock block : func.blocks) {
                changed |= optimizeBlock(block);
            }
            changed |= removeNoOpAssignments(func);
            changed |= foldConstantCalls(func, constantCallEvaluator);
            // 2. 全局死代码消除 (Dead Code Elimination)
            changed |= eliminateDeadCode(func);
            changed |= cleanupControlFlow(func);
        } while (changed);
    }

    private boolean removeNoOpAssignments(IR.FuncDef func) {
        boolean changed = false;
        for (IR.BasicBlock block : func.blocks) {
            Iterator<IR.IrInstr> iterator = block.instructions.iterator();
            while (iterator.hasNext()) {
                IR.IrInstr instr = iterator.next();
                if (instr.op == IR.OpCode.ASSIGN
                    && instr.result instanceof IR.TempVar
                    && sameValue(instr.result, instr.arg1)) {
                    iterator.remove();
                    changed = true;
                }
            }
        }
        return changed;
    }

    private boolean foldConstantCalls(IR.FuncDef func, ConstantCallEvaluator evaluator) {
        boolean changed = false;
        for (IR.BasicBlock block : func.blocks) {
            List<IR.IrInstr> rewritten = new ArrayList<>();
            List<IR.IrInstr> pendingParams = new ArrayList<>();

            for (IR.IrInstr instr : block.instructions) {
                if (instr.op == IR.OpCode.PARAM) {
                    pendingParams.add(instr);
                    continue;
                }

                if (instr.op == IR.OpCode.CALL && instr.arg1 instanceof IR.NameValue) {
                    String targetName = ((IR.NameValue) instr.arg1).name;
                    OptionalInt folded = tryFoldCall(targetName, pendingParams, evaluator);
                    if (folded.isPresent()) {
                        if (instr.result != null) {
                            rewritten.add(new IR.IrInstr(IR.OpCode.ASSIGN, new IR.ConstValue(folded.getAsInt()), instr.result));
                        }
                        pendingParams.clear();
                        changed = true;
                        continue;
                    }
                }

                if (!pendingParams.isEmpty()) {
                    rewritten.addAll(pendingParams);
                    pendingParams.clear();
                }
                rewritten.add(instr);
            }

            if (!pendingParams.isEmpty()) {
                rewritten.addAll(pendingParams);
            }
            if (changed) {
                block.instructions = rewritten;
            }
        }
        return changed;
    }

    private OptionalInt tryFoldCall(String targetName, List<IR.IrInstr> pendingParams, ConstantCallEvaluator evaluator) {
        List<Integer> args = new ArrayList<>();
        for (IR.IrInstr param : pendingParams) {
            if (!(param.arg1 instanceof IR.ConstValue)) {
                return OptionalInt.empty();
            }
            args.add(((IR.ConstValue) param.arg1).val);
        }
        return evaluator.evaluate(targetName, args);
    }

    private boolean cleanupControlFlow(IR.FuncDef func) {
        boolean changed = false;
        Map<String, IR.BasicBlock> blockMap = new HashMap<>();
        for (IR.BasicBlock block : func.blocks) {
            blockMap.put(block.name, block);
        }

        for (int i = 0; i < func.blocks.size(); i++) {
            IR.BasicBlock block = func.blocks.get(i);
            changed |= removeInstructionsAfterTerminator(block);
            changed |= foldConstantBranch(block);

            if (!block.instructions.isEmpty()) {
                IR.IrInstr last = block.instructions.get(block.instructions.size() - 1);
                if (last.op == IR.OpCode.JMP && last.result instanceof IR.LabelValue) {
                    String target = ((IR.LabelValue) last.result).name;
                    if (i + 1 < func.blocks.size() && func.blocks.get(i + 1).name.equals(target)) {
                        block.instructions.remove(block.instructions.size() - 1);
                        changed = true;
                    }
                }
            }
        }

        Set<String> reachable = collectReachableBlocks(func, blockMap);
        if (func.blocks.removeIf(block -> !reachable.contains(block.name))) {
            changed = true;
        }

        return changed;
    }

    private boolean removeInstructionsAfterTerminator(IR.BasicBlock block) {
        for (int i = 0; i < block.instructions.size(); i++) {
            IR.IrInstr instr = block.instructions.get(i);
            if (instr.op == IR.OpCode.RET || instr.op == IR.OpCode.JMP) {
                if (i + 1 < block.instructions.size()) {
                    block.instructions = new ArrayList<>(block.instructions.subList(0, i + 1));
                    return true;
                }
                return false;
            }
        }
        return false;
    }

    private boolean foldConstantBranch(IR.BasicBlock block) {
        if (block.instructions.isEmpty()) {
            return false;
        }
        IR.IrInstr last = block.instructions.get(block.instructions.size() - 1);
        if (!(last.arg1 instanceof IR.ConstValue)) {
            return false;
        }
        if (last.op == IR.OpCode.BEQZ) {
            int value = ((IR.ConstValue) last.arg1).val;
            if (value == 0) {
                last.op = IR.OpCode.JMP;
                last.arg1 = null;
            } else {
                block.instructions.remove(block.instructions.size() - 1);
            }
            return true;
        }
        if (last.op == IR.OpCode.BNEZ) {
            int value = ((IR.ConstValue) last.arg1).val;
            if (value != 0) {
                last.op = IR.OpCode.JMP;
                last.arg1 = null;
            } else {
                block.instructions.remove(block.instructions.size() - 1);
            }
            return true;
        }
        return false;
    }

    private Set<String> collectReachableBlocks(IR.FuncDef func, Map<String, IR.BasicBlock> blockMap) {
        Set<String> reachable = new HashSet<>();
        if (func.blocks.isEmpty()) {
            return reachable;
        }

        Deque<IR.BasicBlock> worklist = new ArrayDeque<>();
        worklist.add(func.blocks.get(0));

        while (!worklist.isEmpty()) {
            IR.BasicBlock block = worklist.removeFirst();
            if (!reachable.add(block.name)) {
                continue;
            }
            for (IR.BasicBlock successor : getSuccessors(func, block, blockMap)) {
                if (!reachable.contains(successor.name)) {
                    worklist.addLast(successor);
                }
            }
        }
        return reachable;
    }

    private List<IR.BasicBlock> getSuccessors(IR.FuncDef func, IR.BasicBlock block, Map<String, IR.BasicBlock> blockMap) {
        List<IR.BasicBlock> successors = new ArrayList<>();
        int index = func.blocks.indexOf(block);
        if (block.instructions.isEmpty()) {
            addFallthroughSuccessor(func, successors, index);
            return successors;
        }

        IR.IrInstr last = block.instructions.get(block.instructions.size() - 1);
        if (last.op == IR.OpCode.JMP && last.result instanceof IR.LabelValue) {
            addLabelSuccessor(successors, blockMap, (IR.LabelValue) last.result);
            return successors;
        }
        if ((last.op == IR.OpCode.BEQZ || last.op == IR.OpCode.BNEZ) && last.result instanceof IR.LabelValue) {
            addLabelSuccessor(successors, blockMap, (IR.LabelValue) last.result);
            addFallthroughSuccessor(func, successors, index);
            return successors;
        }
        if (last.op != IR.OpCode.RET) {
            addFallthroughSuccessor(func, successors, index);
        }
        return successors;
    }

    private void addLabelSuccessor(List<IR.BasicBlock> successors, Map<String, IR.BasicBlock> blockMap, IR.LabelValue label) {
        IR.BasicBlock target = blockMap.get(label.name);
        if (target != null && !successors.contains(target)) {
            successors.add(target);
        }
    }

    private void addFallthroughSuccessor(IR.FuncDef func, List<IR.BasicBlock> successors, int index) {
        if (index + 1 < func.blocks.size()) {
            IR.BasicBlock target = func.blocks.get(index + 1);
            if (!successors.contains(target)) {
                successors.add(target);
            }
        }
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
        Map<String, IR.BasicBlock> blockMap = new HashMap<>();
        for (IR.BasicBlock block : func.blocks) {
            blockMap.put(block.name, block);
        }

        Map<IR.BasicBlock, Set<String>> useMap = new HashMap<>();
        Map<IR.BasicBlock, Set<String>> defMap = new HashMap<>();
        Map<IR.BasicBlock, Set<String>> liveIn = new HashMap<>();
        Map<IR.BasicBlock, Set<String>> liveOut = new HashMap<>();

        for (IR.BasicBlock block : func.blocks) {
            Set<String> use = new HashSet<>();
            Set<String> def = new HashSet<>();
            for (IR.IrInstr instr : block.instructions) {
                for (String temp : getTempUses(instr)) {
                    if (!def.contains(temp)) {
                        use.add(temp);
                    }
                }
                String defined = getTempDef(instr);
                if (defined != null && !use.contains(defined)) {
                    def.add(defined);
                }
            }
            useMap.put(block, use);
            defMap.put(block, def);
            liveIn.put(block, new HashSet<>());
            liveOut.put(block, new HashSet<>());
        }

        boolean dataflowChanged;
        do {
            dataflowChanged = false;
            for (int i = func.blocks.size() - 1; i >= 0; i--) {
                IR.BasicBlock block = func.blocks.get(i);
                Set<String> newOut = new HashSet<>();
                for (IR.BasicBlock successor : getSuccessors(func, block, blockMap)) {
                    newOut.addAll(liveIn.getOrDefault(successor, Set.of()));
                }

                Set<String> newIn = new HashSet<>(newOut);
                newIn.removeAll(defMap.get(block));
                newIn.addAll(useMap.get(block));

                if (!newOut.equals(liveOut.get(block)) || !newIn.equals(liveIn.get(block))) {
                    liveOut.put(block, newOut);
                    liveIn.put(block, newIn);
                    dataflowChanged = true;
                }
            }
        } while (dataflowChanged);

        boolean changed = false;
        for (IR.BasicBlock block : func.blocks) {
            Set<String> live = new HashSet<>(liveOut.get(block));
            ListIterator<IR.IrInstr> iterator = block.instructions.listIterator(block.instructions.size());
            while (iterator.hasPrevious()) {
                IR.IrInstr instr = iterator.previous();
                String defined = getTempDef(instr);
                if (defined != null && !live.contains(defined) && !hasSideEffect(instr)) {
                    iterator.remove();
                    changed = true;
                    continue;
                }

                if (defined != null) {
                    live.remove(defined);
                }
                live.addAll(getTempUses(instr));
            }
        }
        return changed;
    }

    private Set<String> getTempUses(IR.IrInstr instr) {
        Set<String> uses = new LinkedHashSet<>();
        addTempUse(uses, instr.arg1);
        addTempUse(uses, instr.arg2);
        if (instr.op == IR.OpCode.STORE) {
            addTempUse(uses, instr.result);
        }
        return uses;
    }

    private String getTempDef(IR.IrInstr instr) {
        if (instr.op == IR.OpCode.STORE || instr.op == IR.OpCode.PARAM || instr.op == IR.OpCode.RET) {
            return null;
        }
        if (instr.op == IR.OpCode.JMP || instr.op == IR.OpCode.BEQZ || instr.op == IR.OpCode.BNEZ) {
            return null;
        }
        if (instr.result instanceof IR.TempVar) {
            return instr.result.toPrintString();
        }
        return null;
    }

    private void addTempUse(Set<String> uses, IR.Value value) {
        if (value instanceof IR.TempVar) {
            uses.add(value.toPrintString());
        }
    }

    private boolean isBinaryArithmetic(IR.OpCode op) {
        return op == IR.OpCode.ADD || op == IR.OpCode.SUB || op == IR.OpCode.MUL || op == IR.OpCode.DIV || op == IR.OpCode.MOD
            || op == IR.OpCode.SEQ || op == IR.OpCode.SNE || op == IR.OpCode.SLT || op == IR.OpCode.SGT || op == IR.OpCode.SLE || op == IR.OpCode.SGE
            || op == IR.OpCode.AND || op == IR.OpCode.OR;
    }

    private boolean isUnaryArithmetic(IR.OpCode op) {
        return op == IR.OpCode.NEG || op == IR.OpCode.NOT;
    }

    private boolean hasSideEffect(IR.IrInstr instr) {
        // ASSIGN、算术运算均无副作用；CALL 和显式内存写必须保留。
        // 全局变量赋值的 result 是 NameValue，不会作为 TempVar 定义被删除。
        return instr.op == IR.OpCode.CALL || instr.op == IR.OpCode.LOAD || instr.op == IR.OpCode.STORE;
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

    private class ConstantCallEvaluator {
        private static final int DEFAULT_FUEL = 5_000_000;

        private final Map<String, IR.FuncDef> functions = new HashMap<>();
        private final Set<String> pureFunctions = new HashSet<>();
        private final Map<CallKey, OptionalInt> cache = new HashMap<>();
        private final Set<CallKey> activeCalls = new HashSet<>();

        ConstantCallEvaluator(IR.Program program) {
            for (IR.FuncDef func : program.functions) {
                functions.put(func.name, func);
                if (!hasDirectSideEffect(func)) {
                    pureFunctions.add(func.name);
                }
            }
            boolean changed;
            do {
                changed = false;
                Iterator<String> iterator = pureFunctions.iterator();
                while (iterator.hasNext()) {
                    IR.FuncDef func = functions.get(iterator.next());
                    if (callsImpureFunction(func)) {
                        iterator.remove();
                        changed = true;
                    }
                }
            } while (changed);
        }

        OptionalInt evaluate(String funcName, List<Integer> args) {
            return evaluate(funcName, args, new Fuel(DEFAULT_FUEL));
        }

        private OptionalInt evaluate(String funcName, List<Integer> args, Fuel fuel) {
            IR.FuncDef func = functions.get(funcName);
            if (func == null || !pureFunctions.contains(funcName) || func.params.size() != args.size()) {
                return OptionalInt.empty();
            }

            CallKey key = new CallKey(funcName, List.copyOf(args));
            if (cache.containsKey(key)) {
                return cache.get(key);
            }
            if (!activeCalls.add(key)) {
                return OptionalInt.empty();
            }

            OptionalInt result = execute(func, args, fuel);
            activeCalls.remove(key);
            cache.put(key, result);
            return result;
        }

        private boolean hasDirectSideEffect(IR.FuncDef func) {
            for (IR.BasicBlock block : func.blocks) {
                for (IR.IrInstr instr : block.instructions) {
                    if (instr.result instanceof IR.NameValue || instr.op == IR.OpCode.STORE || instr.op == IR.OpCode.LOAD) {
                        return true;
                    }
                }
            }
            return false;
        }

        private boolean callsImpureFunction(IR.FuncDef func) {
            for (IR.BasicBlock block : func.blocks) {
                for (IR.IrInstr instr : block.instructions) {
                    if (instr.op == IR.OpCode.CALL) {
                        if (!(instr.arg1 instanceof IR.NameValue)) {
                            return true;
                        }
                        String targetName = ((IR.NameValue) instr.arg1).name;
                        if (!pureFunctions.contains(targetName)) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        private OptionalInt execute(IR.FuncDef func, List<Integer> args, Fuel fuel) {
            Map<String, Integer> env = new HashMap<>();
            for (int i = 0; i < func.params.size(); i++) {
                env.put(func.params.get(i), args.get(i));
            }

            Map<String, IR.BasicBlock> blockMap = new HashMap<>();
            for (IR.BasicBlock block : func.blocks) {
                blockMap.put(block.name, block);
            }

            IR.BasicBlock block = func.blocks.isEmpty() ? null : func.blocks.get(0);
            List<Integer> pendingParams = new ArrayList<>();
            while (block != null) {
                for (int ip = 0; ip < block.instructions.size(); ip++) {
                    if (!fuel.consume()) {
                        return OptionalInt.empty();
                    }

                    IR.IrInstr instr = block.instructions.get(ip);
                    switch (instr.op) {
                        case ASSIGN -> {
                            OptionalInt value = evalValue(instr.arg1, env);
                            if (value.isEmpty() || !(instr.result instanceof IR.TempVar)) {
                                return OptionalInt.empty();
                            }
                            env.put(instr.result.toPrintString(), value.getAsInt());
                        }
                        case ADD, SUB, MUL, DIV, MOD, SEQ, SNE, SLT, SGT, SLE, SGE, AND, OR -> {
                            OptionalInt left = evalValue(instr.arg1, env);
                            OptionalInt right = evalValue(instr.arg2, env);
                            if (left.isEmpty() || right.isEmpty() || !(instr.result instanceof IR.TempVar)) {
                                return OptionalInt.empty();
                            }
                            if ((instr.op == IR.OpCode.DIV || instr.op == IR.OpCode.MOD) && right.getAsInt() == 0) {
                                return OptionalInt.empty();
                            }
                            env.put(instr.result.toPrintString(), foldBinary(instr.op, left.getAsInt(), right.getAsInt()));
                        }
                        case NEG, NOT -> {
                            OptionalInt value = evalValue(instr.arg1, env);
                            if (value.isEmpty() || !(instr.result instanceof IR.TempVar)) {
                                return OptionalInt.empty();
                            }
                            env.put(instr.result.toPrintString(), foldUnary(instr.op, value.getAsInt()));
                        }
                        case PARAM -> {
                            OptionalInt value = evalValue(instr.arg1, env);
                            if (value.isEmpty()) {
                                return OptionalInt.empty();
                            }
                            pendingParams.add(value.getAsInt());
                        }
                        case CALL -> {
                            if (!(instr.arg1 instanceof IR.NameValue) || !(instr.result instanceof IR.TempVar)) {
                                return OptionalInt.empty();
                            }
                            String targetName = ((IR.NameValue) instr.arg1).name;
                            OptionalInt value = evaluate(targetName, pendingParams, fuel);
                            pendingParams.clear();
                            if (value.isEmpty()) {
                                return OptionalInt.empty();
                            }
                            env.put(instr.result.toPrintString(), value.getAsInt());
                        }
                        case BEQZ -> {
                            OptionalInt value = evalValue(instr.arg1, env);
                            if (value.isEmpty() || !(instr.result instanceof IR.LabelValue)) {
                                return OptionalInt.empty();
                            }
                            if (value.getAsInt() == 0) {
                                block = blockMap.get(((IR.LabelValue) instr.result).name);
                                ip = -1;
                            }
                        }
                        case BNEZ -> {
                            OptionalInt value = evalValue(instr.arg1, env);
                            if (value.isEmpty() || !(instr.result instanceof IR.LabelValue)) {
                                return OptionalInt.empty();
                            }
                            if (value.getAsInt() != 0) {
                                block = blockMap.get(((IR.LabelValue) instr.result).name);
                                ip = -1;
                            }
                        }
                        case JMP -> {
                            if (!(instr.result instanceof IR.LabelValue)) {
                                return OptionalInt.empty();
                            }
                            block = blockMap.get(((IR.LabelValue) instr.result).name);
                            ip = -1;
                        }
                        case RET -> {
                            if (instr.arg1 == null) {
                                return OptionalInt.empty();
                            }
                            return evalValue(instr.arg1, env);
                        }
                        case LOAD, STORE -> {
                            return OptionalInt.empty();
                        }
                    }
                }

                int index = func.blocks.indexOf(block);
                block = index + 1 < func.blocks.size() ? func.blocks.get(index + 1) : null;
            }
            return OptionalInt.empty();
        }

        private OptionalInt evalValue(IR.Value value, Map<String, Integer> env) {
            if (value instanceof IR.ConstValue) {
                return OptionalInt.of(((IR.ConstValue) value).val);
            }
            if (value instanceof IR.TempVar) {
                Integer resolved = env.get(value.toPrintString());
                return resolved == null ? OptionalInt.empty() : OptionalInt.of(resolved);
            }
            return OptionalInt.empty();
        }
    }

    private record CallKey(String funcName, List<Integer> args) {
    }

    private static class Fuel {
        private int remaining;

        Fuel(int remaining) {
            this.remaining = remaining;
        }

        boolean consume() {
            if (remaining <= 0) {
                return false;
            }
            remaining--;
            return true;
        }
    }
}
