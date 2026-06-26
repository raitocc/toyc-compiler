package com.toyc.compiler.backend;

import com.toyc.compiler.ir.IR.*;

import java.util.*;

/**
 * 基本块级活跃变量分析器。
 *
 * <p>它负责建立函数内的控制流图，并计算每个基本块入口/出口处仍然活跃的
 * 虚拟寄存器集合。后端寄存器分配会用这些集合把跨分支、跨循环的生命周期
 * 保守地补齐。</p>
 */
public class LivenessAnalyzer {
    
    public static class BlockInfo {
        public BasicBlock block;
        public List<BlockInfo> successors = new ArrayList<>();
        public List<BlockInfo> predecessors = new ArrayList<>();
        
        public Set<String> def = new HashSet<>();
        public Set<String> use = new HashSet<>();
        public Set<String> in = new HashSet<>();
        public Set<String> out = new HashSet<>();
        
        public BlockInfo(BasicBlock block) {
            this.block = block;
        }
    }
    
    private final Map<String, BlockInfo> blockMap = new HashMap<>();
    public final List<BlockInfo> allBlocks = new ArrayList<>();
    
    public LivenessAnalyzer(FuncDef func) {
        // 1. 初始化基本块信息表
        for (BasicBlock b : func.blocks) {
            BlockInfo info = new BlockInfo(b);
            blockMap.put(b.name, info);
            allBlocks.add(info);
        }
        
        // 2. 根据基本块末尾的跳转指令建立 CFG 边
        for (int i = 0; i < func.blocks.size(); i++) {
            BasicBlock b = func.blocks.get(i);
            BlockInfo info = allBlocks.get(i);
            
            if (b.instructions.isEmpty()) {
                if (i + 1 < func.blocks.size()) {
                    addCFGEdge(info, allBlocks.get(i + 1));
                }
                continue;
            }
            
            IrInstr lastInst = b.instructions.get(b.instructions.size() - 1);
            boolean fallsThrough = true;
            
            if (lastInst.op == OpCode.JMP && lastInst.result instanceof LabelValue) {
                String target = ((LabelValue) lastInst.result).name;
                addCFGEdge(info, blockMap.get(target));
                fallsThrough = false;
            } else if ((lastInst.op == OpCode.BEQZ || lastInst.op == OpCode.BNEZ) && lastInst.result instanceof LabelValue) {
                String target = ((LabelValue) lastInst.result).name;
                addCFGEdge(info, blockMap.get(target));
            } else if (lastInst.op == OpCode.RET) {
                fallsThrough = false;
            }
            
            if (fallsThrough && i + 1 < func.blocks.size()) {
                addCFGEdge(info, allBlocks.get(i + 1));
            }
        }
        
        // 3. 计算每个基本块的 use/def 集合
        for (BlockInfo info : allBlocks) {
            for (IrInstr inst : info.block.instructions) {
                for (String r : getUses(inst)) {
                    if (!info.def.contains(r)) {
                        info.use.add(r);
                    }
                }
                String r = getDef(inst);
                if (r != null) {
                    if (!info.use.contains(r)) {
                        info.def.add(r);
                    }
                }
            }
        }
        
        // 4. 后向定点迭代：out[B] = U in[S]，in[B] = use[B] U (out[B] - def[B])
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int i = allBlocks.size() - 1; i >= 0; i--) {
                BlockInfo info = allBlocks.get(i);
                
                Set<String> newOut = new HashSet<>();
                for (BlockInfo succ : info.successors) {
                    newOut.addAll(succ.in);
                }
                info.out = newOut;
                
                Set<String> newIn = new HashSet<>(info.out);
                newIn.removeAll(info.def);
                newIn.addAll(info.use);
                
                if (!newIn.equals(info.in)) {
                    info.in = newIn;
                    changed = true;
                }
            }
        }
    }

    public static Set<String> getUses(IrInstr inst) {
        Set<String> uses = new LinkedHashSet<>();
        addTemp(uses, inst.arg1);
        addTemp(uses, inst.arg2);
        if (inst.op == OpCode.STORE) {
            addTemp(uses, inst.result);
        }
        return uses;
    }

    public static String getDef(IrInstr inst) {
        if (inst.op == OpCode.STORE || inst.op == OpCode.PARAM || inst.op == OpCode.RET) {
            return null;
        }
        if (inst.op == OpCode.JMP || inst.op == OpCode.BEQZ || inst.op == OpCode.BNEZ) {
            return null;
        }
        if (inst.result instanceof TempVar) {
            return inst.result.toPrintString();
        }
        return null;
    }

    private static void addTemp(Set<String> vars, Value value) {
        if (value instanceof TempVar) {
            vars.add(value.toPrintString());
        }
    }
    
    private void addCFGEdge(BlockInfo from, BlockInfo to) {
        if (to == null) return;
        if (!from.successors.contains(to)) {
            from.successors.add(to);
            to.predecessors.add(from);
        }
    }
    
    public BlockInfo getBlockInfo(BasicBlock b) {
        return blockMap.get(b.name);
    }
}
