package com.toyc.compiler.ir;

import com.toyc.compiler.ast.AST;
import com.toyc.compiler.ast.AST.*;

/**
 * 遍历 AST 树生成 基于基本块的 IR 代码
 */
public class IrGenerator implements AST.Visitor<IR.Value> {

    public IR.Program program = new IR.Program();
    private IR.FuncDef currentFunc = null;

    // 需要维护一个“当前正在写指令”的基本块
    private IR.BasicBlock currentBlock = null;

    // 虚拟寄存器分配器
    private int nextTempId = 0;
    // 基本块/标签分配器
    private int nextBlockId = 0;

    // 申请一个新的临时变量
    public IR.TempVar newTemp() {
        return new IR.TempVar(nextTempId++);
    }

    // 申请并创建一个新的基本块（同时也相当于分配了一个标签）
    public IR.BasicBlock newBlock(String prefix) {
        return new IR.BasicBlock(prefix + "_" + (nextBlockId++));
    }

    // 将基本块加入当前函数，并立刻切换为“当前写入块”
    public void appendBlock(IR.BasicBlock block) {
        if (currentFunc != null) {
            currentFunc.blocks.add(block);
        }
        currentBlock = block; // 接下来所有的 addInstr 都会写到这个块里
    }

    // 往当前基本块追加一条指令
    public void addInstr(IR.IrInstr instr) {
        if (currentBlock != null) {
            currentBlock.instructions.add(instr);
        }
    }

    // ==========================================
    // 顶层结构
    // ==========================================
    @Override
    public IR.Value visit(CompUnit node) {
        for (Node element : node.elements) {
            element.accept(this);
        }
        return null;
    }

    @Override
    public IR.Value visit(ConstDecl node) {
        return null;
    }

    @Override
    public IR.Value visit(VarDecl node) {
        return null;
    }

    @Override
    public IR.Value visit(BlockStmt node) {
        return null;
    }

    @Override
    public IR.Value visit(EmptyStmt node) {
        return null;
    }

    @Override
    public IR.Value visit(ExprStmt node) {
        return null;
    }

    @Override
    public IR.Value visit(AssignStmt node) {
        return null;
    }

    @Override
    public IR.Value visit(IfStmt node) {
        return null;
    }

    @Override
    public IR.Value visit(WhileStmt node) {
        return null;
    }

    @Override
    public IR.Value visit(BreakStmt node) {
        return null;
    }

    @Override
    public IR.Value visit(ContinueStmt node) {
        return null;
    }

    @Override
    public IR.Value visit(ReturnStmt node) {
        return null;
    }

    @Override
    public IR.Value visit(FuncDef node) {
        currentFunc = new IR.FuncDef(node.name);
        program.functions.add(currentFunc);

        // 为函数创建一个入口基本块，并将其设置为当前块
        IR.BasicBlock entryBlock = newBlock("entry");
        appendBlock(entryBlock);

        // TODO: 处理形参映射（将 AST 的 Param 绑定到 IR 的局部临时变量）

        // 遍历函数体，内部的 if/while 会自己去申请新的 BasicBlock 并拼接到当前函数中
        node.body.accept(this);

        currentFunc = null;
        currentBlock = null;
        return null;
    }

    @Override
    public IR.Value visit(Param node) {
        return null;
    }

    // ==========================================
    // 表达式
    // ==========================================
    @Override
    public IR.Value visit(BinaryExpr node) {
        IR.Value leftVal = node.left.accept(this);
        IR.Value rightVal = node.right.accept(this);

        IR.TempVar result = newTemp(); // TODO 还有
        IR.OpCode op = switch (node.op) {
            case "+" -> IR.OpCode.ADD;
            case "-" -> IR.OpCode.SUB;
            case "*" -> IR.OpCode.MUL;
            case "/" -> IR.OpCode.DIV;
            default -> IR.OpCode.ADD;
        };

        // 直接追加到 currentBlock 中
        addInstr(new IR.IrInstr(op, leftVal, rightVal, result));
        return result;
    }

    @Override
    public IR.Value visit(NumberExpr node) {
        return new IR.ConstValue(node.value);
    }

    @Override
    public IR.Value visit(IdExpr node) {
        return null;
    }

    @Override
    public IR.Value visit(CallExpr node) {
        return null;
    }

    @Override
    public IR.Value visit(UnaryExpr node) {
        return null;
    }
}
