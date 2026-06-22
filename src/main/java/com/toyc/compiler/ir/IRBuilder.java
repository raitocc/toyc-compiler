package com.toyc.compiler.ir;

import com.toyc.compiler.ast.AST.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class IRBuilder implements Visitor<String> {
    public final List<TAC> instructions = new ArrayList<>();
    private int tempCounter = 0;
    private int labelCounter = 0;
    
    private final Stack<String> breakLabels = new Stack<>();
    private final Stack<String> continueLabels = new Stack<>();

    public List<TAC> build(Node root) {
        root.accept(this);
        return instructions;
    }

    private String newTemp() {
        return "t" + (tempCounter++);
    }

    private String newLabel() {
        return "L" + (labelCounter++);
    }

    private void emit(TAC.Op op, String result, String arg1, String arg2) {
        instructions.add(new TAC(op, result, arg1, arg2));
    }

    private boolean inFunc = false;

    @Override
    public String visit(CompUnit node) {
        for (Node e : node.elements) {
            e.accept(this);
        }
        return null;
    }

    @Override
    public String visit(ConstDecl node) {
        // ConstDecl has already been folded in semantic analysis!
        return null; 
    }

    @Override
    public String visit(VarDecl node) {
        if (!inFunc) {
            String valStr = ((NumberExpr) node.initExpr).value + "";
            emit(TAC.Op.GLOBAL_VAR, node.name, valStr, null);
        } else {
            String val = node.initExpr.accept(this);
            emit(TAC.Op.ASSIGN, node.name, val, null);
        }
        return null;
    }

    @Override
    public String visit(BlockStmt node) {
        for (Stmt s : node.stmts) {
            s.accept(this);
        }
        return null;
    }

    @Override
    public String visit(EmptyStmt node) {
        return null;
    }

    @Override
    public String visit(ExprStmt node) {
        node.expr.accept(this);
        return null;
    }

    @Override
    public String visit(AssignStmt node) {
        String val = node.expr.accept(this);
        emit(TAC.Op.ASSIGN, node.name, val, null);
        return null;
    }

    @Override
    public String visit(IfStmt node) {
        String cond = node.cond.accept(this);
        String elseLabel = newLabel();
        String endLabel = newLabel();
        
        emit(TAC.Op.BEQZ, elseLabel, cond, null);
        node.thenStmt.accept(this);
        emit(TAC.Op.JMP, endLabel, null, null);
        
        emit(TAC.Op.LABEL, elseLabel, null, null);
        if (node.elseStmt != null) {
            node.elseStmt.accept(this);
        }
        emit(TAC.Op.LABEL, endLabel, null, null);
        return null;
    }

    @Override
    public String visit(WhileStmt node) {
        String startLabel = newLabel();
        String endLabel = newLabel();
        
        emit(TAC.Op.LABEL, startLabel, null, null);
        String cond = node.cond.accept(this);
        emit(TAC.Op.BEQZ, endLabel, cond, null);
        
        continueLabels.push(startLabel);
        breakLabels.push(endLabel);
        node.body.accept(this);
        breakLabels.pop();
        continueLabels.pop();
        
        emit(TAC.Op.JMP, startLabel, null, null);
        emit(TAC.Op.LABEL, endLabel, null, null);
        return null;
    }

    @Override
    public String visit(BreakStmt node) {
        if (breakLabels.isEmpty()) throw new RuntimeException("break outside of loop");
        emit(TAC.Op.JMP, breakLabels.peek(), null, null);
        return null;
    }

    @Override
    public String visit(ContinueStmt node) {
        if (continueLabels.isEmpty()) throw new RuntimeException("continue outside of loop");
        emit(TAC.Op.JMP, continueLabels.peek(), null, null);
        return null;
    }

    @Override
    public String visit(ReturnStmt node) {
        String val = null;
        if (node.expr != null) {
            val = node.expr.accept(this);
        }
        emit(TAC.Op.RET, null, val, null);
        return null;
    }

    @Override
    public String visit(FuncDef node) {
        inFunc = true;
        emit(TAC.Op.FUNC_BEGIN, node.name, null, null);
        for (Param p : node.params) {
            p.accept(this);
        }
        node.body.accept(this);
        emit(TAC.Op.FUNC_END, null, null, null);
        inFunc = false;
        return null;
    }

    @Override
    public String visit(Param node) {
        emit(TAC.Op.LABEL, "PARAM_DEF_" + node.name, node.name, null);
        return null;
    }

    @Override
    public String visit(NumberExpr node) {
        String t = newTemp();
        emit(TAC.Op.LI, t, String.valueOf(node.value), null);
        return t;
    }

    @Override
    public String visit(IdExpr node) {
        return node.name; // return variable name
    }

    @Override
    public String visit(CallExpr node) {
        List<String> args = new ArrayList<>();
        for (Expr arg : node.args) {
            args.add(arg.accept(this));
        }
        for (String arg : args) {
            emit(TAC.Op.PARAM, null, arg, null);
        }
        String t = newTemp();
        emit(TAC.Op.CALL, t, node.funcName, null);
        return t;
    }

    @Override
    public String visit(UnaryExpr node) {
        String val = node.expr.accept(this);
        String t = newTemp();
        if (node.op.equals("+")) {
            return val;
        } else if (node.op.equals("-")) {
            String zero = newTemp();
            emit(TAC.Op.LI, zero, "0", null);
            emit(TAC.Op.SUB, t, zero, val);
        } else if (node.op.equals("!")) {
            emit(TAC.Op.SEQ, t, val, "0");
        }
        return t;
    }

    @Override
    public String visit(BinaryExpr node) {
        if (node.op.equals("&&")) {
            String left = node.left.accept(this);
            String endLabel = newLabel();
            String falseLabel = newLabel();
            String res = newTemp();
            
            emit(TAC.Op.BEQZ, falseLabel, left, null);
            String right = node.right.accept(this);
            emit(TAC.Op.BEQZ, falseLabel, right, null);
            
            emit(TAC.Op.LI, res, "1", null);
            emit(TAC.Op.JMP, endLabel, null, null);
            
            emit(TAC.Op.LABEL, falseLabel, null, null);
            emit(TAC.Op.LI, res, "0", null);
            
            emit(TAC.Op.LABEL, endLabel, null, null);
            return res;
        } else if (node.op.equals("||")) {
            String left = node.left.accept(this);
            String endLabel = newLabel();
            String trueLabel = newLabel();
            String res = newTemp();
            
            emit(TAC.Op.BNEZ, trueLabel, left, null);
            String right = node.right.accept(this);
            emit(TAC.Op.BNEZ, trueLabel, right, null);
            
            emit(TAC.Op.LI, res, "0", null);
            emit(TAC.Op.JMP, endLabel, null, null);
            
            emit(TAC.Op.LABEL, trueLabel, null, null);
            emit(TAC.Op.LI, res, "1", null);
            
            emit(TAC.Op.LABEL, endLabel, null, null);
            return res;
        }

        String left = node.left.accept(this);
        String right = node.right.accept(this);
        String t = newTemp();
        TAC.Op tacOp;
        switch (node.op) {
            case "+": tacOp = TAC.Op.ADD; break;
            case "-": tacOp = TAC.Op.SUB; break;
            case "*": tacOp = TAC.Op.MUL; break;
            case "/": tacOp = TAC.Op.DIV; break;
            case "%": tacOp = TAC.Op.MOD; break;
            case "==": tacOp = TAC.Op.SEQ; break;
            case "!=": tacOp = TAC.Op.SNE; break;
            case "<": tacOp = TAC.Op.SLT; break;
            case "<=": tacOp = TAC.Op.SLE; break;
            case ">": tacOp = TAC.Op.SGT; break;
            case ">=": tacOp = TAC.Op.SGE; break;
            default: throw new RuntimeException("Unknown op: " + node.op);
        }
        emit(tacOp, t, left, right);
        return t;
    }
}
