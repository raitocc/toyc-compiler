package toycc;

import toycc.ToyCParser.*;
import toycc.ToyCBaseVisitor;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the AST from the ANTLR parse tree and performs semantic analysis.
 */
public class SemanticAnalyzer extends ToyCBaseVisitor<Object> {

    private final SymbolTable symtab;
    private AST.Program program;
    private int loopDepth = 0;
    private boolean hasError = false;
    private boolean inFunction = false;

    // Track declared functions for call-before-definition validation
    private final List<String> declaredFunctions = new ArrayList<>();

    public SemanticAnalyzer(SymbolTable symtab) {
        this.symtab = symtab;
    }

    public AST.Program getProgram() {
        return program;
    }

    public boolean hasError() {
        return hasError;
    }

    private void error(int line, String msg) {
        System.err.println("Semantic error at line " + line + ": " + msg);
        hasError = true;
    }

    // ================================================================
    // Compilation Unit
    // ================================================================

    @Override
    public Object visitCompUnit(CompUnitContext ctx) {
        program = new AST.Program();
        program.line = 1;

        // First pass: register all function names for forward references
        for (int i = 0; i < ctx.getChildCount(); i++) {
            if (ctx.getChild(i) instanceof FuncDefContext fctx) {
                String name = fctx.ID().getText();
                if (!declaredFunctions.contains(name)) {
                    declaredFunctions.add(name);
                }
                // Register as a known identifier (for forward references)
                // We'll fully process them during the actual visit
            }
        }

        // Second pass: visit all declarations and function definitions
        for (int i = 0; i < ctx.getChildCount(); i++) {
            if (ctx.getChild(i) instanceof DeclContext
                    || ctx.getChild(i) instanceof FuncDefContext) {
                Object result = visit(ctx.getChild(i));
                if (result instanceof AST.DeclNode) {
                    program.decls.add((AST.DeclNode) result);
                }
            }
        }

        // Verify main function exists
        SymbolTable.Symbol mainSym = symtab.lookup("main");
        if (mainSym == null || mainSym.kind != SymbolTable.Kind.FUNC) {
            error(0, "Program must contain a 'main' function returning int");
        } else if (mainSym.type != AST.Type.INT) {
            error(0, "Function 'main' must return int");
        } else if (mainSym.paramCount != 0) {
            error(0, "Function 'main' must have no parameters");
        }

        return program;
    }

    // ================================================================
    // Declarations
    // ================================================================

    @Override
    public Object visitConstDecl(ConstDeclContext ctx) {
        String name = ctx.ID().getText();
        int line = ctx.getStart().getLine();

        // Check duplicate in current scope
        if (symtab.lookupInCurrentScope(name) != null) {
            error(line, "Redeclaration of '" + name + "'");
        }

        // Evaluate initializer (must be compile-time constant)
        AST.ExprNode init = (AST.ExprNode) visit(ctx.expr());
        int value = evalConstExpr(init, line);

        boolean isGlobal = !inFunction;

        if (isGlobal) {
            symtab.addGlobalConst(name, value);
        } else {
            symtab.addLocalConst(name, value);
        }

        AST.ConstDecl decl = new AST.ConstDecl(name, init, isGlobal);
        decl.line = line;
        decl.value = value;
        return decl;
    }

    @Override
    public Object visitVarDecl(VarDeclContext ctx) {
        String name = ctx.ID().getText();
        int line = ctx.getStart().getLine();

        if (symtab.lookupInCurrentScope(name) != null) {
            error(line, "Redeclaration of '" + name + "'");
        }

        AST.ExprNode init = (AST.ExprNode) visit(ctx.expr());

        boolean isGlobal = !inFunction;

        if (isGlobal) {
            symtab.addGlobalVar(name, init);
        } else {
            symtab.addLocalVar(name);
        }

        AST.VarDecl decl = new AST.VarDecl(name, init, isGlobal);
        decl.line = line;
        return decl;
    }

    // ================================================================
    // Function Definition
    // ================================================================

    @Override
    public Object visitFuncDef(FuncDefContext ctx) {
        String name = ctx.ID().getText();
        int line = ctx.getStart().getLine();

        // Check duplicate function
        if (symtab.lookupInCurrentScope(name) != null) {
            error(line, "Redeclaration of function '" + name + "'");
            return null;
        }

        // Determine return type
        AST.Type retType = AST.Type.INT;
        if (ctx.VOID() != null) {
            retType = AST.Type.VOID;
        }

        // Register function symbol
        SymbolTable.Symbol funcSym = new SymbolTable.Symbol(name, SymbolTable.Kind.FUNC, retType);
        funcSym.line = line;
        symtab.addFunction(funcSym);

        // Push function scope (for params and body)
        symtab.pushScope();
        inFunction = true;
        symtab.setCurrentFunction(funcSym);

        // Process parameters
        List<AST.Param> params = new ArrayList<>();
        if (ctx.param() != null) {
            for (ParamContext pc : ctx.param()) {
                Object p = visit(pc);
                if (p instanceof AST.Param) params.add((AST.Param) p);
            }
        }
        funcSym.paramCount = params.size();
        for (int i = 0; i < params.size(); i++) {
            funcSym.paramTypes.add(AST.Type.INT);
        }

        // Process body
        AST.Block body = (AST.Block) visit(ctx.block());

        // Finalize frame
        symtab.finalizeFunctionFrame(name);

        // Pop function scope
        symtab.popScope();
        inFunction = false;

        // Build FuncDef node
        AST.FuncDef funcDef = new AST.FuncDef(name, retType, body);
        funcDef.line = line;
        funcDef.params = params;

        // Check return paths for int functions
        if (retType == AST.Type.INT) {
            if (!allPathsReturn(body)) {
                error(line, "Function '" + name +
                        "' with int return type must return on all paths");
            }
        }

        return funcDef;
    }

    @Override
    public Object visitParam(ParamContext ctx) {
        String name = ctx.ID().getText();
        symtab.addParam(name);
        return new AST.Param(name);
    }

    // ================================================================
    // Statements
    // ================================================================

    @Override
    public Object visitBlock(BlockContext ctx) {
        symtab.pushScope();
        AST.Block block = new AST.Block();
        block.line = ctx.getStart().getLine();

        for (StmtContext sc : ctx.stmt()) {
            Object s = visit(sc);
            if (s instanceof AST.StmtNode) {
                block.stmts.add((AST.StmtNode) s);
            }
        }

        symtab.popScope();
        return block;
    }

    @Override
    public Object visitEmptyStmt(EmptyStmtContext ctx) {
        AST.EmptyStmt stmt = new AST.EmptyStmt();
        stmt.line = ctx.getStart().getLine();
        return stmt;
    }

    @Override
    public Object visitExprStmt(ExprStmtContext ctx) {
        AST.ExprNode expr = (AST.ExprNode) visit(ctx.expr());
        AST.ExprStmt stmt = new AST.ExprStmt(expr);
        stmt.line = ctx.getStart().getLine();
        return stmt;
    }

    @Override
    public Object visitAssignStmt(AssignStmtContext ctx) {
        String name = ctx.ID().getText();
        int line = ctx.getStart().getLine();
        AST.ExprNode value = (AST.ExprNode) visit(ctx.expr());

        // Check that the variable exists and is mutable
        SymbolTable.Symbol sym = symtab.lookup(name);
        if (sym == null) {
            error(line, "Undefined variable '" + name + "'");
        } else if (sym.kind == SymbolTable.Kind.CONST) {
            error(line, "Cannot assign to constant '" + name + "'");
        } else if (sym.kind == SymbolTable.Kind.FUNC) {
            error(line, "Cannot assign to function '" + name + "'");
        } else if (value.type == AST.Type.VOID) {
            error(line, "Cannot assign void value to '" + name + "'");
        }

        AST.AssignStmt stmt = new AST.AssignStmt(name, value);
        stmt.line = line;
        return stmt;
    }

    @Override
    public Object visitDeclStmt(DeclStmtContext ctx) {
        AST.DeclNode decl = (AST.DeclNode) visit(ctx.decl());
        AST.DeclStmt stmt = new AST.DeclStmt(decl);
        stmt.line = ctx.getStart().getLine();
        return stmt;
    }

    @Override
    public Object visitIfStmt(IfStmtContext ctx) {
        int line = ctx.getStart().getLine();
        AST.ExprNode cond = (AST.ExprNode) visit(ctx.expr());
        AST.StmtNode thenBody = (AST.StmtNode) visit(ctx.stmt(0));
        AST.StmtNode elseBody = null;
        if (ctx.stmt().size() > 1) {
            elseBody = (AST.StmtNode) visit(ctx.stmt(1));
        }

        if (cond.type == AST.Type.VOID) {
            error(line, "Condition of 'if' must be int type");
        }

        AST.IfStmt stmt = new AST.IfStmt(cond, thenBody, elseBody);
        stmt.line = line;
        return stmt;
    }

    @Override
    public Object visitWhileStmt(WhileStmtContext ctx) {
        int line = ctx.getStart().getLine();
        AST.ExprNode cond = (AST.ExprNode) visit(ctx.expr());

        if (cond.type == AST.Type.VOID) {
            error(line, "Condition of 'while' must be int type");
        }

        loopDepth++;
        AST.StmtNode body = (AST.StmtNode) visit(ctx.stmt());
        loopDepth--;

        AST.WhileStmt stmt = new AST.WhileStmt(cond, body);
        stmt.line = line;
        return stmt;
    }

    @Override
    public Object visitBreakStmt(BreakStmtContext ctx) {
        int line = ctx.getStart().getLine();
        if (loopDepth == 0) {
            error(line, "Break statement outside of loop");
        }
        AST.BreakStmt stmt = new AST.BreakStmt();
        stmt.line = line;
        return stmt;
    }

    @Override
    public Object visitContinueStmt(ContinueStmtContext ctx) {
        int line = ctx.getStart().getLine();
        if (loopDepth == 0) {
            error(line, "Continue statement outside of loop");
        }
        AST.ContinueStmt stmt = new AST.ContinueStmt();
        stmt.line = line;
        return stmt;
    }

    @Override
    public Object visitReturnStmt(ReturnStmtContext ctx) {
        int line = ctx.getStart().getLine();
        AST.ExprNode value = null;

        if (ctx.expr() != null) {
            value = (AST.ExprNode) visit(ctx.expr());
        }

        // Check return type compatibility
        SymbolTable.Symbol func = symtab.getCurrentFunction();
        if (func != null) {
            if (func.type == AST.Type.INT && value == null) {
                error(line, "Function returning int must return a value");
            } else if (func.type == AST.Type.VOID && value != null) {
                error(line, "Void function cannot return a value");
            }
        }

        AST.ReturnStmt stmt = new AST.ReturnStmt(value);
        stmt.line = line;
        return stmt;
    }

    // ================================================================
    // Expressions
    // ================================================================

    @Override
    public Object visitLOrBase(LOrBaseContext ctx) {
        return visit(ctx.lAndExpr());
    }

    @Override
    public Object visitLOrBin(LOrBinContext ctx) {
        AST.ExprNode left = (AST.ExprNode) visit(ctx.lOrExpr());
        AST.ExprNode right = (AST.ExprNode) visit(ctx.lAndExpr());
        AST.BinaryExpr expr = new AST.BinaryExpr(left, right, "||");
        expr.line = ctx.getStart().getLine();

        if (left.isConst && right.isConst) {
            expr.isConst = true;
            expr.constVal = ((left.constVal != 0) || (right.constVal != 0)) ? 1 : 0;
        }
        expr.type = AST.Type.INT;
        return expr;
    }

    @Override
    public Object visitLAndBase(LAndBaseContext ctx) {
        return visit(ctx.relExpr());
    }

    @Override
    public Object visitLAndBin(LAndBinContext ctx) {
        AST.ExprNode left = (AST.ExprNode) visit(ctx.lAndExpr());
        AST.ExprNode right = (AST.ExprNode) visit(ctx.relExpr());
        AST.BinaryExpr expr = new AST.BinaryExpr(left, right, "&&");
        expr.line = ctx.getStart().getLine();

        if (left.isConst && right.isConst) {
            expr.isConst = true;
            expr.constVal = ((left.constVal != 0) && (right.constVal != 0)) ? 1 : 0;
        }
        expr.type = AST.Type.INT;
        return expr;
    }

    @Override
    public Object visitRelBase(RelBaseContext ctx) {
        return visit(ctx.addExpr());
    }

    @Override
    public Object visitRelBin(RelBinContext ctx) {
        AST.ExprNode left = (AST.ExprNode) visit(ctx.relExpr());
        AST.ExprNode right = (AST.ExprNode) visit(ctx.addExpr());
        String op = ctx.op.getText();
        AST.BinaryExpr expr = new AST.BinaryExpr(left, right, op);
        expr.line = ctx.getStart().getLine();

        if (left.isConst && right.isConst) {
            expr.isConst = true;
            expr.constVal = evalRelOp(left.constVal, right.constVal, op) ? 1 : 0;
        }
        expr.type = AST.Type.INT;
        return expr;
    }

    @Override
    public Object visitAddBase(AddBaseContext ctx) {
        return visit(ctx.mulExpr());
    }

    @Override
    public Object visitAddBin(AddBinContext ctx) {
        AST.ExprNode left = (AST.ExprNode) visit(ctx.addExpr());
        AST.ExprNode right = (AST.ExprNode) visit(ctx.mulExpr());
        String op = ctx.op.getText();
        AST.BinaryExpr expr = new AST.BinaryExpr(left, right, op);
        expr.line = ctx.getStart().getLine();

        if (left.isConst && right.isConst) {
            expr.isConst = true;
            if (op.equals("+")) {
                expr.constVal = left.constVal + right.constVal;
            } else {
                expr.constVal = left.constVal - right.constVal;
            }
        }
        expr.type = AST.Type.INT;
        return expr;
    }

    @Override
    public Object visitMulBase(MulBaseContext ctx) {
        return visit(ctx.unaryExpr());
    }

    @Override
    public Object visitMulBin(MulBinContext ctx) {
        AST.ExprNode left = (AST.ExprNode) visit(ctx.mulExpr());
        AST.ExprNode right = (AST.ExprNode) visit(ctx.unaryExpr());
        String op = ctx.op.getText();
        AST.BinaryExpr expr = new AST.BinaryExpr(left, right, op);
        expr.line = ctx.getStart().getLine();

        if (left.isConst && right.isConst) {
            expr.isConst = true;
            switch (op) {
                case "*":
                    expr.constVal = left.constVal * right.constVal;
                    break;
                case "/":
                    if (right.constVal == 0) {
                        error(expr.line, "Division by zero");
                        expr.constVal = 0;
                    } else {
                        expr.constVal = left.constVal / right.constVal;
                    }
                    break;
                case "%":
                    if (right.constVal == 0) {
                        error(expr.line, "Modulo by zero");
                        expr.constVal = 0;
                    } else {
                        expr.constVal = left.constVal % right.constVal;
                    }
                    break;
            }
        }
        expr.type = AST.Type.INT;
        return expr;
    }

    @Override
    public Object visitUnaryBase(UnaryBaseContext ctx) {
        return visit(ctx.primaryExpr());
    }

    @Override
    public Object visitUnaryOp(UnaryOpContext ctx) {
        AST.ExprNode operand = (AST.ExprNode) visit(ctx.unaryExpr());
        String op = ctx.op.getText();
        AST.UnaryExpr expr = new AST.UnaryExpr(operand, op);
        expr.line = ctx.getStart().getLine();

        if (operand.isConst) {
            expr.isConst = true;
            switch (op) {
                case "+":
                    expr.constVal = operand.constVal;
                    break;
                case "-":
                    expr.constVal = -operand.constVal;
                    break;
                case "!":
                    expr.constVal = (operand.constVal == 0) ? 1 : 0;
                    break;
            }
        }
        expr.type = AST.Type.INT;
        return expr;
    }

    @Override
    public Object visitNumberExpr(NumberExprContext ctx) {
        int value = Integer.parseInt(ctx.NUMBER().getText());
        AST.NumberExpr expr = new AST.NumberExpr(value);
        expr.line = ctx.getStart().getLine();
        return expr;
    }

    @Override
    public Object visitIdExpr(IdExprContext ctx) {
        String name = ctx.ID().getText();
        int line = ctx.getStart().getLine();

        SymbolTable.Symbol sym = symtab.lookup(name);
        if (sym == null) {
            error(line, "Undefined identifier '" + name + "'");
            AST.IdExpr expr = new AST.IdExpr(name);
            expr.line = line;
            return expr;
        }

        if (sym.kind == SymbolTable.Kind.FUNC) {
            error(line, "Function '" + name + "' used as value");
        }

        AST.IdExpr expr = new AST.IdExpr(name);
        expr.line = line;

        if (sym.kind == SymbolTable.Kind.CONST) {
            expr.isConst = true;
            expr.constVal = sym.constValue;
        }
        expr.type = sym.type;
        return expr;
    }

    @Override
    public Object visitParenExpr(ParenExprContext ctx) {
        return visit(ctx.expr());
    }

    @Override
    public Object visitCallExpr(CallExprContext ctx) {
        String name = ctx.ID().getText();
        int line = ctx.getStart().getLine();

        // Check if function is declared (forward reference support)
        SymbolTable.Symbol sym = symtab.lookup(name);
        if (sym == null || sym.kind != SymbolTable.Kind.FUNC) {
            // Check if it's been forward-declared but not yet visited
            if (declaredFunctions.contains(name)) {
                // OK, function is declared later
            } else if (sym != null && sym.kind != SymbolTable.Kind.FUNC) {
                error(line, "'" + name + "' is not a function");
            } else {
                error(line, "Undefined function '" + name + "'");
            }
        }

        AST.CallExpr call = new AST.CallExpr(name);
        call.line = line;

        if (ctx.expr() != null) {
            for (ExprContext ec : ctx.expr()) {
                AST.ExprNode arg = (AST.ExprNode) visit(ec);
                call.args.add(arg);
            }
        }

        // Check argument count
        if (sym != null && sym.kind == SymbolTable.Kind.FUNC) {
            if (call.args.size() != sym.paramCount) {
                error(line, "Function '" + name + "' expects " + sym.paramCount +
                        " arguments, got " + call.args.size());
            }
        }

        // Return type
        if (sym != null) {
            call.type = sym.type;
        } else {
            call.type = AST.Type.INT;
        }

        return call;
    }

    // ================================================================
    // Helpers
    // ================================================================

    /**
     * Evaluate a constant expression at compile time.
     */
    private int evalConstExpr(AST.ExprNode expr, int line) {
        if (expr.isConst) {
            return expr.constVal;
        } else {
            error(line, "Constant expression must be evaluable at compile time");
            return 0;
        }
    }

    private boolean evalRelOp(int left, int right, String op) {
        return switch (op) {
            case "<" -> left < right;
            case ">" -> left > right;
            case "<=" -> left <= right;
            case ">=" -> left >= right;
            case "==" -> left == right;
            case "!=" -> left != right;
            default -> false;
        };
    }

    /**
     * Check if all execution paths in this statement end with a return.
     */
    private boolean allPathsReturn(AST.StmtNode stmt) {
        if (stmt instanceof AST.ReturnStmt) {
            return true;
        } else if (stmt instanceof AST.Block block) {
            for (AST.StmtNode s : block.stmts) {
                if (allPathsReturn(s)) {
                    return true;
                }
            }
            return false;
        } else if (stmt instanceof AST.IfStmt ifStmt) {
            if (ifStmt.elseBody != null) {
                return allPathsReturn(ifStmt.thenBody) && allPathsReturn(ifStmt.elseBody);
            }
            return false;
        } else if (stmt instanceof AST.WhileStmt) {
            return false;
        } else {
            return false;
        }
    }
}
