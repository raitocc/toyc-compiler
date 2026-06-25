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
        currentOffset = 0;
        
        // 1. 预扫描该函数：给形参和所有遇见的局部虚拟变量分配栈空间
        // TODO: 写一个循环遍历 func 里面的所有指令，把用到的 t0, t1 甚至新建立的局部变量，在 offsetMap 里分配好位子
        
        // 计算最终栈帧总大小 (对齐到 16 字节)
        int stackSize = (-currentOffset + 15) / 16 * 16;
        if (stackSize < 16) stackSize = 16; // 至少保证有足够的空间存 ra 和 fp

        // 2. 生成函数名和 Prologue (函数序言)
        asm.append("    .globl ").append(func.name).append("\n");
        asm.append(func.name).append(":\n");
        
        // TODO: 在这里开辟栈空间 (addi sp, sp, -size)，并保存 ra 和 fp 寄存器
        
        // 3. 遍历基本块并生成指令
        for (IR.BasicBlock block : func.blocks) {
            generateBlock(block);
        }
        
        // 4. 生成 Epilogue (函数尾声) 的统一点
        // 当我们在指令遇到 RET 时，就直接跳转到这个统一的尾声代码
        asm.append(func.name).append("_epilogue:\n");
        
        // TODO: 在这里从栈中恢复 ra 和 fp 寄存器，并退栈 (addi sp, sp, size)，最后加上 ret
        
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
                // TODO: 实现算术运算 (loadToReg -> 算术指令 -> storeFromReg)
                break;
                
            case ASSIGN:
                // TODO: 赋值 (loadToReg -> storeFromReg)
                break;
                
            case JMP:
            case BEQZ:
            case BNEZ:
                // TODO: 跳转指令 (beq, bne, j)
                break;
                
            case PARAM:
                // TODO: 函数传参 (存到 a0-a7 寄存器里)
                break;
                
            case CALL:
                // TODO: 函数调用 (call，并取回 a0 作为结果)
                break;
                
            case RET:
                // TODO: 函数返回 (把返回值放到 a0，然后 j 函数的 epilogue)
                break;

            default:
                // 占位
                break;
        }
    }

    // ==========================================
    // 辅助工具函数：内存搬运工
    // ==========================================

    /**
     * 将 IR.Value 里的值加载到指定的物理寄存器 (如 "t0", "t1")
     * 如果 val 是常数 (ConstValue)，则生成 li 指令
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
            asm.append("    lw ").append(physicalReg).append(", ").append(offset).append("(fp)\n");
        } else if (val instanceof IR.NameValue) {
            // TODO: 全局变量的加载处理 (la -> lw)
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
            asm.append("    sw ").append(physicalReg).append(", ").append(offset).append("(fp)\n");
        } else if (target instanceof IR.NameValue) {
            // TODO: 全局变量的存回处理 (la -> sw)
        }
    }
}
