package com.toyc.compiler.backend;

import com.toyc.compiler.ir.IR;
import java.util.HashMap;
import java.util.Map;

/**
 * 极简版 RISC-V 32位 (RV32IM) 目标代码生成器
 * 采用 Spill-Everything (全部压栈) 的无寄存器分配策略
 */
public class RiscvGenerator {

    private StringBuilder asm;
    
    // 当前函数的状态维护表
    private Map<String, Integer> offsetMap; // 变量名 (如 "t0", "@a") -> 相对于 fp 的栈偏移量
    private int currentOffset; // 当前栈深 (必须是 4 的倍数)
    
    // 追踪当前函数有多少个参数，方便 PARAM 指令使用
    private int currentParamCount = 0;
    
    // 当前处理的函数名 (用于 RET 跳转到 _epilogue)
    private String currentFuncName;

    public RiscvGenerator() {
        this.asm = new StringBuilder();
    }

    /**
     * 主入口：把完整的 IR 程序翻译成 RISC-V 汇编
     */
    public String generate(IR.Program program) {
        // 1. 生成 .data 数据段（处理全局变量和全局常量）
        if (!program.globalVars.isEmpty()) {
            asm.append("    .data\n");
            for (IR.GlobalVar gVar : program.globalVars) {
                asm.append("    .globl ").append(gVar.name).append("\n");
                asm.append(gVar.name).append(":\n");
                // 32 位整数占 4 个字节，用 .word 指令存它的初始值
                asm.append("    .word ").append(gVar.initValue).append("\n\n");
            }
        }

        // 2. 生成 .text 代码段
        asm.append("    .text\n\n");
        
        for (IR.FuncDef func : program.functions) {
            generateFunc(func);
        }
        return asm.toString();
    }

    /**
     * 生成单个函数的汇编 (包含 Prologue 和 Epilogue)
     */
    private void generateFunc(IR.FuncDef func) {
        offsetMap = new HashMap<>();
        // 给 ra 和旧的 s0 预留 8 个字节的顶部空间 (即 -4(s0) 和 -8(s0))
        currentOffset = -8;
        currentFuncName = func.name;
        // 1. 给形参分配栈空间
        for (String param : func.params) {
            currentOffset -= 4;
            offsetMap.put(param, currentOffset);
        }
        // 2. 预扫描所有指令，给所有没见过的新 TempVar 分配栈空间 ---
        for (IR.BasicBlock block : func.blocks) {
            for (IR.IrInstr instr : block.instructions) {
                // 如果结果是个临时变量，且还没登记过，就登记
                if (instr.result instanceof IR.TempVar) {
                    String name = instr.result.toPrintString();
                    if (!offsetMap.containsKey(name)) {
                        currentOffset -= 4;
                        offsetMap.put(name, currentOffset);
                    }
                }
            }
        }

        // 计算最终栈帧总大小 (对齐到 16 字节)
        int stackSize = (-currentOffset + 15) / 16 * 16;
        if (stackSize < 16) stackSize = 16; // 至少保证有足够的空间存 ra 和 fp

        // 生成函数名和 Prologue (函数序言)
        asm.append("    .globl ").append(func.name).append("\n");
        asm.append(func.name).append(":\n");

        // 核心三步走：开辟空间 -> 保存 ra 和旧 fp -> 更新新 fp
        asm.append("    addi sp, sp, -").append(stackSize).append("\n");
        asm.append("    sw ra, ").append(stackSize - 4).append("(sp)\n");
        asm.append("    sw s0, ").append(stackSize - 8).append("(sp)\n");
        asm.append("    addi s0, sp, ").append(stackSize).append("\n\n");
        
        // --- 【新增关键步骤】：将传入的形参寄存器 (a0-a7) 存入到分配好的内存坑位中 ---
        for (int i = 0; i < func.params.size(); i++) {
            if (i < 8) {
                int offset = offsetMap.get(func.params.get(i));
                asm.append("    sw a").append(i).append(", ").append(offset).append("(s0)\n");
            }
        }
        asm.append("\n");

        // 3. 遍历基本块并生成指令
        for (IR.BasicBlock block : func.blocks) {
            generateBlock(block);
        }
        
        // 4. 生成 Epilogue (函数尾声) 的统一点
        // 当我们在指令遇到 RET 时，就直接跳转到这个统一的尾声代码
        asm.append(func.name).append("_epilogue:\n");
        
        // 从栈中恢复 ra 和 s0 寄存器，并退栈 (addi sp, sp, size)，最后加上 ret
        asm.append("    lw ra, ").append(stackSize - 4).append("(sp)\n");
        asm.append("    lw s0, ").append(stackSize - 8).append("(sp)\n");
        asm.append("    addi sp, sp, ").append(stackSize).append("\n");
        asm.append("    ret\n");
        
        asm.append("\n");
    }

    /**
     * 生成单个基本块
     */
    private void generateBlock(IR.BasicBlock block) {
        // 打印基本块的标签
        asm.append(block.name).append(":\n");
        for (IR.IrInstr instr : block.instructions) {
            generateInstr(instr);
        }
    }

    /**
     * 核心！翻译单独的 IR 四元组指令
     */
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
                loadToReg(instr.arg1, "t0");
                loadToReg(instr.arg2, "t1");
                switch (instr.op) {
                    case ADD -> asm.append("    add t2, t0, t1\n");
                    case SUB -> asm.append("    sub t2, t0, t1\n");
                    case MUL -> asm.append("    mul t2, t0, t1\n");
                    case DIV -> asm.append("    div t2, t0, t1\n");
                    case MOD -> asm.append("    rem t2, t0, t1\n");
                    case SEQ -> {
                        asm.append("    sub t2, t0, t1\n");
                        asm.append("    seqz t2, t2\n");
                    }
                    case SNE -> {
                        asm.append("    sub t2, t0, t1\n");
                        asm.append("    snez t2, t2\n");
                    }
                    case SLT -> asm.append("    slt t2, t0, t1\n");
                    case SGT -> asm.append("    slt t2, t1, t0\n");
                    case SLE -> {
                        asm.append("    slt t2, t1, t0\n");
                        asm.append("    xori t2, t2, 1\n");
                    }
                    case SGE -> {
                        asm.append("    slt t2, t0, t1\n");
                        asm.append("    xori t2, t2, 1\n");
                    }
                }
                storeFromReg("t2", instr.result);
                break;

            case NEG:
            case NOT:
                loadToReg(instr.arg1, "t0");
                if (instr.op == IR.OpCode.NEG) {
                    asm.append("    neg t1, t0\n");
                } else {
                    asm.append("    seqz t1, t0\n");
                }
                storeFromReg("t1", instr.result);
                break;

            case ASSIGN:
                loadToReg(instr.arg1, "t0");
                storeFromReg("t0", instr.result);
                break;
                
            case JMP:
                asm.append("    j ").append(instr.result.toPrintString()).append("\n");
                break;
                
            case BEQZ:
                loadToReg(instr.arg1, "t0");
                asm.append("    beq t0, zero, ").append(instr.result.toPrintString()).append("\n");
                break;
                
            case BNEZ:
                loadToReg(instr.arg1, "t0");
                asm.append("    bne t0, zero, ").append(instr.result.toPrintString()).append("\n");
                break;
                
            case PARAM:
                loadToReg(instr.arg1, "t0");
                asm.append("    mv a").append(currentParamCount).append(", t0\n");
                currentParamCount++;
                break;
                
            case CALL:
                String targetFunc = ((IR.NameValue) instr.arg1).name;
                asm.append("    call ").append(targetFunc).append("\n");
                if (instr.result != null) {
                    // C 规范：函数返回值保存在 a0 中
                    storeFromReg("a0", instr.result);
                }
                currentParamCount = 0; // 为下一次调用重置参数计数器
                break;
                
            case RET:
                if (instr.arg1 != null) {
                    // 有返回值时，放入 a0 供调用者使用
                    loadToReg(instr.arg1, "a0");
                }
                // 跳转到统一的 Epilogue 退出函数
                asm.append("    j ").append(currentFuncName).append("_epilogue\n");
                break;

            default:
                break;
        }
    }

    // ==========================================
    // 辅助工具函数：内存搬运工
    // ==========================================

    /**
     * 将 IR.Value 里的值加载到指定的物理寄存器 (如 "t0", "t1")<br>
     * 如果 val 是常数 (ConstValue)，则生成 li 指令<br>
     * 如果 val 是虚拟变量 (TempVar)，则从栈上 lw
     */
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
            // 全局变量：先加载地址，再从地址中取值
            asm.append("    la ").append(physicalReg).append(", ").append(globalName).append("\n");
            asm.append("    lw ").append(physicalReg).append(", 0(").append(physicalReg).append(")\n");
        }
    }

    /**
     * 将物理寄存器 (如 "t0") 的值，存回 target 指定的虚拟变量的内存位置
     */
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
            // 全局变量：借用不常用的临时寄存器 t6 存放全局变量的地址，然后存进去
            asm.append("    la t6, ").append(globalName).append("\n");
            asm.append("    sw ").append(physicalReg).append(", 0(t6)\n");
        }
    }
}
