package com.toyc.compiler.ir;

public class TAC {
    public enum Op {
        ADD, SUB, MUL, DIV, MOD,
        SEQ, SNE, SLT, SLE, SGT, SGE, // Set if Equal, Not Equal, Less Than... (returns 1 or 0)
        ASSIGN,   // result = arg1
        LI,       // result = immediate(arg1)
        JMP,      // jump to label(result)
        BEQZ,     // if arg1 == 0 jump to label(result)
        BNEZ,     // if arg1 != 0 jump to label(result)
        CALL,     // result = call arg1(func_name)
        PARAM,    // param arg1 (pass to function)
        RET,      // return arg1
        LABEL,    // define label(result)
        GLOBAL_VAR, // define global variable result
        FUNC_BEGIN, // define function result
        FUNC_END    // end function
    }

    public Op op;
    public String result;
    public String arg1;
    public String arg2;

    public TAC(Op op, String result, String arg1, String arg2) {
        this.op = op;
        this.result = result;
        this.arg1 = arg1;
        this.arg2 = arg2;
    }

    @Override
    public String toString() {
        switch (op) {
            case ADD: return result + " = " + arg1 + " + " + arg2;
            case SUB: return result + " = " + arg1 + " - " + arg2;
            case MUL: return result + " = " + arg1 + " * " + arg2;
            case DIV: return result + " = " + arg1 + " / " + arg2;
            case MOD: return result + " = " + arg1 + " % " + arg2;
            case SEQ: return result + " = " + arg1 + " == " + arg2;
            case SNE: return result + " = " + arg1 + " != " + arg2;
            case SLT: return result + " = " + arg1 + " < " + arg2;
            case SLE: return result + " = " + arg1 + " <= " + arg2;
            case SGT: return result + " = " + arg1 + " > " + arg2;
            case SGE: return result + " = " + arg1 + " >= " + arg2;
            case ASSIGN: return result + " = " + arg1;
            case LI: return result + " = " + arg1;
            case JMP: return "goto " + result;
            case BEQZ: return "if " + arg1 + " == 0 goto " + result;
            case BNEZ: return "if " + arg1 + " != 0 goto " + result;
            case CALL: return (result != null ? result + " = " : "") + "call " + arg1;
            case PARAM: return "param " + arg1;
            case RET: return "return " + (arg1 != null ? arg1 : "");
            case LABEL: return result + ":";
            case GLOBAL_VAR: return "global " + result;
            case FUNC_BEGIN: return "func " + result + " begin";
            case FUNC_END: return "func end";
            default: return op.name();
        }
    }
}
