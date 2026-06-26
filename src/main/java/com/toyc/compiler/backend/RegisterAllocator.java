package com.toyc.compiler.backend;

import com.toyc.compiler.backend.LivenessAnalyzer.BlockInfo;
import com.toyc.compiler.ir.IR.*;

import java.util.*;

/**
 * 基于线性扫描的物理寄存器分配器。
 *
 * <p>这里不追求图着色那种极限分配质量，而是把正确性放在第一位：
 * 先按 IR 指令顺序给每个临时变量建立一个保守生命周期区间，再把区间
 * 扫描进 {@code s1-s11}。区间重叠或寄存器不够时，变量会被溢出到栈上。</p>
 */
public class RegisterAllocator {

    private final FuncDef func;
    private final LivenessAnalyzer liveness;

    // 分配结果：虚拟寄存器名 -> RISC-V 物理寄存器名
    public final Map<String, String> regMap = new LinkedHashMap<>();
    // 未能放入寄存器的虚拟寄存器，后端会为它们分配栈槽
    public final Set<String> spilledVars = new LinkedHashSet<>();

    private static final String[] PHYSICAL_REGS = {
        "s1", "s2", "s3", "s4", "s5", "s6", "s7", "s8", "s9", "s10", "s11"
    };

    public RegisterAllocator(FuncDef func) {
        this.func = func;
        this.liveness = new LivenessAnalyzer(func);
    }

    public void allocate() {
        List<LiveInterval> intervals = buildIntervals();
        List<LiveInterval> active = new ArrayList<>();
        Deque<String> freeRegs = new ArrayDeque<>(Arrays.asList(PHYSICAL_REGS));

        for (LiveInterval current : intervals) {
            expireOldIntervals(active, freeRegs, current.start);

            if (!freeRegs.isEmpty()) {
                assignRegister(current, freeRegs.removeFirst(), active);
                continue;
            }

            spillOneInterval(current, active);
        }
    }

    private List<LiveInterval> buildIntervals() {
        Map<String, LiveInterval> intervalMap = new LinkedHashMap<>();
        Map<BasicBlock, Integer> blockStart = new HashMap<>();
        Map<BasicBlock, Integer> blockEnd = new HashMap<>();

        int pos = 0;
        for (BasicBlock block : func.blocks) {
            blockStart.put(block, pos);
            for (IrInstr instr : block.instructions) {
                touchInstructionVars(intervalMap, instr, pos);
                pos++;
            }
            blockEnd.put(block, Math.max(blockStart.get(block), pos - 1));
        }

        // 活跃变量分析会把跨块、循环回边上的生命周期补长，避免线性顺序低估冲突。
        for (BlockInfo info : liveness.allBlocks) {
            int start = blockStart.getOrDefault(info.block, 0);
            int end = blockEnd.getOrDefault(info.block, start);
            for (String var : info.in) {
                intervalMap.computeIfAbsent(var, LiveInterval::new).touch(start);
            }
            for (String var : info.out) {
                intervalMap.computeIfAbsent(var, LiveInterval::new).touch(end);
            }
        }

        List<LiveInterval> intervals = new ArrayList<>(intervalMap.values());
        intervals.sort(Comparator
            .comparingInt((LiveInterval interval) -> interval.start)
            .thenComparingInt(interval -> interval.end)
            .thenComparing(interval -> interval.name));
        return intervals;
    }

    private void touchInstructionVars(Map<String, LiveInterval> intervals, IrInstr instr, int pos) {
        for (String use : LivenessAnalyzer.getUses(instr)) {
            LiveInterval interval = intervals.computeIfAbsent(use, LiveInterval::new);
            interval.touch(pos);
            interval.weight++;
        }

        String def = LivenessAnalyzer.getDef(instr);
        if (def != null) {
            LiveInterval interval = intervals.computeIfAbsent(def, LiveInterval::new);
            interval.touch(pos);
            interval.weight++;
        }
    }

    private void expireOldIntervals(List<LiveInterval> active, Deque<String> freeRegs, int currentStart) {
        Iterator<LiveInterval> iterator = active.iterator();
        while (iterator.hasNext()) {
            LiveInterval interval = iterator.next();
            if (interval.end < currentStart) {
                insertFreeReg(freeRegs, interval.physicalReg);
                iterator.remove();
            }
        }
        active.sort(Comparator.comparingInt(interval -> interval.end));
    }

    private void spillOneInterval(LiveInterval current, List<LiveInterval> active) {
        LiveInterval victim = active.stream()
            .max(Comparator
                .comparingInt((LiveInterval interval) -> interval.end)
                .thenComparingInt(interval -> -interval.weight)
                .thenComparing(interval -> interval.name))
            .orElse(null);

        if (victim != null && victim.end > current.end) {
            regMap.remove(victim.name);
            spilledVars.add(victim.name);

            current.physicalReg = victim.physicalReg;
            regMap.put(current.name, current.physicalReg);
            active.remove(victim);
            active.add(current);
            active.sort(Comparator.comparingInt(interval -> interval.end));
        } else {
            spilledVars.add(current.name);
        }
    }

    private void assignRegister(LiveInterval interval, String physicalReg, List<LiveInterval> active) {
        interval.physicalReg = physicalReg;
        regMap.put(interval.name, physicalReg);
        active.add(interval);
        active.sort(Comparator.comparingInt(item -> item.end));
    }

    private void insertFreeReg(Deque<String> freeRegs, String reg) {
        List<String> regs = new ArrayList<>(freeRegs);
        regs.add(reg);
        regs.sort(Comparator.comparingInt(this::physicalRegOrder));
        freeRegs.clear();
        freeRegs.addAll(regs);
    }

    private int physicalRegOrder(String reg) {
        for (int i = 0; i < PHYSICAL_REGS.length; i++) {
            if (PHYSICAL_REGS[i].equals(reg)) {
                return i;
            }
        }
        return PHYSICAL_REGS.length;
    }

    private static class LiveInterval {
        final String name;
        int start = Integer.MAX_VALUE;
        int end = Integer.MIN_VALUE;
        int weight = 0;
        String physicalReg;

        LiveInterval(String name) {
            this.name = name;
        }

        void touch(int pos) {
            start = Math.min(start, pos);
            end = Math.max(end, pos);
        }
    }
}
