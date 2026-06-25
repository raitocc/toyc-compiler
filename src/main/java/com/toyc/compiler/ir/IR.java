package com.toyc.compiler.ir;

import java.util.ArrayList;
import java.util.List;

/**
 * 中间代码 (Intermediate Representation) 数据结构定义<br>
 * 我们采用【基于基本块 (Basic Block)】的四元组形式，这是主流现代编译器做优化的标配结构。
 */
public class IR {

    // ==========================================
    // 1. 操作数 (Value) 体系
    // 为什么需要这个？因为 IR 指令里的操作数不仅可能是临时变量，
    // 还可能是常数、全局变量名，或者是用于跳转的目标标签。
    // 将它们抽象为统一的 Value 父类，极大简化了指令的定义和管理。
    // ==========================================
    public static abstract class Value {
        public abstract String toPrintString();
    }

    /** 临时变量 (Temporary Variable)，代表寄存器或者栈上分配的一个槽位。比如 t0, t1 */
    public static class TempVar extends Value {
        public final int id;
        public TempVar(int id) { this.id = id; }
        @Override public String toPrintString() { return "t" + id; }
    }

    /** 常数 (Constant Value)，代表代码里硬编码的数字字面量或编译期算出来的常量。比如 5, -10 */
    public static class ConstValue extends Value {
        public final int val;
        public ConstValue(int val) { this.val = val; }
        @Override public String toPrintString() { return String.valueOf(val); }
    }

    /** 具名符号 (Name Value)，通常用于指代全局变量或函数调用时的函数名。比如 @count, @foo */
    public static class NameValue extends Value {
        public final String name;
        public NameValue(String name) { this.name = name; }
        @Override public String toPrintString() { return "@" + name; }
    }

    /** 标签引用 (Label Value)，专门用于 JMP/BEQZ 等跳转指令，它的名字对应某个 BasicBlock 的名称 */
    public static class LabelValue extends Value {
        public final String name;
        public LabelValue(String name) { this.name = name; }
        @Override public String toPrintString() { return name; }
    }

    // ==========================================
    // 2. 操作码 (OpCode)
    // ==========================================
    public enum OpCode {
        ADD, SUB, MUL, DIV, MOD,    // 算术运算
        NEG, NOT,                   // 一元运算
        SEQ, SNE, SLT, SGT, SLE, SGE, // 关系运算 (Set if Equal, Set if Not Equal...)
        AND, OR,                    // 逻辑运算
        ASSIGN,                     // 简单赋值 t1 = t2
        LOAD, STORE,                // 内存读写
        JMP, BEQZ, BNEZ,            // 跳转控制：无条件跳(JMP)、等于0跳(BEQZ)、不等于0跳(BNEZ)
        CALL, RET,                  // 函数调用与返回
        PARAM                       // 准备函数调用的参数
    }

    // ==========================================
    // 3. 四元组指令 (IrInstr)
    // 结构：[操作码, 参数1, 参数2, 目标存放地/目标标签]
    // ==========================================
    public static class IrInstr {
        public OpCode op;
        public Value arg1;  // 源操作数1 (可能为 null)
        public Value arg2;  // 源操作数2 (可能为 null)
        public Value result; // 目标 (可能为 null，如果是跳转指令，这里通常放 LabelValue)

        public IrInstr(OpCode op, Value arg1, Value arg2, Value result) {
            this.op = op;
            this.arg1 = arg1;
            this.arg2 = arg2;
            this.result = result;
        }

        public IrInstr(OpCode op, Value arg1, Value result) {
            this(op, arg1, null, result);
        }

        public IrInstr(OpCode op, Value result) {
            this(op, null, null, result);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            
            // 为了打印好看：如果是算术或者赋值指令，打印为 t1 = ADD t2, t3
            if (result != null && !(result instanceof LabelValue) && op != OpCode.STORE) {
                sb.append(result.toPrintString()).append(" = ");
            }
            sb.append(op);
            if (arg1 != null) {
                sb.append(" ").append(arg1.toPrintString());
            }
            if (arg2 != null) {
                sb.append(", ").append(arg2.toPrintString());
            }
            
            // 如果是跳转类指令，打印目标标签
            if (result instanceof LabelValue) {
                sb.append(" -> ").append(result.toPrintString());
            }
            return sb.toString();
        }
    }

    // ==========================================
    // 4. 控制流与程序结构层 (BasicBlock & FuncDef)
    // ==========================================

    /**
     * 基本块 (Basic Block)：一段连续的、"一进一出"的指令序列。
     * <li>只有从第一条指令才能进入该块，不能在块中间跳入。</li>
     * <li>一旦进入，指令一定是从头顺序执行到尾，中途没有任何跳转。</li>
     * <li>任何分支（如 if/while 生成的条件或无条件跳转指令）只能出现在最后一条指令。</li>
     * 因为块内绝对是顺次执行的，所以做“死代码消除”、“常量折叠”、“局部公共子表达式提取”等优化会极其方便。
     */
    public static class BasicBlock {
        public final String name; // 块的名字，通常兼作汇编里的 Label 名字，如 "L_1", "L_2"
        public List<IrInstr> instructions = new ArrayList<>();
        
        public BasicBlock(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name + ":";
        }
    }

    /**
     * 函数定义层
     * 一个函数不再是一个扁平的 {@code List<IrInstr>}，而是由多个互相之间存在跳转关系的 BasicBlock 组成。
     * 这构成了一个函数的 控制流图 (CFG - Control Flow Graph)。
     */
    public static class FuncDef {
        public String name;
        public List<String> params = new ArrayList<>();
        // 函数体由一系列的基本块组成
        public List<BasicBlock> blocks = new ArrayList<>();
        
        public FuncDef(String name) {
            this.name = name;
        }
    }

    public static class GlobalVar {
        public String name;
        public int initValue; // 全局变量的初始值必须是编译期常量
        
        public GlobalVar(String name, int initValue) {
            this.name = name;
            this.initValue = initValue;
        }
    }

    /**
     * 整个程序
     */
    public static class Program {
        public List<FuncDef> functions = new ArrayList<>();
        public List<GlobalVar> globalVars = new ArrayList<>();
    }
}
