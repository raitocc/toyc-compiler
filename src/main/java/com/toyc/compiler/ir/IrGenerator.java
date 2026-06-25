package com.toyc.compiler.ir;

import com.toyc.compiler.ast.AST;
import com.toyc.compiler.ast.AST.*;

/**
 * 遍历 AST 树生成 基于基本块的 IR 代码
 */
public class IrGenerator implements AST.Visitor<IR.Value> {

    public IR.Program program = new IR.Program();
    private IR.FuncDef currentFunc = null;

    // 维护一个“当前正在写指令”的基本块
    private IR.BasicBlock currentBlock = null;

    // IR 环境映射！用 Symbol 对象本身查它的 IR Value，同名屏蔽
    private final java.util.Map<com.toyc.compiler.semantic.SymbolTable.Symbol, IR.Value> env = new java.util.HashMap<>();

    // 循环控制流栈，用于为内层的 break 和 continue 指明跳转的 BasicBlock 目标
    private final java.util.Stack<IR.BasicBlock> breakStack = new java.util.Stack<>();
    private final java.util.Stack<IR.BasicBlock> continueStack = new java.util.Stack<>();

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
        // 对于常量，我们不需要生成指令。
        // 直接从 SemanticAnalyzer 存在 Symbol 里的常量值包装成 IR.ConstValue，塞进映射表中即可。
        IR.ConstValue constVal = new IR.ConstValue(node.resolvedSymbol.constValue);
        env.put(node.resolvedSymbol, constVal);
        return null;
    }

    @Override
    public IR.Value visit(VarDecl node) {
        if (currentFunc == null) {
            // 这是全局变量
            IR.NameValue globalName = new IR.NameValue(node.name);
            env.put(node.resolvedSymbol, globalName);
            int val = node.resolvedSymbol.initValue;
            program.globalVars.add(new IR.GlobalVar(node.name, val));
            return null;
        } else {
            // 这是局部变量
            // 1. 给它分配一个专属的临时变量
            IR.TempVar t = newTemp();
            // 2. 绑定到我们映射环境表中
            env.put(node.resolvedSymbol, t);
            // 3. 递归遍历右边的初始化表达式，拿到计算后的结果存放地（可能是临时变量也可能是常数）
            IR.Value initVal = node.initExpr.accept(this);
            // 4. 生成一条赋值指令： t = initVal，并追加到当前基本块里
            addInstr(new IR.IrInstr(IR.OpCode.ASSIGN, initVal, t));
            return null; // 语句（Stmt）和声明都不需要向上方抛出任何值，返回 null 即可
        }
    }

    @Override
    public IR.Value visit(BlockStmt node) {
        for (Stmt stmt : node.stmts) {
            stmt.accept(this);
        }
        return null;
    }

    @Override
    public IR.Value visit(EmptyStmt node) {
        return null;
    }

    @Override
    public IR.Value visit(ExprStmt node) {
        node.expr.accept(this);
        return null;
    }

    @Override
    public IR.Value visit(AssignStmt node) {
        // 获取左边的变量（可能是全局的 NameValue，也可能是局部的 TempVar）
        IR.Value left = env.get(node.resolvedSymbol);
        //递归算出等号右边的结果
        IR.Value right = node.expr.accept(this);
        addInstr(new IR.IrInstr(IR.OpCode.ASSIGN, right, left));
        return null;
    }

    @Override
    public IR.Value visit(IfStmt node) {
        // 1. 算出条件表达式的结果
        IR.Value condVal = node.cond.accept(this);

        // 2. 准备好所需的积木块（基本块）
        IR.BasicBlock thenBlock = newBlock("if_then");
        IR.BasicBlock endBlock = newBlock("if_end");
        IR.LabelValue endLabel = new IR.LabelValue(endBlock.name);

        if (node.elseStmt != null) {
            // ====== 带有 else 分支 ======
            IR.BasicBlock elseBlock = newBlock("if_else");
            IR.LabelValue elseLabel = new IR.LabelValue(elseBlock.name);

            // 在当前块的末尾追加条件跳转：如果为假（0），跳去 else
            addInstr(new IR.IrInstr(IR.OpCode.BEQZ, condVal, elseLabel));

            // 拼接并填充 thenBlock
            appendBlock(thenBlock);
            node.thenStmt.accept(this);
            addInstr(new IR.IrInstr(IR.OpCode.JMP, endLabel)); // then 跑完跳出去

            // 拼接并填充 elseBlock
            appendBlock(elseBlock);
            node.elseStmt.accept(this);
            addInstr(new IR.IrInstr(IR.OpCode.JMP, endLabel)); // else 跑完跳出去
        } else {
            // ====== 没有 else 分支 ======
            // 如果为假（0），直接跳到 end 出去
            addInstr(new IR.IrInstr(IR.OpCode.BEQZ, condVal, endLabel));

            // 拼接并填充 thenBlock
            appendBlock(thenBlock);
            node.thenStmt.accept(this);
            addInstr(new IR.IrInstr(IR.OpCode.JMP, endLabel)); // then 跑完跳出去
        }

        // 3. 收尾：把 endBlock 拼接到最后，作为以后代码继续往下写的落脚点
        appendBlock(endBlock);

        return null;
    }

    @Override
    public IR.Value visit(WhileStmt node) {
        // 1. 准备三个基本块
        IR.BasicBlock condBlock = newBlock("while_cond");
        IR.BasicBlock bodyBlock = newBlock("while_body");
        IR.BasicBlock endBlock = newBlock("while_end");

        // 2. 从当前块进入 condBlock (直接滑落拼接即可)
        appendBlock(condBlock);

        // 3. 在 condBlock 里生成判断条件的指令
        IR.Value condVal = node.cond.accept(this);
        // 如果为假(0)，直接跳出循环去 endBlock
        addInstr(new IR.IrInstr(IR.OpCode.BEQZ, condVal, new IR.LabelValue(endBlock.name)));

        // 4. 压栈给里面的 break 和 continue 指明方向
        continueStack.push(condBlock); // continue 应该跳回条件判断块
        breakStack.push(endBlock);     // break 应该跳到循环结束块

        // 5. 拼接循环体，并遍历里面的语句
        appendBlock(bodyBlock);
        node.body.accept(this);

        // 6. 循环体执行完后，必须无条件跳回 condBlock 进行下一次判断
        addInstr(new IR.IrInstr(IR.OpCode.JMP, new IR.LabelValue(condBlock.name)));

        // 7. 退出循环体了，完成使命，弹栈
        continueStack.pop();
        breakStack.pop();

        // 8. 收尾：拼接 endBlock，承接后面的代码
        appendBlock(endBlock);

        return null;
    }

    @Override
    public IR.Value visit(BreakStmt node) {
        // 语义分析阶段已经查过 break 必须在循环内，这里栈肯定不为空
        IR.BasicBlock target = breakStack.peek();
        addInstr(new IR.IrInstr(IR.OpCode.JMP, new IR.LabelValue(target.name)));
        return null;
    }

    @Override
    public IR.Value visit(ContinueStmt node) {
        IR.BasicBlock target = continueStack.peek();
        addInstr(new IR.IrInstr(IR.OpCode.JMP, new IR.LabelValue(target.name)));
        return null;
    }

    @Override
    public IR.Value visit(ReturnStmt node) {
        IR.Value value = null;
        if (node.expr != null) {
            value = node.expr.accept(this);
        }
        addInstr(new IR.IrInstr(IR.OpCode.RET, value, null));
        return null;
    }

    @Override
    public IR.Value visit(FuncDef node) {
        currentFunc = new IR.FuncDef(node.name);
        program.functions.add(currentFunc);

        // 为函数创建一个入口基本块，并将其设置为当前块
        IR.BasicBlock entryBlock = newBlock("entry");
        appendBlock(entryBlock);

        for (Param param : node.params) {
            IR.TempVar t = newTemp();
            env.put(param.resolvedSymbol, t);
            // 顺手记录一下：当前函数的参数由哪些虚拟变量来接管
            currentFunc.params.add(t.toPrintString());
        }

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
    public IR.Value visit(CallExpr node) {
        for (Expr arg : node.args) {
            IR.Value value = arg.accept(this);
            addInstr(new IR.IrInstr(IR.OpCode.PARAM, value, null));
        }
        IR.TempVar result = newTemp();
        addInstr(new IR.IrInstr(IR.OpCode.CALL, new IR.NameValue(node.funcName), result));
        return result;
    }

    @Override
    public IR.Value visit(BinaryExpr node) {
        // ========== 逻辑与 (&&) 的短路求值 ==========
        if (node.op.equals("&&")) {
            IR.TempVar result = newTemp();
            IR.BasicBlock rightBlock = newBlock("and_right");
            IR.BasicBlock endBlock = newBlock("and_end");

            // 1. 先默认结果为 0 (假)
            addInstr(new IR.IrInstr(IR.OpCode.ASSIGN, new IR.ConstValue(0), result));

            // 2. 只计算左边
            IR.Value leftVal = node.left.accept(this);
            // 【短路发生点】：如果左边为假(0)，直接跳到 end，右边绝对不执行！
            addInstr(new IR.IrInstr(IR.OpCode.BEQZ, leftVal, new IR.LabelValue(endBlock.name)));

            // 3. 左边为真，才进入右边块继续判断
            appendBlock(rightBlock);
            IR.Value rightVal = node.right.accept(this);
            // 如果右边也为假(0)，跳到 end (此时 result 是 0)
            addInstr(new IR.IrInstr(IR.OpCode.BEQZ, rightVal, new IR.LabelValue(endBlock.name)));

            // 4. 两边都挺过了没跳走，说明全真，把结果设为 1
            addInstr(new IR.IrInstr(IR.OpCode.ASSIGN, new IR.ConstValue(1), result));
            addInstr(new IR.IrInstr(IR.OpCode.JMP, new IR.LabelValue(endBlock.name)));

            // 5. 结束块
            appendBlock(endBlock);
            return result;
        }

        // ========== 逻辑或 (||) 的短路求值 ==========
        if (node.op.equals("||")) {
            IR.TempVar result = newTemp();
            IR.BasicBlock rightBlock = newBlock("or_right");
            IR.BasicBlock endBlock = newBlock("or_end");

            // 1. 先默认结果为 1 (真)
            addInstr(new IR.IrInstr(IR.OpCode.ASSIGN, new IR.ConstValue(1), result));

            // 2. 只计算左边
            IR.Value leftVal = node.left.accept(this);
            // 【短路发生点】：如果左边为真(非0)，直接跳到 end，右边绝对不执行！
            addInstr(new IR.IrInstr(IR.OpCode.BNEZ, leftVal, new IR.LabelValue(endBlock.name)));

            // 3. 左边为假，只能硬着头皮看右边脸色
            appendBlock(rightBlock);
            IR.Value rightVal = node.right.accept(this);
            // 如果右边也为真(非0)，跳到 end (此时 result 已经是 1)
            addInstr(new IR.IrInstr(IR.OpCode.BNEZ, rightVal, new IR.LabelValue(endBlock.name)));

            // 4. 两边都是假，把结果设为 0
            addInstr(new IR.IrInstr(IR.OpCode.ASSIGN, new IR.ConstValue(0), result));
            addInstr(new IR.IrInstr(IR.OpCode.JMP, new IR.LabelValue(endBlock.name)));

            // 5. 结束块
            appendBlock(endBlock);
            return result;
        }

        // ========== 其他普通二元运算（贪婪求值，左右都算） ==========
        IR.Value leftVal = node.left.accept(this);
        IR.Value rightVal = node.right.accept(this);

        IR.TempVar result = newTemp();
        IR.OpCode op = switch (node.op) {
            case "+" -> IR.OpCode.ADD;
            case "-" -> IR.OpCode.SUB;
            case "*" -> IR.OpCode.MUL;
            case "/" -> IR.OpCode.DIV;
            case "%" -> IR.OpCode.MOD;
            case "==" -> IR.OpCode.SEQ;
            case "!=" -> IR.OpCode.SNE;
            case "<" -> IR.OpCode.SLT;
            case ">" -> IR.OpCode.SGT;
            case "<=" -> IR.OpCode.SLE;
            case ">=" -> IR.OpCode.SGE;
            default -> throw new RuntimeException("Unknown binary operator: " + node.op);
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
        // 遇到标识符，直接通过它绑定的独一无二的 Symbol 去映射表里查，一秒搞定！
        return env.get(node.resolvedSymbol);
    }

    @Override
    public IR.Value visit(UnaryExpr node) {
        IR.Value val = node.expr.accept(this);
        
        // 正号不需要任何指令，原封不动返回原来的值即可
        if (node.op.equals("+")) {
            return val;
        }

        IR.TempVar result = newTemp();
        IR.OpCode op = switch (node.op) {
            case "-" -> IR.OpCode.NEG;
            case "!" -> IR.OpCode.NOT;
            default -> throw new RuntimeException("Unknown unary operator: " + node.op);
        };

        addInstr(new IR.IrInstr(op, val, result));
        return result;
    }
}
