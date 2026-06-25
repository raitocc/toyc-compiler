package com.toyc.compiler.backend;

import com.toyc.compiler.ir.IR;
import java.util.*;

/**
 * 极简版 RISC-V 32位 (RV32IM) 目标代码生成器
 * 升级版：贪心物理寄存器分配 (Greedy Register Allocation)
 */
public class RiscvGenerator {

    private StringBuilder asm;
    
    // 当前函数的状态维护表
    private Map<String, Integer> offsetMap; // 变量名 -> 相对于 fp 的栈偏移量
    private Map<String, String> regMap; // 变量名 -> 物理寄存器名 (如 "s1")
    private List<String> usedSRegs; // 当前函数使用的 s 寄存器列表 (用于 Prologue/Epilogue 保护)
    
    private int currentOffset; // 当前栈深 (必须是 4 的倍数)
    private int currentParamCount = 0;
    private String currentFuncName;

    private static final String[] S_REGS = {
        "s1", "s2", "s3", "s4", "s5", "s6", "s7", "s8", "s9", "s10", "s11"
    };

    public RiscvGenerator() {
        this.asm = new StringBuilder();
    }

    public String generate(IR.Program program) {
        if (!program.globalVars.isEmpty()) {
            asm.append("    .data\n");
            for (IR.GlobalVar gVar : program.globalVars) {
                asm.append("    .globl ").append(gVar.name).append("\n");
                asm.append(gVar.name).append(":\n");
                asm.append("    .word ").append(gVar.initValue).append("\n\n");
            }
        }

        asm.append("    .text\n\n");
        for (IR.FuncDef func : program.functions) {
            generateFunc(func);
        }
        return asm.toString();
    }

    private void generateFunc(IR.FuncDef func) {
        currentFuncName = func.name;
        
        // 1. 频率统计 (Frequency Profiling)
        Map<String, Integer> freqMap = new HashMap<>();
        for (String param : func.params) freqMap.put(param, freqMap.getOrDefault(param, 0) + 1);
        for (IR.BasicBlock block : func.blocks) {
            for (IR.IrInstr instr : block.instructions) {
                if (instr.arg1 instanceof IR.TempVar) {
                    String n = instr.arg1.toPrintString(); freqMap.put(n, freqMap.getOrDefault(n, 0) + 1);
                }
                if (instr.arg2 instanceof IR.TempVar) {
                    String n = instr.arg2.toPrintString(); freqMap.put(n, freqMap.getOrDefault(n, 0) + 1);
                }
                if (instr.result instanceof IR.TempVar) {
                    String n = instr.result.toPrintString(); freqMap.put(n, freqMap.getOrDefault(n, 0) + 1);
                }
            }
        }

        // 2. 贪心寄存器分配 (Greedy Register Allocation)
        List<Map.Entry<String, Integer>> sortedFreq = new ArrayList<>(freqMap.entrySet());
        sortedFreq.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        
        regMap = new HashMap<>();
        usedSRegs = new ArrayList<>();
        
        for (int i = 0; i < sortedFreq.size() && i < S_REGS.length; i++) {
            String varName = sortedFreq.get(i).getKey();
            regMap.put(varName, S_REGS[i]);
            usedSRegs.add(S_REGS[i]);
        }

        // 3. 计算 Prologue 保护头大小，并降级溢出的变量
        offsetMap = new HashMap<>();
        int headerSize = 4 * (usedSRegs.size() + 2); // ra, s0, 和 usedSRegs
        currentOffset = -headerSize;

        for (int i = 0; i < func.params.size(); i++) {
            String param = func.params.get(i);
            if (!regMap.containsKey(param)) {
                currentOffset -= 4;
                offsetMap.put(param, currentOffset);
            }
        }

        for (String varName : freqMap.keySet()) {
            if (!regMap.containsKey(varName) && !offsetMap.containsKey(varName)) {
                currentOffset -= 4;
                offsetMap.put(varName, currentOffset);
            }
        }

        // 保证 16 字节对齐
        int stackSize = (-currentOffset + 15) / 16 * 16;
        if (stackSize < 16) stackSize = 16;

        // 4. 生成函数名和 Prologue
        asm.append("    .globl ").append(func.name).append("\n");
        asm.append(func.name).append(":\n");
        asm.append("    addi sp, sp, -").append(stackSize).append("\n");
        asm.append("    sw ra, ").append(stackSize - 4).append("(sp)\n");
        asm.append("    sw s0, ").append(stackSize - 8).append("(sp)\n");
        
        for (int i = 0; i < usedSRegs.size(); i++) {
            asm.append("    sw ").append(usedSRegs.get(i)).append(", ").append(stackSize - 12 - i * 4).append("(sp)\n");
        }
        asm.append("    addi s0, sp, ").append(stackSize).append("\n\n");

        // 5. 初始化形参：将传入的 a0-a7 转存到目标地 (寄存器或栈)
        for (int i = 0; i < func.params.size() && i < 8; i++) {
            String param = func.params.get(i);
            if (regMap.containsKey(param)) {
                asm.append("    mv ").append(regMap.get(param)).append(", a").append(i).append("\n");
            } else {
                asm.append("    sw a").append(i).append(", ").append(offsetMap.get(param)).append("(s0)\n");
            }
        }
        asm.append("\n");

        // 6. 生成基础块
        for (IR.BasicBlock block : func.blocks) {
            generateBlock(block);
        }
        
        // 7. 生成 Epilogue
        asm.append(func.name).append("_epilogue:\n");
        for (int i = 0; i < usedSRegs.size(); i++) {
            asm.append("    lw ").append(usedSRegs.get(i)).append(", ").append(stackSize - 12 - i * 4).append("(sp)\n");
        }
        asm.append("    lw s0, ").append(stackSize - 8).append("(sp)\n");
        asm.append("    lw ra, ").append(stackSize - 4).append("(sp)\n");
        asm.append("    addi sp, sp, ").append(stackSize).append("\n");
        asm.append("    ret\n\n");
    }

    private void generateBlock(IR.BasicBlock block) {
        asm.append(block.name).append(":\n");
        for (IR.IrInstr instr : block.instructions) {
            generateInstr(instr);
        }
    }

    private void generateInstr(IR.IrInstr instr) {
        switch (instr.op) {
            case ADD:
            case SUB:
            case MUL:
            case DIV:
            case MOD:
            case SEQ:
            case SNE:
            case SLT:
            case SGT:
            case SLE:
            case SGE:
                String r1 = resolveReg(instr.arg1, "t0");
                String r2 = resolveReg(instr.arg2, "t1");
                String rTarget = getTargetReg(instr.result, "t2");

                switch (instr.op) {
                    case ADD -> asm.append("    add ").append(rTarget).append(", ").append(r1).append(", ").append(r2).append("\n");
                    case SUB -> asm.append("    sub ").append(rTarget).append(", ").append(r1).append(", ").append(r2).append("\n");
                    case MUL -> asm.append("    mul ").append(rTarget).append(", ").append(r1).append(", ").append(r2).append("\n");
                    case DIV -> asm.append("    div ").append(rTarget).append(", ").append(r1).append(", ").append(r2).append("\n");
                    case MOD -> asm.append("    rem ").append(rTarget).append(", ").append(r1).append(", ").append(r2).append("\n");
                    case SEQ -> {
                        asm.append("    sub ").append(rTarget).append(", ").append(r1).append(", ").append(r2).append("\n");
                        asm.append("    seqz ").append(rTarget).append(", ").append(rTarget).append("\n");
                    }
                    case SNE -> {
                        asm.append("    sub ").append(rTarget).append(", ").append(r1).append(", ").append(r2).append("\n");
                        asm.append("    snez ").append(rTarget).append(", ").append(rTarget).append("\n");
                    }
                    case SLT -> asm.append("    slt ").append(rTarget).append(", ").append(r1).append(", ").append(r2).append("\n");
                    case SGT -> asm.append("    slt ").append(rTarget).append(", ").append(r2).append(", ").append(r1).append("\n");
                    case SLE -> {
                        asm.append("    slt ").append(rTarget).append(", ").append(r2).append(", ").append(r1).append("\n");
                        asm.append("    xori ").append(rTarget).append(", ").append(rTarget).append(", 1\n");
                    }
                    case SGE -> {
                        asm.append("    slt ").append(rTarget).append(", ").append(r1).append(", ").append(r2).append("\n");
                        asm.append("    xori ").append(rTarget).append(", ").append(rTarget).append(", 1\n");
                    }
                }
                commitTargetReg(rTarget, instr.result);
                break;

            case NEG:
            case NOT:
                r1 = resolveReg(instr.arg1, "t0");
                rTarget = getTargetReg(instr.result, "t1");
                if (instr.op == IR.OpCode.NEG) {
                    asm.append("    neg ").append(rTarget).append(", ").append(r1).append("\n");
                } else {
                    asm.append("    seqz ").append(rTarget).append(", ").append(r1).append("\n");
                }
                commitTargetReg(rTarget, instr.result);
                break;

            case ASSIGN:
                r1 = resolveReg(instr.arg1, "t0");
                rTarget = getTargetReg(instr.result, "t1");
                
                // 如果目标被溢出到栈，或者是全局变量，直接写内存，省去 mv
                if (!(instr.result instanceof IR.TempVar && regMap.containsKey(instr.result.toPrintString()))) {
                    commitTargetReg(r1, instr.result);
                } else {
                    if (!r1.equals(rTarget)) {
                        asm.append("    mv ").append(rTarget).append(", ").append(r1).append("\n");
                    }
                    commitTargetReg(rTarget, instr.result);
                }
                break;
                
            case JMP:
                asm.append("    j ").append(instr.result.toPrintString()).append("\n");
                break;
                
            case BEQZ:
                r1 = resolveReg(instr.arg1, "t0");
                asm.append("    beq ").append(r1).append(", zero, ").append(instr.result.toPrintString()).append("\n");
                break;
                
            case BNEZ:
                r1 = resolveReg(instr.arg1, "t0");
                asm.append("    bne ").append(r1).append(", zero, ").append(instr.result.toPrintString()).append("\n");
                break;
                
            case PARAM:
                String rParam = resolveReg(instr.arg1, "t0");
                asm.append("    mv a").append(currentParamCount).append(", ").append(rParam).append("\n");
                currentParamCount++;
                break;
                
            case CALL:
                String targetFunc = ((IR.NameValue) instr.arg1).name;
                asm.append("    call ").append(targetFunc).append("\n");
                if (instr.result != null) {
                    rTarget = getTargetReg(instr.result, "a0");
                    if (!rTarget.equals("a0")) {
                        asm.append("    mv ").append(rTarget).append(", a0\n");
                    } else {
                        commitTargetReg("a0", instr.result);
                    }
                }
                currentParamCount = 0;
                break;
                
            case RET:
                if (instr.arg1 != null) {
                    String rRet = resolveReg(instr.arg1, "a0");
                    if (!rRet.equals("a0")) {
                        asm.append("    mv a0, ").append(rRet).append("\n");
                    }
                }
                asm.append("    j ").append(currentFuncName).append("_epilogue\n");
                break;

            default:
                break;
        }
    }

    // ==========================================
    // 内存与寄存器抽象层
    // ==========================================

    private String resolveReg(IR.Value val, String fallbackReg) {
        if (val instanceof IR.TempVar) {
            String name = val.toPrintString();
            if (regMap.containsKey(name)) {
                return regMap.get(name);
            }
        }
        // 不在寄存器中，加载到临时寄存器中返回
        loadToReg(val, fallbackReg);
        return fallbackReg;
    }

    private String getTargetReg(IR.Value val, String fallbackReg) {
        if (val instanceof IR.TempVar) {
            String name = val.toPrintString();
            if (regMap.containsKey(name)) {
                return regMap.get(name);
            }
        }
        return fallbackReg;
    }

    private void commitTargetReg(String reg, IR.Value val) {
        if (val instanceof IR.TempVar) {
            String name = val.toPrintString();
            if (regMap.containsKey(name)) {
                return; // 如果在寄存器中，汇编指令已直接写入，无需额外存储
            }
        }
        // 如果溢出了，或者是全局变量，则老老实实写内存
        storeFromReg(reg, val);
    }

    private void loadToReg(IR.Value val, String physicalReg) {
        if (val instanceof IR.ConstValue) {
            int num = ((IR.ConstValue) val).val;
            asm.append("    li ").append(physicalReg).append(", ").append(num).append("\n");
        } else if (val instanceof IR.TempVar) {
            String tempName = val.toPrintString();
            if (!offsetMap.containsKey(tempName)) {
                throw new RuntimeException("TempVar " + tempName + " 未分配栈空间");
            }
            int offset = offsetMap.get(tempName);
            asm.append("    lw ").append(physicalReg).append(", ").append(offset).append("(s0)\n");
        } else if (val instanceof IR.NameValue) {
            String globalName = ((IR.NameValue) val).name;
            asm.append("    la ").append(physicalReg).append(", ").append(globalName).append("\n");
            asm.append("    lw ").append(physicalReg).append(", 0(").append(physicalReg).append(")\n");
        }
    }

    private void storeFromReg(String physicalReg, IR.Value target) {
        if (target instanceof IR.TempVar) {
            String tempName = target.toPrintString();
            if (!offsetMap.containsKey(tempName)) {
                throw new RuntimeException("TempVar " + tempName + " 未分配栈空间");
            }
            int offset = offsetMap.get(tempName);
            asm.append("    sw ").append(physicalReg).append(", ").append(offset).append("(s0)\n");
        } else if (target instanceof IR.NameValue) {
            String globalName = ((IR.NameValue) target).name;
            asm.append("    la t6, ").append(globalName).append("\n");
            asm.append("    sw ").append(physicalReg).append(", 0(t6)\n");
        }
    }
}
